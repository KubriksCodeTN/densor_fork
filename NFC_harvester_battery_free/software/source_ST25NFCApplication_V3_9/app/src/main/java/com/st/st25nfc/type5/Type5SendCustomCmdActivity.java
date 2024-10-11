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

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.navigation.NavigationView;
import com.st.st25nfc.R;
import com.st.st25nfc.generic.MainActivity;
import com.st.st25nfc.generic.STFragmentActivity;
import com.st.st25sdk.Helper;
import com.st.st25sdk.RFReaderInterface;
import com.st.st25sdk.STException;
import com.st.st25sdk.command.Iso15693Protocol;
import com.st.st25sdk.type5.STType5Tag;
import com.st.st25sdk.type5.Type5Tag;
import com.st.st25sdk.type5.st25tv.ST25TVTag;

import static com.st.st25sdk.STException.STExceptionCode.CMD_FAILED;
import static com.st.st25sdk.command.Iso15693Command.ISO15693_CMD_INVENTORY;
import static com.st.st25sdk.command.Iso15693Command.ISO15693_CMD_STAY_QUIET;
import static com.st.st25sdk.command.Iso15693Command.ISO15693_CMD_READ_SINGLE_BLOCK;
import static com.st.st25sdk.command.Iso15693Command.ISO15693_CMD_WRITE_SINGLE_BLOCK;
import static com.st.st25sdk.command.Iso15693Command.ISO15693_CMD_LOCK_BLOCK;
import static com.st.st25sdk.command.Iso15693Command.ISO15693_CMD_READ_MULTIPLE_BLOCK;
import static com.st.st25sdk.command.Iso15693Command.ISO15693_CMD_WRITE_MULTIPLE_BLOCK;
import static com.st.st25sdk.command.Iso15693Command.ISO15693_CMD_SELECT;
import static com.st.st25sdk.command.Iso15693Command.ISO15693_CMD_RESET_TO_READY;
import static com.st.st25sdk.command.Iso15693Command.ISO15693_CMD_WRITE_AFI;
import static com.st.st25sdk.command.Iso15693Command.ISO15693_CMD_LOCK_AFI;
import static com.st.st25sdk.command.Iso15693Command.ISO15693_CMD_WRITE_DSFID;
import static com.st.st25sdk.command.Iso15693Command.ISO15693_CMD_LOCK_DSFID;
import static com.st.st25sdk.command.Iso15693Command.ISO15693_CMD_GET_SYSTEM_INFO;
import static com.st.st25sdk.command.Iso15693Command.ISO15693_CMD_GET_MULTIPLE_BLOCK_SEC_STATUS;
import static com.st.st25sdk.command.Iso15693Command.ISO15693_CMD_EXTENDED_READ_SINGLE_BLOCK;
import static com.st.st25sdk.command.Iso15693Command.ISO15693_CMD_EXTENDED_WRITE_SINGLE_BLOCK;
import static com.st.st25sdk.command.Iso15693Command.ISO15693_CMD_EXTENDED_LOCK_SINGLE_BLOCK;
import static com.st.st25sdk.command.Iso15693Command.ISO15693_CMD_EXTENDED_READ_MULTIPLE_BLOCK;
import static com.st.st25sdk.command.Iso15693Command.ISO15693_CMD_EXTENDED_WRITE_MULTIPLE_BLOCK;
import static com.st.st25sdk.command.Iso15693Command.ISO15693_CMD_EXTENDED_GET_SYSTEM_INFO;
import static com.st.st25sdk.command.Iso15693Command.ISO15693_CMD_EXTENDED_GET_MULTIPLE_BLOCK_SEC_STATUS;

