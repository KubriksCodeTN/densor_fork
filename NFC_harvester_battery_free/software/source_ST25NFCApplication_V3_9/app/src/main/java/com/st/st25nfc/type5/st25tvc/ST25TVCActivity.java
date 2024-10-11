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

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.navigation.NavigationView;
import com.st.st25nfc.R;
import com.st.st25nfc.generic.MainActivity;
import com.st.st25nfc.generic.PwdDialogFragment;
import com.st.st25nfc.generic.STFragmentActivity;
import com.st.st25nfc.generic.STType5PwdDialogFragment;
import com.st.st25nfc.generic.SlidingTabLayout;
import com.st.st25nfc.generic.STFragment;
import com.st.st25nfc.generic.STPagerAdapter;
import com.st.st25nfc.generic.util.UIHelper;
import com.st.st25nfc.generic.util.UIHelper.STFragmentId;
import com.st.st25sdk.STException;
import com.st.st25sdk.UntraceableModeInterface;
import com.st.st25sdk.ndef.NDEFMsg;
import com.st.st25sdk.ndef.UriRecord;
import com.st.st25sdk.type5.st25tvc.ST25TVCTag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.st.st25nfc.type5.st25tvc.ST25TVCActivity.ActionStatus.*;
import static com.st.st25nfc.type5.st25tvc.ST25TVCActivity.Action.*;
import static com.st.st25sdk.UntraceableModeInterface.UntraceableModeDefaultSettings.PRIVACY_BY_DEFAULT_DISABLED;

