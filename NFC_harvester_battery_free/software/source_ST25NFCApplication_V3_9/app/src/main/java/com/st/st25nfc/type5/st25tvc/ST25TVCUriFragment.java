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
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.fragment.app.FragmentManager;

import com.st.st25nfc.R;
import com.st.st25nfc.generic.MainActivity;
import com.st.st25nfc.generic.PwdDialogFragment;
import com.st.st25nfc.generic.STFragmentActivity;
import com.st.st25nfc.generic.STType5PwdDialogFragment;
import com.st.st25nfc.generic.ndef.NDEFActivity;
import com.st.st25nfc.generic.ndef.NDEFEditorFragment;
import com.st.st25nfc.generic.ndef.NDEFRecordFragment;
import com.st.st25nfc.generic.util.DisplayTapTagRequest;
import com.st.st25nfc.generic.util.DisplayTapTagRequest.TapTagDialogBoxListener;
import com.st.st25nfc.generic.util.UIHelper;
import com.st.st25sdk.Helper;
import com.st.st25sdk.NFCTag;
import com.st.st25sdk.STException;
import com.st.st25sdk.ndef.NDEFMsg;
import com.st.st25sdk.ndef.UriRecord;
import com.st.st25sdk.type5.st25tvc.ST25TVCTag;

import java.io.UnsupportedEncodingException;
import java.util.Collection;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import static com.st.st25nfc.generic.ndef.NDEFEditorFragment.VIEW_NDEF_RECORD;
import static com.st.st25nfc.type5.st25tvc.ST25TVCUriFragment.ActionStatus.*;

public class ST25TVCUriFragment extends NDEFRecordFragment implements AdapterView.OnItemSelectedListener, NDEFActivity.WriteNdefMessageHook {

    final static String TAG = "TVANDEFUriFragment";

    enum Action {
        READ_ANDEF_STATE,
        READ_CURRENT_CONFIGURATION,

        WRITE_CUSTOM_MSG,
        WRITE_TAMPER_DETECT,
        WRITE_NEW_CONFIGURATION,

        DELETE_RECORD
    };

    enum ActionStatus {
        ACTION_SUCCESSFUL,
        ACTION_FAILED,
        TAG_NOT_IN_THE_FIELD,
        CONFIG_PASSWORD_NEEDED,
        AREA_PASSWORD_NEEDED,
        EDITION_NOT_ALLOWED,
        ANDEF_CONFIGURATION_IS_LOCKED,
        INVALID_SEPARATOR
    };

    private ST25TVCTag mST25TVCTag;
    FragmentManager mFragmentManager;

    private UriRecord mUriRecord;
    private EditText mUriEditText;
    private Spinner mUriSpinner;
    private int mRecordAction;
    private boolean mIsNdefRecordEditable;
    boolean mIsCurrentConfigurationRead = false;

    private Action mCurrentAction;

    private NDEFActivity mNDEFActivity;

    Switch mEnableAndefSwitch;
    AppCompatCheckBox mUseSeparatorCheckbox;
    AppCompatCheckBox mUidCheckbox;
    AppCompatCheckBox mCustomMsgCheckbox;
    AppCompatCheckBox mUniqueTapCodeCheckbox;
    AppCompatCheckBox mTdCheckbox;

    LinearLayout mAndefLayout;
    LinearLayout mUidLayout;
    LinearLayout mCustomMsgLayout;
    LinearLayout mUniqueTapCodeLayout;
    LinearLayout mTamperDetectLayout;

    ImageButton mUidImageButton;
    ImageButton mCustomMsgImageButton;
    ImageButton mUniqueTapCodeImageButton;
    ImageButton mTamperDetectImageButton;

    EditText mSeparatorCharEditText;
    TextView mCustomerMsgSeparatorTextView;
    TextView mUniqueTapCodeSeparatorTextView;
    TextView mTamperDetectSeparatorTextView;

    TextView mGeneratedUrlTextView;

    private boolean mIsAndefEnabled = false;

    private boolean mIsTamperDetectLocked;
    private boolean mIsAndefConfigurationLocked;
    private boolean mIsAndefSigConfigurationLocked;

    // Boolean reflecting what the user has put in 'useSeparatorCheckbox'
    private boolean mUseSeparator = false;
    // Boolean indicating if Separators are needed. It depends of 'useSeparator' but also
    // of the number of ANDEF Items selected
    private boolean mIsSeparatorNeeded = false;

    private byte[] mNdefBytes;
    private byte[] mCurrentPayload;
    private int mAndefStartOffset = 0;

    String mAndefSeparatorString = "";

    String mUid = "";
    String mCustomMsg = "";
    String mUniqueTapCodeTxt = "";
    String mTdStatus = "";
    String mTdEvent = "";
    String mTdWireOpenedMsg = "";
    String mTdWireClosedMsg = "";
    String mTdHistorySealedMsg = "";
    String mTdHistoryOpenedMsg = "";
    String mTdHistoryResealedMsg = "";

    String mCustomMsgToWrite = "";
    String mTdWireOpenedMsgToWrite = "";
    String mTdWireClosedMsgToWrite = "";
    String mTdHistorySealedMsgToWrite = "";
    String mTdHistoryOpenedMsgToWrite = "";
    String mTdHistoryResealedMsgToWrite = "";

    private boolean mIsAndefSignatureSupported = true;
    private boolean mIsTamperDetectSupported = true;

    private boolean mAddAndefUID = false;
    private boolean mAddAndefCustomMsg = false;
    private boolean mAddAndefUniqueTapCode = false;
    private boolean mAddAndefTamperDetect = false;

