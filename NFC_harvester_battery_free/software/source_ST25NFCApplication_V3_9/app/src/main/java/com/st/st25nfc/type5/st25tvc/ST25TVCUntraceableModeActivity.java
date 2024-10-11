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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.ActionBarDrawerToggle;
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
import android.widget.FrameLayout;
import android.widget.RadioButton;

import com.google.android.material.navigation.NavigationView;
import com.st.st25nfc.R;
import com.st.st25nfc.generic.MainActivity;
import com.st.st25nfc.generic.PwdDialogFragment;
import com.st.st25nfc.generic.STFragmentActivity;
import com.st.st25nfc.generic.STType5PwdDialogFragment;
import com.st.st25sdk.NFCTag;
import com.st.st25sdk.STException;
import com.st.st25sdk.type5.st25tvc.ST25TVCTag;
import com.st.st25sdk.UntraceableModeInterface.UntraceableModeDefaultSettings;

import static com.st.st25nfc.type5.st25tvc.ST25TVCUntraceableModeActivity.Action.ENTER_NEW_UNTRACEABLE_MODE_PWD;
import static com.st.st25nfc.type5.st25tvc.ST25TVCUntraceableModeActivity.Action.ENTER_CURRENT_UNTRACEABLE_MODE_PWD;
import static com.st.st25nfc.type5.st25tvc.ST25TVCUntraceableModeActivity.Action.CHECK_UNTRACEABLE_MODE_PWD;
import static com.st.st25nfc.type5.st25tvc.ST25TVCUntraceableModeActivity.Action.CHECK_CONFIGURATION_PWD;

import static com.st.st25nfc.type5.st25tvc.ST25TVCUntraceableModeActivity.ActionStatus.ACTION_FAILED;
import static com.st.st25nfc.type5.st25tvc.ST25TVCUntraceableModeActivity.ActionStatus.ACTION_SUCCESSFUL;
import static com.st.st25nfc.type5.st25tvc.ST25TVCUntraceableModeActivity.ActionStatus.TAG_NOT_IN_THE_FIELD;

import static com.st.st25sdk.UntraceableModeInterface.UntraceableModeDefaultSettings.PRIVACY_BY_DEFAULT_ENABLED;
import static com.st.st25sdk.UntraceableModeInterface.UntraceableModeDefaultSettings.PRIVACY_BY_DEFAULT_ENABLED_WHEN_TAMPER_DETECT_CLOSED;
import static com.st.st25sdk.UntraceableModeInterface.UntraceableModeDefaultSettings.PRIVACY_BY_DEFAULT_ENABLED_WHEN_TAMPER_DETECT_OPEN;
import static com.st.st25sdk.UntraceableModeInterface.UntraceableModePolicy.RESPONSIVE_MODE;
import static com.st.st25sdk.UntraceableModeInterface.UntraceableModePolicy.SILENT_MODE;

public class ST25TVCUntraceableModeActivity extends STFragmentActivity implements STType5PwdDialogFragment.STType5PwdDialogListener, NavigationView.OnNavigationItemSelectedListener {

    enum Action {
        ENABLE_UNTRACEABLE_MODE,
        ENTER_CURRENT_UNTRACEABLE_MODE_PWD,     // Used when changing the Untraceable Mode password
        ENTER_NEW_UNTRACEABLE_MODE_PWD,
        CHECK_UNTRACEABLE_MODE_PWD,             // Used by the configuration wizard when enabling the Untraceable Mode feature
        CHECK_CONFIGURATION_PWD
    };

    enum ActionStatus {
        ACTION_SUCCESSFUL,
        ACTION_FAILED,
        TAG_NOT_IN_THE_FIELD,
        CONFIG_PASSWORD_NEEDED
    };

    // Set here the Toolbar to use for this activity
    private int toolbar_res = R.menu.toolbar_empty;