import static com.st.st25sdk.command.Iso15693CustomCommand.ISO15693_CUSTOM_ST_CMD_READ_CONFIG;
import static com.st.st25sdk.command.Iso15693CustomCommand.ISO15693_CUSTOM_ST_CMD_WRITE_CONFIG;
import static com.st.st25sdk.command.Iso15693CustomCommand.ISO15693_CUSTOM_ST_CMD_READ_DYN_CONFIG;
import static com.st.st25sdk.command.Iso15693CustomCommand.ISO15693_CUSTOM_ST_CMD_WRITE_DYN_CONFIG;
import static com.st.st25sdk.command.Iso15693CustomCommand.ISO15693_CUSTOM_ST_CMD_WRITE_PASSWORD;
import static com.st.st25sdk.command.Iso15693CustomCommand.ISO15693_CUSTOM_ST_CMD_LOCK_SECTOR;
import static com.st.st25sdk.command.Iso15693CustomCommand.ISO15693_CUSTOM_ST_CMD_PRESENT_PASSWORD;
import static com.st.st25sdk.command.Iso15693CustomCommand.ISO15693_CUSTOM_ST_CMD_GET_RANDOM_NUMBER;
import static com.st.st25sdk.command.Iso15693CustomCommand.ISO15693_CUSTOM_ST_CMD_FAST_READ_SINGLE_BLOCK;
import static com.st.st25sdk.command.Iso15693CustomCommand.ISO15693_CUSTOM_ST_CMD_FAST_READ_MULTIPLE_BLOCK;
import static com.st.st25sdk.command.Iso15693CustomCommand.ISO15693_CUSTOM_ST_CMD_FAST_READ_DYN_CONFIG;
import static com.st.st25sdk.command.Iso15693CustomCommand.ISO15693_CUSTOM_ST_CMD_FAST_WRITE_DYN_CONFIG;
import static com.st.st25sdk.command.Iso15693CustomCommand.ISO15693_CUSTOM_ST_CMD_FAST_EXTENDED_READ_SINGLE_BLOCK;
import static com.st.st25sdk.command.Iso15693CustomCommand.ISO15693_CUSTOM_ST_CMD_FAST_EXTENDED_READ_MULTIPLE_BLOCK;
import static com.st.st25sdk.command.Iso15693CustomCommand.ISO15693_CUSTOM_ST_CMD_KILL;
import static com.st.st25sdk.command.Iso15693CustomCommand.ISO15693_CUSTOM_ST_CMD_WRITE_KILL;
import static com.st.st25sdk.command.Iso15693CustomCommand.ISO15693_CUSTOM_ST_CMD_LOCK_KILL;
import static com.st.st25sdk.command.Iso15693CustomCommand.ISO15693_CUSTOM_ST_PARAM_KILL_ACCESS;
import static com.st.st25sdk.command.Iso15693CustomCommand.ISO15693_CUSTOM_ST_CMD_INVENTORY_INITIATED;
import static com.st.st25sdk.command.Iso15693CustomCommand.ISO15693_CUSTOM_ST_CMD_INITIATE;
import static com.st.st25sdk.command.Iso15693CustomCommand.ISO15693_CUSTOM_ST_CMD_FAST_INITIATE;
import static com.st.st25sdk.command.Iso15693CustomCommand.ISO15693_CUSTOM_ST_CMD_FAST_INVENTORY_INITIATED;
import static com.st.st25sdk.command.Iso15693CustomCommand.ISO15693_CUSTOM_ST_CMD_WRITE_EH_CFG;
import static com.st.st25sdk.command.Iso15693CustomCommand.ISO15693_CUSTOM_ST_CMD_SET_RST_EH_EN;
import static com.st.st25sdk.command.Iso15693CustomCommand.ISO15693_CUSTOM_ST_CMD_CHECK_EH_EN;
import static com.st.st25sdk.command.Iso15693CustomCommand.ISO15693_CUSTOM_ST_CMD_WRITE_DO_CFG;
import static com.st.st25sdk.command.Iso15693CustomCommand.ISO15693_CUSTOM_ST_CMD_MANAGE_GPO;
import static com.st.st25sdk.command.Iso15693CustomCommand.ISO15693_CUSTOM_ST_CMD_MB_WRITE_MSG;
import static com.st.st25sdk.command.Iso15693CustomCommand.ISO15693_CUSTOM_ST_CMD_MB_READ_MSG_LENGTH;
import static com.st.st25sdk.command.Iso15693CustomCommand.ISO15693_CUSTOM_ST_CMD_MB_READ_MSG;
import static com.st.st25sdk.command.Iso15693CustomCommand.ISO15693_CUSTOM_ST_CMD_MB_FAST_WRITE_MSG;
import static com.st.st25sdk.command.Iso15693CustomCommand.ISO15693_CUSTOM_ST_CMD_MB_FAST_READ_MSG_LENGTH;
import static com.st.st25sdk.command.Iso15693CustomCommand.ISO15693_CUSTOM_ST_CMD_MB_FAST_READ_MSG;
import static com.st.st25sdk.command.Iso15693CustomCommand.ISO15693_CUSTOM_ST_CMD_SET_EAS;
import static com.st.st25sdk.command.Iso15693CustomCommand.ISO15693_CUSTOM_ST_CMD_RESET_EAS;
import static com.st.st25sdk.command.Iso15693CustomCommand.ISO15693_CUSTOM_ST_CMD_LOCK_EAS;
import static com.st.st25sdk.command.Iso15693CustomCommand.ISO15693_CUSTOM_ST_CMD_ENABLE_EAS;
import static com.st.st25sdk.command.Iso15693CustomCommand.ISO15693_CUSTOM_ST_CMD_WRITE_EAS_ID;
import static com.st.st25sdk.command.Iso15693CustomCommand.ISO15693_CUSTOM_ST_CMD_WRITE_EAS_CONFIG;
import static com.st.st25sdk.command.Iso15693CustomCommand.ISO15693_CUSTOM_ST_CMD_ENABLE_UNTRACEABLE_MODE;
import static com.st.st25sdk.command.Iso15693CustomCommand.ISO15693_CUSTOM_ST_CMD_TOGGLE_UNTRACEABLE_MODE;
import static com.st.st25sdk.command.Iso15693CustomCommand.ISO15693_CUSTOM_ST_CMD_INVENTORY_READ;
import static com.st.st25sdk.command.Iso15693CustomCommand.ISO15693_CUSTOM_ST_CMD_FAST_INVENTORY_READ;
import static com.st.st25sdk.command.Iso15693CustomCommand.ISO15693_CUSTOM_ST_CMD_READ_SIGNATURE;