    private View mView;


    public static ST25TVCUriFragment newInstance(Context context) {
        ST25TVCUriFragment f = new ST25TVCUriFragment();
        /* If needed, pass some argument to the fragment
        Bundle args = new Bundle();
        args.putInt("index", index);
        f.setArguments(args);
        */
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_st25tvc_uri, container, false);
        mView = view;

        Bundle bundle = getArguments();
        if (bundle == null) {
            Log.e(TAG, "Fatal error! Arguments are missing!");
            return null;
        }

        mFragmentManager = getActivity().getSupportFragmentManager();
        mNDEFActivity = (NDEFActivity) getActivity();

        NFCTag nfcTag = MainActivity.getTag();
        if (nfcTag instanceof ST25TVCTag) {
            mST25TVCTag = (ST25TVCTag) nfcTag;
        } else {
            showToast(R.string.invalid_tag);
            ((STFragmentActivity) getActivity()).goBackToMainActivity();
        }

        NDEFMsg ndefMsg = (NDEFMsg) bundle.getSerializable(NDEFRecordFragment.NDEFKey);
        int recordNbr = bundle.getInt(NDEFRecordFragment.RecordNbrKey);
        mUriRecord = (UriRecord) ndefMsg.getNDEFRecord(recordNbr);

        // Get the IDs of all the Widgets
        getWidgetsIds(view);

        mRecordAction = bundle.getInt(NDEFEditorFragment.EditorKey);

        initFragmentWidgets();

        if(mRecordAction == VIEW_NDEF_RECORD) {
            // We are displaying an existing record. By default it is not editable
            ndefRecordEditable(false);

        } else {
            // We are adding a new TextRecord or editing an existing record
            ndefRecordEditable(true);
        }

        mST25TVCTag.getRegisterAndefEnable().invalidateCache();

        mAndefLayout.setVisibility(GONE);

        mEnableAndefSwitch.setChecked(false);
        mUseSeparatorCheckbox.setChecked(false);
        mUidCheckbox.setChecked(false);
        mCustomMsgCheckbox.setChecked(false);
        mUniqueTapCodeCheckbox.setChecked(false);
        mTdCheckbox.setChecked(false);

        mUidImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayFieldContent("UID field content:\n" + mUid);
            }
        });


        mCustomMsgImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mIsAndefConfigurationLocked) {
                    UIHelper.displayMessage(getActivity(), R.string.andef_configuration_is_locked);
                } else {
                    displayCustomMsgDialogBox();
                }
            }
        });

        mUniqueTapCodeImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayFieldContent("Unique Tap Code field content: " + mUniqueTapCodeTxt);
            }
        });

        mTamperDetectImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mIsTamperDetectLocked) {
                    UIHelper.displayMessage(getActivity(), R.string.tamper_detect_configuration_locked);
                } else {
                    displayTamperDetectDialogBox();
                }
            }
        });

        executeAsynchronousAction(Action.READ_ANDEF_STATE);

        // Now that the UI is initialized, we can set all the Listeners
        setListeners();

        mNDEFActivity.hookWriteActions(this);

        return view;
    }

    private void getWidgetsIds(View view ) {
        mUriEditText = view.findViewById(R.id.ndef_fragment_uri_text);
        mUriSpinner = view.findViewById(R.id.ndef_fragment_uri_title);

        mGeneratedUrlTextView = view.findViewById(R.id.generatedUrlTextView);

        mUidLayout = view.findViewById(R.id.uidLayout);
        mCustomMsgLayout = view.findViewById(R.id.customMsgLayout);
        mUniqueTapCodeLayout = view.findViewById(R.id.uniqueTapCodeLayout);
        mTamperDetectLayout = view.findViewById(R.id.tamperDetectLayout);

        mSeparatorCharEditText = view.findViewById(R.id.separatorCharEditText);

        mCustomerMsgSeparatorTextView = view.findViewById(R.id.customerMsgSeparatorTextView);
        mUniqueTapCodeSeparatorTextView = view.findViewById(R.id.uniqueTapCodeSeparatorTextView);
        mTamperDetectSeparatorTextView = view.findViewById(R.id.tamperDetectSeparatorTextView);

        mAndefLayout = view.findViewById(R.id.andefLayout);

        mEnableAndefSwitch = view.findViewById(R.id.enableAndefSwitch);
        mUseSeparatorCheckbox = view.findViewById(R.id.useSeparatorCheckbox);
        mUidCheckbox = view.findViewById(R.id.uidCheckbox);
        mCustomMsgCheckbox = view.findViewById(R.id.customMsgCheckbox);
        mUniqueTapCodeCheckbox = view.findViewById(R.id.uniqueTapCodeCheckbox);
        mTdCheckbox = view.findViewById(R.id.tdCheckbox);
        mUidImageButton = view.findViewById(R.id.uidImageButton);

        mCustomMsgImageButton = view.findViewById(R.id.customMsgImageButton);
        mUniqueTapCodeImageButton = view.findViewById(R.id.uniqueTapCodeImageButton);
        mTamperDetectImageButton = view.findViewById(R.id.tamperDetectImageButton);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    private void initFragmentWidgets() {
        Collection<String> spinnerList = UriRecord.getUriCodesList();
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(this.getActivity(), R.layout.spinner_text_view){
            @Override
            public View getDropDownView(int position, View convertView,
                                        ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView tv = (TextView) view;
                if(position%2 == 1) {
                    // Set the item background color
                    tv.setBackgroundColor(getResources().getColor(R.color.st_light_grey));
                }
                else {
                    // Set the alternate item background color
                    tv.setBackgroundColor(getResources().getColor(R.color.st_very_light_blue));
                }
                return view;
            }
        };
        spinnerAdapter.addAll(spinnerList);
        spinnerAdapter.setDropDownViewResource(R.layout.spinner_text_view);
        mUriSpinner.setAdapter(spinnerAdapter);
        mUriSpinner.setOnItemSelectedListener(this);

        setContent();
    }

    /**
     * The content from the NDEF Record is displayed in the Fragment
     */
    public void setContent() {
        String uri = mUriRecord.getContent();
        mUriEditText.setText(uri);

        UriRecord.NdefUriIdCode uriCode = mUriRecord.getUriID();
        mUriSpinner.setSelection(UriRecord.getUriCodePositionInList(uriCode));
    }

    /**
     * The content from the fragment is saved into the NDEF Record
     */
    @Override
    public void updateContent() {
        UriRecord.NdefUriIdCode uriID = UriRecord.getUriCodeFromStr((String) mUriSpinner.getSelectedItem());
        mUriRecord.setUriID(uriID);

        String text = mUriEditText.getText().toString();
        mUriRecord.setContent(text);
    }


    @Override
    public void ndefRecordEditable(boolean editable) {
        mIsNdefRecordEditable = editable;

        if (editable) {
            mUriEditText.setBackgroundColor(getResources().getColor(R.color.st_very_light_blue));
            mUriSpinner.setBackgroundColor(getResources().getColor(R.color.st_very_light_blue));
            mSeparatorCharEditText.setBackgroundColor(getResources().getColor(R.color.st_very_light_blue));
        } else {
            mUriEditText.setBackgroundColor(getResources().getColor(R.color.st_light_grey));
            mUriSpinner.setBackgroundColor(getResources().getColor(R.color.st_light_grey));
            mSeparatorCharEditText.setBackgroundColor(getResources().getColor(R.color.st_light_grey));
        }

        mUriEditText.setClickable(editable);
        mUriEditText.setFocusable(editable);
        mUriEditText.setFocusableInTouchMode(editable);

        mUriSpinner.setClickable(editable);
        mUriSpinner.setFocusable(editable);
        mUriSpinner.setFocusableInTouchMode(editable);
        mUriSpinner.setEnabled(editable);

        mEnableAndefSwitch.setEnabled(editable);

        mUseSeparatorCheckbox.setEnabled(editable);
        mUidCheckbox.setEnabled(editable);

        mCustomMsgCheckbox.setEnabled(editable);
        mUniqueTapCodeCheckbox.setEnabled(editable);
        if (mIsTamperDetectSupported) {
            mTdCheckbox.setEnabled(editable);
        } else {
            mTdCheckbox.setEnabled(false);
        }

        mSeparatorCharEditText.setClickable(editable);
        mSeparatorCharEditText.setFocusable(editable);
        mSeparatorCharEditText.setFocusableInTouchMode(editable);

        if(!editable) {
            // The Fragment is no more editable. Reload its content
            if (mIsCurrentConfigurationRead && mIsAndefEnabled) {
                readCurrentConfiguration();
            }
            setContent();
        }
    }


    private void displayNotImplemented() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());

        // set dialog message
        alertDialogBuilder
                .setMessage(R.string.not_implemented_yet)
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


    private void displayKeepPhoneOnTagMessage() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());

        // set dialog message
        alertDialogBuilder
                .setMessage(R.string.please_keep_the_phone_on_the_tag)
                .setCancelable(true)
                .setPositiveButton(getString(R.string.ok),new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        dialog.cancel();
                        // Restart the same action
                        executeAsynchronousAction(mCurrentAction);
                    }
                });

        // create alert dialog
        AlertDialog dialog = alertDialogBuilder.create();

        // show it
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.st_light_blue));
    }


    private int getNumberOfAndefItemSelected() {
        int nbrOfItems = 0;

        if (mUidCheckbox.isChecked()) nbrOfItems++;
        if (mCustomMsgCheckbox.isChecked()) nbrOfItems++;
        if (mUniqueTapCodeCheckbox.isChecked()) nbrOfItems++;
        if (mTdCheckbox.isChecked()) nbrOfItems++;

        return nbrOfItems;
    }

    // Add question mark to URI (if none is present in current URI)
    private void addQuestionMarkToUri() {
        String txt = mUriEditText.getText().toString();
        if (!txt.contains("?")) {
            mUriEditText.setText(txt+"?data=");
        }
    }

    private void removeAndefDataFromUri() {
        int payloadOffsetInUriRecord = Helper.findSubArrayPosition(mNdefBytes, mCurrentPayload);
        if (payloadOffsetInUriRecord != -1) {

            // NB: The first byte of the payload is the prefix ID. Skip it
            payloadOffsetInUriRecord++;

            // Before the UriRecord, there are 4 Bytes for the CCFile and 2 Bytes for the NDEF Header
            int payloadOffsetInMemory = payloadOffsetInUriRecord + 6;

            if (mAndefStartOffset < payloadOffsetInMemory) {
                displayInvalidAndefStartOffsetWarning();
                return;
            }

            int nbrOfBytesToKeep = mAndefStartOffset - payloadOffsetInMemory;
            String uri = mUriRecord.getContent();
            String uriWithoutAndef = uri.substring(0, nbrOfBytesToKeep);
            Log.i(TAG, uriWithoutAndef);

            mUriEditText.setText(uriWithoutAndef);
        }
    }

    private void updateUI() {

        if (mEnableAndefSwitch.isChecked()) {
            mAndefLayout.setVisibility(VISIBLE);
        } else {
            mAndefLayout.setVisibility(GONE);
        }

        if (mUidCheckbox.isChecked()) {
            mUidLayout.setVisibility(VISIBLE);
        } else {
            mUidLayout.setVisibility(GONE);
        }

        if (mCustomMsgCheckbox.isChecked()) {
            mCustomMsgLayout.setVisibility(VISIBLE);
        } else {
            mCustomMsgLayout.setVisibility(GONE);
        }

        if (mUniqueTapCodeCheckbox.isChecked()) {
            mUniqueTapCodeLayout.setVisibility(VISIBLE);
        } else {
            mUniqueTapCodeLayout.setVisibility(GONE);
        }

        if (mTdCheckbox.isChecked()) {
            mTamperDetectLayout.setVisibility(VISIBLE);
        } else {
            mTamperDetectLayout.setVisibility(GONE);
        }

        ////////////////////// Configure the separators //////////////

        int numberOfAndefItems = getNumberOfAndefItemSelected();
        if ((numberOfAndefItems > 1) && mUseSeparatorCheckbox.isChecked()) {
            mIsSeparatorNeeded = true;
        } else {
            mIsSeparatorNeeded = false;
        }

        if (isCustomMsgSeparatorNeeded()) {
            mCustomerMsgSeparatorTextView.setVisibility(VISIBLE);
        } else {
            mCustomerMsgSeparatorTextView.setVisibility(GONE);
        }

        if (isUniqueTapCodeSeparatorNeeded()) {
            mUniqueTapCodeSeparatorTextView.setVisibility(VISIBLE);
        } else {
            mUniqueTapCodeSeparatorTextView.setVisibility(GONE);
        }

        if (isTamperDetectSeparatorNeeded()) {
            mTamperDetectSeparatorTextView.setVisibility(VISIBLE);
        } else {
            mTamperDetectSeparatorTextView.setVisibility(GONE);
        }

        String separator = mSeparatorCharEditText.getText().toString();
        mCustomerMsgSeparatorTextView.setText(separator);
        mUniqueTapCodeSeparatorTextView.setText(separator);
        mTamperDetectSeparatorTextView.setText(separator);


        generateURL(numberOfAndefItems);
    }

    private void generateURL(int numberOfAndefItems) {
        String url = mUriSpinner.getSelectedItem().toString() + mUriEditText.getText();
        String andefData = "";

        // UID
        if(mUidCheckbox.isChecked()) {
            andefData += mUid;
        }

        // Custom message
        if(isCustomMsgSeparatorNeeded()) {
            andefData += mSeparatorCharEditText.getText().toString();
        }

        if(mCustomMsgCheckbox.isChecked()) {
            andefData += mCustomMsg;
        }

        // Unique Tap Code
        if(isUniqueTapCodeSeparatorNeeded()) {
            andefData += mSeparatorCharEditText.getText().toString();
        }

        if(mUniqueTapCodeCheckbox.isChecked()) {
            andefData += mUniqueTapCodeTxt;
        }

        // Tamper Detect
        if(isTamperDetectSeparatorNeeded()) {
            andefData += mSeparatorCharEditText.getText().toString();
        }

        if(mTdCheckbox.isChecked()) {
            andefData += mTdEvent + mTdStatus;
        }

        url += andefData;
        mGeneratedUrlTextView.setText(url);
    }

    private boolean isCustomMsgSeparatorNeeded() {
        if (!mIsSeparatorNeeded || !mCustomMsgCheckbox.isChecked()) {
            return false;
        }

        // Check if a previous element is enabled
        // Warning: We don't call 'addAndefTruST25Signature' because signature will be displayed only if UID is enabled.
        if (mUidCheckbox.isChecked()) {
            return true;
        } else {
            // No previous element
            return false;
        }
    }

    private boolean isUniqueTapCodeSeparatorNeeded() {
        if (!mIsSeparatorNeeded || !mUniqueTapCodeCheckbox.isChecked()) {
            return false;
        }

        // Check if a previous element is enabled
        // Warning: We don't check Andef TruST25 Signature because signature will be displayed only if UID is enabled.
        if (mUidCheckbox.isChecked() || mCustomMsgCheckbox.isChecked()) {
            return true;
        } else {
            // No previous element
            return false;
        }
    }

    private boolean isTamperDetectSeparatorNeeded() {
        if (!mIsSeparatorNeeded || !mTdCheckbox.isChecked()) {
            return false;
        }

        // Check if a previous element is enabled
        // Warning: We don't check Andef TruST25 Signature because signature will be displayed only if UID is enabled.
        if (mUidCheckbox.isChecked() ||
            mCustomMsgCheckbox.isChecked() ||
            mUniqueTapCodeCheckbox.isChecked()) {
            return true;
        } else {
            // No previous element
            return false;
        }
    }

    private void setListeners() {

        mUriSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                updateUI();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {

            }

        });

        mUriEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {}

            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                updateUI();
            }
        });


        mUseSeparatorCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                                                            @Override
                                                            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                                                updateUI();
                                                            }
                                                        }
        );

        mSeparatorCharEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                updateUI();
            }
        });


        mEnableAndefSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!mIsCurrentConfigurationRead) {
                    addQuestionMarkToUri();
                    readCurrentConfiguration();
                } else {
                    updateUI();
                }
            }
        });

        mUidCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                                                   @Override
                                                   public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                                       updateUI();
                                                   }
                                               }
        );

        mCustomMsgCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                                                         @Override
                                                         public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                                             updateUI();
                                                         }
                                                     }
        );

        mUniqueTapCodeCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                                                          @Override
                                                          public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                                              updateUI();
                                                          }
                                                      }
        );


        mTdCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                                                  @Override
                                                  public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                                      updateUI();
                                                  }
                                              }
        );

    }

    /**
     * Display an alert message if the URL doesn't contain '?'
     */
    private void displayQuestionMarkWarning() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());

        String dialogMsg = getResources().getString(R.string.question_mark_warning);

        // set dialog message
        alertDialogBuilder
                .setMessage(dialogMsg)
                .setCancelable(true)
                .setNegativeButton(getString(R.string.cancel),new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        dialog.cancel();
                    }
                })
                .setPositiveButton(getString(R.string.continue_message),new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        executeAsynchronousAction(Action.WRITE_NEW_CONFIGURATION);
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

    @Override
    public boolean writeNdefMessage() {
        // check if URI contains a '?'
        String txt = mUriEditText.getText().toString();
        if (txt.contains("?")) {
            executeAsynchronousAction(Action.WRITE_NEW_CONFIGURATION);
        } else {
            displayQuestionMarkWarning();
        }
        return true;
    }

    @Override
    public boolean deleteNdefMessage() {
        if (mEnableAndefSwitch.isChecked()) {
            executeAsynchronousAction(Action.DELETE_RECORD);
            return true;
        } else {
            // ANDEF is not enabled. Let NDEFActivity write the NDEF
            return false;
        }
    }

    private void readCurrentConfiguration() {
        mIsCurrentConfigurationRead = true;
        executeAsynchronousAction(Action.READ_CURRENT_CONFIGURATION);
    }

    private void executeAsynchronousAction(Action action) {
        Log.d(TAG, "Starting background action " + action);
        mCurrentAction = action;
        new myAsyncTask(action).execute();
    }

    private void displayInvalidAndefStartOffsetWarning() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());

        // set dialog message
        alertDialogBuilder
                .setMessage(R.string.invalid_andef_start_address)
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

    private void displayWarningForNonPortableCharacters() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());

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

    private void displayFieldContent(final String fieldContent) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());

        // set dialog message
        alertDialogBuilder
                .setMessage(fieldContent)
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

    private void displayCustomMsgDialogBox() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final AlertDialog dialog = builder.create();

        // set title
        //alertDialog.setTitle(getString(R.string.XXX));

        dialog.setCancelable(false);

        // inflate XML content
        View dialogView = getLayoutInflater().inflate(R.layout.fragment_st25tvc_custom_msg, null);
        dialog.setView(dialogView);

        final EditText asciiCustomMsgEditText = dialogView.findViewById(R.id.asciiCustomMsgEditText);
        final TextView hexCustomMsgTextView = dialogView.findViewById(R.id.hexCustomMsgTextView);
        final Button cancelButton = dialogView.findViewById(R.id.cancelButton);
        final Button updateTagButton = dialogView.findViewById(R.id.updateTagButton);

        asciiCustomMsgEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {}

            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                String asciiText = asciiCustomMsgEditText.getText().toString();
                String hexData = Helper.convertAsciiStringToHexString(asciiText);
                hexCustomMsgTextView.setText(hexData);

                boolean isUpdatePossible = false;
                if (mIsNdefRecordEditable) {
                    if (asciiText.length() == 8) {
                        if (!Helper.isStringInST25AsciiTable(asciiText)) {
                            displayWarningForNonPortableCharacters();
                        }
                        isUpdatePossible = true;
                    }
                }

                if (isUpdatePossible) {
                    updateTagButton.setEnabled(true);
                    updateTagButton.setTextColor(getResources().getColor(R.color.st_light_blue));
                } else {
                    updateTagButton.setEnabled(false);
                    updateTagButton.setTextColor(getResources().getColor(R.color.st_middle_grey));
                }

            }
        });

        asciiCustomMsgEditText.setText(mCustomMsg);

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.cancel();
            }
        });

        updateTagButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String asciiText = asciiCustomMsgEditText.getText().toString();
                dialog.cancel();
                mCustomMsgToWrite = asciiText;
                executeAsynchronousAction(Action.WRITE_CUSTOM_MSG);
            }
        });

        // show the dialog box
        dialog.show();
    }

    private void displayTamperDetectDialogBox() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final AlertDialog dialog = builder.create();

        // set title
        //alertDialog.setTitle(getString(R.string.XXX));

        dialog.setCancelable(false);

        // inflate XML content
        View dialogView = getLayoutInflater().inflate(R.layout.fragment_st25tvc_tamper_detect_light, null);
        dialog.setView(dialogView);

        final TextView tdStatusTextView = dialogView.findViewById(R.id.tdStatusTextView);
        final TextView tdEventTextView = dialogView.findViewById(R.id.tdEventTextView);

        final EditText wireOpenedEditText = dialogView.findViewById(R.id.wireOpenedEditText);
        final EditText wireClosedEditText = dialogView.findViewById(R.id.wireClosedEditText);
        final EditText historySealedEditText = dialogView.findViewById(R.id.historySealedEditText);
        final EditText historyOpenedEditText = dialogView.findViewById(R.id.historyUnsealedEditText);
        final EditText historyResealedEditText = dialogView.findViewById(R.id.historyResealedEditText);

        final Button cancelButton = dialogView.findViewById(R.id.cancelButton);
        final Button updateTagButton = dialogView.findViewById(R.id.updateTagButton);

        tdStatusTextView.setText(mTdStatus);
        tdEventTextView.setText(mTdEvent);
        wireOpenedEditText.setText(mTdWireOpenedMsg);
        wireClosedEditText.setText(mTdWireClosedMsg);
        historySealedEditText.setText(mTdHistorySealedMsg);
        historyOpenedEditText.setText(mTdHistoryOpenedMsg);
        historyResealedEditText.setText(mTdHistoryResealedMsg);

        wireOpenedEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {}

            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                String asciiText = wireOpenedEditText.getText().toString();
                if (!Helper.isStringInST25AsciiTable(asciiText)) {
                    displayWarningForNonPortableCharacters();
                }
            }
        });

        wireClosedEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {}

            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                String asciiText = wireClosedEditText.getText().toString();
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

        historyOpenedEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {}

            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                String asciiText = historyOpenedEditText.getText().toString();
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
                mTdWireOpenedMsgToWrite = wireOpenedEditText.getText().toString();
                mTdWireClosedMsgToWrite = wireClosedEditText.getText().toString();
                mTdHistorySealedMsgToWrite = historySealedEditText.getText().toString();
                mTdHistoryOpenedMsgToWrite = historyOpenedEditText.getText().toString();
                mTdHistoryResealedMsgToWrite = historyResealedEditText.getText().toString();
                dialog.cancel();
                executeAsynchronousAction(Action.WRITE_TAMPER_DETECT);
            }
        });

        if (mIsNdefRecordEditable) {
            updateTagButton.setEnabled(true);
            updateTagButton.setTextColor(getResources().getColor(R.color.st_light_blue));
        } else {
            updateTagButton.setEnabled(false);
            updateTagButton.setTextColor(getResources().getColor(R.color.st_middle_grey));
        }

        // show the dialog box
        dialog.show();
    }

    private void displayConfigurationPasswordDialogBox() {

        STType5PwdDialogFragment pwdDialogFragment = STType5PwdDialogFragment.newInstance(
                STType5PwdDialogFragment.STPwdAction.PRESENT_CURRENT_PWD,
                ST25TVCTag.ST25TVC_CONFIGURATION_PASSWORD_ID,
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

    private class myAsyncTask extends AsyncTask<Void, Void, ActionStatus> {
        Action mAction;

        public myAsyncTask(Action action) {
            mAction = action;
        }

        @Override
        protected ActionStatus doInBackground(Void... param) {
            ActionStatus result;

            try {
                if ((mAction == Action.WRITE_CUSTOM_MSG) ||
                    (mAction == Action.WRITE_TAMPER_DETECT) ||
                    (mAction == Action.WRITE_NEW_CONFIGURATION) ) {
                    // Check that edition is alllowed
                    if (!mIsNdefRecordEditable) {
                        return EDITION_NOT_ALLOWED;
                    }
                }

                UIHelper.displayCircularProgressBar(mNDEFActivity, getString(R.string.please_wait));

                switch (mAction) {
                    case READ_ANDEF_STATE:
                        mIsAndefEnabled = mST25TVCTag.isAndefEnabled();
                        result = ActionStatus.ACTION_SUCCESSFUL;
                        break;

                    case READ_CURRENT_CONFIGURATION:
                        mIsAndefEnabled = mST25TVCTag.isAndefEnabled();

                        if(mIsAndefEnabled) {
                            NDEFMsg ndefMsg;
                            try {
                                ndefMsg = mST25TVCTag.readNdefMessage();
                            } catch (STException e) {
                                switch(e.getError()) {
                                    case INVALID_CCFILE:
                                    case INVALID_NDEF_DATA:
                                        // Create an empty NDEF message
                                        ndefMsg = new NDEFMsg();
                                        break;
                                    default:
                                        // Other exceptions are escalated
                                        throw e;
                                }
                            }
                            mNdefBytes = ndefMsg.serialize();
                            mCurrentPayload = mUriRecord.getPayload();
                        }

                        mIsTamperDetectLocked = mST25TVCTag.isTamperDetectConfigurationLocked();
                        mIsAndefConfigurationLocked = mST25TVCTag.isAndefConfigurationLocked();
                        mAndefStartOffset = mST25TVCTag.getAndefStartAddressInBytes();

                        mUseSeparator = mST25TVCTag.isAndefSeparatorEnabled();
                        if(mIsAndefConfigurationLocked) {
                            mAndefSeparatorString = "?";
                        } else {
                            mAndefSeparatorString = mST25TVCTag.getAndefSeparator();
                        }

                        mAddAndefUID = mST25TVCTag.isAndefUidEnabled();

                        mAddAndefCustomMsg = mST25TVCTag.isAndefCustomMsgEnabled();
                        mAddAndefUniqueTapCode = mST25TVCTag.isAndefUniqueTapCodeEnabled();
                        mIsTamperDetectSupported = mST25TVCTag.isTamperDetectSupported();
                        if (mIsTamperDetectSupported) {
                            mAddAndefTamperDetect = mST25TVCTag.isAndefTamperDetectMsgEnabled();
                        }

                        // UID
                        mUid = mST25TVCTag.getUidString();
                        // Custom message
                        if(mIsAndefConfigurationLocked) {
                            mCustomMsg = "????????";
                        } else {
                            mCustomMsg = mST25TVCTag.getAndefCustomMsg();
                        }

                        // Unique Tap Code
                        mUniqueTapCodeTxt = mST25TVCTag.getUniqueTapCodeString();
                        // Tamper detect
                        if (mIsTamperDetectSupported) {
                            mTdStatus = mST25TVCTag.getTamperDetectLoopStatusString();
                            mTdEvent = mST25TVCTag.getTamperDetectEventStatusString();
                            if (mIsTamperDetectLocked) {
                                mTdWireOpenedMsg = "?";
                                mTdWireClosedMsg = "?";
                                mTdHistorySealedMsg = "??";
                                mTdHistoryOpenedMsg = "??";
                                mTdHistoryResealedMsg = "??";
                            } else {
                                mTdWireOpenedMsg = mST25TVCTag.getTamperDetectOpenMsg();
                                mTdWireClosedMsg = mST25TVCTag.getTamperDetectShortMsg();
                                mTdHistorySealedMsg = mST25TVCTag.getTamperDetectSealMsg();
                                mTdHistoryOpenedMsg = mST25TVCTag.getTamperDetectUnsealMsg();
                                mTdHistoryResealedMsg = mST25TVCTag.getTamperDetectResealMsg();
                            }
                        }

                        result = ActionStatus.ACTION_SUCCESSFUL;
                        break;

                    case WRITE_CUSTOM_MSG:
                        if (!mCustomMsgToWrite.equals(mCustomMsg)) {
                            mST25TVCTag.setAndefCustomMsg(mCustomMsgToWrite);
                            mCustomMsg = mCustomMsgToWrite;
                        }
                        result = ACTION_SUCCESSFUL;
                        break;

                    case WRITE_TAMPER_DETECT:
                        if (mIsTamperDetectSupported) {
                            if (!mTdWireOpenedMsgToWrite.equals(mTdWireOpenedMsg)) {
                                mST25TVCTag.setTamperDetectOpenMsg(mTdWireOpenedMsgToWrite);
                                mTdWireOpenedMsg = mTdWireOpenedMsgToWrite;
                            }

                            if (!mTdWireClosedMsgToWrite.equals(mTdWireClosedMsg)) {
                                mST25TVCTag.setTamperDetectShortMsg(mTdWireClosedMsgToWrite);
                                mTdWireClosedMsg = mTdWireClosedMsgToWrite;
                            }

                            if (!mTdHistorySealedMsgToWrite.equals(mTdHistorySealedMsg)) {
                                mST25TVCTag.setTamperDetectSealMsg(mTdHistorySealedMsgToWrite);
                                mTdHistorySealedMsg = mTdHistorySealedMsgToWrite;
                            }

                            if (!mTdHistoryOpenedMsgToWrite.equals(mTdHistoryOpenedMsg)) {
                                mST25TVCTag.setTamperDetectUnsealMsg(mTdHistoryOpenedMsgToWrite);
                                mTdHistoryOpenedMsg = mTdHistoryOpenedMsgToWrite;
                            }

                            if (!mTdHistoryResealedMsgToWrite.equals(mTdHistoryResealedMsg)) {
                                mST25TVCTag.setTamperDetectResealMsg(mTdHistoryResealedMsgToWrite);
                                mTdHistoryResealedMsg = mTdHistoryResealedMsgToWrite;
                            }
                            result = ACTION_SUCCESSFUL;
                        } else {
                            result = ACTION_FAILED;
                        }
                        break;

                    case WRITE_NEW_CONFIGURATION:
                        // Write the ANDEF configuration

                        if(mIsAndefConfigurationLocked) {
                            return ANDEF_CONFIGURATION_IS_LOCKED;
                        }

                        if (mEnableAndefSwitch.isChecked() != mIsAndefEnabled) {
                            mST25TVCTag.enableAndef(mEnableAndefSwitch.isChecked());
                            mIsAndefEnabled = mEnableAndefSwitch.isChecked();
                        }

                        if (mEnableAndefSwitch.isChecked()) {

                            if ((mUseSeparatorCheckbox.isChecked() != mUseSeparator)) {
                                mST25TVCTag.enableAndefSeparator(mUseSeparatorCheckbox.isChecked());
                                mUseSeparator = mUseSeparatorCheckbox.isChecked();
                            }

                            if ((mUidCheckbox.isChecked() != mAddAndefUID)) {
                                mST25TVCTag.enableAndefUid(mUidCheckbox.isChecked());
                                mAddAndefUID = mUidCheckbox.isChecked();
                            }

                            if ((mCustomMsgCheckbox.isChecked() != mAddAndefCustomMsg)) {
                                mST25TVCTag.enableAndefCustomMsg(mCustomMsgCheckbox.isChecked());
                                mAddAndefCustomMsg = mCustomMsgCheckbox.isChecked();
                            }

                            if ((mUniqueTapCodeCheckbox.isChecked() != mAddAndefUniqueTapCode)) {
                                mST25TVCTag.enableAndefUniqueTapCode(mUniqueTapCodeCheckbox.isChecked());
                                mAddAndefUniqueTapCode = mUniqueTapCodeCheckbox.isChecked();
                            }

                            if (mIsTamperDetectSupported) {
                                if ((mTdCheckbox.isChecked() != mAddAndefTamperDetect)) {
                                    mST25TVCTag.enableAndefTamperDetectMsg(mTdCheckbox.isChecked());
                                    mAddAndefTamperDetect = mTdCheckbox.isChecked();
                                }
                            }

                            String newSeparator = mSeparatorCharEditText.getText().toString();
                            if (newSeparator.length() != 1) {
                                return INVALID_SEPARATOR;
                            }
                            if (!mAndefSeparatorString.equals(newSeparator)) {
                                mST25TVCTag.setAndefSeparator(newSeparator);
                            }

                        } else {
                            // ANDEF is disabled
                            if(mIsCurrentConfigurationRead) {
                                mST25TVCTag.enableAndefSeparator(false);
                                mUseSeparator = false;

                                mST25TVCTag.enableAndefUid(false);
                                mAddAndefUID = false;


                                mST25TVCTag.enableAndefCustomMsg(false);
                                mAddAndefCustomMsg = false;

                                mST25TVCTag.enableAndefUniqueTapCode(false);
                                mAddAndefUniqueTapCode = false;

                                if (mIsTamperDetectSupported) {
                                    mST25TVCTag.enableAndefTamperDetectMsg(false);
                                    mAddAndefTamperDetect = false;
                                }
                            }
                        }

                        // Update mUriRecord and write it
                        updateContent();
                        if(mEnableAndefSwitch.isChecked()) {
                            mST25TVCTag.writeAndefUri(mUriRecord);
                        } else {
                            NDEFMsg ndefMsg = new NDEFMsg();
                            ndefMsg.addRecord(mUriRecord);
                            mST25TVCTag.writeNdefMessage(ndefMsg);
                        }
                        result = ACTION_SUCCESSFUL;
                        break;
                    case DELETE_RECORD:
                        if (mST25TVCTag.isAndefEnabled()) {
                            mST25TVCTag.enableAndef(false);
                        }
                        NDEFMsg emptyNDEFMsg = new NDEFMsg();
                        mST25TVCTag.writeNdefMessage(emptyNDEFMsg);
                        result = ACTION_SUCCESSFUL;
                        break;
                    default:
                        result = ACTION_FAILED;
                        break;
                }

            } catch (STException e) {
                switch (e.getError()) {
                    case CONFIG_PASSWORD_NEEDED:
                        e.printStackTrace();
                        result = ActionStatus.CONFIG_PASSWORD_NEEDED;
                        break;

                    case TAG_NOT_IN_THE_FIELD:
                        result = TAG_NOT_IN_THE_FIELD;
                        break;

                    case ISO15693_BLOCK_IS_LOCKED:
                        e.printStackTrace();
                        result = ActionStatus.AREA_PASSWORD_NEEDED;
                        break;

                    default:
                        e.printStackTrace();
                        result = ACTION_FAILED;
                        break;
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                result = ACTION_FAILED;
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
                        case READ_ANDEF_STATE:
                            if (mIsAndefEnabled) {
                                readCurrentConfiguration();
                            }
                            break;
                        case READ_CURRENT_CONFIGURATION:
                            if((!mIsAndefEnabled) && (mEnableAndefSwitch.isChecked())) {
                                // ANDEF enabled by the user. Turn ON all the features
                                mSeparatorCharEditText.setText("x");
                                mUseSeparatorCheckbox.setChecked(true);
                                mUidCheckbox.setChecked(true);

                                mCustomMsgCheckbox.setChecked(true);
                                mUniqueTapCodeCheckbox.setChecked(true);
                                if (mIsTamperDetectSupported) {
                                    mTdCheckbox.setChecked(true);
                                } else {
                                    mTdCheckbox.setChecked(false);
                                    mTdCheckbox.setEnabled(false);
                                }
                            } else {
                                mSeparatorCharEditText.setText(mAndefSeparatorString);
                                mEnableAndefSwitch.setChecked(mIsAndefEnabled);
                                mUseSeparatorCheckbox.setChecked(mUseSeparator);
                                mUidCheckbox.setChecked(mAddAndefUID);
                                mCustomMsgCheckbox.setChecked(mAddAndefCustomMsg);
                                mUniqueTapCodeCheckbox.setChecked(mAddAndefUniqueTapCode);
                                mTdCheckbox.setChecked(mAddAndefTamperDetect);
                            }

                            if (mIsAndefEnabled) {
                                // If ANDEF is currently enabled in the tag, we should truncate the URI to remove the ANDEF bytes
                                removeAndefDataFromUri();
                            }
                            updateUI();
                            break;
                        case WRITE_TAMPER_DETECT:
                        case WRITE_CUSTOM_MSG:
                            showToast(R.string.tag_updated);
                            updateUI();
                            break;
                        case WRITE_NEW_CONFIGURATION:
                            updateUI();
                            DisplayTapTagRequest.run(mNDEFActivity, mST25TVCTag, getString(R.string.please_tap_the_tag_again), new TapTagDialogBoxListener() {
                                @Override
                                public void tapTagDialogBoxClosed() {
                                    mNDEFActivity.finish();
                                    showToast(R.string.tag_updated);
                                }
                            });
                            break;
                        case DELETE_RECORD:
                            DisplayTapTagRequest.run(mNDEFActivity, mST25TVCTag, getString(R.string.please_tap_the_tag_again), new TapTagDialogBoxListener() {
                                @Override
                                public void tapTagDialogBoxClosed() {
                                    mNDEFActivity.finish();
                                    showToast(R.string.tag_updated);
                                }
                            });
                            break;
                    }
                    break;

                case INVALID_SEPARATOR:
                    UIHelper.displayMessage(getActivity(), R.string.invalid_separator);
                    break;

                case CONFIG_PASSWORD_NEEDED:
                    displayConfigurationPasswordDialogBox();
                    break;

                case ANDEF_CONFIGURATION_IS_LOCKED:
                    UIHelper.displayMessage(getActivity(), R.string.andef_configuration_is_locked);
                    break;

                case AREA_PASSWORD_NEEDED:
                    mNDEFActivity.showWritePasswordDialog();
                    break;

                case ACTION_FAILED:
                    showToast(R.string.command_failed);
                    break;

                case TAG_NOT_IN_THE_FIELD:
                    updateUI();
                    displayKeepPhoneOnTagMessage();
                    break;

                case EDITION_NOT_ALLOWED:
                    showToast(R.string.edition_not_activated);
                    break;
            }

            return;
        }
    }

}


