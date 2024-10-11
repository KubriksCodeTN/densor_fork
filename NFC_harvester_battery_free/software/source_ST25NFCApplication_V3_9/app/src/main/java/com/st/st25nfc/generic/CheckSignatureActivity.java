/*
  * @author STMicroelectronics MMY Application team
  *
  ******************************************************************************
  * @attention
  *
  * <h2><center>&copy; COPYRIGHT 2017 STMicroelectronics</center></h2>
  *
  * Licensed under ST MIX_MYLIBERTY SOFTWARE LICENSE AGREEMENT (the "License");
  * You may not use this file except in compliance with the License.
  * You may obtain a copy of the License at:
  *
  *        http://www.st.com/Mix_MyLiberty
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied,
  * AND SPECIFICALLY DISCLAIMING THE IMPLIED WARRANTIES OF MERCHANTABILITY,
  * FITNESS FOR A PARTICULAR PURPOSE, AND NON-INFRINGEMENT.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  *
  ******************************************************************************
*/

package com.st.st25nfc.generic;

import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import com.google.android.material.navigation.NavigationView;
import androidx.fragment.app.FragmentManager;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.st.st25nfc.R;
import com.st.st25nfc.generic.util.UIHelper;
import com.st.st25sdk.About;
import com.st.st25sdk.Helper;
import com.st.st25sdk.NFCTag;
import com.st.st25sdk.STException;
import com.st.st25sdk.SignatureInterface;
import com.st.st25sdk.type2.st25tn.ST25TNTag;

import java.util.Set;

import static com.st.st25sdk.type2.st25tn.ST25TNTag.ST25TNMemoryConfiguration.*;
import static com.st.st25sdk.type2.st25tn.ST25TNTag.ST25TNLocks.*;
import static com.st.st25nfc.generic.CheckSignatureActivity.ActionStatus.*;

public class CheckSignatureActivity extends STFragmentActivity implements NavigationView.OnNavigationItemSelectedListener {

    enum Action {
        READ_SIGNATURE_LOCK,
        LOCK_SIGNATURE
    };

    enum ActionStatus {
        ACTION_SUCCESSFUL,
        ACTION_FAILED,
        ACTION_CANCELED,
        TAG_NOT_IN_THE_FIELD,
        CONFIG_PASSWORD_NEEDED
    };

    // Set here the Menu and the Toolbar to use for this activity
    private int toolbar_res = R.menu.toolbar_empty;

    static final String TAG = "CheckSign";
    private Handler mHandler;
    private SignatureInterface mSignatureInterface;
    FragmentManager mFragmentManager;

    private NFCTag mNFCTag;
    private ST25TNTag mST25TNTag;

    private TextView mKeyIdTextView;
    private TextView mWarningTextView;
    private TextView mSignatureStatusTextView;
    private TextView mTagSignatureTextView;
    private CheckBox mLockSignatureCheckbox;

