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
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager.widget.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;
import androidx.core.widget.CompoundButtonCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.navigation.NavigationView;
import com.st.st25nfc.R;
import com.st.st25nfc.generic.MainActivity;
import com.st.st25nfc.generic.PwdDialogFragment;
import com.st.st25nfc.generic.STFragmentActivity;
import com.st.st25nfc.generic.STType5PwdDialogFragment;
import com.st.st25nfc.generic.util.UIHelper;
import com.st.st25sdk.STException;
import com.st.st25sdk.type5.st25tvc.ST25TVCTag;

import java.util.LinkedList;
import java.util.List;

import static com.st.st25nfc.type5.st25tvc.ST25TVCLockConfigurationActivity.ActionStatus.ACTION_FAILED;
import static com.st.st25nfc.type5.st25tvc.ST25TVCLockConfigurationActivity.ActionStatus.ACTION_SUCCESSFUL;
import static com.st.st25nfc.type5.st25tvc.ST25TVCLockConfigurationActivity.ActionStatus.TAG_NOT_IN_THE_FIELD;
import static com.st.st25sdk.type5.st25tvc.ST25TVCTag.ST25TVC_FID_REGISTER_AFI;
import static com.st.st25sdk.type5.st25tvc.ST25TVCTag.ST25TVC_FID_REGISTER_ANDEF;
import static com.st.st25sdk.type5.st25tvc.ST25TVCTag.ST25TVC_FID_REGISTER_PRIVACY;
import static com.st.st25sdk.type5.st25tvc.ST25TVCTag.ST25TVC_FID_REGISTER_RW_PROTECTION_A1;
import static com.st.st25sdk.type5.st25tvc.ST25TVCTag.ST25TVC_FID_REGISTER_RW_PROTECTION_A2;
import static com.st.st25sdk.type5.st25tvc.ST25TVCTag.ST25TVC_FID_REGISTER_TAMPER_DETECT;
import static com.st.st25sdk.type5.st25tvc.ST25TVCTag.ST25TVC_FID_REGISTER_SIGNATURE_KEYID;
import static com.st.st25sdk.type5.st25tvc.ST25TVCTag.ST25TVC_FID_REGISTER_UNIQUE_TAP_CODE;


public class ST25TVCLockConfigurationActivity extends STFragmentActivity implements NavigationView.OnNavigationItemSelectedListener {

    enum Action {
        READ_CURRENT_LOCKS,
        WRITE_LOCKS
    };

    enum ActionStatus {
        ACTION_SUCCESSFUL,
        ACTION_FAILED,
        TAG_NOT_IN_THE_FIELD,
        CONFIG_PASSWORD_NEEDED
    };

    public class Feature {
        String name;
        int fid;
        boolean isLocked;

        public Feature(String name, int fid, boolean isLocked) {
            this.name = name;
            this.fid = fid;
            this.isLocked = isLocked;
        }

        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }

        public int getFid() {
            return fid;
        }
        public void setFid(int fid) {
            this.fid = fid;
        }