public class ST25TVCActivity extends STFragmentActivity
        implements NavigationView.OnNavigationItemSelectedListener, STFragment.STFragmentListener, STType5PwdDialogFragment.STType5PwdDialogListener {

    enum Action {
        READ_CURRENT_UNTRACEABLE_MODE_CONFIGURATION,
        DISABLE_UNTRACEABLE_MODE,
        PERFORM_SAFETY_CHECKS,
        DISABLE_ANDEF,
        FIX_CCFILE
    };

    enum ActionStatus {
        ACTION_SUCCESSFUL,
        ACTION_FAILED,
        TAG_NOT_IN_THE_FIELD,
        CONFIG_PASSWORD_NEEDED,
        AREA1_PASSWORD_NEEDED,

        INVALID_CCFILE,
        SUSPICIOUS_ANDEF_CONFIGURATION
    };

    // Set here the Toolbar to use for this activity
    private int toolbar_res = R.menu.toolbar_empty;

    final static String TAG = "ST25TVCActivity";
    public ST25TVCTag mST25TVCTag;

    FragmentManager mFragmentManager;
    STPagerAdapter mPagerAdapter;
    ViewPager mViewPager;

    private SlidingTabLayout mSlidingTabLayout;
    private SharedPreferences mSharedPreferences;
    private Action mCurrentAction;

    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.pager_layout);

        if (super.getTag() instanceof ST25TVCTag) {
            mST25TVCTag = (ST25TVCTag) super.getTag();
        }
        if (mST25TVCTag == null) {
            showToast(R.string.invalid_tag);
            goBackToMainActivity();
            return;
        }

        mFragmentManager = getSupportFragmentManager();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(mST25TVCTag.getName());

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.navigation_view);
        navigationView.setNavigationItemSelectedListener(this);
        mMenu.inflateMenu(navigationView);

        List<STFragmentId> fragmentList = new ArrayList<STFragmentId>();

        fragmentList.add(UIHelper.STFragmentId.TAG_INFO_FRAGMENT_ID);
        fragmentList.add(UIHelper.STFragmentId.NDEF_DETAILS_FRAGMENT_ID);
        fragmentList.add(UIHelper.STFragmentId.CC_FILE_TYPE5_FRAGMENT_ID);
        fragmentList.add(UIHelper.STFragmentId.SYS_FILE_TYP5_FRAGMENT_ID);
        fragmentList.add(UIHelper.STFragmentId.RAW_DATA_FRAGMENT_ID);

        mPagerAdapter = new STPagerAdapter(getSupportFragmentManager(), getApplicationContext(), fragmentList);

        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mPagerAdapter);

        mSlidingTabLayout = (SlidingTabLayout) findViewById(R.id.sliding_tabs);
        mSlidingTabLayout.setViewPager(mViewPager);

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        // Check if the activity was started with a request to select a specific tab
        Intent mIntent = getIntent();
        int tabNbr = mIntent.getIntExtra("select_tab", -1);
        if(tabNbr != -1) {
            mViewPager.setCurrentItem(tabNbr);
        }

    }

    @Override
    public void onResume() {
        super.onResume();

        boolean tagChanged = tagChanged(this, mST25TVCTag);
        if(!tagChanged) {
            byte[] uid = new byte[0];
            try {
                uid = mST25TVCTag.getUid();
            } catch (STException e) {
                e.printStackTrace();
            }

            if (Arrays.equals(uid, MainActivity.UNTRACEABLE_UID)) {
                // Tag in Untraceable mode
                executeAsynchronousAction(READ_CURRENT_UNTRACEABLE_MODE_CONFIGURATION);
            } else {
                executeAsynchronousAction(PERFORM_SAFETY_CHECKS);
            }
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
        finish();
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

    void processIntent(Intent intent) {
        Log.d(TAG, "Process Intent");
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onNewIntent(Intent intent) {
        // onResume gets called after this to handle the intent
        setIntent(intent);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        return mMenu.selectItem(this, item);
    }

    public ST25TVCTag getTag() {
        return mST25TVCTag;
    }

    private void restartLastAsynchronousAction() {
        executeAsynchronousAction(mCurrentAction);
    }

    private void executeAsynchronousAction(Action action) {
        Log.d(TAG, "Starting background action " + action);
        mCurrentAction = action;
        new myAsyncTask(action).execute();
    }


    private class myAsyncTask extends AsyncTask<Void, Void, ActionStatus> {
        Action mAction;
        boolean mIsPrivConfigurationLocked;
        UntraceableModeInterface.UntraceableModeDefaultSettings mUntraceableModeDefaultSettings;

        public myAsyncTask(Action action) {
            mAction = action;
        }

        @Override
        protected ActionStatus doInBackground(Void... param) {
            ActionStatus result;

            try {
                switch(mAction) {
                    case READ_CURRENT_UNTRACEABLE_MODE_CONFIGURATION:
                        mIsPrivConfigurationLocked = mST25TVCTag.isPrivConfigurationLocked();
                        mUntraceableModeDefaultSettings = mST25TVCTag.getUntraceableModeDefaultSettings();
                        result = ACTION_SUCCESSFUL;
                        break;

                    case DISABLE_UNTRACEABLE_MODE:
                        mST25TVCTag.setUntraceableModeDefaultSettings(PRIVACY_BY_DEFAULT_DISABLED);
                        result = ACTION_SUCCESSFUL;
                        break;

                    case PERFORM_SAFETY_CHECKS:
                         NDEFMsg currentNdefMsg = mST25TVCTag.readNdefMessage();

                        // CHECK 1: If ANDEF is enabled, check that the NDEF message contains an URI
                        if (mST25TVCTag.isAndefEnabled()) {
                            if  ((currentNdefMsg == null) ||
                                 (currentNdefMsg.getNbrOfRecords() != 1) ||
                                 (!(currentNdefMsg.getNDEFRecord(0) instanceof UriRecord)) ) {
                                return SUSPICIOUS_ANDEF_CONFIGURATION;
                            }
                        }

                        // CHECK 2: If an NDEF is available, check that the size indicated in the CCFile is correct
                        // (otherwise the native processing of the NDEF will not work on Android versions lower than Android 9)
                        if ((currentNdefMsg != null) && (!mST25TVCTag.isCCFileValid())) {
                            return INVALID_CCFILE;
                        }

                        result = ACTION_SUCCESSFUL;
                        break;

                    case DISABLE_ANDEF:
                        mST25TVCTag.enableAndef(false);
                        result = ACTION_SUCCESSFUL;
                        break;

                    case FIX_CCFILE:
                        mST25TVCTag.initEmptyCCFile();
                        mST25TVCTag.writeCCFile();
                        result = ACTION_SUCCESSFUL;
                        break;

                    default:
                        // Unknown action
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

                    case ISO15693_BLOCK_IS_LOCKED:
                        switch(mAction) {
                            case FIX_CCFILE:
                                result = AREA1_PASSWORD_NEEDED;
                                break;
                            default:
                                result = TAG_NOT_IN_THE_FIELD;
                                break;
                        }
                        break;

                    case INVALID_CCFILE:
                    case INVALID_NDEF_DATA:
                        if (mAction == PERFORM_SAFETY_CHECKS) {
                            // These errors happen if the tag doesn't contain a valid CCFile or NDEF File
                            // No need to continue de Safety checks in that case
                            result = ACTION_SUCCESSFUL;
                        } else {
                            result = ACTION_FAILED;
                        }
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
                    switch(mAction) {
                        case READ_CURRENT_UNTRACEABLE_MODE_CONFIGURATION:
                            if (mUntraceableModeDefaultSettings == PRIVACY_BY_DEFAULT_DISABLED) {
                                // Untraceable Mode has been disabled permanently
                                displayUntraceableModeDisabledMessage();
                            } else {
                                if(mIsPrivConfigurationLocked) {
                                    // Untraceable Mode configuration is permanently locked and cannot be changed
                                    displayUntraceableModePermanentlyLockedWarning();
                                } else {
                                    // Ask the user if he wants to permanently disable the Untraceable Mode
                                    displayUntraceableModeQuestion();
                                }
                            }
                            break;

                        case DISABLE_ANDEF:
                        case FIX_CCFILE:
                            showToast(R.string.tag_updated);
                            // Restart the safetyCheck;
                            executeAsynchronousAction(PERFORM_SAFETY_CHECKS);
                            break;

                        case DISABLE_UNTRACEABLE_MODE:
                            showToast(R.string.tag_updated);
                            break;
                        case PERFORM_SAFETY_CHECKS:
                            Log.v(TAG, "Safety check: No problem detected");
                            break;
                    }
                    break;

                case INVALID_CCFILE:
                    displayCCFileWarning();
                    break;

                case SUSPICIOUS_ANDEF_CONFIGURATION:
                    displayDisableAndefQuestion();
                    break;

                case CONFIG_PASSWORD_NEEDED:
                    displayConfigurationPasswordDialogBox();
                    break;

                case AREA1_PASSWORD_NEEDED:
                    displayArea1PasswordDialogBox();
                    break;

                case ACTION_FAILED:
                    showToast(R.string.Command_failed);
                    break;

                case TAG_NOT_IN_THE_FIELD:
                    showToast(R.string.tag_not_in_the_field);
                    break;
            }

            return;
        }
     }

    private void displayUntraceableModeDisabledMessage() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        // set dialog message
        alertDialogBuilder
                .setMessage(R.string.untraceable_mode_permanently_disabled)
                .setCancelable(true)
                .setPositiveButton(getString(R.string.ok),new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        dialog.cancel();
                    }
                });

        // create alert dialog
        AlertDialog dialog = alertDialogBuilder.create();

        // show it
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.st_light_blue));
    }


    private void displayDisableAndefQuestion() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        String dialogMsg = getResources().getString(R.string.warning_invalid_andef_configuration);

        // set dialog message
        alertDialogBuilder
                .setMessage(dialogMsg)
                .setCancelable(true)
                .setNegativeButton(getString(R.string.no),new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        dialog.cancel();
                    }
                })
                .setPositiveButton(getString(R.string.yes),new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        executeAsynchronousAction(DISABLE_ANDEF);
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

    private void displayCCFileWarning() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        String dialogMsg = getResources().getString(R.string.warning_invalid_ccfile);

        // set dialog message
        alertDialogBuilder
                .setMessage(dialogMsg)
                .setCancelable(true)
                .setNegativeButton(getString(R.string.no),new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        dialog.cancel();
                    }
                })
                .setPositiveButton(getString(R.string.yes),new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        executeAsynchronousAction(FIX_CCFILE);
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

    private void displayUntraceableModePermanentlyLockedWarning() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        // set dialog message
        alertDialogBuilder
                .setMessage(R.string.untraceable_mode_configuration_is_permanently_locked)
                .setCancelable(true)
                .setPositiveButton(getString(R.string.ok),new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        dialog.cancel();
                    }
                });

        // create alert dialog
        AlertDialog dialog = alertDialogBuilder.create();

        // show it
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.st_light_blue));
    }

    private void displayUntraceableModeQuestion() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        // set dialog message
        alertDialogBuilder
                .setMessage(R.string.do_you_want_to_permamently_disable_the_untraceable_mode)
                .setCancelable(true)
                .setPositiveButton(getString(R.string.yes),new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        dialog.cancel();
                        executeAsynchronousAction(DISABLE_UNTRACEABLE_MODE);
                    }
                })
                .setNegativeButton(getString(R.string.no),new DialogInterface.OnClickListener() {
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

        STType5PwdDialogFragment pwdDialogFragment = STType5PwdDialogFragment.newInstance(
                STType5PwdDialogFragment.STPwdAction.PRESENT_CURRENT_PWD,
                ST25TVCTag.ST25TVC_CONFIGURATION_PASSWORD_ID,
                getResources().getString(R.string.enter_configuration_pwd));
        if(pwdDialogFragment!=null) pwdDialogFragment.show(mFragmentManager, "pwdDialogFragment");
    }

    private void displayArea1PasswordDialogBox() {
        Log.v(TAG, "displayArea1PasswordDialogBox");

        STType5PwdDialogFragment pwdDialogFragment = STType5PwdDialogFragment.newInstance(
                STType5PwdDialogFragment.STPwdAction.PRESENT_CURRENT_PWD,
                ST25TVCTag.ST25TVC_AREA1_PASSWORD_ID,
                getResources().getString(R.string.enter_area1_pwd));
        if(pwdDialogFragment!=null) pwdDialogFragment.show(mFragmentManager, "pwdDialogFragment");
    }

    @Override
    public void onSTType5PwdDialogFinish(int result) {
        Log.v(TAG, "onSTType5PwdDialogFinish. result = " + result);

        if (result == PwdDialogFragment.RESULT_OK) {
            // The correct password has been enterred. Retry the last action
            restartLastAsynchronousAction();
        } else {
            Log.e(TAG, "Action failed! Tag not updated!");
        }
    }

}

