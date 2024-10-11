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

package com.st.st25nfc.generic.ndef;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import com.st.st25nfc.R;
import com.st.st25sdk.STLog;
import com.st.st25sdk.ndef.NDEFMsg;
import com.st.st25sdk.ndef.TextRecord;

public class NDEFTextFragment extends NDEFRecordFragment {

    final static String TAG = "NDEFTextFragment";

    private EditText mEditText;
    private Button mAddDummyTextButton;

    private TextRecord mTextRecord;
    private int mAction;
    private View mView;

    public static NDEFTextFragment newInstance(Context context) {
        NDEFTextFragment f = new NDEFTextFragment();
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

        View view = inflater.inflate(R.layout.fragment_ndef_text, container, false);
        mView = view;

        Bundle bundle = getArguments();
        if (bundle == null) {
            Log.e(TAG, "Fatal error! Arguments are missing!");
            return null;
        }

        NDEFMsg ndefMsg = (NDEFMsg) bundle.getSerializable(NDEFRecordFragment.NDEFKey);
        int recordNbr = bundle.getInt(NDEFRecordFragment.RecordNbrKey);
        mTextRecord = (TextRecord) ndefMsg.getNDEFRecord(recordNbr);

        initFragmentWidgets();

        mAction = bundle.getInt(NDEFEditorFragment.EditorKey);
        if(mAction == NDEFEditorFragment.VIEW_NDEF_RECORD) {
            // We are displaying an existing record. By default it is not editable
            ndefRecordEditable(false);

        } else {
            // We are adding a new TextRecord or editing an existing record
            ndefRecordEditable(true);
        }

        return view;
    }

    private void initFragmentWidgets() {
        mEditText = (EditText) mView.findViewById(R.id.ndef_fragment_edit_text);

        mAddDummyTextButton = (Button) mView.findViewById(R.id.addDummyTextButton);
        mAddDummyTextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).create();
                final int defaultNumberOfCharacters = 64;

                // set title
                //alertDialog.setTitle(getString(R.string.eas_alarm));

                // inflate XML content
                View dialogView = getActivity().getLayoutInflater().inflate(R.layout.fragment_ndef_add_random_text, null);
                alertDialog.setView(dialogView);

                final EditText numberOfCharactersEditText = (EditText) dialogView.findViewById(R.id.numberOfCharactersEditText);
                numberOfCharactersEditText.setText(String.valueOf(defaultNumberOfCharacters));
                numberOfCharactersEditText.setSelection(numberOfCharactersEditText.getText().length());

                Button addRandomTextButton = (Button) dialogView.findViewById(R.id.addDummyTextButton);
                addRandomTextButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int nbrOfCharacters = defaultNumberOfCharacters;
                        try {
                            nbrOfCharacters = Integer.parseInt(numberOfCharactersEditText.getText().toString());
                        } catch (Exception e) {
                            STLog.e("Bad Value" + e.getMessage());
                        }

                        // Leave the dialog box
                        alertDialog.dismiss();

                        String text = generateDummyText(nbrOfCharacters);
                        mEditText.setText(text);
                    }
                });

                // show it
                alertDialog.show();
            }
        });

        // Hide the Soft Keyboard
        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        setContent();
    }

    private String generateDummyText(int nbrOfCharacters) {
        String header= "This is a text of " + String.valueOf(nbrOfCharacters) + " characters ";
        String text = "";

        if (nbrOfCharacters > header.length()) {
            text = header;
        }

        // Append some characters in order to reach the requested number of characters
        int count =  (nbrOfCharacters - text.length());

        for(int i=0; i<count; i++) {
            text = text + "=";
        }

        return text;
    }

    /**
     * The content from the fragment is saved into the NDEF Record
     */
    @Override
    public void updateContent() {
        String txt = mEditText.getText().toString();
        mTextRecord.setText(txt);
    }

    /**
     * The content from the NDEF Record is displayed in the Fragment
     */
    public void setContent() {
        String text = mTextRecord.getText();
        mEditText.setText(text);
    }

    @Override
    public void ndefRecordEditable(boolean editable) {
        mEditText.setClickable(editable);
        mEditText.setFocusable(editable);
        mEditText.setFocusableInTouchMode(editable);

        if(!editable) {
            // The Fragment is no more editable. Reload its content
            setContent();
        }
    }
}


