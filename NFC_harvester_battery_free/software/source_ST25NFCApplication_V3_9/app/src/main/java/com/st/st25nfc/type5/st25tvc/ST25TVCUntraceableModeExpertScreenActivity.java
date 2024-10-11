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

import android.os.AsyncTask;
import android.os.Bundle;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.AppCompatCheckBox;
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
import com.st.st25sdk.UntraceableModeInterface.UntraceableModeDefaultSettings;
import com.st.st25sdk.type5.st25tvc.ST25TVCTag;

import static com.st.st25nfc.type5.st25tvc.ST25TVCUntraceableModeExpertScreenActivity.Action.ENTER_CONFIGURATION_PWD;
import static com.st.st25nfc.type5.st25tvc.ST25TVCUntraceableModeExpertScreenActivity.ActionStatus.ACTION_FAILED;
import static com.st.st25nfc.type5.st25tvc.ST25TVCUntraceableModeExpertScreenActivity.ActionStatus.ACTION_SUCCESSFUL;
import static com.st.st25nfc.type5.st25tvc.ST25TVCUntraceableModeExpertScreenActivity.ActionStatus.TAG_NOT_IN_THE_FIELD;
import static com.st.st25sdk.UntraceableModeInterface.UntraceableModeDefaultSettings.PRIVACY_BY_DEFAULT_DISABLED;
import static com.st.st25sdk.UntraceableModeInterface.UntraceableModeDefaultSettings.PRIVACY_BY_DEFAULT_ENABLED;
import static com.st.st25sdk.UntraceableModeInterface.UntraceableModeDefaultSettings.PRIVACY_BY_DEFAULT_ENABLED_WHEN_TAMPER_DETECT_CLOSED;
import static com.st.st25sdk.UntraceableModeInterface.UntraceableModeDefaultSettings.PRIVACY_BY_DEFAULT_ENABLED_WHEN_TAMPER_DETECT_OPEN;
import static com.st.st25sdk.UntraceableModeInterface.UntraceableModePolicy.RESPONSIVE_MODE;
import static com.st.st25sdk.UntraceableModeInterface.UntraceableModePolicy.SILENT_MODE;
import static com.st.st25sdk.UntraceableModeInterface.UntraceableModePolicy;

public class ST25TVCUntraceableModeExpertScreenActivity extends STFragmentActivity implements STType5PwdDialogFragment.STType5PwdDialogListener, NavigationView.OnNavigationItemSelectedListener {

    enum Action {
        READ_CURRENT_CONFIGURATION,
        WRITE_NEW_CONFIGURATION,
        TOGGLE_UNTRACEABLE_MODE,

        ENTER_CONFIGURATION_PWD,
        ENTER_CURRENT_UNTRACEABLE_MODE_PWD,
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
    FragmentManager mFragmentManager;
    private Action mCurrentAction;
    int configurationPasswordNumber;
    int untraceableModePasswordNumber;
    private ST25TVCTag mST25TVCTag;
    private UntraceableModePolicy mUntraceableModePolicy;
    private UntraceableModeDefaultSettings mUntraceableModeDefaultSettings;
    private boolean mIsPrivCfgLocked;

    RadioButton uuidDisabledRadioButton;
    RadioButton uuidEnabledRadioButton;

    RadioButton disabledEveryTimesRadioButton;
    RadioButton enabledEveryTimesRadioButton;
    RadioButton enabledWhenTDOpenedRadioButton;
    RadioButton enabledWhenTDClosedRadioButton;

    AppCompatCheckBox lockCheckbox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.default_layout);

        // Inflate content of FrameLayout
        FrameLayout frameLayout=(FrameLayout) findViewById(R.id.frame_content);
        View childView = getLayoutInflater().inflate(R.layout.st25tvc_untraceable_mode_expert_screen, null);
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

        uuidDisabledRadioButton = findViewById(R.id.uuidDisabledRadioButton);
        uuidEnabledRadioButton = findViewById(R.id.uuidEnabledRadioButton);

        disabledEveryTimesRadioButton = findViewById(R.id.disabledEveryTimesRadioButton);
        enabledEveryTimesRadioButton = findViewById(R.id.enabledEveryTimesRadioButton);
        enabledWhenTDOpenedRadioButton = findViewById(R.id.enabledWhenTDOpenedRadioButton);
        enabledWhenTDClosedRadioButton = findViewById(R.id.enabledWhenTDClosedRadioButton);

        lockCheckbox = findViewById(R.id.lockCheckbox);

