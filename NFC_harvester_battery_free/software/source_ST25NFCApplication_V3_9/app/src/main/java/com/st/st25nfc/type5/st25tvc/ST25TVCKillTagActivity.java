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

package com.st.st25nfc.type5.st25tvc;

import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager.widget.ViewPager;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.android.material.navigation.NavigationView;
import com.st.st25nfc.R;
import com.st.st25nfc.generic.MainActivity;
import com.st.st25nfc.generic.PwdDialogFragment;
import com.st.st25nfc.generic.STFragmentActivity;
import com.st.st25nfc.generic.STType5PwdDialogFragment;
import com.st.st25nfc.generic.util.UIHelper;
import com.st.st25sdk.NFCTag;
import com.st.st25sdk.STException;
import com.st.st25sdk.command.Iso15693CustomKillCommandInterface;
import com.st.st25sdk.type5.st25tvc.ST25TVCTag;

import static com.st.st25nfc.type5.st25tvc.ST25TVCKillTagActivity.Action.*;
import static com.st.st25nfc.type5.st25tvc.ST25TVCKillTagActivity.ActionStatus.*;
import static com.st.st25nfc.type5.st25tvc.ST25TVCKillTagActivity.Action.ENTER_CURRENT_KILL_PWD;
import static com.st.st25nfc.type5.st25tvc.ST25TVCKillTagActivity.Action.ENTER_NEW_KILL_PWD;
import static com.st.st25nfc.type5.st25tvc.ST25TVCKillTagActivity.Action.KILL_TAG;


public class ST25TVCKillTagActivity extends STFragmentActivity implements STType5PwdDialogFragment.STType5PwdDialogListener, NavigationView.OnNavigationItemSelectedListener {

    enum Action {
        KILL_TAG,
        ENTER_CURRENT_KILL_PWD,
        ENTER_NEW_KILL_PWD,
        READ_KILL_FEATURE_ENABLED,
        WRITE_KILL_FEATURE_ENABLED
    };

    enum ActionStatus {
        ACTION_SUCCESSFUL,
        ACTION_FAILED,
        TAG_NOT_IN_THE_FIELD,
        CONFIG_PASSWORD_NEEDED
    };

    // Set here the Toolbar to use for this activity
    private int toolbar_res = R.menu.toolbar_empty;

    static final String TAG = "KillActivity";
    private Handler mHandler;
    FragmentManager mFragmentManager;
    private Action mCurrentAction;
    private ST25TVCTag mST25TVCTag;
    private CheckBox mEnableKillFeatureCheckbox;
    private boolean mIsKillFeatureEnabled;

