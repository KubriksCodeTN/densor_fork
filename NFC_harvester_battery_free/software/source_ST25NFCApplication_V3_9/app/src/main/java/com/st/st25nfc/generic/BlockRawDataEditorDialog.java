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

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.st.st25nfc.R;
import com.st.st25sdk.Helper;
import com.st.st25sdk.STException;

import static com.st.st25sdk.STException.STExceptionCode.CMD_FAILED;


public class BlockRawDataEditorDialog extends DialogFragment {

    public interface BlockRawDataEditorDialogListener {
        public void onBlockRawDataUpdateDialogFinish(int result, byte[] data, int pos);
    }


    static final String TAG = "BlockUpdateDialog";
    private Handler mHandler;


    private BlockRawDataEditorDialogListener mListener;
    private FragmentManager mFragmentManager;

    public void setListener(BlockRawDataEditorDialogListener listener) {
        this.mListener = listener;
    }

    private static final String ARG_DIALOG_MSG = "dialogMessage";
    private static final String ARG_ADDRESS_TXT = "addressTxt";
    private static final String DATA_LENGTH_IN_BYTES = "dataLengthInBytes";
    private static final String DATA_ARRAY_IN_BYTES = "dataArrayInBytes";
    private static final String DATA_POSITION = "dataPosition";


    private View mCurFragmentView = null; // Store view corresponding to current fragment

    public static final int RESULT_FAIL = 0;
    public static final int RESULT_OK = 1;

    private String mDialogMessage;
    private String mAddressTxt;

    private boolean mIsBlockUpdateOk = false;
    private int mBlockLengthInBytes = 0;
    private byte[] mBlockBytes;

    private byte[] mBlockInputData;
    private int mPos = 0;


    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment BlockRawDataEditorDialog.
     */
    public static BlockRawDataEditorDialog newInstance(String dialogMessage,
                                                       String addressTxt,
                                                       FragmentManager fragmentManager,
                                                       BlockRawDataEditorDialogListener listener,
                                                       int pwdLengthInBytes,
                                                       byte[] data, int pos) {
        BlockRawDataEditorDialog fragment = newInstance(dialogMessage, addressTxt, pwdLengthInBytes,data,pos);
        fragment.setListener(listener);
        fragment.mFragmentManager = fragmentManager;
        return fragment;
    }

    public static BlockRawDataEditorDialog newInstance(String dialogMessage, String addressTxt, int pwdLengthInBytes, byte[] data, int pos) {
        BlockRawDataEditorDialog fragment = new BlockRawDataEditorDialog();

        Bundle args = new Bundle();
        args.putSerializable(ARG_DIALOG_MSG, dialogMessage);
        args.putSerializable(ARG_ADDRESS_TXT, addressTxt);
        args.putSerializable(DATA_LENGTH_IN_BYTES, pwdLengthInBytes);
        args.putSerializable(DATA_ARRAY_IN_BYTES, data);
        args.putSerializable(DATA_POSITION, pos);

        fragment.setArguments(args);
        return fragment;
    }


    public BlockRawDataEditorDialog() {
        // Required empty public constructor
    }


    public void setPwdDialogListener(BlockRawDataEditorDialogListener listener) {
        this.mListener = listener;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mDialogMessage = (String) getArguments().get(ARG_DIALOG_MSG);
            mAddressTxt = (String) getArguments().get(ARG_ADDRESS_TXT);
            mBlockLengthInBytes = (int) getArguments().get(DATA_LENGTH_IN_BYTES);
            mBlockInputData = (byte[]) getArguments().get(DATA_ARRAY_IN_BYTES);
            mPos = (int) getArguments().get(DATA_POSITION);
        }

        mHandler = new Handler();

        if (mListener == null) {
            // No listener was passed to newInstance(). Assume the activity is used as listener
            mListener = (BlockRawDataEditorDialogListener) getActivity();
            Log.v(TAG, "mListener = " + mListener);
        }

        setStyle(STYLE_NO_TITLE, 0); // remove title from DialogFragment
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        String message;

        // Inflate the layout for this fragment
        mCurFragmentView = inflater.inflate(R.layout.fragment_block_update_dialog, container, false);

        mFragmentManager = getFragmentManager();