    private boolean mIsSignatureLocked;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.default_layout);

        // Inflate content of FrameLayout
        FrameLayout frameLayout=(FrameLayout) findViewById(R.id.frame_content);
        View childView = getLayoutInflater().inflate(R.layout.fragment_check_signature, null);
        frameLayout.addView(childView);

        if (super.getTag() == null) {
            showToast(R.string.invalid_tag);
            goBackToMainActivity();
            return;
        }

        mNFCTag = MainActivity.getTag();
        try {
            mSignatureInterface = (SignatureInterface) mNFCTag;
        } catch (ClassCastException e) {
            showToast(R.string.tag_not_implementing_signature);
            return;
        }

        mHandler = new Handler();
        mFragmentManager = getSupportFragmentManager();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.navigation_view);
        navigationView.setNavigationItemSelectedListener(this);
        mMenu.inflateMenu(navigationView);

        Button checkSignatureButton = (Button) findViewById(R.id.checkSignatureButton);
        checkSignatureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkSignature();
            }
        });

        mKeyIdTextView = (TextView) findViewById(R.id.keyIdTextView);
        mSignatureStatusTextView = (TextView) findViewById(R.id.signatureStatusTextView);
        mTagSignatureTextView = (TextView) findViewById(R.id.tagSignatureTextView);
        mLockSignatureCheckbox = findViewById(R.id.lockSignatureCheckbox);

        mWarningTextView = (TextView) findViewById(R.id.warningTextView);
        mWarningTextView.setVisibility(View.GONE);

        // Add a safety check specific to ST25TN
        if (mNFCTag instanceof ST25TNTag) {
            mST25TNTag = (ST25TNTag) mNFCTag;

            mLockSignatureCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                                                                  @Override
                                                                  public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                                                      if (mLockSignatureCheckbox.isEnabled()) {
                                                                          if (isChecked) {
                                                                              askLockConfirmation();
                                                                          }
                                                                      }
                                                                  }
                                                              }
            );

            executeAsynchronousAction(Action.READ_SIGNATURE_LOCK);

        } else {
            mLockSignatureCheckbox.setVisibility(View.GONE);
        }
    }

    private void askLockConfirmation() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        // set title
        alertDialogBuilder.setTitle(getString(R.string.confirmation_needed));

        // set dialog message
        alertDialogBuilder
                .setMessage(getString(R.string.lock_signature_question))
                .setCancelable(true)

                .setPositiveButton(getString(R.string.lock_signature),new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        dialog.cancel();
                        executeAsynchronousAction(Action.LOCK_SIGNATURE);
                    }
                })
                .setNegativeButton(getString(R.string.cancel),new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        dialog.cancel();
                        mLockSignatureCheckbox.setChecked(false);
                    }
                });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();

        alertDialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(R.color.st_light_blue));
        alertDialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.st_light_blue));

    }

    private void checkSignature() {

        if (!isSdkWithSignature()) {
            showToast(R.string.feature_under_nda);
            return;
        }

        new Thread(new Runnable() {
            public void run() {
                try {
                    final byte keyId = mSignatureInterface.getKeyIdNDA();

                    runOnUiThread(new Runnable() {
                        public void run() {
                            mTagSignatureTextView.setText("");
                            mSignatureStatusTextView.setText("");

                            int key = (keyId & 0xFF);
                            String keyIdStr = String.format("%02d", key);
                            mKeyIdTextView.setText(keyIdStr);

                            if (key == 0) {
                                mWarningTextView.setVisibility(View.VISIBLE);
                            } else {
                                mWarningTextView.setVisibility(View.GONE);
                            }
                        }
                    });

                    boolean isSignatureValid = mSignatureInterface.isSignatureOkNDA();

                    Log.w(TAG, "Decoded certificate: " + mSignatureInterface.getDecodedCertificateNDA());

                    displaySignatureStatus(isSignatureValid);

                    // Read tag' signature and display it
                    byte[] signature = mSignatureInterface.readSignatureNDA();
                    displaySignature(signature);

                } catch (STException e) {
                    switch (e.getError()) {
                        case TAG_NOT_IN_THE_FIELD:
                            showToast(R.string.tag_not_in_the_field);
                            break;
                        case IMPLEMENTED_IN_NDA_VERSION:
                            showToast(R.string.feature_under_nda);
                            break;
                        case MISSING_LIBRARY:
                            showToast(R.string.spongy_castle_crypto_libraries_are_missing);
                            break;
                        default:
                            e.printStackTrace();
                            showToast(R.string.error_while_checking_the_signature);
                            // Reset the satus field
                            displaySignatureStatus(false);
                    }
                }
            }
        }).start();
    }


    private boolean isSdkWithSignature() {
        Set<String> sdkFeatureList = About.getExtraFeatureList();

        for (String featureString: sdkFeatureList) {
            if (featureString.equals("signature")) {
                return true;
            }
        }

        return false;
    }

    private void displaySignatureStatus(final boolean isSignatureValid) {
        // Warning: Function called from background thread! Post a request to the UI thread
        mHandler.post(new Runnable() {
            public void run() {
                if(isSignatureValid) {
                    mSignatureStatusTextView.setTextColor(getResources().getColor(R.color.st_light_green));
                    mSignatureStatusTextView.setText(R.string.signature_ok);
                } else {
                    mSignatureStatusTextView.setTextColor(getResources().getColor(R.color.st_light_purple));
                    mSignatureStatusTextView.setText(R.string.signature_nok);

                    if (mST25TNTag != null) {
                        ST25TNTag.ST25TNMemoryConfiguration memConf = mST25TNTag.getMemoryConfiguration();
                        if ((memConf == EXTENDED_TLVS_AREA_192_BYTES) ||
                            (memConf == EXTENDED_TLVS_AREA_208_BYTES)) {
                            UIHelper.displayMessage(CheckSignatureActivity.this, R.string.warning_extended_memory_mode);
                        }
                    }
                }
            }
        });
    }

    private void displaySignature(final byte[] signature) {
        // Warning: Function called from background thread! Post a request to the UI thread
        mHandler.post(new Runnable() {
            public void run() {
                mTagSignatureTextView.setText(Helper.convertHexByteArrayToString(signature));
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds read_list_items to the action bar if it is present.
        getMenuInflater().inflate(toolbar_res, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long

        // as you specify a parent activity in AndroidManifest.xml.


        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        return mMenu.selectItem(this, item);
    }


    private void executeAsynchronousAction(Action action) {
        Log.d(TAG, "Starting background action " + action);
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
                UIHelper.displayCircularProgressBar(CheckSignatureActivity.this, getString(R.string.please_wait));

                switch (mAction) {
                    case READ_SIGNATURE_LOCK:
                        boolean isPart1Locked = mST25TNTag.isLocked(BLOCKS_34H_TO_35H);
                        boolean isPart2Locked = mST25TNTag.isLocked(BLOCKS_36H_TO_37H);
                        boolean isPart3Locked = mST25TNTag.isLocked(BLOCKS_38H_TO_39H);
                        boolean isPart4Locked = mST25TNTag.isLocked(BLOCKS_3AH_TO_3BH);
                        mIsSignatureLocked = isPart1Locked | isPart2Locked | isPart3Locked | isPart4Locked;
                        result = ACTION_SUCCESSFUL;
                        break;

                    case LOCK_SIGNATURE:
                        mST25TNTag.lock(BLOCKS_34H_TO_35H);
                        mST25TNTag.lock(BLOCKS_36H_TO_37H);
                        mST25TNTag.lock(BLOCKS_38H_TO_39H);
                        mST25TNTag.lock(BLOCKS_3AH_TO_3BH);
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
                        case READ_SIGNATURE_LOCK:
                            if (mIsSignatureLocked) {
                                mLockSignatureCheckbox.setEnabled(false);
                                mLockSignatureCheckbox.setChecked(true);
                            }
                            break;
                        case LOCK_SIGNATURE:
                            mLockSignatureCheckbox.setEnabled(false);
                            showToast(R.string.tag_updated);
                            break;
                    }
                    break;

                case ACTION_CANCELED:
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
