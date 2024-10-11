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

package com.st.st25nfc.type5;

import android.content.res.ColorStateList;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;

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
import com.st.st25sdk.type5.STType5Tag;
import com.st.st25sdk.type5.st25tvc.ST25TVCTag;

import static com.st.st25nfc.type5.STType5AfiDsfidActivity.Action.READ_AFI_AND_DSFID;
import static com.st.st25nfc.type5.STType5AfiDsfidActivity.Action.WRITE_AFI_AND_DSFID;
import static com.st.st25nfc.type5.STType5AfiDsfidActivity.ActionStatus.*;


public class STType5AfiDsfidActivity extends STFragmentActivity implements NavigationView.OnNavigationItemSelectedListener {

    enum Action {
        READ_AFI_AND_DSFID,
        WRITE_AFI_AND_DSFID
    };

    enum ActionStatus {
        ACTION_SUCCESSFUL,
        ACTION_FAILED,
        TAG_NOT_IN_THE_FIELD,
        CONFIG_PASSWORD_NEEDED,
        AREA1_PASSWORD_NEEDED
    };

    // Set here the Toolbar to use for this activity
    private int toolbar_res = R.menu.toolbar_empty;

    static final String TAG = "AfiDsfidActivity";
    private Handler mHandler;
    FragmentManager mFragmentManager;
    private Action mCurrentAction;
    private STType5Tag mSTType5Tag;

    private EditText mAfiEditText;
    private CheckBox mAfiLockedCheckbox;
    private CheckBox mAfiSecurityConfCheckbox;

    private EditText mDsfidEditText;
    private CheckBox mDsfidLockedCheckbox;

    private byte mAfi;
    private boolean mIsAfiLocked;
    private byte mDsfid;
    private boolean mIsDsfidLocked;
    private boolean mIsAfiWriteProtected;