        switch (mBlockLengthInBytes) {
            case 4:
                message = mDialogMessage + "\n" + getResources().getString(R.string.thirty_two_bits_block) + "\n" + mAddressTxt;
                break;
            default:
                message = mDialogMessage + "\n" + "Block Length not yet implemented";
                Log.e(TAG, message);
                dismiss();
                break;
        }

        TextView messageTextView = (TextView) mCurFragmentView.findViewById(R.id.messageTextView);
        messageTextView.setText(message);

        setDataInitialValues();

        Button okButton = (Button) mCurFragmentView.findViewById(R.id.okButton);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    mBlockBytes = getBytesDataTypedByUser();
                    mIsBlockUpdateOk = true;
                    // remove keyboard focus if any
                    removeKeyBoard();
                    dismiss();

                } catch (STException e) {
                    e.printStackTrace();
                }
            }
        });

        Button cancelButton = (Button) mCurFragmentView.findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Leave the current activity
                // remove keyboard focus if any
                removeKeyBoard();
                dismiss();
            }
        });

        return mCurFragmentView;
    }

    private void removeKeyBoard() {
        if (mCurFragmentView != null) {
            InputMethodManager inputManager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(mCurFragmentView.getWindowToken(), 0);
        }
    }

    private byte getInputByte(int position) throws STException {
        EditText byteEditText;

        try {
            switch (position) {
                case 0:
                    byteEditText = (EditText) mCurFragmentView.findViewById(R.id.byte0EditText);
                    return (byte) Integer.parseInt(byteEditText.getText().toString(), 16);                 // This is the LSB
                case 1:
                    byteEditText = (EditText) mCurFragmentView.findViewById(R.id.byte1EditText);
                    return (byte) Integer.parseInt(byteEditText.getText().toString(), 16);
                case 2:
                    byteEditText = (EditText) mCurFragmentView.findViewById(R.id.byte2EditText);
                    return (byte) Integer.parseInt(byteEditText.getText().toString(), 16);
                case 3:
                    byteEditText = (EditText) mCurFragmentView.findViewById(R.id.byte3EditText);
                    return (byte) Integer.parseInt(byteEditText.getText().toString(), 16);                 // This is the MSB

                default:
                    String message = mDialogMessage + "\n" + "Block Length not yet implemented";
                    Log.e(TAG, message);
                    break;
            }

        } catch (NumberFormatException e) {
            showToast(R.string.invalid_hexadecimal_value);
            throw new STException(CMD_FAILED);
        }

        return (byte) 0xFF;
    }

    private void setInputByte(int position, byte data) {
        EditText byteEditText = null;

            switch (position) {
                case 0:
                    byteEditText = (EditText) mCurFragmentView.findViewById(R.id.byte0EditText);
                    break;
                case 1:
                    byteEditText = (EditText) mCurFragmentView.findViewById(R.id.byte1EditText);
                    break;
                case 2:
                    byteEditText = (EditText) mCurFragmentView.findViewById(R.id.byte2EditText);
                    break;
                case 3:
                    byteEditText = (EditText) mCurFragmentView.findViewById(R.id.byte3EditText);
                    break;

            }
            if (byteEditText != null) {
                byteEditText.setText(Helper.convertByteToHexString(data).toUpperCase());
            }
    }

    private byte[] getBytesDataTypedByUser() throws STException {
        byte[] bytesData = new byte[mBlockLengthInBytes];

        for (int i = 0; i < mBlockLengthInBytes; i++) {
            bytesData[i] = getInputByte(i);
        }

        return bytesData;
    }

    private void setDataInitialValues()  {
        for (int i = 0; i < mBlockLengthInBytes; i++) {
            setInputByte(i,mBlockInputData[i]);
        }
    }

    /**
     * Function called by background thread to display a toast on UI
     *
     * @param resource_id
     */
    private void showToast(final int resource_id) {
        // Warning: Function called from background thread! Post a request to the UI thread
        mHandler.post(new Runnable() {
            public void run() {
                Toast.makeText(getContext(), getResources().getString(resource_id), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onDismiss(final DialogInterface dialog) {

        Log.v(TAG, "onDismiss");

        if (mListener != null) {
            if (mIsBlockUpdateOk == true) {
                mListener.onBlockRawDataUpdateDialogFinish(RESULT_OK, mBlockBytes,mPos);
            } else {
                mListener.onBlockRawDataUpdateDialogFinish(RESULT_FAIL, null,mPos);
            }
        }
        super.onDismiss(dialog);
    }


}

