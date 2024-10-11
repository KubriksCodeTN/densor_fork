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
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager.widget.ViewPager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.navigation.NavigationView;
import com.st.st25nfc.R;
import com.st.st25nfc.generic.MainActivity;
import com.st.st25nfc.generic.PwdDialogFragment;
import com.st.st25nfc.generic.STFragmentActivity;
import com.st.st25nfc.generic.STType5PwdDialogFragment;
import com.st.st25nfc.generic.util.UIHelper;
import com.st.st25sdk.Helper;
import com.st.st25sdk.STException;
import com.st.st25sdk.type5.st25tvc.ST25TVCTag;

import static com.st.st25nfc.type5.st25tvc.ST25TVCTamperDetectActivity.ActionStatus.*;


public class ST25TVCTamperDetectActivity extends STFragmentActivity implements NavigationView.OnNavigationItemSelectedListener {

    enum Action {
        READ_CURRENT_TD_CONFIGURATION,
        WRITE_TD_CHARACTERS,
        WRITE_TD_HISTORY,
        ENABLE_TD_HISTORY
    };

    enum ActionStatus {
        ACTION_SUCCESSFUL,
        ACTION_FAILED,
        TAG_NOT_IN_THE_FIELD,
        CONFIG_PASSWORD_NEEDED,
        TAMPER_DETECT_CONFIGURATION_LOCKED
    };

    // Set here the Toolbar to use for this activity
    private int toolbar_res = R.menu.toolbar_save;

    static final String TAG = "TD_Activity";
    private ST25TVCTag mST25TVCTag;
    FragmentManager mFragmentManager;

    boolean mIsTamperDetectLocked;
    boolean mIsTdEventUpdateEnabled;
    private Action mCurrentAction;

    Button mChangeTdCharactersButton;
    CheckBox mEnableHistoryCheckbox;
    ImageView mTamperImageView;
    TextView mTdStatusTextView;
    TextView mTdStatusCaptionTextView;
    TextView mTdEventTextView1;
    TextView mTdEventTextView;
    TextView mTdEventCaptionTextView;

    String mTdStatusLoop = "";
    String mTdEventStatus = "";
    String mTdOpenMsg = "";
    String mTdShortMsg = "";
    String mTdSealMsg = "";
    String mTdUnsealMsg = "";
    String mTdResealMsg = "";