    private boolean mIsArea1PwdPresented = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.default_layout);

        // Inflate content of FrameLayout
        FrameLayout frameLayout = findViewById(R.id.frame_content);
        View childView = getLayoutInflater().inflate(R.layout.fragment_sttype5_afi_dsfid, null);
        frameLayout.addView(childView);

        TextView warningTextView = findViewById(R.id.warningTextView);

        NFCTag nfcTag = MainActivity.getTag();
        if (nfcTag instanceof STType5Tag) {
            mSTType5Tag = (STType5Tag) nfcTag;
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

        mAfiEditText = findViewById(R.id.afiEditText);
        mAfiLockedCheckbox = findViewById(R.id.lockAfiCheckbox);
        mAfiSecurityConfCheckbox = findViewById(R.id.afiSecurityConfCheckbox);
        mDsfidEditText = findViewById(R.id.dsfidEditText);
        mDsfidLockedCheckbox = findViewById(R.id.lockDsfidCheckbox);
    }

    @Override
    public void onResume() {
        super.onResume();

        boolean tagChanged = tagChanged(this, mSTType5Tag);
        if(!tagChanged) {
            executeAsynchronousAction(READ_AFI_AND_DSFID);
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
                        executeAsynchronousAction(WRITE_AFI_AND_DSFID);
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
        mAfiEditText.setText(String.format("%02x", mAfi).toUpperCase());
        mDsfidEditText.setText(String.format("%02x", mDsfid).toUpperCase());

        mAfiLockedCheckbox.setChecked(mIsAfiLocked);
        mDsfidLockedCheckbox.setChecked(mIsDsfidLocked);

        if(mIsAfiLocked) {
            mAfiEditText.setEnabled(false);
            mAfiLockedCheckbox.setEnabled(false);
            //mAfiLockedCheckbox.setTextColor(getResources().getColor(R.color.st_dark_blue));
            //CompoundButtonCompat.setButtonTintList(mAfiLockedCheckbox, ColorStateList.valueOf(getResources().getColor(R.color.st_dark_blue)));
        } else {
            mAfiEditText.setEnabled(true);
            mAfiLockedCheckbox.setEnabled(true);
            //mAfiLockedCheckbox.setTextColor(getResources().getColor(R.color.st_dark_blue));
            //CompoundButtonCompat.setButtonTintList(mAfiLockedCheckbox, ColorStateList.valueOf(getResources().getColor(R.color.st_light_blue)));
        }

        if(mIsDsfidLocked) {
            mDsfidEditText.setEnabled(false);
            mDsfidLockedCheckbox.setEnabled(false);
            //mDsfidLockedCheckbox.setTextColor(getResources().getColor(R.color.st_dark_blue));
            //CompoundButtonCompat.setButtonTintList(mDsfidLockedCheckbox, ColorStateList.valueOf(getResources().getColor(R.color.st_dark_blue)));
        } else {
            mDsfidEditText.setEnabled(true);
            mDsfidLockedCheckbox.setEnabled(true);
            //mDsfidLockedCheckbox.setTextColor(getResources().getColor(R.color.st_dark_blue));
            //CompoundButtonCompat.setButtonTintList(mDsfidLockedCheckbox, ColorStateList.valueOf(getResources().getColor(R.color.st_light_blue)));
        }

        if(mSTType5Tag instanceof ST25TVCTag) {
            mAfiSecurityConfCheckbox.setChecked(mIsAfiWriteProtected);
            mAfiSecurityConfCheckbox.setVisibility(View.VISIBLE);
        } else {
            mAfiSecurityConfCheckbox.setVisibility(View.GONE);
        }
    }

    private void displayConfigurationPasswordDialogBox() {

        // For the moment, the Configuration password is used only for ST25TVCTag
        if (!(mSTType5Tag instanceof ST25TVCTag)) {
            Log.e(TAG, "Not implemented!");
            return;
        }
        int passwordNumber = ST25TVCTag.ST25TVC_CONFIGURATION_PASSWORD_ID;

        STType5PwdDialogFragment pwdDialogFragment = STType5PwdDialogFragment.newInstance(
                STType5PwdDialogFragment.STPwdAction.PRESENT_CURRENT_PWD,
                passwordNumber,
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


    private void displayArea1PasswordDialogBox() {

        // For the moment, the Area1 password is used only for ST25TVC
        if (!(mSTType5Tag instanceof ST25TVCTag)) {
            Log.e(TAG, "Not implemented!");
            return;
        }
        int passwordNumber = ST25TVCTag.ST25TVC_AREA1_PASSWORD_ID;

        STType5PwdDialogFragment pwdDialogFragment = STType5PwdDialogFragment.newInstance(
                STType5PwdDialogFragment.STPwdAction.PRESENT_CURRENT_PWD,
                passwordNumber,
                getResources().getString(R.string.enter_area1_pwd),
                new STType5PwdDialogFragment.STType5PwdDialogListener() {
                    @Override
                    public void onSTType5PwdDialogFinish(int result) {
                        if (result == PwdDialogFragment.RESULT_OK) {
                            mIsArea1PwdPresented = true;
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
                UIHelper.displayCircularProgressBar(STType5AfiDsfidActivity.this, getString(R.string.please_wait));

                switch (mAction) {
                    case READ_AFI_AND_DSFID:
                        mAfi = mSTType5Tag.getAFI();
                        mDsfid = mSTType5Tag.getDSFID();

                        mIsAfiLocked = isAFILocked();
                        mIsDsfidLocked = isDSFIDLocked();

                        if(mSTType5Tag instanceof ST25TVCTag) {
                            mIsAfiWriteProtected = ((ST25TVCTag) mSTType5Tag).isAfiWriteProtected();
                            if (mIsAfiWriteProtected && !mIsArea1PwdPresented) {
                                return AREA1_PASSWORD_NEEDED;
                            }
                        }

                        result = ACTION_SUCCESSFUL;
                        break;

                    case WRITE_AFI_AND_DSFID:
                        byte afi = (byte) Integer.parseInt(mAfiEditText.getText().toString(), 16);
                        if (afi != mAfi) {
                            mSTType5Tag.writeAFI(afi);
                            mAfi = afi;
                        }

                        byte dsfid = (byte) Integer.parseInt(mDsfidEditText.getText().toString(), 16);
                        if (dsfid != mDsfid) {
                            mSTType5Tag.writeDSFID(dsfid);
                            mDsfid = dsfid;
                        }

                        boolean isAfiLocked = mAfiLockedCheckbox.isChecked();
                        if (isAfiLocked != mIsAfiLocked) {
                            mSTType5Tag.lockAFI();
                            mIsAfiLocked = isAfiLocked;
                        }

                        boolean isDsfidLocked = mDsfidLockedCheckbox.isChecked();
                        if (isDsfidLocked != mIsDsfidLocked) {
                            mSTType5Tag.lockDSFID();
                            mIsDsfidLocked = isDsfidLocked;
                        }

                        if(mSTType5Tag instanceof ST25TVCTag) {
                            boolean isAfiWriteProtected = mAfiSecurityConfCheckbox.isChecked();
                            if (isAfiWriteProtected != mIsAfiWriteProtected) {
                                ((ST25TVCTag) mSTType5Tag).setAfiWriteProtected(isAfiWriteProtected);
                                mIsAfiWriteProtected = isAfiWriteProtected;
                            }
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
                        result = CONFIG_PASSWORD_NEEDED;
                        break;

                    case ISO15693_BLOCK_IS_LOCKED:
                        result = AREA1_PASSWORD_NEEDED;
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


        /**
         * Indicates if DSFID is locked
         * @return
         * @throws STException
         */
        public boolean isDSFIDLocked() throws STException {
            boolean isDSFIDLocked;
            byte dsfid = mSTType5Tag.getDSFID();

            try {
                // Try to write the same value
                mSTType5Tag.writeDSFID(dsfid);
                isDSFIDLocked = false;

            } catch (STException e) {
                switch (e.getError()) {
                    case ISO15693_BLOCK_IS_LOCKED:
                        isDSFIDLocked = true;
                        break;
                    default:
                        // Other exceptions are escalated
                        throw e;
                }
            }

            return isDSFIDLocked;
        }


        /**
         * Indicates if AFI is locked
         * @return
         * @throws STException
         */
        public boolean isAFILocked() throws STException {
            boolean isAFILocked;
            byte afi = mSTType5Tag.getAFI();

            try {
                // Try to write the same value
                mSTType5Tag.writeAFI(afi);
                isAFILocked = false;

            } catch (STException e) {
                switch (e.getError()) {
                    case ISO15693_BLOCK_IS_LOCKED:
                        isAFILocked = true;
                        break;
                    default:
                        // Other exceptions are escalated
                        throw e;
                }
            }

            return isAFILocked;
        }

        @Override
        protected void onPostExecute(ActionStatus actionStatus) {

            UIHelper.dismissCircularProgressBar();

            switch(actionStatus) {
                case ACTION_SUCCESSFUL:
                    switch (mAction) {
                        case READ_AFI_AND_DSFID:
                            updateUI();
                            break;
                        case WRITE_AFI_AND_DSFID:
                            if(mSTType5Tag instanceof ST25TVCTag) {
                                mIsArea1PwdPresented = false;
                                DisplayTapTagRequest.run(STType5AfiDsfidActivity.this, mSTType5Tag, getString(R.string.please_tap_the_tag_again));
                            }
                            updateUI();
                            break;
                    }
                    break;

                case CONFIG_PASSWORD_NEEDED:
                    displayConfigurationPasswordDialogBox();
                    break;

                case AREA1_PASSWORD_NEEDED:
                    displayArea1PasswordDialogBox();
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
