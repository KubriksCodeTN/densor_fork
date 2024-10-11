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
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.android.material.navigation.NavigationView;
import com.st.st25nfc.R;
import com.st.st25nfc.generic.MainActivity;
import com.st.st25nfc.generic.PwdDialogFragment;
import com.st.st25nfc.generic.STFragmentActivity;
import com.st.st25nfc.generic.STType5PwdDialogFragment;
import com.st.st25nfc.generic.util.DisplayTapTagRequest;
import com.st.st25nfc.generic.util.UIHelper;
import com.st.st25sdk.NFCTag;
import com.st.st25sdk.STException;
import com.st.st25sdk.type5.st25tvc.ST25TVCTag;

import static com.st.st25nfc.type5.st25tvc.ST25TVCUniqueTapCodeActivity.Action.READ_UNIQUE_TAP_CODE_VALUE;
import static com.st.st25nfc.type5.st25tvc.ST25TVCUniqueTapCodeActivity.Action.WRITE_UNIQUE_TAP_CODE_ENABLED;
import static com.st.st25nfc.type5.st25tvc.ST25TVCUniqueTapCodeActivity.ActionStatus.*;

public class ST25TVCUniqueTapCodeActivity extends STFragmentActivity implements NavigationView.OnNavigationItemSelectedListener {

    enum Action {
        READ_UNIQUE_TAP_CODE_VALUE,
        WRITE_UNIQUE_TAP_CODE_ENABLED
    };

    enum ActionStatus {
        ACTION_SUCCESSFUL,
        ACTION_FAILED,
        TAG_NOT_IN_THE_FIELD,
        CONFIG_PASSWORD_NEEDED,
        UNIQUE_TAP_CODE_CONFIG_LOCKED
    };

    // Set here the Toolbar to use for this activity
    private int toolbar_res = R.menu.toolbar_empty;

    static final String TAG = "UniqueTapCodeActivity";
    private Handler mHandler;
    FragmentManager mFragmentManager;
    private Action mCurrentAction;
    private ST25TVCTag mST25TVCTag;

    private CheckBox mEnableUniqueTapCodeCheckbox;
    private TextView mCounterValueTextView;
    private TextView mCounterAsciiValueTextView;

    private boolean mIsUniqueTapCodeEnabled;
    private boolean mIsUniqueTapCodeConfigLocked;
    private String mUniqueTapCodeAsciiValue = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.default_layout);

        // Inflate content of FrameLayout
        FrameLayout frameLayout = findViewById(R.id.frame_content);
        View childView = getLayoutInflater().inflate(R.layout.fragment_st25tvc_unique_tap_code, null);
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

        mEnableUniqueTapCodeCheckbox = findViewById(R.id.uniqueTapCodeEnableCheckbox);
        mCounterAsciiValueTextView = findViewById(R.id.counterAsciiValueTextView);
    }

    @Override
    public void onResume() {
        super.onResume();

        boolean tagChanged = tagChanged(this, mST25TVCTag);
        if(!tagChanged) {
            executeAsynchronousAction(READ_UNIQUE_TAP_CODE_VALUE);
        }

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
                        executeAsynchronousAction(WRITE_UNIQUE_TAP_CODE_ENABLED);
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

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        return mMenu.selectItem(this, item);
    }

    private void updateUI() {
        mEnableUniqueTapCodeCheckbox.setChecked(mIsUniqueTapCodeEnabled);
        mCounterAsciiValueTextView.setText("'" + mUniqueTapCodeAsciiValue + "'");
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
                UIHelper.displayCircularProgressBar(ST25TVCUniqueTapCodeActivity.this, getString(R.string.please_wait));

                switch (mAction) {
                    case READ_UNIQUE_TAP_CODE_VALUE:
                        mIsUniqueTapCodeConfigLocked = mST25TVCTag.isUniqueTapCodeConfigurationLocked();
                        mIsUniqueTapCodeEnabled = mST25TVCTag.isUniqueTapCodeEnabled();
                        mUniqueTapCodeAsciiValue = mST25TVCTag.getUniqueTapCodeString();
                        result = ActionStatus.ACTION_SUCCESSFUL;
                        break;

                    case WRITE_UNIQUE_TAP_CODE_ENABLED:
                        if(mIsUniqueTapCodeConfigLocked) {
                            return UNIQUE_TAP_CODE_CONFIG_LOCKED;
                        }
                        boolean uniqueTapCodeEnabled = mEnableUniqueTapCodeCheckbox.isChecked();
                        mST25TVCTag.enableUniqueTapCode(uniqueTapCodeEnabled);
                        mIsUniqueTapCodeEnabled = uniqueTapCodeEnabled;
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
                        case READ_UNIQUE_TAP_CODE_VALUE:
                            updateUI();
                            break;
                        case WRITE_UNIQUE_TAP_CODE_ENABLED:
                            DisplayTapTagRequest.run(ST25TVCUniqueTapCodeActivity.this, mST25TVCTag, getString(R.string.please_tap_the_tag_again));
                            updateUI();
                            break;
                    }
                    break;

                case CONFIG_PASSWORD_NEEDED:
                    displayConfigurationPasswordDialogBox();
                    break;

                case UNIQUE_TAP_CODE_CONFIG_LOCKED:
                    UIHelper.displayMessage(ST25TVCUniqueTapCodeActivity.this, R.string.unique_tap_code_configuration_is_locked);
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