    String mTdWireOpenMsgToWrite = "";
    String mTdWireShortMsgToWrite = "";
    String mTdSealMsgToWrite = "";
    String mTdUnsealMsgToWrite = "";
    String mTdResealMsgToWrite = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.default_layout);

        // Inflate content of FrameLayout
        FrameLayout frameLayout = findViewById(R.id.frame_content);
        View childView = getLayoutInflater().inflate(R.layout.fragment_st25tvc_tamper_detect, null);
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

        mEnableHistoryCheckbox = findViewById(R.id.enableHistoryCheckbox);
        mTamperImageView = findViewById(R.id.tamperImageView);
        mTdStatusTextView = findViewById(R.id.tdStatusTextView);
        mTdStatusCaptionTextView = findViewById(R.id.tdStatusCaptionTextView);
        mTdEventTextView1 = findViewById(R.id.tdEventTextView1);
        mTdEventTextView = findViewById(R.id.tdEventTextView);
        mTdEventCaptionTextView = findViewById(R.id.tdEventCaptionTextView);

        mTdStatusTextView.setText("");
        mTdStatusCaptionTextView.setText("");
        mTdEventTextView.setText("");
        mTdEventCaptionTextView.setText("");

        mChangeTdCharactersButton = findViewById(R.id.changeTdCharactersButton);
        mChangeTdCharactersButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayTamperDetectDialogBox();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        boolean tagChanged = tagChanged(this, mST25TVCTag);
        if(!tagChanged) {
            executeAsynchronousAction(Action.READ_CURRENT_TD_CONFIGURATION);
        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds read_list_items to the action bar if it is present.
        if (!mIsTamperDetectLocked) {
            getMenuInflater().inflate(R.menu.toolbar_save, menu);
            MenuItem item = menu.findItem(R.id.action_save);

            item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    switch (item.getItemId()) {
                        case R.id.action_save:
                            executeAsynchronousAction(Action.WRITE_TD_HISTORY);
                            break;
                    }
                    return false;
                }

            });
        }

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

        mEnableHistoryCheckbox.setChecked(mIsTdEventUpdateEnabled);

        if (mTdStatusLoop.equals(mTdOpenMsg)) {
            // Tamper Detect open
            mTdStatusTextView.setText(mTdStatusLoop);
            mTdStatusCaptionTextView.setText(R.string.open);
            mTamperImageView.setImageResource(R.drawable.st25tvc_tamper_detect_open);
        } else if (mTdStatusLoop.equals(mTdShortMsg)) {
            // Tamper Detect short
            mTdStatusTextView.setText(mTdStatusLoop);
            mTdStatusCaptionTextView.setText(R.string.short_closed);
            mTamperImageView.setImageResource(R.drawable.st25tvc_tamper_detect_close);
        } else {
            // Invalid Tamper Detect status
            mTdStatusTextView.setText("");
            mTdStatusCaptionTextView.setText("");
            mTamperImageView.setImageResource(R.drawable.st25tvc_tamper_detect_no_info);
        }

        if(mIsTdEventUpdateEnabled) {
            mTdEventTextView1.setTextColor(getResources().getColor(R.color.st_dark_blue));
            if (mTdEventStatus.equals(mTdSealMsg)) {
                mTdEventTextView.setText(mTdEventStatus);
                mTdEventCaptionTextView.setText(R.string.sealed);
            } else if (mTdEventStatus.equals(mTdUnsealMsg)) {
                mTdEventTextView.setText(mTdEventStatus);
                mTdEventCaptionTextView.setText(R.string.unsealed);
            } else if (mTdEventStatus.equals(mTdResealMsg)) {
                mTdEventTextView.setText(mTdEventStatus);
                mTdEventCaptionTextView.setText(R.string.resealed);
            } else {
                // Invalid Tamper Detect status
                mTdEventTextView.setText("");
                mTdEventCaptionTextView.setText("");
            }
        } else {
            // TD History disabled
            mTdEventTextView1.setTextColor(getResources().getColor(R.color.st_light_grey));
            mTdEventTextView.setText("");
            mTdEventCaptionTextView.setText("");
        }


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

    private void displayTamperDetectDialogBox() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final AlertDialog dialog = builder.create();

        // set title
        //alertDialog.setTitle(getString(R.string.XXX));

        dialog.setCancelable(false);

        // inflate XML content
        View dialogView = getLayoutInflater().inflate(R.layout.fragment_st25tvc_tamper_detect_light, null);
        dialog.setView(dialogView);

        final TextView tdStatusTextView = dialogView.findViewById(R.id.tdStatusTextView);
        final TextView tdEventTextView = dialogView.findViewById(R.id.tdEventTextView);

        final EditText wireOpenEditText = dialogView.findViewById(R.id.wireOpenedEditText);
        final EditText wireShortEditText = dialogView.findViewById(R.id.wireClosedEditText);
        final EditText historySealedEditText = dialogView.findViewById(R.id.historySealedEditText);
        final EditText historyUnsealedEditText = dialogView.findViewById(R.id.historyUnsealedEditText);
        final EditText historyResealedEditText = dialogView.findViewById(R.id.historyResealedEditText);

        final Button cancelButton = dialogView.findViewById(R.id.cancelButton);
        final Button updateTagButton = dialogView.findViewById(R.id.updateTagButton);

        tdStatusTextView.setText(mTdStatusLoop);
        if (mIsTdEventUpdateEnabled) {
            tdEventTextView.setText(mTdEventStatus);
        } else {
            tdEventTextView.setText("");
        }

        wireOpenEditText.setText(mTdOpenMsg);
        wireShortEditText.setText(mTdShortMsg);
        historySealedEditText.setText(mTdSealMsg);
        historyUnsealedEditText.setText(mTdUnsealMsg);
        historyResealedEditText.setText(mTdResealMsg);

        wireOpenEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {}

            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                String asciiText = wireOpenEditText.getText().toString();
                if (!Helper.isStringInST25AsciiTable(asciiText)) {
                    displayWarningForNonPortableCharacters();
                }
            }
        });

        wireShortEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {}

            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                String asciiText = wireShortEditText.getText().toString();
                if (!Helper.isStringInST25AsciiTable(asciiText)) {
                    displayWarningForNonPortableCharacters();
                }
            }
        });

        historySealedEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {}

            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                String asciiText = historySealedEditText.getText().toString();
                if (!Helper.isStringInST25AsciiTable(asciiText)) {
                    displayWarningForNonPortableCharacters();
                }
            }
        });

        historyUnsealedEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {}

            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                String asciiText = historyUnsealedEditText.getText().toString();
                if (!Helper.isStringInST25AsciiTable(asciiText)) {
                    displayWarningForNonPortableCharacters();
                }
            }
        });

        historyResealedEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {}

            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                String asciiText = historyResealedEditText.getText().toString();
                if (!Helper.isStringInST25AsciiTable(asciiText)) {
                    displayWarningForNonPortableCharacters();
                }
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.cancel();
            }
        });

        updateTagButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //String asciiText = asciiCustomMsgEditText.getText().toString();
                mTdWireOpenMsgToWrite = wireOpenEditText.getText().toString();
                mTdWireShortMsgToWrite = wireShortEditText.getText().toString();
                mTdSealMsgToWrite = historySealedEditText.getText().toString();
                mTdUnsealMsgToWrite = historyUnsealedEditText.getText().toString();
                mTdResealMsgToWrite = historyResealedEditText.getText().toString();
                dialog.cancel();
                executeAsynchronousAction(Action.WRITE_TD_CHARACTERS);
            }
        });

        // show the dialog box
        dialog.show();
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
                UIHelper.displayCircularProgressBar(ST25TVCTamperDetectActivity.this, getString(R.string.please_wait));

                switch (mAction) {
                    case READ_CURRENT_TD_CONFIGURATION:
                        mIsTamperDetectLocked = mST25TVCTag.isTamperDetectConfigurationLocked();

                        mIsTdEventUpdateEnabled = mST25TVCTag.isTamperDetectEventUpdateEnabled();

                        mTdStatusLoop = mST25TVCTag.getTamperDetectLoopStatusString();
                        mTdEventStatus = mST25TVCTag.getTamperDetectEventStatusString();

                        mTdOpenMsg = mST25TVCTag.getTamperDetectOpenMsg();
                        mTdShortMsg = mST25TVCTag.getTamperDetectShortMsg();
                        mTdSealMsg = mST25TVCTag.getTamperDetectSealMsg();
                        mTdUnsealMsg = mST25TVCTag.getTamperDetectUnsealMsg();
                        mTdResealMsg = mST25TVCTag.getTamperDetectResealMsg();

                        result = ACTION_SUCCESSFUL;
                        break;

                    case WRITE_TD_HISTORY:
                        boolean enableHistory = mEnableHistoryCheckbox.isChecked();
                        mST25TVCTag.setTamperDetectEventUpdateEnable(enableHistory);
                        mIsTdEventUpdateEnabled = enableHistory;
                        result = ACTION_SUCCESSFUL;
                        break;

                    case WRITE_TD_CHARACTERS:
                        if (!mTdWireOpenMsgToWrite.equals(mTdOpenMsg)) {
                            mST25TVCTag.setTamperDetectOpenMsg(mTdWireOpenMsgToWrite);
                            mTdOpenMsg = mTdWireOpenMsgToWrite;
                        }

                        if (!mTdWireShortMsgToWrite.equals(mTdShortMsg)) {
                            mST25TVCTag.setTamperDetectShortMsg(mTdWireShortMsgToWrite);
                            mTdShortMsg = mTdWireShortMsgToWrite;
                        }

                        if (!mTdSealMsgToWrite.equals(mTdSealMsg)) {
                            mST25TVCTag.setTamperDetectSealMsg(mTdSealMsgToWrite);
                            mTdSealMsg = mTdSealMsgToWrite;
                        }

                        if (!mTdUnsealMsgToWrite.equals(mTdUnsealMsg)) {
                            mST25TVCTag.setTamperDetectUnsealMsg(mTdUnsealMsgToWrite);
                            mTdUnsealMsg = mTdUnsealMsgToWrite;
                        }

                        if (!mTdResealMsgToWrite.equals(mTdResealMsg)) {
                            mST25TVCTag.setTamperDetectResealMsg(mTdResealMsgToWrite);
                            mTdResealMsg = mTdResealMsgToWrite;
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
                        if (mIsTamperDetectLocked) {
                            result = TAMPER_DETECT_CONFIGURATION_LOCKED;
                        } else {
                            result = CONFIG_PASSWORD_NEEDED;
                        }
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
                        case READ_CURRENT_TD_CONFIGURATION:
                            updateUI();
                            break;
                        case WRITE_TD_CHARACTERS:
                        case WRITE_TD_HISTORY:
                            showToast(R.string.tag_updated);
                            updateUI();
                            break;
                    }
                    break;

                case CONFIG_PASSWORD_NEEDED:
                    displayConfigurationPasswordDialogBox();
                    break;

                case TAMPER_DETECT_CONFIGURATION_LOCKED:
                    mChangeTdCharactersButton.setEnabled(false);
                    mChangeTdCharactersButton.setBackgroundResource(R.drawable.light_grey_round_area);
                    mEnableHistoryCheckbox.setEnabled(false);
                    UIHelper.displayMessage(ST25TVCTamperDetectActivity.this, R.string.tamper_detect_configuration_locked);
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