    static final String TAG = "UntraceableMode";
    private Handler mHandler;
    FragmentManager mFragmentManager;
    private Action mCurrentAction;
    int configurationPasswordNumber;
    int untraceableModePasswordNumber;
    private boolean mUseSilentMode;
    private UntraceableModeDefaultSettings mUntraceableModeDefaultSettings;
    private boolean mPrivCfgShouldBeLocked;
    private ST25TVCTag mST25TVCTag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.default_layout);

        // Inflate content of FrameLayout
        FrameLayout frameLayout=(FrameLayout) findViewById(R.id.frame_content);
        View childView = getLayoutInflater().inflate(R.layout.fragment_st25tvc_untraceable_mode, null);
        frameLayout.addView(childView);

        NFCTag nfcTag = MainActivity.getTag();
        if (nfcTag instanceof ST25TVCTag) {
            mST25TVCTag = (ST25TVCTag) nfcTag;
            configurationPasswordNumber = ST25TVCTag.ST25TVC_CONFIGURATION_PASSWORD_ID;
            untraceableModePasswordNumber = ST25TVCTag.ST25TVC_UNTRACEABLE_MODE_PASSWORD_ID;

        } else {
            showToast(R.string.invalid_tag);
            goBackToMainActivity();
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

        Button untraceableButton = (Button) findViewById(R.id.untraceableButton);
        untraceableButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayUntraceableModeSelectionAlert();
            }
        });

        Button expertModeButton = (Button) findViewById(R.id.expertModeButton);
        expertModeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ST25TVCUntraceableModeActivity.this,  ST25TVCUntraceableModeExpertScreenActivity.class);
                startActivityForResult(intent, 1);
            }
        });

        Button changeUntraceablePasswordButton = (Button) findViewById(R.id.changeUntraceablePasswordButton);
        changeUntraceablePasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                presentCurrentUntraceableModePassword();
            }
        });

    }

    private void enableUntraceableMode() {
        new myAsyncTask(Action.ENABLE_UNTRACEABLE_MODE).execute();
    }

    private void enterNewUntraceableModePassword() {
        new Thread(new Runnable() {
            public void run() {
                Log.v(TAG, "enterNewUntraceableModePassword");

                mCurrentAction = ENTER_NEW_UNTRACEABLE_MODE_PWD;
                int passwordNumber = untraceableModePasswordNumber;
                String message = getResources().getString(R.string.enter_new_untraceable_mode_pwd);
                STType5PwdDialogFragment.STPwdAction action = STType5PwdDialogFragment.STPwdAction.ENTER_NEW_PWD;

                STType5PwdDialogFragment pwdDialogFragment = STType5PwdDialogFragment.newInstance(action, passwordNumber, message);
                if(pwdDialogFragment!=null) pwdDialogFragment.show(mFragmentManager, "pwdDialogFragment");
            }
        }).start();
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

    private void presentCurrentUntraceableModePassword() {
        Log.v(TAG, "presentCurrentUntraceableModePassword");

        // Warning: Function called from background thread! Post a request to the UI thread
        mHandler.post(new Runnable() {
            public void run() {
                mCurrentAction = ENTER_CURRENT_UNTRACEABLE_MODE_PWD;
                STType5PwdDialogFragment pwdDialogFragment = STType5PwdDialogFragment.newInstance(
                        STType5PwdDialogFragment.STPwdAction.PRESENT_CURRENT_PWD,
                        untraceableModePasswordNumber,
                        getResources().getString(R.string.enter_current_untraceable_mode_pwd));
                if(pwdDialogFragment!=null) pwdDialogFragment.show(mFragmentManager, "pwdDialogFragment");
            }
        });
    }

    public void onSTType5PwdDialogFinish(int result) {
        Log.v(TAG, "onSTType5PwdDialogFinish. result = " + result);

        if (result == PwdDialogFragment.RESULT_OK) {
            switch(mCurrentAction) {
                case ENTER_CURRENT_UNTRACEABLE_MODE_PWD:
                    enterNewUntraceableModePassword();
                    break;
                case ENTER_NEW_UNTRACEABLE_MODE_PWD:
                    showToast(R.string.change_pwd_succeeded);
                    break;
                case CHECK_UNTRACEABLE_MODE_PWD:
                    askUserConfirmation();
                    break;
                case CHECK_CONFIGURATION_PWD:
                    enableUntraceableMode();
                    break;
            }
        } else {
            Log.e(TAG, "Action failed! Tag not updated!");
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        return mMenu.selectItem(this, item);
    }



    private void displayUntraceableModeSelectionAlert() {
        AlertDialog.Builder builder = new AlertDialog.Builder(ST25TVCUntraceableModeActivity.this);
        final AlertDialog dialog = builder.create();

        // set title
        //alertDialog.setTitle(getString(R.string.XXX));

        dialog.setCancelable(false);

        // inflate XML content
        View dialogView = getLayoutInflater().inflate(R.layout.fragment_st25tvc_untraceable_mode_selection, null);
        dialog.setView(dialogView);

        final RadioButton silentModeRadioButton = dialogView.findViewById(R.id.silentModeRadioButton);

        Button cancelButton = dialogView.findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.cancel();
            }
        });

        Button continueButton = dialogView.findViewById(R.id.continueButton);
        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mUseSilentMode = silentModeRadioButton.isChecked();
                dialog.cancel();
                displayUntraceableModeDefaultBehaviorAlert();
            }
        });

        // show the dialog box
        dialog.show();
    }

    private void displayUntraceableModeDefaultBehaviorAlert() {
        AlertDialog.Builder builder = new AlertDialog.Builder(ST25TVCUntraceableModeActivity.this);
        final AlertDialog dialog = builder.create();

        // set title
        //alertDialog.setTitle(getString(R.string.XXX));

        dialog.setCancelable(false);

        // inflate XML content
        View dialogView = getLayoutInflater().inflate(R.layout.fragment_st25tvc_untraceable_mode_default_behavior, null);
        dialog.setView(dialogView);

        final RadioButton enabledEveryTimesRadioButton = dialogView.findViewById(R.id.enabledEveryTimesRadioButton);
        final RadioButton enabledWhenTDOpenedRadioButton = dialogView.findViewById(R.id.enabledWhenTDOpenedRadioButton);

        Button cancelButton = dialogView.findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.cancel();
            }
        });

        Button continueButton = dialogView.findViewById(R.id.continueButton);
        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (enabledEveryTimesRadioButton.isChecked()) {
                    mUntraceableModeDefaultSettings = PRIVACY_BY_DEFAULT_ENABLED;
                } else {
                    if (enabledWhenTDOpenedRadioButton.isChecked()) {
                        mUntraceableModeDefaultSettings = PRIVACY_BY_DEFAULT_ENABLED_WHEN_TAMPER_DETECT_OPEN;
                    } else {
                        mUntraceableModeDefaultSettings = PRIVACY_BY_DEFAULT_ENABLED_WHEN_TAMPER_DETECT_CLOSED;
                    }
                }
                dialog.cancel();
                displayUntraceableModeLockAlert();
            }
        });

        // show the dialog box
        dialog.show();
    }


    private void displayUntraceableModeLockAlert() {
        AlertDialog.Builder builder = new AlertDialog.Builder(ST25TVCUntraceableModeActivity.this);
        final AlertDialog dialog = builder.create();

        // set title
        //alertDialog.setTitle(getString(R.string.XXX));

        dialog.setCancelable(false);

        // inflate XML content
        View dialogView = getLayoutInflater().inflate(R.layout.fragment_st25tvc_untraceable_mode_lock, null);
        dialog.setView(dialogView);

        final RadioButton lockRadioButton = dialogView.findViewById(R.id.lockRadioButton);

        Button cancelButton = dialogView.findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.cancel();
            }
        });

        Button continueButton = dialogView.findViewById(R.id.continueButton);
        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPrivCfgShouldBeLocked = lockRadioButton.isChecked();
                dialog.cancel();
                displayUntraceableModePasswordDialogBox();
            }
        });

        // show the dialog box
        dialog.show();
    }

    private void displayUntraceableModePasswordDialogBox() {
        Log.v(TAG, "displayUntraceableModePasswordDialogBox");

        // Warning: Function called from background thread! Post a request to the UI thread
        runOnUiThread(new Runnable() {
            public void run() {
                mCurrentAction = CHECK_UNTRACEABLE_MODE_PWD;
                STType5PwdDialogFragment pwdDialogFragment = STType5PwdDialogFragment.newInstance(
                        STType5PwdDialogFragment.STPwdAction.PRESENT_CURRENT_PWD,
                        ST25TVCTag.ST25TVC_UNTRACEABLE_MODE_PASSWORD_ID,
                        getResources().getString(R.string.please_enter_untraceable_mode_pwd_for_verification));
                if(pwdDialogFragment!=null) pwdDialogFragment.show(mFragmentManager, "pwdDialogFragment");
            }
        });
    }

    /**
     * Ask user confirmation before enabling the Untraceable Mode
     */
    private void askUserConfirmation() {
        String summary = "\n\nSummary of the choices made:\n" +
                        (mUseSilentMode ? "- Use Silent mode" : "- Use Responsive mode");

        summary += "\n- ";
        switch(mUntraceableModeDefaultSettings) {
            case PRIVACY_BY_DEFAULT_DISABLED:
                // Should not happen!
                break;
            case PRIVACY_BY_DEFAULT_ENABLED:
                summary += getResources().getString(R.string.untraceable_mode_enabled_every_times);
                break;
            case PRIVACY_BY_DEFAULT_ENABLED_WHEN_TAMPER_DETECT_OPEN:
                summary += getResources().getString(R.string.untraceable_mode_enabled_when_tamper_detect_open);
                break;
            case PRIVACY_BY_DEFAULT_ENABLED_WHEN_TAMPER_DETECT_CLOSED:
                summary += getResources().getString(R.string.untraceable_mode_enabled_when_tamper_detect_short);
                break;
        }

        summary += "\n- ";
        if(mPrivCfgShouldBeLocked) {
            summary += getResources().getString(R.string.lock_priv_cfg);
        } else {
            summary += getResources().getString(R.string.keep_priv_cfg_unlocked);
        }

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        // set title
        alertDialogBuilder.setTitle(getString(R.string.confirmation_needed));

        // set dialog message
        alertDialogBuilder
                .setMessage(getResources().getString(R.string.untraceable_mode_final_confirmation) + summary)
                .setCancelable(true)
                .setPositiveButton(getString(R.string.activate),new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        dialog.cancel();
                        enableUntraceableMode();
                    }
                })
                .setNegativeButton(getString(R.string.cancel),new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        dialog.cancel();
                    }
                });

        // create alert dialog
        AlertDialog dialog = alertDialogBuilder.create();

        // show it
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(R.color.st_light_blue));
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.st_light_blue));
    }

    private void displayConfigurationPasswordDialogBox() {
        Log.v(TAG, "displayConfigurationPasswordDialogBox");

        // Warning: Function called from background thread! Post a request to the UI thread
        runOnUiThread(new Runnable() {
            public void run() {
                mCurrentAction = CHECK_CONFIGURATION_PWD;
                STType5PwdDialogFragment pwdDialogFragment = STType5PwdDialogFragment.newInstance(
                        STType5PwdDialogFragment.STPwdAction.PRESENT_CURRENT_PWD,
                        ST25TVCTag.ST25TVC_CONFIGURATION_PASSWORD_ID,
                        getResources().getString(R.string.enter_configuration_pwd));
                if(pwdDialogFragment!=null) pwdDialogFragment.show(mFragmentManager, "pwdDialogFragment");
            }
        });
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
                if (mUseSilentMode) {
                    mST25TVCTag.setUntraceableModePolicy(SILENT_MODE);
                } else {
                    mST25TVCTag.setUntraceableModePolicy(RESPONSIVE_MODE);
                }

                mST25TVCTag.setUntraceableModeDefaultSettings(mUntraceableModeDefaultSettings);

                if(mPrivCfgShouldBeLocked) {
                    mST25TVCTag.lockPrivConfiguration();
                }

                result = ACTION_SUCCESSFUL;

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
            }

            return result;
        }

        @Override
        protected void onPostExecute(ActionStatus actionStatus) {

            switch(actionStatus) {
                case ACTION_SUCCESSFUL:
                    showToast(R.string.tag_updated);
                    break;

                case CONFIG_PASSWORD_NEEDED:
                    displayConfigurationPasswordDialogBox();
                    break;

                case ACTION_FAILED:
                    showToast(R.string.error_while_updating_the_tag);
                    break;

                case TAG_NOT_IN_THE_FIELD:
                    showToast(R.string.tag_not_in_the_field);
                    break;
            }

            return;
        }
    }

}