    private Iso15693CustomKillCommandInterface mKillCommandInterface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.default_layout);

        // Inflate content of FrameLayout
        FrameLayout frameLayout = findViewById(R.id.frame_content);
        View childView = getLayoutInflater().inflate(R.layout.fragment_st25tvc_kill_tag, null);
        frameLayout.addView(childView);

        TextView warningTextView = findViewById(R.id.warningTextView);

        NFCTag nfcTag = MainActivity.getTag();
        if (nfcTag instanceof ST25TVCTag) {
            mST25TVCTag = (ST25TVCTag) nfcTag;
        } else {
            showToast(R.string.invalid_tag);
            goBackToMainActivity();
            return;
        }

        mHandler = new Handler();
        mFragmentManager = getSupportFragmentManager();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.navigation_view);
        navigationView.setNavigationItemSelectedListener(this);
        mMenu.inflateMenu(navigationView);

        Button killButton = (Button) findViewById(R.id.killButton);
        killButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                killTag();
            }
        });

        mEnableKillFeatureCheckbox = findViewById(R.id.enableKillFeatureCheckbox);

        Button changeKillPasswordButton = (Button) findViewById(R.id.changeKillPasswordButton);
        changeKillPasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                presentCurrentKillPassword();
            }
        });

        Button lockKillPasswordButton = (Button) findViewById(R.id.lockKillPasswordButton);
        lockKillPasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                askConfirmation();
            }
        });

        // This Activity will fail if the tag doesn't implement a Iso15693CustomKillCommandInterface
        try {
            mKillCommandInterface = (Iso15693CustomKillCommandInterface) MainActivity.getTag();
        } catch (ClassCastException e) {
            Log.e(TAG, "Error! Tag not implementing Iso15693CustomKillCommandInterface!");
            return;
        }

        executeAsynchronousAction(READ_KILL_FEATURE_ENABLED);
    }

    private void killTag() {
        mCurrentAction = KILL_TAG;
        STType5PwdDialogFragment pwdDialogFragment = STType5PwdDialogFragment.newInstance(
                STType5PwdDialogFragment.STPwdAction.KILL_TAG,
                mST25TVCTag.ST25TVC_KILL_PASSWORD_ID,
                getResources().getString(R.string.kill_warning));
        if(pwdDialogFragment!=null) pwdDialogFragment.show(mFragmentManager, "pwdDialogFragment");
    }

    private void enterNewKillPassword() {
        new Thread(new Runnable() {
            public void run() {
                Log.v(TAG, "enterNewKillPassword");

                mCurrentAction = ENTER_NEW_KILL_PWD;
                int passwordNumber = mST25TVCTag.ST25TVC_KILL_PASSWORD_ID;
                String message = getResources().getString(R.string.enter_new_kill_pwd);
                STType5PwdDialogFragment.STPwdAction action = STType5PwdDialogFragment.STPwdAction.ENTER_NEW_KILL_PWD;

                STType5PwdDialogFragment pwdDialogFragment = STType5PwdDialogFragment.newInstance(action, passwordNumber, message);
                if(pwdDialogFragment!=null) pwdDialogFragment.show(mFragmentManager, "pwdDialogFragment");
            }
        }).start();
    }

    private void askConfirmation() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        // set title
        alertDialogBuilder.setTitle(getString(R.string.confirmation_needed));

        // set dialog message
        alertDialogBuilder
                .setMessage(getString(R.string.lock_kill_pwd_question))
                .setCancelable(true)

                .setPositiveButton(getString(R.string.lock_password),new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        dialog.cancel();
                        lockKillPassword();
                    }
                })
                .setNegativeButton(getString(R.string.cancel),new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        dialog.cancel();
                    }
                });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
    }

    private void lockKillPassword() {
        new Thread(new Runnable() {
            public void run() {
                Log.v(TAG, "lockKillPassword");

                try {
                    mKillCommandInterface.lockKill();
                    showToast(R.string.command_successful);
                } catch (STException e) {
                    e.printStackTrace();
                    showToast(R.string.command_failed);
                }

            }
        }).start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds read_list_items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.toolbar_save, menu);
        MenuItem item = menu.findItem(R.id.action_save);

        item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_save:
                        executeAsynchronousAction(WRITE_KILL_FEATURE_ENABLED);
                        break;
                }
                return false;
            }

        });
        super.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long

        // as you specify a parent activity in AndroidManifest.xml.


        return super.onOptionsItemSelected(item);
    }

    private void presentCurrentKillPassword() {
        Log.v(TAG, "presentCurrentKillPassword");

        // Warning: Function called from background thread! Post a request to the UI thread
        mHandler.post(new Runnable() {
            public void run() {
                mCurrentAction = ENTER_CURRENT_KILL_PWD;
                STType5PwdDialogFragment pwdDialogFragment = STType5PwdDialogFragment.newInstance(
                        STType5PwdDialogFragment.STPwdAction.PRESENT_CURRENT_PWD,
                        mST25TVCTag.ST25TVC_KILL_PASSWORD_ID,
                        getResources().getString(R.string.enter_kill_pwd));
                if(pwdDialogFragment!=null) pwdDialogFragment.show(mFragmentManager, "pwdDialogFragment");
            }
        });
    }

    public void onSTType5PwdDialogFinish(int result) {
        Log.v(TAG, "onSTType5PwdDialogFinish. result = " + result);

        switch(mCurrentAction) {
            case ENTER_CURRENT_KILL_PWD:
                if (result == PwdDialogFragment.RESULT_OK) {
                    enterNewKillPassword();
                }
                break;

            default:
                break;
        }

    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        return mMenu.selectItem(this, item);
    }

    private void updateUI() {
        mEnableKillFeatureCheckbox.setChecked(mIsKillFeatureEnabled);
    }

    private void displayConfigurationPasswordDialogBox() {

        STType5PwdDialogFragment pwdDialogFragment = STType5PwdDialogFragment.newInstance(
                STType5PwdDialogFragment.STPwdAction.PRESENT_CURRENT_PWD,
                mST25TVCTag.ST25TVC_CONFIGURATION_PASSWORD_ID,
                getResources().getString(R.string.enter_configuration_pwd),
                new STType5PwdDialogFragment.STType5PwdDialogListener() {
                    @Override
                    public void onSTType5PwdDialogFinish(int result) {
                        if (result == PwdDialogFragment.RESULT_OK) {
                            // Restart the last action
                            executeAsynchronousAction(mCurrentAction);
                        } else {
                            Log.e(TAG, "Action failed! Tag not updated!");
                        }
                    }
                }
        );

        if(pwdDialogFragment!=null) pwdDialogFragment.show(mFragmentManager, "pwdDialogFragment");
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
                UIHelper.displayCircularProgressBar(ST25TVCKillTagActivity.this, getString(R.string.please_wait));

                switch (mAction) {
                    case READ_KILL_FEATURE_ENABLED:
                        mIsKillFeatureEnabled = mST25TVCTag.isKillFeatureEnabled();
                        result = ActionStatus.ACTION_SUCCESSFUL;
                        break;

                    case WRITE_KILL_FEATURE_ENABLED:
                        boolean killFeaturedEnabled = mEnableKillFeatureCheckbox.isChecked();
                        mST25TVCTag.enableKillFeature(killFeaturedEnabled);
                        mIsKillFeatureEnabled = killFeaturedEnabled;
                        result = ACTION_SUCCESSFUL;
                        break;

                    default:
                        result = ACTION_FAILED;
                        break;
                }

            } catch (STException e) {
                switch (e.getError()) {
                    case CONFIG_PASSWORD_NEEDED:
                        result = ActionStatus.CONFIG_PASSWORD_NEEDED;
                        break;

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
                        case READ_KILL_FEATURE_ENABLED:
                            updateUI();
                            break;
                        case WRITE_KILL_FEATURE_ENABLED:
                            showToast(R.string.tag_updated);
                            updateUI();
                            break;
                    }
                    break;

                case CONFIG_PASSWORD_NEEDED:
                    displayConfigurationPasswordDialogBox();
                    break;

                case ACTION_FAILED:
                    showToast(R.string.command_failed);
                    break;

                case TAG_NOT_IN_THE_FIELD:
                    updateUI();
                    break;
            }

            return;
        }
    }

}
