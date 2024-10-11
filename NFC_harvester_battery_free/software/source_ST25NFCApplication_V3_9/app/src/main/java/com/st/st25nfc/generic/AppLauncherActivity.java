package com.st.st25nfc.generic;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import com.google.android.material.navigation.NavigationView;
import com.st.st25nfc.R;
import com.st.st25nfc.generic.util.DisplayTapTagRequest;
import com.st.st25nfc.generic.util.UIHelper;
import com.st.st25nfc.type5.st25tvc.ST25TVCUniqueTapCodeActivity;
import com.st.st25sdk.NFCTag;
import com.st.st25sdk.STException;
import com.st.st25sdk.ndef.AarRecord;
import com.st.st25sdk.ndef.ExternalRecord;
import com.st.st25sdk.ndef.MimeRecord;
import com.st.st25sdk.ndef.NDEFMsg;
import com.st.st25sdk.ndef.NDEFRecord;
import com.st.st25sdk.ndef.UriRecord;

import static com.st.st25nfc.generic.AppLauncherActivity.ActionStatus.ACTION_FAILED;
import static com.st.st25nfc.generic.AppLauncherActivity.ActionStatus.ACTION_SUCCESSFUL;
import static com.st.st25nfc.generic.AppLauncherActivity.ActionStatus.TAG_NOT_IN_THE_FIELD;
import static com.st.st25sdk.ndef.MimeRecord.NdefMimeIdCode.NDEF_MIME_APPLICATION_XML;

public class AppLauncherActivity extends STFragmentActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    enum Action {
        WRITE_TAG
    };

    enum ActionStatus {
        ACTION_SUCCESSFUL,
        ACTION_FAILED,
        TAG_NOT_IN_THE_FIELD,
        AREA_PASSWORD_NEEDED
    };
    
    private NFCTag mNfcTag;
    private Action mCurrentAction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.default_layout);

        // Inflate content of FrameLayout
        FrameLayout frameLayout = (FrameLayout) findViewById(R.id.frame_content);
        View childView = getLayoutInflater().inflate(R.layout.activity_app_launcher, null);
        frameLayout.addView(childView);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");

        mNfcTag = MainActivity.getTag();

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.navigation_view);
        navigationView.setNavigationItemSelectedListener(this);
        mMenu.inflateMenu(navigationView);

        Button writeTagButton = findViewById(R.id.writeTagButton);
        writeTagButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                executeAsynchronousAction(Action.WRITE_TAG);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_empty, menu);
        return true;
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        return mMenu.selectItem(this, item);
    }

    private void executeAsynchronousAction(Action action) {
        Log.d(TAG, "Starting background action " + action);
        mCurrentAction = action;
        new myAsyncTask(action).execute();
    }


    private class myAsyncTask extends AsyncTask<Void, Void, ActionStatus> {
        Action mAction;

        public myAsyncTask(Action action) {
            mAction = action;
        }

        @Override
        protected ActionStatus doInBackground(Void... param) {
            ActionStatus result;

            try {
                UIHelper.displayCircularProgressBar(AppLauncherActivity.this, getString(R.string.please_wait));

                switch (mAction) {
                    case WRITE_TAG:
                        NDEFMsg ndefMsg = new NDEFMsg();

                        // Record to install or launch the iOS version of ST25 NFC Tap application
                        UriRecord uriRecord = new UriRecord(UriRecord.NdefUriIdCode.NDEF_RTD_URI_ID_HTTPS_WWW, "myst25.com");
                        ndefMsg.addRecord(uriRecord);

                        // Record to install or launch the Android version of ST25 NFC Tap application
                        AarRecord aarRecord = new AarRecord("com.st.st25nfc");
                        ndefMsg.addRecord(aarRecord);

                        // Dummy External Record used to show that we can also store some application data
                        String dummyContent = "Example of record containing application data";
                        ExternalRecord externalRecord = new ExternalRecord();
                        externalRecord.setExternalDomain("my_organization");
                        externalRecord.setExternalType("my_type_name");
                        externalRecord.setContent(dummyContent.getBytes());
                        ndefMsg.addRecord(externalRecord);

                        mNfcTag.writeNdefMessage(ndefMsg);
                        result = ACTION_SUCCESSFUL;
                        break;

                    default:
                        result = ACTION_FAILED;
                        break;
                }

            } catch (STException e) {
                switch (e.getError()) {
                    case TAG_NOT_IN_THE_FIELD:
                        result = TAG_NOT_IN_THE_FIELD;
                        break;

                    default:
                        e.printStackTrace();
                        result = ACTION_FAILED;
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
                result = ACTION_FAILED;
            }

            return result;
        }

        @Override
        protected void onPostExecute(ActionStatus actionStatus) {

            UIHelper.dismissCircularProgressBar();

            switch(actionStatus) {
                case ACTION_SUCCESSFUL:
                    switch (mAction) {
                        case WRITE_TAG:
                            UIHelper.displayMessage(AppLauncherActivity.this, R.string.app_launcher_tag_updated);
                            break;
                    }
                    break;

                case AREA_PASSWORD_NEEDED:
                    //displayAreaPasswordDialogBox();
                    break;

                case ACTION_FAILED:
                    showToast(R.string.command_failed);
                    break;

                case TAG_NOT_IN_THE_FIELD:
                    showToast(R.string.tag_not_in_the_field);
                    break;
            }

            return;
        }
    }
    
}