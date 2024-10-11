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

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.st.st25sdk.NFCTag;
import com.st.st25nfc.R;
import com.st.st25sdk.STException;
import com.st.st25sdk.STLog;
import com.st.st25sdk.SectorInterface;
import com.st.st25nfc.generic.STFragment;
import com.st.st25sdk.TagHelper;

import static com.st.st25sdk.Helper.convertByteToHexString;
import static com.st.st25sdk.TagHelper.ReadWriteProtection;
import static com.st.st25sdk.TagHelper.ReadWriteProtection.READABLE_AND_WRITABLE;
import static com.st.st25sdk.TagHelper.ReadWriteProtection.READABLE_AND_WRITE_PROTECTED_BY_PWD;
import static com.st.st25sdk.TagHelper.ReadWriteProtection.READ_AND_WRITE_PROTECTED_BY_PWD;
import static com.st.st25sdk.TagHelper.ReadWriteProtection.READ_PROTECTED_BY_PWD_AND_WRITE_IMPOSSIBLE;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SectorLockFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SectorLockFragment extends STFragment {

    TextView mSectorNumberTextview;
    TextView mCurrentSecurityStatusTextView;
    TextView mNewSecurityStatusTextView;
    TextView mSectorLockedWarningTextView;

    RadioButton mReadableWritableRadioButton;
    RadioButton mReadableWritePWDProtectedRadioButton;
    RadioButton mReadWritePWDProtectedRadioButton;
    RadioButton mReadPWDProtectedWriteImpossibleRadioButton;

    RadioButton mNoPasswordRadioButton;
    RadioButton mPassword1RadioButton;
    RadioButton mPassword2RadioButton;
    RadioButton mPassword3RadioButton;

    byte mValue;
    private int mSectorNbr;

    private RadioGroup mSectorAccessRightsRadioGroup;
    private RadioGroup mPasswordRadioGroup;

    private byte MASK_PASSWORD = 0x18;
    private byte MASK_PROTECTION = 0x06;


    public SectorLockFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment.
     *
     * @return A new instance of fragment AreasEditorFragment.
     */
    public static SectorLockFragment newInstance() {
        SectorLockFragment fragment = new SectorLockFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.fragment_sector_sec_status, container, false);

        Intent intent = getActivity().getIntent();
        if (intent != null) {
            mSectorNbr = intent.getIntExtra("sector_nbr", 0);
        } else {
            mSectorNbr = 0;
        }

        mSectorNumberTextview = mView.findViewById(R.id.sector_number_textview);
        mCurrentSecurityStatusTextView = mView.findViewById(R.id.security_sector_value);
        mNewSecurityStatusTextView =mView.findViewById(R.id.new_security_sector_value);
        mSectorLockedWarningTextView = mView.findViewById(R.id.sector_locked_textview);

        mReadableWritableRadioButton = mView.findViewById(R.id.readableWritableRadioButton);
        mReadableWritePWDProtectedRadioButton = mView.findViewById(R.id.readableWritePWDProtectedRadioButton);
        mReadWritePWDProtectedRadioButton = mView.findViewById(R.id.readWritePWDProtectedRadioButton);
        mReadPWDProtectedWriteImpossibleRadioButton = mView.findViewById(R.id.readPWDProtectedWriteImpossibleRadioButton);

        mNoPasswordRadioButton = mView.findViewById(R.id.noPasswordRadioButton);
        mPassword1RadioButton = mView.findViewById(R.id.password1RadioButton);
        mPassword2RadioButton = mView.findViewById(R.id.password2RadioButton);
        mPassword3RadioButton = mView.findViewById(R.id.password3RadioButton);

        Button updateTagButton = mView.findViewById(R.id.updateTagButton);
        updateTagButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                askConfirmation();
            }
        });
        mSectorAccessRightsRadioGroup = mView.findViewById(R.id.permissionsRadioGroup);
        mSectorAccessRightsRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                displayNewSectorSecurityStatusValue();
            }
        });
        mPasswordRadioGroup = (RadioGroup) mView.findViewById(R.id.passwordRadioGroup);
        mPasswordRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                displayNewSectorSecurityStatusValue();
            }
        });

        return mView;
    }

    private int getSectorAccessRightsValue(ReadWriteProtection sectorReadWriteProtection) {
        int sectorAccessRightsValue;

        switch (sectorReadWriteProtection) {
            default:
            case READABLE_AND_WRITE_PROTECTED_BY_PWD:
                sectorAccessRightsValue = 1;
                break;
            case READABLE_AND_WRITABLE:
                sectorAccessRightsValue = 0;
                break;
            case READ_AND_WRITE_PROTECTED_BY_PWD:
                sectorAccessRightsValue = 2;
                break;
            case READ_PROTECTED_BY_PWD_AND_WRITE_IMPOSSIBLE:
                sectorAccessRightsValue = 3;
                break;
        }

        return sectorAccessRightsValue;
    }

    private TagHelper.ReadWriteProtection getSectorAccessRightsInRG(int index) {
        TagHelper.ReadWriteProtection readWriteProtection;

        switch(index) {
            default:
            case 0:
                readWriteProtection = READABLE_AND_WRITE_PROTECTED_BY_PWD;
                break;
            case 1:
                readWriteProtection = READABLE_AND_WRITABLE;
                break;
            case 2:
                readWriteProtection = READ_AND_WRITE_PROTECTED_BY_PWD;
                break;
            case 3:
                readWriteProtection = READ_PROTECTED_BY_PWD_AND_WRITE_IMPOSSIBLE;
                break;
        }
        return readWriteProtection;
    }

    private int getPasswordValue(int sectorSecurityStatusValue) {
        int pwd;
        switch(sectorSecurityStatusValue & MASK_PASSWORD) {
            default:
            case 0: //No Password
                pwd = 0;
                break;
            case 1: // Password 1
                pwd = 1;
                break;
            case 2: // Password 2
                pwd = 2;
                break;
            case 3: // Password 3
                pwd = 3;
                break;
        }
        return pwd;
    }

    private void setSecurityStatusRadioButton(int value) {
        ReadWriteProtection readWriteProtection;

        if ((value & 0x01) == 0x01) {
            // b0 (aka "sector lock") is set.

            // Sector is locked. We should check b1b2 value to know the protection
            int b1b2 = (value & MASK_PROTECTION) >> 1;
            switch(b1b2) {
                case 0:
                    readWriteProtection = READABLE_AND_WRITE_PROTECTED_BY_PWD;
                    break;
                default:
                case 1:
                    readWriteProtection = READABLE_AND_WRITABLE;
                    break;
                case 2:
                    readWriteProtection = READ_AND_WRITE_PROTECTED_BY_PWD;
                    break;
                case 3:
                    readWriteProtection = READ_PROTECTED_BY_PWD_AND_WRITE_IMPOSSIBLE;
                    break;
            }
        } else {
            // b0 (aka "sector lock") is unset
            readWriteProtection = READABLE_AND_WRITABLE;
        }

        int pos = getSectorAccessRightsValue(readWriteProtection);

        int radioButtonId = mSectorAccessRightsRadioGroup.getChildAt(pos).getId();
        mSectorAccessRightsRadioGroup.check(radioButtonId);

    }
    private void setPasswordRadioButton(int value) {
        int pwd = (value & MASK_PASSWORD) >> 3;

        int radioButtonId = mPasswordRadioGroup.getChildAt(pwd).getId();
        mPasswordRadioGroup.check(radioButtonId);
    }

    private int displayNewSectorSecurityStatusValue() {
        byte passwordValue = 0;
        byte protectionValue = 0x00;

        // Get the Permission selected in the radio button
        if (mReadableWritableRadioButton.isChecked()) {
            protectionValue = 0x01;
        }
        if (mReadableWritePWDProtectedRadioButton.isChecked()) {
            protectionValue = 0x00;
        }
        if (mReadWritePWDProtectedRadioButton.isChecked()) {
            protectionValue = 0x02;
        }
        if (mReadPWDProtectedWriteImpossibleRadioButton.isChecked()) {
            protectionValue = 0x03;
        }

        // Get the Password number selected in the radio button
        if (mNoPasswordRadioButton.isChecked()) {
            passwordValue = 0;
        }
        if (mPassword1RadioButton.isChecked()) {
            passwordValue = 1;
        }
        if (mPassword2RadioButton.isChecked()) {
            passwordValue = 2;
        }
        if (mPassword3RadioButton.isChecked()) {
            passwordValue = 3;
        }

        byte newValue = (byte) (((protectionValue << 1) & MASK_PROTECTION) | ((passwordValue << 3) & MASK_PASSWORD) | 0x01);
        mNewSecurityStatusTextView.setText("0x" + convertByteToHexString(newValue));

        return newValue;
    }

    private void askConfirmation() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());

        // set title
        alertDialogBuilder.setTitle(getString(R.string.confirmation_needed));

        // set dialog message
        alertDialogBuilder
                .setMessage(getString(R.string.change_security_status_confirmation))
                .setCancelable(true)

                .setPositiveButton(getString(R.string.change_security_status),new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        dialog.cancel();
                        changeCurrentSecurityStatus();
                    }
                })
                .setNegativeButton(getString(R.string.cancel),new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        dialog.cancel();
                    }
                });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();// Refresh content
        fillView();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    protected class FillViewTask extends STFragment.FillViewTask {

        public FillViewTask() {

        }

        @Override
        protected Integer doInBackground(NFCTag... param) {
            if (myTag != null) {
                try {
                    mValue = ((SectorInterface) myTag).getSecurityStatus(mSectorNbr);
                } catch (STException e) {
                    return -1;
                }

                return 0;
            }
            return -1;
        }

        @Override
        protected void onPostExecute(Integer result) {

            if (result == 0) {
                if (mView != null) {
                    mSectorNumberTextview.setText(getResources().getString(R.string.sector, mSectorNbr));
                    displayCurrentSecurityStatusValue(mValue);
                }
            }
        }

    }

    private void displayCurrentSecurityStatusValue(byte value) {
        mCurrentSecurityStatusTextView.setText("0x" + convertByteToHexString(value));

        setSecurityStatusRadioButton(value);
        setPasswordRadioButton(value);

        if ((value & 0x01) == 0x01) {
            mSectorLockedWarningTextView.setVisibility(View.VISIBLE);
        } else {
            mSectorLockedWarningTextView.setVisibility(View.GONE);
        }
    }

    @Override
    public void fillView() {
        new FillViewTask().execute(myTag);
    }

    public void changeCurrentSecurityStatus() {
        final int value;
        value = displayNewSectorSecurityStatusValue();

        // 31 because acccording data sheet b7=b6=b5=0, hence max value is 31 0b11111
        if (value > 31 || value < 0) {
            STLog.e("Input error");
            showToast(R.string.update_failled_value_out_of_bounds);
            return;
        }

        new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    ((SectorInterface) myTag).setSecurityStatus(mSectorNbr, (byte) (value & 0xFF));
                } catch (STException e) {
                 //
                    showToast(R.string.sector_already_locked);
                    return;
                }
                mValue = (byte) (value & 0xFF);
                showToast(R.string.tag_updated);
            }
        }).start();

    }

}