import static com.st.st25nfc.type5.Type5SendCustomCmdActivity.ActionStatus.ACTION_FAILED;
import static com.st.st25nfc.type5.Type5SendCustomCmdActivity.ActionStatus.ACTION_SUCCESSFUL;
import static com.st.st25nfc.type5.Type5SendCustomCmdActivity.ActionStatus.TAG_NOT_IN_THE_FIELD;


public class Type5SendCustomCmdActivity extends STFragmentActivity implements NavigationView.OnNavigationItemSelectedListener {

    // Set here the Toolbar to use for this activity
    private int toolbar_res = R.menu.toolbar_send;

    enum Action {
        SEND_CMD
    }

    enum ActionStatus {
        ACTION_SUCCESSFUL,
        ACTION_FAILED,
        TAG_NOT_IN_THE_FIELD
    };

    static final String TAG = "SendCustomCmd";
    private Handler mHandler;
    FragmentManager mFragmentManager;
    private SharedPreferences mSharedPreferences;

    private Type5Tag mType5Tag;
    private Iso15693Protocol mIso15693Protocol;

    private byte[] mUid;
    private byte[] mGeneratedCmd;

    private TextView mFlagTextView;
    private EditText mCmdEditText;
    private CheckBox mManufacturerCheckbox;
    private EditText mManufacturerEditText;
    private EditText mDataEditText;
    private CheckBox mOptionFlagCheckbox;
    private CheckBox mAddressedModeCheckbox;
    private CheckBox mSelectModeCheckbox;
    private CheckBox mHighDataRateCheckbox;
    private CheckBox mFormatExtensionCheckbox;
    private TextView mCommandTextView;
    private TextView mResultTextView;
    private TextView mTagResponseTextView;
    private TextView mCmdNameText;

