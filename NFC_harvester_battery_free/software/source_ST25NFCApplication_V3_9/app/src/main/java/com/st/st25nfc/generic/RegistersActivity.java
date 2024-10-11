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

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import com.google.android.material.navigation.NavigationView;
import androidx.fragment.app.FragmentManager;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.st.st25nfc.generic.util.DisplayTapTagRequest;
import com.st.st25nfc.generic.util.UIHelper;
import com.st.st25sdk.Helper;
import com.st.st25sdk.NFCTag;
import com.st.st25nfc.R;
import com.st.st25sdk.RegisterInterface;
import com.st.st25sdk.STException;
import com.st.st25sdk.STRegister;
import com.st.st25sdk.type5.st25tvc.ST25TVCTag;

import java.util.ArrayList;
import java.util.List;

import static com.st.st25nfc.generic.RegistersActivity.ActionStatus.*;
import static com.st.st25sdk.STRegister.RegisterAccessRights.*;


public class RegistersActivity extends STFragmentActivity
        implements NavigationView.OnNavigationItemSelectedListener, STType5PwdDialogFragment.STType5PwdDialogListener {

    final static String TAG = "ST25RegistersActivity";
    private NFCTag mTag;
    private RegisterInterface mRegisterInterface;

    private ListView mListView;
    private Handler mHandler;
    private CustomListAdapter mAdapter;
    private TextView mStatusTextView;

    FragmentManager mFragmentManager;
    private boolean mForDynamicRegister = false;
    int configurationPasswordNumber;

    enum Action {
        READ_REGISTERS,
        WRITE_REGISTERS
    }
    Action mAction;

    // Local class used to store the information about each register
    class RegisterInfo {
        STRegister register;

        // Boolean indicating if the user has typed a new value in the TextView.
        boolean isValueUpdated;

        boolean isRegisterReadable;

        // 'value' is meaningful only if isValueUpdated==true.
        int value;

        public RegisterInfo(STRegister register) {
            this.register = register;
            this.isValueUpdated = false;
            this.value = 0;
            this.isRegisterReadable = true;
        }

        public boolean isRegisterReadable() {
            return isRegisterReadable;
        }

        public void setRegisterReadable(boolean registerReadable) {
            isRegisterReadable = registerReadable;
        }
    }

    enum ActionStatus {
        ACTION_SUCCESSFUL,
        ACTION_FAILED,
        TAG_NOT_IN_THE_FIELD,
        CONFIGURATION_PASSWORD_NEEDED
    };


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.default_layout);

        // Inflate content of FrameLayout
        FrameLayout frameLayout = (FrameLayout) findViewById(R.id.frame_content);
        View childView = getLayoutInflater().inflate(R.layout.config_registers_activity, null);
        frameLayout.addView(childView);

        if (super.getTag() == null) {
            showToast(R.string.invalid_tag);
            goBackToMainActivity();
            return;
        }

        mFragmentManager = getSupportFragmentManager();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.register_list);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.navigation_view);
        navigationView.setNavigationItemSelectedListener(this);
        mMenu.inflateMenu(navigationView);

        mForDynamicRegister = (this instanceof DynamicRegisterActivity);

        mTag = MainActivity.getTag();

        try {
            mRegisterInterface = (RegisterInterface) mTag;
            if (mRegisterInterface.getDynamicRegisterList() == null && mForDynamicRegister) {
                throw new STException("Error! Tag not implementing Dynamic RegisterInterface!");
            }
        } catch (ClassCastException e) {
            // Tag not implementing RegisterInterface
            Log.e(TAG, "Error! Tag not implementing RegisterInterface!");
            return;
        } catch (STException e) {
            Log.e(TAG, e.getMessage());
            return;
        }

        mListView = (ListView) findViewById(R.id.config_register_list_view);
        mListView.setItemsCanFocus(true);

        // Create an empty register list
        List<RegisterInfo> st25RegisterList = new ArrayList<RegisterInfo>();
        mAdapter = new CustomListAdapter(st25RegisterList);

        mHandler = new Handler();

        if (mHandler != null && mListView != null) {
            mListView.setAdapter(mAdapter);
        }

        mStatusTextView = (TextView) findViewById(R.id.statusTextView);
    }

    @Override
    public void onResume() {
        super.onResume();
        // mAdapter.getCount() == 0 is necessary to update the list only if the mRegisterList is empty
        if((mAdapter != null) && (mAdapter.getCount() == 0) ) {
            refreshRegisterList();
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
        getMenuInflater().inflate(R.menu.toolbar_registers, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save:
                checkOtpRegisters();
                return true;

            case R.id.action_refresh:
                refreshRegisterList();
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Check if some OTP registers have been changed. In that case, display a warning
     */
    private void checkOtpRegisters() {


        List<RegisterInfo> registerList = mAdapter.getData();

        for(RegisterInfo registerInfo : registerList) {
            if (registerInfo.isValueUpdated &&
               (registerInfo.register.getRegisterAccessRights() == REGISTER_READ_WRITE_OTP)) {
                // An OTP register has been changed
                askConfirmationBeforeWritingOtpRegister();
                return;
            }
        }

        // No OTP register has been changed. We can continue with the write operation
        writeRegisters();
    }

    private void askConfirmationBeforeWritingOtpRegister() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        // set dialog message
        alertDialogBuilder
                .setMessage(R.string.warning_otp_registers)
                .setCancelable(true)
                .setNegativeButton(getString(R.string.no),new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        dialog.cancel();
                    }
                })
                .setPositiveButton(getString(R.string.yes),new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        dialog.cancel();
                        writeRegisters();
                    }
                });

        // create alert dialog
        AlertDialog dialog = alertDialogBuilder.create();

        // show it
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(R.color.st_light_blue));
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.st_light_blue));

    }

    private void refreshRegisterList() {
        // Registers should not be read in UI Thread context. We use an AsyncTask to read them.
        Log.d(TAG, "refreshRegisterList");
        new readRegisterValues().execute();
    }

    private void writeRegisters() {
        new Thread(new Runnable() {
            public void run() {
                try {
                    Log.d(TAG, "writeRegisters");
                    mAction = Action.WRITE_REGISTERS;

                    List<RegisterInfo> registerList = mAdapter.getData();
                    int nbrOfRegistersWritten = 0;

                    for(int registerNbr=0; registerNbr<registerList.size(); registerNbr++) {
                        RegisterInfo registerInfo = registerList.get(registerNbr);

                        if(registerInfo.isValueUpdated) {
                            // User has entered a  new value for this register. Write it to the tag
                            registerInfo.register.setRegisterValue(registerInfo.value);
                            nbrOfRegistersWritten++;
                        }
                    }

                    if(nbrOfRegistersWritten > 0) {
                        if (mTag instanceof  ST25TVCTag) {
                            DisplayTapTagRequest.run(RegistersActivity.this, mTag, getString(R.string.please_tap_the_tag_again));
                        } else {
                            showToast(R.string.tag_updated);
                        }

                        // Registers have been written successfully. Go through all the registers and reset "isValueUpdated"
                        for (RegisterInfo registerInfo : registerList) {
                            registerInfo.isValueUpdated = false;
                        }

                        // Refresh the display of the register list
                        refreshRegisterList();

                    } else {
                        showToast(R.string.no_register_to_update);
                    }

                } catch (STException e) {
                    switch (e.getError()) {
                        case CONFIG_PASSWORD_NEEDED:
                            displayConfigurationPasswordDialogBox();
                            break;

                        case TAG_NOT_IN_THE_FIELD:
                            showToast(R.string.tag_not_in_the_field);
                            break;
                        default:
                            e.printStackTrace();
                            showToast(R.string.error_while_updating_the_tag);
                    }
                }
            }
        }).start();
    }

    private void displayConfigurationPasswordDialogBox() {
        Log.v(TAG, "displayConfigurationPasswordDialogBox");

        final int passworNumber = UIHelper.getConfigurationPasswordNumber(mTag);

        // Warning: Function called from background thread! Post a request to the UI thread
        runOnUiThread(new Runnable() {
            public void run() {
                FragmentManager fragmentManager = getSupportFragmentManager();

                STType5PwdDialogFragment pwdDialogFragment = STType5PwdDialogFragment.newInstance(
                        STType5PwdDialogFragment.STPwdAction.PRESENT_CURRENT_PWD,
                        passworNumber,
                        getResources().getString(R.string.enter_configuration_pwd));
                if(pwdDialogFragment!=null) pwdDialogFragment.show(fragmentManager, "pwdDialogFragment");
            }
        });

    }

    public void onSTType5PwdDialogFinish(int result) {
        Log.v(TAG, "onSTType5PwdDialogFinish. result = " + result);

        switch(mAction) {
            case READ_REGISTERS:
                if (result == PwdDialogFragment.RESULT_OK) {
                    // Config password has been entered successfully so we can now retry to read the register values
                    refreshRegisterList();
                } else {
                    Log.e(TAG, "Action failed! Register list not updated");
                    showToast(R.string.command_failed);
                }
                break;

            case WRITE_REGISTERS:
                if (result == PwdDialogFragment.RESULT_OK) {
                    // Config password has been entered successfully so we can now retry to write the register values
                    writeRegisters();
                } else {
                    Log.e(TAG, "Action failed! Tag not updated!");
                    showToast(R.string.register_action_not_completed);
                }
                break;
        }
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onNewIntent(Intent intent) {
        // onResume gets called after this to handle the intent
        setIntent(intent);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        return mMenu.selectItem(this, item);
    }


    static class ViewHolder{
        public TextView mRegisterNameTextView;
        public ImageView mRegisterRightImageView;
        public EditText mRegisterValueEditText;
        public TextView mRegisterDescriptionTextView;

        // Custom watcher used to activate or not the watching of EditText modification
        public MutableWatcher mWatcher;
    }

    class MutableWatcher implements TextWatcher {

        private int mPosition;
        private boolean mActive;
        private EditText mRegisterValueEditText;

        public MutableWatcher(EditText registerValueEditText) {
            this.mRegisterValueEditText = registerValueEditText;
        }

        void setPosition(int position) {
            mPosition = position;
        }

        void setActive(boolean active) {
            mActive = active;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) { }

        @Override
        public void afterTextChanged(Editable s) {
            if (mActive) {
                int newValue;

                try {
                    String valueEntered = s.toString();
                    if (valueEntered != null && !valueEntered.isEmpty()) {
                        newValue = Helper.convertHexStringToInt(valueEntered);
                    } else {
                        newValue = 0;
                    }
                } catch(STException e1) {
                    // String doesn't contain a number
                    return;
                }

                List<RegisterInfo> registerList = mAdapter.getData();
                RegisterInfo registerInfo = registerList.get(mPosition);
                int currentValue = 0;
                try {
                    currentValue = registerInfo.register.getRegisterValue();
                } catch (STException e) {
                    e.printStackTrace();
                }

                if(newValue != currentValue) {
                    // Value changed
                    mRegisterValueEditText.setBackgroundColor(getResources().getColor(R.color.light_red));
                    registerInfo.isValueUpdated = true;
                    registerInfo.value = newValue;

                } else {
                    // Value not changed
                    mRegisterValueEditText.setBackgroundColor(getResources().getColor(R.color.st_very_light_blue));
                    registerInfo.isValueUpdated = false;
                }
            }
        }
    }


    class CustomListAdapter extends BaseAdapter {
        List<RegisterInfo> mRegisterList;

        public CustomListAdapter(List<RegisterInfo> registerList) {
            mRegisterList = registerList;
        }

        public List<RegisterInfo> getData() {
            return mRegisterList;
        }

        @Override
        public int getCount() {
            if (mRegisterList != null) {
                return mRegisterList.size();
            } else {
                return 0;
            }
        }

        //get read_list_items position
        @Override
        public Object getItem(int position) {
            return position;
        }

        //get read_list_items id at selected position
        @Override
        public long getItemId(int position) {
            return position;
        }


        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {

            try {

                ViewHolder holder = null;
                if (convertView == null) {
                    convertView = getLayoutInflater().inflate(R.layout.config_registers_items, parent, false);

                    holder = new ViewHolder();

                    holder.mRegisterNameTextView = (TextView) convertView.findViewById(R.id.registerNameTextView);
                    holder.mRegisterRightImageView  = (ImageView) convertView.findViewById(R.id.registerRightImageView);
                    holder.mRegisterDescriptionTextView = (TextView) convertView.findViewById(R.id.registerDescriptionTextView);
                    holder.mRegisterValueEditText = (EditText) convertView.findViewById(R.id.registerValueEditText);

                    holder.mWatcher = new MutableWatcher(holder.mRegisterValueEditText);
                    holder.mRegisterValueEditText.addTextChangedListener(holder.mWatcher);

                    // Warning: This is not a NFC Tag but a simple 'tag' used to retrieve this ViewHolder
                    convertView.setTag(holder);
                } else{
                    holder = (ViewHolder) convertView.getTag();
                }

                holder.mWatcher.setPosition(position);

                RegisterInfo registerInfo = mRegisterList.get(position);
                STRegister register = registerInfo.register;

                if(registerInfo.isRegisterReadable) {
                    holder.mRegisterValueEditText.setTextColor(getResources().getColor(R.color.st_dark_blue));
                    holder.mRegisterValueEditText.setBackgroundColor(getResources().getColor(R.color.st_very_light_blue));
                    holder.mRegisterRightImageView.setVisibility(View.VISIBLE);

                    switch(register.getRegisterAccessRights()) {
                        case REGISTER_READ_ONLY:
                            holder.mRegisterValueEditText.setEnabled(false);
                            holder.mRegisterRightImageView.setImageResource(R.drawable.read_only);
                            break;
                        case REGISTER_READ_WRITE:
                            holder.mRegisterValueEditText.setEnabled(true);
                            holder.mRegisterRightImageView.setImageResource(R.drawable.read_write);
                            break;
                        case REGISTER_READ_WRITE_OTP:
                            holder.mRegisterValueEditText.setEnabled(true);
                            holder.mRegisterRightImageView.setImageResource(R.drawable.otp);
                            break;
                    }
                } else {
                    holder.mRegisterValueEditText.setEnabled(false);
                    holder.mRegisterValueEditText.setTextColor(getResources().getColor(R.color.red));
                    holder.mRegisterValueEditText.setBackgroundColor(Color.parseColor("#00FF0000"));    // Transparent color
                    holder.mRegisterRightImageView.setVisibility(View.INVISIBLE);
                }

                String registerName;

                // Add the register address
                if (register.isExtendedRegisterAddressingModeUsed()) {
                    registerName = String.format("%02x-%02x", register.getRegisterAddress(), register.getRegisterParameterAddress()).toUpperCase();
                } else {
                    registerName = "#" + position;
                }

                // Add the register name
                registerName += " " + register.getRegisterName();

                holder.mRegisterNameTextView.setText(registerName);
                holder.mRegisterDescriptionTextView.setText(register.getRegisterContentDescription());

                if (registerInfo.isRegisterReadable) {
                    String registerValueStr;
                    int registerValue = register.getRegisterValue();
                    if(registerInfo.isValueUpdated) {
                        registerValue = registerInfo.value;
                    }

                    switch (register.getRegisterDataSize()) {
                        default:
                        case REGISTER_DATA_ON_8_BITS:
                            registerValueStr = String.format("%02x", registerValue).toUpperCase();
                            break;
                        case REGISTER_DATA_ON_16_BITS:
                            registerValueStr = String.format("%04x", registerValue).toUpperCase();
                            break;
                        case REGISTER_DATA_ON_24_BITS:
                            registerValueStr = String.format("%06x", registerValue).toUpperCase();
                            break;
                        case REGISTER_DATA_ON_32_BITS:
                            registerValueStr = String.format("%08x", registerValue).toUpperCase();
                            break;
                    }
                    holder.mWatcher.setActive(true);
                    holder.mRegisterValueEditText.setText(registerValueStr);

                } else {
                    holder.mWatcher.setActive(true);
                    holder.mRegisterValueEditText.setText("Locked!");

                }

                return convertView;

            } catch (STException e) {
                e.printStackTrace();
                // TODO
                return null;
            }
        }
    }

    private class readRegisterValues extends AsyncTask<Void, Void, ActionStatus> {

        List<RegisterInfo> mRegisterInfoList = new ArrayList<RegisterInfo>();

        public readRegisterValues() {

        }

        @Override
        protected ActionStatus doInBackground(Void... param) {
            ActionStatus result;
            int lastFid;

            Log.d(TAG, "readRegisterValues");
            mAction = Action.READ_REGISTERS;

            // be sure that Tag implement RegisterInterface
            if (mRegisterInterface == null) {
                return ACTION_FAILED;
            }
            // Read all the registers in order to be sure that their value is present in cache
            List<STRegister> registerList;
            if (mForDynamicRegister) {
                registerList = mRegisterInterface.getDynamicRegisterList();
            } else {
                registerList = mRegisterInterface.getRegisterList();
            }

            for (STRegister register : registerList) {
                int registerAddress = register.getRegisterAddress();

                try {
                    // Read the register value
                    register.invalidateCache();
                    register.getRegisterValue();

                    // Create a new entry in mRegisterInfoList
                    RegisterInfo registerInfo = new RegisterInfo(register);
                    mRegisterInfoList.add(registerInfo);

                } catch (STException e) {
                    result = ACTION_FAILED;

                    switch (e.getError()) {
                        case CONFIG_PASSWORD_NEEDED:
                            if (mTag instanceof ST25TVCTag) {
                                try {
                                    // This failure can happen:
                                    // - if the Configuration password is needed
                                    // - if this FeatureId is locked
                                    lastFid = registerAddress;
                                    boolean isFidLocked = ((ST25TVCTag) mTag).isFeatureLocked(lastFid);
                                    if (isFidLocked) {
                                        // This FID is permanently locked
                                        RegisterInfo registerInfo = new RegisterInfo(register);
                                        registerInfo.setRegisterReadable(false);
                                        mRegisterInfoList.add(registerInfo);
                                        // Continue with next register
                                        continue;
                                    } else {
                                        // This FID is not locked. The Configuration password is needed to read it
                                        result = CONFIGURATION_PASSWORD_NEEDED;
                                    }
                                } catch (STException e2) {
                                    e2.printStackTrace();
                                }
                            } else {
                            }
                            break;
                        case TAG_NOT_IN_THE_FIELD:
                            result = TAG_NOT_IN_THE_FIELD;
                            break;
                        default:
                            e.printStackTrace();
                            break;
                    }
                    // No need to continue with the other registers
                    return result;
                }
            }

            return ACTION_SUCCESSFUL;
        }

        @Override
        protected void onPostExecute(ActionStatus actionStatus) {

            switch(actionStatus) {
                case ACTION_SUCCESSFUL:
                    Log.d(TAG, "readRegisterValues: ACTION_SUCCESSFUL");

                    // No status message to display
                    mStatusTextView.setVisibility(View.GONE);

                    // update data in our adapter
                    mAdapter.getData().clear();
                    mAdapter.getData().addAll(mRegisterInfoList);

                    mAdapter.notifyDataSetChanged();
                    break;

                case ACTION_FAILED:
                    showToast(R.string.error_while_reading_register_values);
                    mStatusTextView.setText(R.string.error_while_reading_the_tag);
                    mStatusTextView.setVisibility(View.VISIBLE);
                    break;

                case TAG_NOT_IN_THE_FIELD:
                    mStatusTextView.setText(R.string.tag_not_in_the_field);
                    mStatusTextView.setVisibility(View.VISIBLE);
                    break;

                case CONFIGURATION_PASSWORD_NEEDED:
                    STType5PwdDialogFragment pwdDialogFragment = STType5PwdDialogFragment.newInstance(
                            STType5PwdDialogFragment.STPwdAction.PRESENT_CURRENT_PWD,
                            configurationPasswordNumber,
                            getResources().getString(R.string.enter_configuration_pwd));
                    if(pwdDialogFragment!=null) pwdDialogFragment.show(mFragmentManager, "pwdDialogFragment");
                    break;
            }

            return;
        }
    }

}