        public boolean isLocked() {
            return isLocked;
        }
        public void setLocked(boolean locked) {
            isLocked = locked;
        }
    }

    public static final int ST25TV_REGISTER_AREA1_SECURITY_ATTRIBUTE    = 0x0;

    // Set here the Toolbar to use for this activity
    private int toolbar_res = R.menu.toolbar_save;

    static final String TAG = "LockActivity";
    private ST25TVCTag mST25TVCTag;
    FragmentManager mFragmentManager;

    private Action mCurrentAction;

    private ListView mListView;
    private FeatureListAdapter mFeatureListAdapter;
    private List<Feature> mFeatureLinkedList = new LinkedList<>();

    private boolean mIsAfiConfigLocked;
    private boolean mIsPrivacyConfigLocked;
    private boolean mIsAndefConfigLocked;
    private boolean mIsTamperDetectConfigLocked;
    private boolean mIsUniqueTapCodeConfigLocked;
    private boolean mIsArea2ConfigLocked;
    private boolean mIsArea1ConfigLocked;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.default_layout);

        // Inflate content of FrameLayout
        FrameLayout frameLayout = findViewById(R.id.frame_content);
        View childView = getLayoutInflater().inflate(R.layout.fragment_st25tvc_lock_configuration, null);
        frameLayout.addView(childView);

        mST25TVCTag = (ST25TVCTag) MainActivity.getTag();
        if (mST25TVCTag == null) {
            showToast(R.string.invalid_tag);
            goBackToMainActivity();
            return;
        }

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

        mListView = (ListView)findViewById(R.id.listView);

        mFeatureLinkedList.add(new Feature("AREA1 feature", 0x00, false));
        mFeatureLinkedList.add(new Feature("AREA2 feature", 0x01, false));
        mFeatureLinkedList.add(new Feature("COUNTER feature", 0x02, false));
        mFeatureLinkedList.add(new Feature("TAMPER DETECT feature", 0x03, false));
        mFeatureLinkedList.add(new Feature("ANDEF feature", 0x04, false));
        mFeatureLinkedList.add(new Feature("PRIVACY feature", 0x05, false));
        mFeatureLinkedList.add(new Feature("SIG_ANDEF feature", 0x06, false));
        mFeatureLinkedList.add(new Feature("PWDCNT feature", 0x07, false));
        mFeatureLinkedList.add(new Feature("AFI_SEC feature", 0x08, false));

        mFeatureListAdapter = new FeatureListAdapter(this, mFeatureLinkedList);
        mListView.setAdapter(mFeatureListAdapter);

    }

    @Override
    public void onResume() {
        super.onResume();

        boolean tagChanged = tagChanged(this, mST25TVCTag);
        if(!tagChanged) {
            executeAsynchronousAction(Action.READ_CURRENT_LOCKS);
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
                        executeAsynchronousAction(Action.WRITE_LOCKS);
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
        UIHelper.checkIfUiThread();
        mFeatureListAdapter.notifyDataSetChanged();
    }

    private void displayWarningForNonPortableCharacters() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        // set dialog message
        alertDialogBuilder
                .setMessage(R.string.warning_about_ascii_characters)
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

    public class FeatureListAdapter extends BaseAdapter {
        Context mContext;
        List<Feature> linkedList;
        protected LayoutInflater layoutInflater;

        public FeatureListAdapter(Context context, List<Feature> linkedList) {
            this.mContext = context;
            this.linkedList = linkedList;
            this.layoutInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return linkedList.size();
        }

        @Override
        public Feature getItem(int position) {
            Feature feature = linkedList.get(position);
            return feature;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {

            final ViewHolder holder;
            if (convertView == null) {
                convertView = layoutInflater.inflate(R.layout.st25tvc_lock_configuration_row, parent, false);
                holder = new ViewHolder();
                holder.titleTextView = (TextView) convertView.findViewById(R.id.textView);
                holder.selectionCheckBox = (CheckBox) convertView.findViewById(R.id.checkBox);
                convertView.setTag(holder);

            }
            else {
                holder = (ViewHolder) convertView.getTag();
            }

            final Feature feature = linkedList.get(position);

            holder.titleTextView.setText(String.format("FID = 0x%02x", feature.getFid()));
            holder.selectionCheckBox.setText(feature.getName());
            holder.selectionCheckBox.setChecked(feature.isLocked);

            if(feature.isLocked) {
                holder.selectionCheckBox.setEnabled(false);
                holder.selectionCheckBox.setTextColor(getResources().getColor(R.color.st_dark_blue));
                CompoundButtonCompat.setButtonTintList(holder.selectionCheckBox, ColorStateList.valueOf(getResources().getColor(R.color.st_dark_blue)));
            } else {
                holder.selectionCheckBox.setEnabled(true);
                holder.selectionCheckBox.setTextColor(getResources().getColor(R.color.st_dark_blue));
                CompoundButtonCompat.setButtonTintList(holder.selectionCheckBox, ColorStateList.valueOf(getResources().getColor(R.color.st_light_blue)));
            }

            holder.selectionCheckBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(((CheckBox)view).isChecked()) {
                        setChecked(position,true);
                    }
                    else {
                        setChecked(position,false);
                    }
                }
            });

            /**Set tag to all checkBox**/
            holder.selectionCheckBox.setTag(feature);

            return convertView;
        }

        private class ViewHolder {
            TextView titleTextView;
            CheckBox selectionCheckBox;
        }

        public void setChecked(int position, boolean locked) {
            Feature feature = linkedList.get(position);
            feature.setLocked(locked);
        }

        public boolean isChecked(int position) {
            final Feature feature = linkedList.get(position);
            return feature.isLocked;
        }
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
                UIHelper.displayCircularProgressBar(ST25TVCLockConfigurationActivity.this, getString(R.string.please_wait));

                switch (mAction) {
                    case READ_CURRENT_LOCKS:
                        mIsAfiConfigLocked = mST25TVCTag.isAfiProtectionConfigurationLocked();
                        mFeatureListAdapter.getItem(ST25TVC_FID_REGISTER_AFI).setLocked(mIsAfiConfigLocked);
                        mIsPrivacyConfigLocked = mST25TVCTag.isPrivConfigurationLocked();
                        mFeatureListAdapter.getItem(ST25TVC_FID_REGISTER_PRIVACY).setLocked(mIsPrivacyConfigLocked);

                        mIsAndefConfigLocked = mST25TVCTag.isAndefConfigurationLocked();
                        mFeatureListAdapter.getItem(ST25TVC_FID_REGISTER_ANDEF).setLocked(mIsAndefConfigLocked);

                        mIsTamperDetectConfigLocked = mST25TVCTag.isTamperDetectConfigurationLocked();
                        mFeatureListAdapter.getItem(ST25TVC_FID_REGISTER_TAMPER_DETECT).setLocked(mIsTamperDetectConfigLocked);

                        mIsUniqueTapCodeConfigLocked = mST25TVCTag.isUniqueTapCodeConfigurationLocked();
                        mFeatureListAdapter.getItem(ST25TVC_FID_REGISTER_UNIQUE_TAP_CODE).setLocked(mIsUniqueTapCodeConfigLocked);

                        mIsArea2ConfigLocked = mST25TVCTag.isArea2ConfigurationLocked();
                        mFeatureListAdapter.getItem(ST25TVC_FID_REGISTER_RW_PROTECTION_A2).setLocked(mIsArea2ConfigLocked);

                        mIsArea1ConfigLocked = mST25TVCTag.isArea1ConfigurationLocked();
                        mFeatureListAdapter.getItem(ST25TVC_FID_REGISTER_RW_PROTECTION_A1).setLocked(mIsArea1ConfigLocked);

                        result = ActionStatus.ACTION_SUCCESSFUL;
                        break;

                    case WRITE_LOCKS:
                        boolean isLocked = mFeatureListAdapter.isChecked(ST25TVC_FID_REGISTER_AFI);
                        if(!mIsAfiConfigLocked && isLocked) {
                            mST25TVCTag.lockAfiSecConfiguration();
                        }

                        isLocked = mFeatureListAdapter.isChecked(ST25TVC_FID_REGISTER_PRIVACY);
                        if(!mIsPrivacyConfigLocked && isLocked) {
                            mST25TVCTag.lockPrivConfiguration();
                        }

                        isLocked = mFeatureListAdapter.isChecked(ST25TVC_FID_REGISTER_ANDEF);
                        if(!mIsAndefConfigLocked && isLocked) {
                            mST25TVCTag.lockAndefConfiguration();
                        }

                        isLocked = mFeatureListAdapter.isChecked(ST25TVC_FID_REGISTER_TAMPER_DETECT);
                        if(!mIsTamperDetectConfigLocked && isLocked) {
                            mST25TVCTag.lockTamperDetectConfiguration();
                        }

                        isLocked = mFeatureListAdapter.isChecked(ST25TVC_FID_REGISTER_UNIQUE_TAP_CODE);
                        if(!mIsUniqueTapCodeConfigLocked && isLocked) {
                            mST25TVCTag.lockUniqueTapCodeConfiguration();
                        }

                        isLocked = mFeatureListAdapter.isChecked(ST25TVC_FID_REGISTER_RW_PROTECTION_A2);
                        if(!mIsArea2ConfigLocked && isLocked) {
                            mST25TVCTag.lockArea2Configuration();
                        }

                        isLocked = mFeatureListAdapter.isChecked(ST25TVC_FID_REGISTER_RW_PROTECTION_A1);
                        if(!mIsArea1ConfigLocked && isLocked) {
                            mST25TVCTag.lockArea1Configuration();
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
                        case READ_CURRENT_LOCKS:
                            updateUI();
                            break;
                        case WRITE_LOCKS:
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