    private final String CUSTOM_CMD_FLAG = "CUSTOM_CMD_FLAG";
    private final String CUSTOM_CMD_CODE = "CUSTOM_CMD_CODE";
    private final String CUSTOM_CMD_MANUFACTURER = "CUSTOM_CMD_MANUFACTURER";
    private final String CUSTOM_CMD_DATA = "CUSTOM_CMD_DATA";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.default_layout);

        // Inflate content of FrameLayout
        FrameLayout frameLayout=(FrameLayout) findViewById(R.id.frame_content);
        View childView = getLayoutInflater().inflate(R.layout.type5_send_custom_cmd_activity, null);
        frameLayout.addView(childView);

        mType5Tag = (Type5Tag) MainActivity.getTag();
        if (mType5Tag == null) {
            showToast(R.string.invalid_tag);
            goBackToMainActivity();
            return;
        }

        mHandler = new Handler();
        mFragmentManager = getSupportFragmentManager();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.navigation_view);
        navigationView.setNavigationItemSelectedListener(this);
        mMenu.inflateMenu(navigationView);

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        mFlagTextView = findViewById(R.id.flagTextView);
        mCommandTextView = findViewById(R.id.commandTextView);
        mResultTextView = findViewById(R.id.resultTextView);
        mTagResponseTextView = findViewById(R.id.tagResponseTextView);
        mCmdNameText = findViewById(R.id.cmdNameText);

        mManufacturerEditText = findViewById(R.id.manufacturerEditText);
        mManufacturerEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {}

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                clearTagResponseAndResult();
                buildCommand();
            }
        });

        mManufacturerCheckbox = findViewById(R.id.manufacturerCheckbox);
        mManufacturerCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                            clearTagResponseAndResult();
                            mManufacturerEditText.setEnabled(isChecked);
                            if (isChecked) {
                                mManufacturerEditText.setBackgroundColor(getResources().getColor(R.color.st_very_light_blue));
                            } else {
                                mManufacturerEditText.setBackgroundColor(getResources().getColor(R.color.st_very_light_grey));
                            }
                            buildCommand();
                        }
                    }
        );

        mCmdEditText = findViewById(R.id.cmdEditText);
        mCmdEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {}

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                clearTagResponseAndResult();
                buildCommand();
                setCommandName();
            }
        });

        mDataEditText = findViewById(R.id.dataEditText);
        mDataEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {}

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                clearTagResponseAndResult();
                buildCommand();
            }
        });

        LinearLayout uidLinearLayout = findViewById(R.id.uidLinearLayout);

        mOptionFlagCheckbox = findViewById(R.id.optionFlagCheckbox);
        mOptionFlagCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                                                          @Override
                                                          public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                                              clearTagResponseAndResult();
                                                              int value = Integer.parseInt(mFlagTextView.getText().toString(), 16);
                                                              if(isChecked) {
                                                                  value |= 0x40;
                                                              } else {
                                                                  value &= ~(0x40);
                                                              }
                                                              mFlagTextView.setText(String.format("%02x", value).toUpperCase());
                                                              buildCommand();
                                                          }
                                                      }
        );

        mAddressedModeCheckbox = findViewById(R.id.addressedModeCheckbox);
        mAddressedModeCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                                                          @Override
                                                          public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                                              clearTagResponseAndResult();
                                                              int value = Integer.parseInt(mFlagTextView.getText().toString(), 16);
                                                              if(isChecked) {
                                                                  value |= 0x20;
                                                              } else {
                                                                  value &= ~(0x20);
                                                              }
                                                              mFlagTextView.setText(String.format("%02x", value).toUpperCase());

                                                              if(isChecked) {
                                                                  uidLinearLayout.setVisibility(View.VISIBLE);
                                                              } else {
                                                                  uidLinearLayout.setVisibility(View.GONE);
                                                              }
                                                              buildCommand();
                                                          }
                                                      }
        );

        mSelectModeCheckbox = findViewById(R.id.selectModeCheckbox);
        mSelectModeCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                                                          @Override
                                                          public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                                              clearTagResponseAndResult();
                                                              int value = Integer.parseInt(mFlagTextView.getText().toString(), 16);
                                                              if(isChecked) {
                                                                  value |= 0x10;
                                                              } else {
                                                                  value &= ~(0x10);
                                                              }
                                                              mFlagTextView.setText(String.format("%02x", value).toUpperCase());
                                                              buildCommand();
                                                          }
                                                      }
        );

        mHighDataRateCheckbox = findViewById(R.id.highDataRateCheckbox);
        mHighDataRateCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                                                          @Override
                                                          public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                                              clearTagResponseAndResult();
                                                              int value = Integer.parseInt(mFlagTextView.getText().toString(), 16);
                                                              if(isChecked) {
                                                                  value |= 0x02;
                                                              } else {
                                                                  value &= ~(0x02);
                                                              }
                                                              mFlagTextView.setText(String.format("%02x", value).toUpperCase());
                                                              buildCommand();
                                                          }
                                                      }
        );

        mFormatExtensionCheckbox = findViewById(R.id.formatExtensionCheckbox);
        mFormatExtensionCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                                                          @Override
                                                          public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                                              clearTagResponseAndResult();
                                                              int value = Integer.parseInt(mFlagTextView.getText().toString(), 16);
                                                              if(isChecked) {
                                                                  value |= 0x08;
                                                              } else {
                                                                  value &= ~(0x08);
                                                              }
                                                              mFlagTextView.setText(String.format("%02x", value).toUpperCase());
                                                              buildCommand();
                                                          }
                                                      }
        );

        TextView uidTextView = findViewById(R.id.uidTextView);

        String uid = "";
        try {
            mUid = Helper.reverseByteArray(mType5Tag.getUid());
            uid = Helper.convertHexByteArrayToString(mUid).toUpperCase();
        } catch (STException e) {
            e.printStackTrace();
        }
        uidTextView.setText(uid);

        String lastFlag = mSharedPreferences.getString(CUSTOM_CMD_FLAG, "22");
        byte flagValue = (byte) Integer.parseInt(lastFlag, 16);
        if ((flagValue & Iso15693Protocol.OPTION_FLAG) == Iso15693Protocol.OPTION_FLAG) {
            mOptionFlagCheckbox.setChecked(true);
        }
        if ((flagValue & Iso15693Protocol.ADDRESSED_MODE) == Iso15693Protocol.ADDRESSED_MODE) {
            mAddressedModeCheckbox.setChecked(true);
        }
        if ((flagValue & Iso15693Protocol.SELECTED_MODE) == Iso15693Protocol.SELECTED_MODE) {
            mSelectModeCheckbox.setChecked(true);
        }
        if ((flagValue & Iso15693Protocol.HIGH_DATA_RATE_MODE) == Iso15693Protocol.HIGH_DATA_RATE_MODE) {
            mHighDataRateCheckbox.setChecked(true);
        }
        if ((flagValue & Iso15693Protocol.PROTOCOL_FORMAT_EXTENSION) == Iso15693Protocol.PROTOCOL_FORMAT_EXTENSION) {
            mFormatExtensionCheckbox.setChecked(true);
        }

        String lastCmdCode = mSharedPreferences.getString(CUSTOM_CMD_CODE, "20");
        mCmdEditText.setText(lastCmdCode);

        String lastManufacturer = mSharedPreferences.getString(CUSTOM_CMD_MANUFACTURER, "");
        if (lastManufacturer.isEmpty()) {
            mManufacturerEditText.setText("02");
            mManufacturerEditText.setEnabled(false);
            mManufacturerEditText.setBackgroundColor(getResources().getColor(R.color.st_very_light_grey));

            mManufacturerCheckbox.setChecked(false);
        } else {
            mManufacturerEditText.setEnabled(true);
            mManufacturerEditText.setText(lastManufacturer);
            mManufacturerEditText.setBackgroundColor(getResources().getColor(R.color.st_very_light_blue));

            mManufacturerCheckbox.setChecked(true);
        }

        String lastData = mSharedPreferences.getString(CUSTOM_CMD_DATA, "00");
        mDataEditText.setText(lastData);

        buildCommand();

        try {
            RFReaderInterface rfReaderInterface = mType5Tag.getReaderInterface();
            byte[] tagUid = mType5Tag.getUid();
            byte flag = Iso15693Protocol.DEFAULT_FLAG;
            int nbrOfBytesPerBlock = mType5Tag.getBlockSizeInBytes();
            mIso15693Protocol = new Iso15693Protocol(rfReaderInterface, tagUid, flag, nbrOfBytesPerBlock);
        } catch (STException e) {
            e.printStackTrace();
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds read_list_items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.toolbar_send, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                mCmdEditText.setText("20");
                mManufacturerEditText.setText("02");
                mManufacturerCheckbox.setChecked(false);
                mDataEditText.setText("00");
                mOptionFlagCheckbox.setChecked(false);
                mAddressedModeCheckbox.setChecked(true);
                mSelectModeCheckbox.setChecked(false);
                mHighDataRateCheckbox.setChecked(true);
                mFormatExtensionCheckbox.setChecked(false);
                clearTagResponseAndResult();
                return true;

            case R.id.action_send:
                saveCurrentSettings();
                new myAsyncTask(Action.SEND_CMD).execute();
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        return mMenu.selectItem(this, item);
    }

    private void saveCurrentSettings() {

        String flag = mFlagTextView.getText().toString();
        String cmd = mCmdEditText.getText().toString();
        String data = mDataEditText.getText().toString();
        String manufacturer = mManufacturerEditText.getText().toString();

        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString(CUSTOM_CMD_FLAG, flag);
        editor.putString(CUSTOM_CMD_CODE, cmd);
        if (mManufacturerCheckbox.isChecked()) {
            editor.putString(CUSTOM_CMD_MANUFACTURER, manufacturer);
        } else {
            editor.putString(CUSTOM_CMD_MANUFACTURER, "");
        }
        editor.putString(CUSTOM_CMD_DATA, data);
        editor.apply();
    }

    private void setCommandName() {
        String cmdName = "'Cmd unknown'";
        byte cmd = 0x0;
        try {
            cmd = (byte) Integer.parseInt(mCmdEditText.getText().toString(), 16);
        } catch (NumberFormatException e) {

        }

        switch(cmd) {
            case ISO15693_CMD_INVENTORY:
                cmdName = "'Inventory'";
                break;
            case ISO15693_CMD_STAY_QUIET:
                cmdName = "'StayQuiet'";
                break;
            case ISO15693_CMD_READ_SINGLE_BLOCK:
                cmdName = "'ReadSingleBlock'";
                break;
            case ISO15693_CMD_WRITE_SINGLE_BLOCK:
                cmdName = "'WriteSingleBlock'";
                break;
            case ISO15693_CMD_LOCK_BLOCK:
                cmdName = "'LockBlock'";
                break;
            case ISO15693_CMD_READ_MULTIPLE_BLOCK:
                cmdName = "'ReadMultipleBlock'";
                break;
            case ISO15693_CMD_WRITE_MULTIPLE_BLOCK:
                cmdName = "'WriteMultipleBlock'";
                break;
            case ISO15693_CMD_SELECT:
                cmdName = "'CmdSelect'";
                break;
            case ISO15693_CMD_RESET_TO_READY:
                cmdName = "'ResetToReady'";
                break;
            case ISO15693_CMD_WRITE_AFI:
                cmdName = "'WriteAFI'";
                break;
            case ISO15693_CMD_LOCK_AFI:
                cmdName = "'LockAFI'";
                break;
            case ISO15693_CMD_WRITE_DSFID:
                cmdName = "'WriteDSFID'";
                break;
            case ISO15693_CMD_LOCK_DSFID:
                cmdName = "'LockDSFID'";
                break;
            case ISO15693_CMD_GET_SYSTEM_INFO:
                cmdName = "'GetSystemInfo'";
                break;
            case ISO15693_CMD_GET_MULTIPLE_BLOCK_SEC_STATUS:
                cmdName = "'GetMultipleBlockSecStatus'";
                break;
            case ISO15693_CMD_EXTENDED_READ_SINGLE_BLOCK:
                cmdName = "'ExtendedReadSingleBlock'";
                break;
            case ISO15693_CMD_EXTENDED_WRITE_SINGLE_BLOCK:
                cmdName = "'ExtendedWriteSingleBlock'";
                break;
            case ISO15693_CMD_EXTENDED_LOCK_SINGLE_BLOCK:
                cmdName = "'ExtendedLockSingleBlock'";
                break;
            case ISO15693_CMD_EXTENDED_READ_MULTIPLE_BLOCK:
                cmdName = "'ExtendedReadMultipleBlock'";
                break;
            case ISO15693_CMD_EXTENDED_WRITE_MULTIPLE_BLOCK:
                cmdName = "'ExtendedWriteMultipleBlock'";
                break;
            case ISO15693_CMD_EXTENDED_GET_SYSTEM_INFO:
                cmdName = "'ExtendedGetSystemInfo'";
                break;
            case ISO15693_CMD_EXTENDED_GET_MULTIPLE_BLOCK_SEC_STATUS:
                cmdName = "'ExtendedGetMultipleBlockSecStatus'";
                break;

            default:
                // Unknown command
                break;
        }

        if (mType5Tag instanceof STType5Tag) {
            switch(cmd) {
                case ISO15693_CUSTOM_ST_CMD_READ_CONFIG:
                    cmdName = "'ReadConfig'";
                    break;
                case ISO15693_CUSTOM_ST_CMD_WRITE_CONFIG:
                    cmdName = "'WriteCfg/WriteEHCfg'";
                    break;
                case ISO15693_CUSTOM_ST_CMD_READ_DYN_CONFIG:
                    cmdName = "'ReadDynConfig'";
                    break;
                case ISO15693_CUSTOM_ST_CMD_WRITE_DYN_CONFIG:
                    cmdName = "'WriteDynConfig'";
                    break;
                case ISO15693_CUSTOM_ST_CMD_WRITE_PASSWORD:
                    cmdName = "'WritePwd/WriteKill'";
                    break;
                case ISO15693_CUSTOM_ST_CMD_PRESENT_PASSWORD:
                    cmdName = "'PresentPwd'";
                    break;
                case ISO15693_CUSTOM_ST_CMD_GET_RANDOM_NUMBER:
                    cmdName = "'GetRandomNbr'";
                    break;
                case ISO15693_CUSTOM_ST_CMD_FAST_READ_SINGLE_BLOCK:
                    cmdName = "'FastReadSingleBlock'";
                    break;
                case ISO15693_CUSTOM_ST_CMD_FAST_READ_MULTIPLE_BLOCK:
                    cmdName = "'FastReadMultipleBlock'";
                    break;
                case ISO15693_CUSTOM_ST_CMD_FAST_READ_DYN_CONFIG:
                    cmdName = "'FastReadDynConfig'";
                    break;
                case ISO15693_CUSTOM_ST_CMD_FAST_WRITE_DYN_CONFIG:
                    cmdName = "'FastWriteDynConfig'";
                    break;
                case ISO15693_CUSTOM_ST_CMD_FAST_EXTENDED_READ_SINGLE_BLOCK:
                    cmdName = "'FastExtendedReadSingleBlock'";
                    break;
                case ISO15693_CUSTOM_ST_CMD_FAST_EXTENDED_READ_MULTIPLE_BLOCK:
                    cmdName = "'FastExtendedReadMultipleBlock'";
                    break;
                case ISO15693_CUSTOM_ST_CMD_KILL:
                    cmdName = "'CmdKill'";
                    break;
                case ISO15693_CUSTOM_ST_CMD_LOCK_KILL:
                    cmdName = "'LockKill/LockSector'";
                    break;
                case ISO15693_CUSTOM_ST_PARAM_KILL_ACCESS:
                    cmdName = "'ParamKillAccess'";
                    break;
                case ISO15693_CUSTOM_ST_CMD_INVENTORY_INITIATED:
                    cmdName = "'InventoryInitiated'";
                    break;
                case ISO15693_CUSTOM_ST_CMD_INITIATE:
                    cmdName = "'Initiate'";
                    break;
                case ISO15693_CUSTOM_ST_CMD_FAST_INITIATE:
                    cmdName = "'FastInitiate'";
                    break;
                case ISO15693_CUSTOM_ST_CMD_FAST_INVENTORY_INITIATED:
                    cmdName = "'FastInventoryInitiated'";
                    break;
                case ISO15693_CUSTOM_ST_CMD_SET_RST_EH_EN:
                    cmdName = "'SetRstEHEn'";
                    break;
                case ISO15693_CUSTOM_ST_CMD_CHECK_EH_EN:
                    cmdName = "'CheckEHEn'";
                    break;
                case ISO15693_CUSTOM_ST_CMD_WRITE_DO_CFG:
                    cmdName = "'WriteDoConfig'";
                    break;
                case ISO15693_CUSTOM_ST_CMD_MANAGE_GPO:
                    cmdName = "'ManageGpo'";
                    break;
                case ISO15693_CUSTOM_ST_CMD_MB_WRITE_MSG:
                    cmdName = "'WriteMsg'";
                    break;
                case ISO15693_CUSTOM_ST_CMD_MB_READ_MSG_LENGTH:
                    cmdName = "'MBReadMsgLength'";
                    break;
                case ISO15693_CUSTOM_ST_CMD_MB_READ_MSG:
                    cmdName = "'MBReadMsg'";
                    break;
                case ISO15693_CUSTOM_ST_CMD_MB_FAST_WRITE_MSG:
                    cmdName = "'MBFastWriteMsg'";
                    break;
                case ISO15693_CUSTOM_ST_CMD_MB_FAST_READ_MSG_LENGTH:
                    cmdName = "'MBFastReadMsgLength'";
                    break;
                case ISO15693_CUSTOM_ST_CMD_MB_FAST_READ_MSG:
                    cmdName = "'MBFastReadMsg'";
                    break;
                case ISO15693_CUSTOM_ST_CMD_ENABLE_UNTRACEABLE_MODE:
                    cmdName = "'Enable/ToggleUntraceableMode'";
                    break;
                case ISO15693_CUSTOM_ST_CMD_INVENTORY_READ:
                    cmdName = "'InventoryRead'";
                    break;
                case ISO15693_CUSTOM_ST_CMD_FAST_INVENTORY_READ:
                    cmdName = "'FastInventoryRead'";
                    break;
                case ISO15693_CUSTOM_ST_CMD_READ_SIGNATURE:
                    cmdName = "'ReadSignature'";
                    break;

                default:
                    break;
            }
        }

        if (mType5Tag instanceof ST25TVTag) {
            switch (cmd) {
                case ISO15693_CUSTOM_ST_CMD_SET_EAS:
                    cmdName = "'SetEAS'";
                    break;
                case ISO15693_CUSTOM_ST_CMD_RESET_EAS:
                    cmdName = "'ResetEAS'";
                    break;
                case ISO15693_CUSTOM_ST_CMD_LOCK_EAS:
                    cmdName = "'LockEAS'";
                    break;
                case ISO15693_CUSTOM_ST_CMD_ENABLE_EAS:
                    cmdName = "'Enable EAS'";
                    break;
                case ISO15693_CUSTOM_ST_CMD_WRITE_EAS_ID:
                    cmdName = "'WriteEASId'";
                    break;
                case ISO15693_CUSTOM_ST_CMD_WRITE_EAS_CONFIG:
                    cmdName = "'WriteEASConfig'";
                    break;

                default:
                    break;
            }
        }

        mCmdNameText.setText(cmdName);
    }


    private void buildCommand() {
        byte flag = (byte) Integer.parseInt(mFlagTextView.getText().toString(), 16);

        byte cmd = 0x0;
        try {
            cmd = (byte) Integer.parseInt(mCmdEditText.getText().toString(), 16);
        } catch (NumberFormatException e) {

        }

        mGeneratedCmd = new byte[] { flag, cmd };

        // Manufacturer
        byte manufacturer = 0x0;
        try {
            manufacturer = (byte) Integer.parseInt(mManufacturerEditText.getText().toString(), 16);
        } catch (NumberFormatException e) {

        }

        if (mManufacturerCheckbox.isChecked()) {
            mGeneratedCmd = Helper.concatenateByteArrays(mGeneratedCmd, new byte[] { manufacturer });
        }

        // mUid
        String txtData = mDataEditText.getText().toString();
        if ((txtData.length() % 2) == 1) {
            // Odd length
            txtData += "0";
        }
        byte[] data = Helper.convertHexStringToByteArray(txtData);

        if (mAddressedModeCheckbox.isChecked()) {
            mGeneratedCmd = Helper.concatenateByteArrays(mGeneratedCmd, mUid);
        }
        mGeneratedCmd = Helper.concatenateByteArrays(mGeneratedCmd, data);

        mCommandTextView.setText(Helper.convertHexByteArrayToString(mGeneratedCmd).toUpperCase());

    }

    private void clearTagResponseAndResult() {
        mResultTextView.setText("");
        mTagResponseTextView.setText("");
    }

    private class myAsyncTask extends AsyncTask<Void, Void, ActionStatus> {
        Action mAction;
        byte[] mTagResponse;
        STException.STExceptionCode mErrorCode;
        byte[] mErrorData;

        public myAsyncTask(Action action) {
            mAction = action;
        }

        @Override
        protected ActionStatus doInBackground(Void... param) {
            ActionStatus result;

            try {
                switch(mAction) {
                    case SEND_CMD:
                        mTagResponse = mIso15693Protocol.transceive("custom_cmd", mGeneratedCmd);
                        result = ACTION_SUCCESSFUL;
                        break;

                    default:
                        Log.e(TAG, "Unknown action!");
                        result = ACTION_FAILED;
                        break;
                }

            } catch (STException e) {
                mErrorCode = e.getError();
                mErrorData = e.getErrorData();

                switch (e.getError()) {
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
                    switch(mAction) {
                        case SEND_CMD:
                            mResultTextView.setText(R.string.command_successful);
                            mResultTextView.setTextColor(getResources().getColor(R.color.st_light_green));
                            mTagResponseTextView.setText(Helper.convertHexByteArrayToString(mTagResponse).toUpperCase());
                            break;
                    }
                    break;

                case ACTION_FAILED:
                    String status = "Error " + Helper.convertHexByteArrayToString(mErrorData) + " (" + mErrorCode.toString() + ")";
                    mResultTextView.setText(status);
                    mResultTextView.setTextColor(getResources().getColor(R.color.red));
                    mTagResponseTextView.setText("");
                    break;

                case TAG_NOT_IN_THE_FIELD:
                    mResultTextView.setText(R.string.tag_not_in_the_field);
                    mResultTextView.setTextColor(getResources().getColor(R.color.red));
                    mTagResponseTextView.setText("");
                    break;
            }

            return;
        }
    }

}