        Button updateTagButton = (Button) findViewById(R.id.updateTagButton);
        updateTagButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateTagConfiguration();
            }
        });

        Button sendToggleUntraceableCmdButton = (Button) findViewById(R.id.sendToggleUntraceableCmdButton);
        sendToggleUntraceableCmdButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleUntraceableMode();
            }
        });

        new myAsyncTask(Action.READ_CURRENT_CONFIGURATION).execute();
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

    public void onSTType5PwdDialogFinish(int result) {
        Log.v(TAG, "onSTType5PwdDialogFinish. result = " + result);

        if (result == PwdDialogFragment.RESULT_OK) {
            switch(mCurrentAction) {
                case TOGGLE_UNTRACEABLE_MODE:
                    showToast(R.string.tag_updated);
                    break;
                case ENTER_CONFIGURATION_PWD:
                    // We can write the tag again
                    updateTagConfiguration();
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

    private void updateTagConfiguration() {
        new myAsyncTask(Action.WRITE_NEW_CONFIGURATION).execute();
    }

    private void toggleUntraceableMode() {
        mCurrentAction = Action.TOGGLE_UNTRACEABLE_MODE;
        STType5PwdDialogFragment pwdDialogFragment = STType5PwdDialogFragment.newInstance(
                STType5PwdDialogFragment.STPwdAction.START_UNTRACEABLE_MODE,
                ST25TVCTag.ST25TVC_UNTRACEABLE_MODE_PASSWORD_ID,
                getResources().getString(R.string.please_enter_untraceable_mode_pwd));
        if(pwdDialogFragment!=null) pwdDialogFragment.show(mFragmentManager, "pwdDialogFragment");
    }

    private void displayConfigurationPasswordDialogBox() {
        mCurrentAction = ENTER_CONFIGURATION_PWD;
        STType5PwdDialogFragment pwdDialogFragment = STType5PwdDialogFragment.newInstance(
                STType5PwdDialogFragment.STPwdAction.PRESENT_CURRENT_PWD,
                ST25TVCTag.ST25TVC_CONFIGURATION_PASSWORD_ID,
                getResources().getString(R.string.enter_configuration_pwd));
        if(pwdDialogFragment!=null) pwdDialogFragment.show(mFragmentManager, "pwdDialogFragment");
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
                switch (mAction) {
                    case READ_CURRENT_CONFIGURATION:
                        mUntraceableModePolicy = mST25TVCTag.getUntraceableModePolicy();
                        mUntraceableModeDefaultSettings = mST25TVCTag.getUntraceableModeDefaultSettings();
                        mIsPrivCfgLocked = mST25TVCTag.isPrivConfigurationLocked();
                        result = ACTION_SUCCESSFUL;
                        break;
                    case WRITE_NEW_CONFIGURATION:
                        UntraceableModePolicy mode = uuidDisabledRadioButton.isChecked() ? SILENT_MODE : RESPONSIVE_MODE;
                        mST25TVCTag.setUntraceableModePolicy(mode);

                        UntraceableModeDefaultSettings untraceableModeDefaultSettings;
                        if(disabledEveryTimesRadioButton.isChecked()) {
                            untraceableModeDefaultSettings = PRIVACY_BY_DEFAULT_DISABLED;
                        } else if(enabledEveryTimesRadioButton.isChecked()) {
                            untraceableModeDefaultSettings = PRIVACY_BY_DEFAULT_ENABLED;
                        } else if(enabledWhenTDOpenedRadioButton.isChecked()) {
                            untraceableModeDefaultSettings = PRIVACY_BY_DEFAULT_ENABLED_WHEN_TAMPER_DETECT_OPEN;
                        } else {
                            untraceableModeDefaultSettings = PRIVACY_BY_DEFAULT_ENABLED_WHEN_TAMPER_DETECT_CLOSED;
                        }
                        mST25TVCTag.setUntraceableModeDefaultSettings(untraceableModeDefaultSettings);

                        if (lockCheckbox.isChecked()) {
                            mST25TVCTag.lockPrivConfiguration();
                        }
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
            }

            return result;
        }

        @Override
        protected void onPostExecute(ActionStatus actionStatus) {

            switch(actionStatus) {
                case ACTION_SUCCESSFUL:
                    switch (mAction) {
                        case READ_CURRENT_CONFIGURATION:
                            updateUI();
                             break;
                        case WRITE_NEW_CONFIGURATION:
                            showToast(R.string.tag_updated);
                            break;
                    }
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

    /**
     * Update UI Widgets according to the values of mUntraceableModePolicy, mUntraceableModeDefaultSettings and mIsPrivCfgLocked
     */
    private void updateUI() {
        if (mUntraceableModePolicy == SILENT_MODE) {
            uuidDisabledRadioButton.setChecked(true);
        } else {
            uuidEnabledRadioButton.setChecked(true);
        }

        switch(mUntraceableModeDefaultSettings) {
            case PRIVACY_BY_DEFAULT_DISABLED:
                disabledEveryTimesRadioButton.setChecked(true);
                break;
            case PRIVACY_BY_DEFAULT_ENABLED:
                enabledEveryTimesRadioButton.setChecked(true);
                break;
            case PRIVACY_BY_DEFAULT_ENABLED_WHEN_TAMPER_DETECT_OPEN:
                enabledWhenTDOpenedRadioButton.setChecked(true);
                break;
            case PRIVACY_BY_DEFAULT_ENABLED_WHEN_TAMPER_DETECT_CLOSED:
                enabledWhenTDClosedRadioButton.setChecked(true);
                break;
        }

        lockCheckbox.setChecked(mIsPrivCfgLocked);
    }
}
