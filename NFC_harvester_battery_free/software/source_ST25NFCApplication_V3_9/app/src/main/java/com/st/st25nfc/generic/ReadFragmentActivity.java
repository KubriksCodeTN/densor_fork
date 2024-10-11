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
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import androidx.fragment.app.FragmentManager;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.st.st25nfc.R;
import com.st.st25nfc.generic.util.UIHelper;
import com.st.st25sdk.Helper;
import com.st.st25sdk.MultiAreaInterface;
import com.st.st25sdk.NFCTag;
import com.st.st25sdk.STException;
import com.st.st25sdk.STLog;
import com.st.st25sdk.type2.Type2Tag;
import com.st.st25sdk.type4a.FileControlTlvType4;
import com.st.st25sdk.type4a.STType4PasswordInterface;
import com.st.st25sdk.type4a.STType4Tag;
import com.st.st25sdk.type4a.Type4Tag;
import com.st.st25sdk.type5.STType5PasswordInterface;
import com.st.st25sdk.type5.STType5Tag;
import com.st.st25sdk.type5.Type5Tag;

import java.util.ArrayList;

import static com.st.st25nfc.generic.ReadFragmentActivity.ActionStatus.TAG_NOT_IN_THE_FIELD;
import static com.st.st25sdk.MultiAreaInterface.AREA1;

public class ReadFragmentActivity extends STFragmentActivity
        implements STFragment.STFragmentListener, View.OnClickListener,
        STType5PwdDialogFragment.STType5PwdDialogListener, PwdDialogFragment.PwdDialogListener{

    // Set here the Toolbar to use for this activity
    private int toolbar_res = R.menu.toolbar_empty;

    // The data are now read by Byte but we will still format the display by raw of 4 Bytes
    private final  int NBR_OF_BYTES_PER_RAW = 4;

    private int mStartAddress;
    private int mNumberOfBytes;

    private static final String TAG = "ReadFragmentActivity";
    private ListView lv;
    private CustomListAdapter mAdapter;

    private ContentViewAsync mContentView;

    private AsyncTaskWriteDataMessage mTaskWriteDataMessage;
    private boolean mIsAreaProtectedInWrite;
    private boolean mIsAreaProtectedInRead;

    private EditText mStartAddressEditText;
    private EditText mNbrOfBytesEditText;

    // For type 4 read in case of several area
    // Default value
    private int mAreaId = AREA1;

    private FloatingActionButton mFab;
    private View mChildView;

    private boolean mUnitInBytes;

    private byte[] mReadPassword;
    private byte[] mWritePassword;


    enum ActionStatus {
        ACTION_SUCCESSFUL,
        ACTION_FAILED,
        TAG_WRITE_PROTECTED,
        AREA_PASSWORD_NEEDED,
        TAG_NOT_IN_THE_FIELD
    };

    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.default_layout);

        // Inflate content of FrameLayout
        FrameLayout frameLayout=(FrameLayout) findViewById(R.id.frame_content);
        mChildView = getLayoutInflater().inflate(R.layout.fragment_read_memory, null);
        frameLayout.addView(mChildView);

        if (super.getTag() == null) {
            showToast(R.string.invalid_tag);
            goBackToMainActivity();
            return;
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.read_memory);

        // add back arrow to toolbar
        if (getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        mFab = (FloatingActionButton) findViewById(R.id.fab);
        mFab.setOnClickListener(this);

        mUnitInBytes = true;
        // units selector
        Spinner spinnerUnit = (Spinner)findViewById(R.id.spinner);
        String[] units = getResources().getStringArray(R.array.unit_readMemory);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, units);
        spinnerUnit.setAdapter(adapter);
        spinnerUnit.setSelection(0);
        spinnerUnit.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                String unit = (String) parent.getItemAtPosition(position);
                if (unit.contains("Bytes")) {
                    mUnitInBytes = true;
                } else {
                    // blocks
                    mUnitInBytes = false;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mUnitInBytes = true;
            }
        });

        mStartAddressEditText = (EditText) findViewById(R.id.startAddressEditText);
        mStartAddressEditText.setText(String.valueOf(0));

        mNbrOfBytesEditText = (EditText) findViewById(R.id.nbrOfBytesEditText);
        mNbrOfBytesEditText.setText(String.valueOf(64));

        // Retrieve parameters for start and UI configuration
        Intent mIntent = getIntent();
        // define the start address and numbers of bytes to read for display
        int startAddress = mIntent.getIntExtra("start_address", -1);
        int nbOfBytes = mIntent.getIntExtra("nb_of_bytes", -1);
        // define the data as an array of bytes to display
        byte[] data = mIntent.getByteArrayExtra("data");
        // define the file id for the display - Type4 Tags needed - replace start address and numbers of bytes
        int areaFileID = mIntent.getIntExtra("areaFileID", -1);
        if (areaFileID != -1) mAreaId = areaFileID;
        // information message
        String information = mIntent.getStringExtra("information");
        if (information == null) information = getString(R.string.hexadecimal_dump);

        // Manage the dump display for a Type4 Tag


        // Manage an hexa dump - no tag dependency
        if (data != null) {
            // UI setting
            configureUIItemsForHexaDumpOnly(information);
            // Displaying
            startDisplaying(data);
        } else if (areaFileID != -1 && getTag() instanceof Type4Tag) {
                // UI setting
                configureUIItemsForHexaDumpOnly(information);
                // start reading ...
                mAreaId = areaFileID;
                startType4ReadingAndDisplaying((Type4Tag) getTag(), areaFileID);
        } else if (startAddress >= 0 &&  nbOfBytes >=0 && getTag() instanceof Type5Tag) { // manage the dump with start address and number of bytes - Type5
            mStartAddress = startAddress;
            mNumberOfBytes = nbOfBytes;
            // UI setting
            configureUIItemsForHexaDumpOnly(information);
            // inform user that a read will be performed
            Snackbar snackbar = Snackbar.make(mChildView , "", Snackbar.LENGTH_LONG);
            snackbar.setAction(getString(R.string.reading_x_bytes_starting_y_address,mNumberOfBytes,mStartAddress), this);

            snackbar.setActionTextColor(getResources().getColor(R.color.white));
            snackbar.show();
            // start reading ...
            startType5ReadingAndDisplaying(mStartAddress,mNumberOfBytes);
        } else if (startAddress >= 0 &&  nbOfBytes >=0 && getTag() instanceof Type2Tag) { // manage the dump with start address and number of bytes - Type2
            mStartAddress = startAddress;
            mNumberOfBytes = nbOfBytes;
            // UI setting
            configureUIItemsForHexaDumpOnly(information);
            // inform user that a read will be performed
            Snackbar snackbar = Snackbar.make(mChildView , "", Snackbar.LENGTH_LONG);
            snackbar.setAction(getString(R.string.reading_x_bytes_starting_y_address,mNumberOfBytes,mStartAddress), this);

            snackbar.setActionTextColor(getResources().getColor(R.color.white));
            snackbar.show();
            // start reading ...
            startType2ReadingAndDisplaying(mStartAddress,mNumberOfBytes);
        } else {
            // default behaviour - user have to enter read parameters
            // Manage UI for Tag Type4
            if (getTag() instanceof Type4Tag) {
                // display layout for type4
                displayType4ReadSelectionParameters();
                fillType4SpinnerForSelection((Type4Tag)getTag());
            } else {
                displayType5ReadSelectionParameters();
            }
        }

    }

    private void fillType4SpinnerForSelection(Type4Tag tag){
        Spinner spinnerUnit = (Spinner)findViewById(R.id.areaIdSpinner);
        ArrayList<String> stringArrayList = new ArrayList<String>();

        try {
            int numberOfFiles = tag.getNbrOfFiles();
            for (int i =0; i<numberOfFiles;i++) {
                stringArrayList.add(getString(R.string.area_number_to_name) + (i+1));
            }
        } catch (STException e) {
            e.printStackTrace();
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, stringArrayList);
        spinnerUnit.setAdapter(adapter);
        spinnerUnit.setSelection(0);
        spinnerUnit.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                mAreaId = position+1;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                if (mAreaId == 0) mAreaId = 1;
            }
        });

    }

    private void removeType4ReadSelectionParameters(){
        LinearLayout type4LayoutParameters = (LinearLayout) findViewById(R.id.areaIdLayout);
        type4LayoutParameters.setVisibility(View.GONE);
    }
    private void displayType4ReadSelectionParameters() {
        LinearLayout type4LayoutParameters = (LinearLayout) findViewById(R.id.areaIdLayout);
        type4LayoutParameters.setVisibility(View.VISIBLE);
        removeType5ReadSelectionParameters();
    }
    private void removeType5ReadSelectionParameters(){
        LinearLayout startAddressLayout = (LinearLayout) findViewById(R.id.startAddressLayout);
        LinearLayout nbrOfBytesLayout = (LinearLayout) findViewById(R.id.nbrOfBytesLayout);
        startAddressLayout.setVisibility(View.GONE);
        nbrOfBytesLayout.setVisibility(View.GONE);
        LinearLayout unitLayout = (LinearLayout) findViewById(R.id.unitLayout);
        unitLayout.setVisibility(View.GONE);
    }

    private void displayType5ReadSelectionParameters() {
        LinearLayout startAddressLayout = (LinearLayout) findViewById(R.id.startAddressLayout);
        LinearLayout nbrOfBytesLayout = (LinearLayout) findViewById(R.id.nbrOfBytesLayout);
        startAddressLayout.setVisibility(View.VISIBLE);
        nbrOfBytesLayout.setVisibility(View.VISIBLE);
        LinearLayout unitLayout = (LinearLayout) findViewById(R.id.unitLayout);
        unitLayout.setVisibility(View.VISIBLE);
        removeType4ReadSelectionParameters();
    }

    private void configureUIItemsForHexaDumpOnly (String information) {
        LinearLayout startAddressLayout = (LinearLayout) findViewById(R.id.startAddressLayout);
        LinearLayout nbrOfBytesLayout = (LinearLayout) findViewById(R.id.nbrOfBytesLayout);
        startAddressLayout.setVisibility(View.GONE);
        nbrOfBytesLayout.setVisibility(View.GONE);

        LinearLayout informationLayout = (LinearLayout) findViewById(R.id.informationLayout);
        informationLayout.setVisibility(View.VISIBLE);
        TextView informationTextView = (TextView) findViewById(R.id.informationTextView);
        if (information != null) {
            informationTextView.setText(information);
        }
        this.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        mFab.setVisibility(View.GONE);
        // remove unit selection  - default in Bytes
        LinearLayout unitLayout = (LinearLayout) findViewById(R.id.unitLayout);
        unitLayout.setVisibility(View.GONE);
    }

    private int getMemoryAreaSizeInBytes(Type4Tag myTag, int area) {
        int memoryAreaSizeInBytes = 0;
        try {
            if (myTag instanceof STType4Tag) {

                int fileId = UIHelper.getType4FileIdFromArea(area);
                FileControlTlvType4 controlTlv = ((STType4Tag) myTag).getCCFileTlv(fileId);

                memoryAreaSizeInBytes = controlTlv.getMaxFileSize();

            }  else {
                if (myTag instanceof MultiAreaInterface) {
                    memoryAreaSizeInBytes = ((MultiAreaInterface) myTag).getAreaSizeInBytes(area);
                } else {
                    memoryAreaSizeInBytes = myTag.getMemSizeInBytes();
                }
            }
        } catch (STException e) {
            e.printStackTrace();
        }
        return memoryAreaSizeInBytes;
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
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle arrow click here
        if (item.getItemId() == android.R.id.home) {
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    class ContentViewAsync extends AsyncTask<Void, Integer, Boolean> implements AdapterView.OnItemLongClickListener, BlockRawDataEditorDialog.BlockRawDataEditorDialogListener
    {
        byte mBuffer[] = null;
        NFCTag mTag;
        // Default value
        int  mArea = AREA1;

        public ContentViewAsync(NFCTag myTag, int myArea) {
            mTag = myTag;
            mArea = myArea;
        }

        public ContentViewAsync(byte[] buffer) {
            mBuffer = buffer;
        }
        private ContentViewAsync(NFCTag myTag, int myArea, byte[] buffer) {
            mTag = myTag;
            mArea = myArea;
            mBuffer = buffer;
        }

        protected Boolean doInBackground(Void...arg0) {
            if (mBuffer == null) {
                try {
                    if (UIHelper.isAType4Tag(mTag)) {
                        // Tag type 4
                        int size = getMemoryAreaSizeInBytes(((Type4Tag) mTag), mArea);
                        mNumberOfBytes = size;
                        mStartAddress = 0;

                        int fileId = UIHelper.getType4FileIdFromArea(mArea);

                        // inform user that a read will be performed
                        snackBarUiThread();
                        if (mReadPassword != null && mTag instanceof STType4Tag) {
                            mBuffer = ((STType4Tag) mTag).readBytes(fileId, 0, size, mReadPassword);
                        } else {
                            mBuffer = ((Type4Tag) mTag).readBytes(fileId, 0, size);
                        }
                        mReadPassword = null;
                        int nbrOfBytesRead = 0;
                        if (mBuffer != null) {
                            nbrOfBytesRead = mBuffer.length;
                        }
                        if (nbrOfBytesRead != mNumberOfBytes) {
                            showToast(R.string.error_during_read_operation, nbrOfBytesRead);
                        }
                    } else if (UIHelper.isAType5Tag(mTag)){
                        if (mArea == -1) {
                            mAreaId = getAreaIdFromAddressInBytesForType5Tag(mStartAddress);
                        }
                        if (mAreaId == -1) {
                            // An issue occured retrieving AreaId from Address
                            // Address is probably invalid
                            showToast(R.string.invalid_value);
                            return false;

                        } else {
                            // Type 5
                            mBuffer = getTag().readBytes(mStartAddress, mNumberOfBytes);
                            // Warning: readBytes() may return less bytes than requested
                            int nbrOfBytesRead = 0;
                            if (mBuffer != null) {
                                nbrOfBytesRead = mBuffer.length;
                            }
                            if (nbrOfBytesRead != mNumberOfBytes) {
                                showToast(R.string.error_during_read_operation, nbrOfBytesRead);
                            }
                        }
                    } else if (UIHelper.isAType2Tag(mTag)){
                        if (mArea == -1) {
                            mAreaId = getAreaIdFromAddressInBytesForType2Tag(mStartAddress);
                        }
                        if (mAreaId == -1) {
                            // An issue occured retrieving AreaId from Address
                            // Address is probably invalid
                            showToast(R.string.invalid_value);
                            return false;

                        } else {
                            mBuffer = getTag().readBytes(mStartAddress, mNumberOfBytes);
                            // Warning: readBytes() may return less bytes than requested
                            int nbrOfBytesRead = 0;
                            if (mBuffer != null) {
                                nbrOfBytesRead = mBuffer.length;
                            }
                            if (nbrOfBytesRead != mNumberOfBytes) {
                                showToast(R.string.error_during_read_operation, nbrOfBytesRead);
                            }
                        }
                    } else {
                        // An issue occured retrieving AreaId from Address
                        // Tag type not yet handled
                        showToast(R.string.invalid_value);
                        return false;
                    }
                } catch (STException e) {
                    mReadPassword = null;

                    switch (e.getError()) {
                        case TAG_NOT_IN_THE_FIELD:
                            showToast(R.string.tag_not_in_the_field);
                            break;
                        case PASSWORD_NEEDED:
                        case ISO15693_BLOCK_IS_LOCKED:
                        case ISO15693_BLOCK_PROTECTED:
                        case WRONG_SECURITY_STATUS:
                            showToast(R.string.area_protected_in_read);
                            if (mTag instanceof Type5Tag) {
                                // Type 5
                                // Check that targeted bytes not in several areas
                                if (!isTargetedBytesInOneArea(mStartAddress, mNumberOfBytes)) {
                                    // display information message that write on two areas protected....
                                    snackBarUiThreadWithMessage(getString(R.string.read_of_several_area_report));
                                    return false;
                                }
                            }
                            mIsAreaProtectedInRead = true;
                            showReadPasswordDialog(mAreaId);

                            break;
                        default:
                            showToast(R.string.Command_failed);
                    }
                    Log.e(TAG, e.getMessage());
                    return false;
                }

            } else {
                // buffer already initialized by constructor - no need to read Tag.
                // Nothing to do
            }
            return true;
        }

        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            if (mBuffer != null && result == true) {
                TextView readBlocksInfoListView = (TextView) mChildView.findViewById(R.id.readBlocksInfoListView);
                readBlocksInfoListView.setVisibility(View.VISIBLE);
                mAdapter = new CustomListAdapter(mBuffer);
                lv = (ListView) findViewById(R.id.readBlocksListView);
                lv.setAdapter(mAdapter);
                lv.setOnItemLongClickListener(this);
            }

        }
        public void selfRestart() {
            mContentView = new ContentViewAsync(mTag,mAreaId,mBuffer);
            mContentView.execute();
        }

        private void snackBarUiThread(){
            runOnUiThread (new Thread(new Runnable() {
                public void run() {
                    // inform user that a read will be performed
                    Snackbar snackbar = Snackbar.make(mChildView , "", Snackbar.LENGTH_LONG);
                    snackbar.setText(getString(R.string.reading_x_bytes_starting_y_address,mNumberOfBytes,mStartAddress));
                    // Need this to solve issue displaying text in White
                    View view = snackbar.getView();
                    TextView tv = (TextView) view.findViewById(com.google.android.material.R.id.snackbar_text);
                    tv.setTextColor(getResources().getColor(R.color.white));
                    snackbar.setActionTextColor(getResources().getColor(R.color.white));
                    snackbar.show();
                }
            }));
        }
        private void snackBarUiThreadWithMessage(final String message){
            runOnUiThread (new Thread(new Runnable() {
                public void run() {
                    // inform user that a read will be performed
                    Snackbar snackbar = Snackbar.make(mChildView , "", Snackbar.LENGTH_LONG);
                    snackbar.setText(message);
                    // Need this to solve issue displaying text in White
                    View view = snackbar.getView();
                    TextView tv = (TextView) view.findViewById(com.google.android.material.R.id.snackbar_text);
                    tv.setTextColor(getResources().getColor(R.color.red));
                    snackbar.setActionTextColor(getResources().getColor(R.color.red));
                    snackbar.show();
                }
            }));
        }
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int pos, long id) {
            String data;
            byte[] myByte = new byte[NBR_OF_BYTES_PER_RAW];
            int address;

            // The data are now read by Byte but we will still format the display by raw of 4 Bytes

            // Get the 4 Bytes to display on this raw
            address = pos * NBR_OF_BYTES_PER_RAW;
            if(address < mBuffer.length) {
                myByte[0] = mBuffer[address];

            }
            address = pos * NBR_OF_BYTES_PER_RAW + 1;
            if(address < mBuffer.length) {
                myByte[1] = mBuffer[address];
            }

            address = pos * NBR_OF_BYTES_PER_RAW + 2;
            if(address < mBuffer.length) {
                myByte[2] = mBuffer[address];
            }

            address = pos * NBR_OF_BYTES_PER_RAW + 3;
            if(address < mBuffer.length) {
                myByte[3] = mBuffer[address];
            }

            String addressTxt = String.format("%s %3d: ", getResources().getString(R.string.addr), mStartAddress + pos * NBR_OF_BYTES_PER_RAW);

            BlockRawDataEditorDialog rawEditor = BlockRawDataEditorDialog.newInstance(
                    getString(R.string.raw_data_update),
                    addressTxt,
                    getSupportFragmentManager(),
                    this,
                    NBR_OF_BYTES_PER_RAW,
                    myByte,pos * NBR_OF_BYTES_PER_RAW);
            rawEditor.show(getSupportFragmentManager(), "BlockRawDataEditorDialog");
            return false;
        }

        @Override
        public void onBlockRawDataUpdateDialogFinish(int result, byte[] data, int pos) {
            if (result == BlockRawDataEditorDialog.RESULT_OK) {
                if (data != null) {
                    mTaskWriteDataMessage = new AsyncTaskWriteDataMessage(getTag(),data,pos);
                    mTaskWriteDataMessage.execute();
                }
            }
        }

    }
// FOR PWD

    private void showWritePasswordDialog(int areaID) {

        if(UIHelper.isAType5Tag(getTag())) {
            new AsyncTaskDisplayPasswordDialogBoxForType5Tag(areaID).execute();

        } else if(UIHelper.isAType4Tag(getTag())) {
            new AsyncTaskDisplayWritePasswordDialogBoxForType4Tag().execute();

        } else {
            // Tag type not supported yet
        }
    }


    private void showReadPasswordDialog(int areaID) {
        if(UIHelper.isAType5Tag(getTag())) {
            new AsyncTaskDisplayPasswordDialogBoxForType5Tag(areaID).execute();

        } else if(UIHelper.isAType4Tag(getTag())) {
            new AsyncTaskDisplayReadPasswordDialogBoxForType4Tag(this).execute();

        } else if(UIHelper.isAType2Tag(getTag())) {
            Log.i(TAG, "Information! This tag doesn't have a read password interface!");

        } else {
            // Tag type not supported yet
            Log.i(TAG, "Information! This tag doesn't have a read password interface!");
        }
    }
    /**
     * AsyncTask retrieving the passwordNumber corresponding to an area of a Type5 tag.
     * When the password number is available, a password dialog box is displayed.
     */
    private class AsyncTaskDisplayPasswordDialogBoxForType5Tag extends AsyncTask<Void, Void, ActionStatus> {

        int mPasswordNumber;
        int mAreaID;

        public AsyncTaskDisplayPasswordDialogBoxForType5Tag(int areaId) {
            mAreaID = areaId;
        }

        @Override
        protected ActionStatus doInBackground(Void... param) {
            ActionStatus result = ActionStatus.ACTION_FAILED;

            if (getTag() instanceof STType5PasswordInterface) {
                try {
                    mPasswordNumber = ((STType5PasswordInterface) getTag()).getPasswordNumber(mAreaID );
                    result = ActionStatus.ACTION_SUCCESSFUL;

                } catch (STException e) {
                    e.printStackTrace();
                }
            } else {
                Log.e(TAG, "Error! This tag doesn't have a password interface!");
            }

            return result;
        }

        @Override
        protected void onPostExecute(ActionStatus actionStatus) {
            switch(actionStatus) {
                case ACTION_SUCCESSFUL:
                    String dialogMsg = getResources().getString(R.string.enter_area_pwd, UIHelper.getAreaName(mAreaId));

                    FragmentManager fragmentManager = getSupportFragmentManager();

                    STType5PwdDialogFragment pwdDialogFragment = STType5PwdDialogFragment.newInstance(
                            STType5PwdDialogFragment.STPwdAction.PRESENT_CURRENT_PWD,
                            mPasswordNumber,
                            dialogMsg,
                            ReadFragmentActivity.this);
                    if(pwdDialogFragment!=null) pwdDialogFragment.show(fragmentManager, "pwdDialogFragment");
                    break;

                case ACTION_FAILED:
                    showToast(R.string.error_while_reading_the_tag);
                    break;

                case TAG_NOT_IN_THE_FIELD:
                    showToast(R.string.tag_not_in_the_field);
                    break;
            }

            return;
        }
    }

    private class AsyncTaskDisplayReadPasswordDialogBoxForType4Tag extends AsyncTask<Void, Void, ActionStatus> {
        private ReadFragmentActivity mFragment;

        public AsyncTaskDisplayReadPasswordDialogBoxForType4Tag(ReadFragmentActivity readFragmentActivity) {
            mFragment = readFragmentActivity;
        }

        @Override
        protected ActionStatus doInBackground(Void... param) {
            ActionStatus result = ActionStatus.ACTION_FAILED;

            try {
                // Request Read Password
                int fileId = UIHelper.getType4FileIdFromArea(mAreaId);

                PwdDialogFragment pwdDialogFragment = PwdDialogFragment.newInstance(getString(R.string.enter_read_password),
                        getSupportFragmentManager(),
                        mFragment,
                        ((STType4PasswordInterface) getTag()).getReadPasswordLengthInBytes(mAreaId));
                mReadPassword = pwdDialogFragment.getPassword();
            }
            catch (STException e) {
                return result;
            }

            result = ActionStatus.ACTION_SUCCESSFUL;
            return result;
        }
    }


    private class AsyncTaskDisplayWritePasswordDialogBoxForType4Tag extends AsyncTask<Void, Void, ActionStatus> {

        public AsyncTaskDisplayWritePasswordDialogBoxForType4Tag() {

        }

        @Override
        protected ActionStatus doInBackground(Void... param) {
            ActionStatus result = ActionStatus.ACTION_FAILED;

            try {
                // Request write password
                PwdDialogFragment pwdDialogFragment = PwdDialogFragment.newInstance(getString(R.string.enter_write_password),
                        getSupportFragmentManager(),
                        ReadFragmentActivity.this,
                        ((STType4PasswordInterface) getTag()).getWritePasswordLengthInBytes(mAreaId));
                mWritePassword = pwdDialogFragment.getPassword();
            } catch (STException e) {
                return result;
            }

            result = ActionStatus.ACTION_SUCCESSFUL;
            return result;
        }
    }

    @Override
    public void onPwdDialogFinish(int result, byte[] password) {
        if (password != null)
            onSTType5PwdDialogFinish(result);
    }

    @Override
    public void onSTType5PwdDialogFinish(int result) {
        Log.v(TAG, "onSTType5PwdDialogFinish. result = " + result);
        if (result == PwdDialogFragment.RESULT_OK) {
                // The password was requested because the area is protected in read
                // The area is now unlocked so we can refresh the display
            if (mIsAreaProtectedInRead) {
                mIsAreaProtectedInRead = false;
                mContentView.selfRestart();
            }
            if (mIsAreaProtectedInWrite) {
                mIsAreaProtectedInWrite = false;
                mTaskWriteDataMessage.selfRestart();
            }
        } else {
            Log.e(TAG, "Failed to unlock the area!");
            if (mIsAreaProtectedInRead) {
                mIsAreaProtectedInRead = false;
            }
            if (mIsAreaProtectedInWrite) {
                mIsAreaProtectedInWrite = false;
            }
        }

    }
    // END PWD


    /**
     * AsyncTask writing the data to the tag
     */
    protected class AsyncTaskWriteDataMessage extends AsyncTask<Void, Void, ActionStatus> {
        byte[] mMessageData;
        int mMemoryOffsetData;
        NFCTag mNFCTag;
        int mRetrievedAreaID = mAreaId;

        public AsyncTaskWriteDataMessage(NFCTag tag, byte[] data, int offset) {
            mMessageData = data;
            mMemoryOffsetData = offset;
            mNFCTag = tag;
        }
        public void selfRestart() {
            mTaskWriteDataMessage = new AsyncTaskWriteDataMessage(mNFCTag,mMessageData,mMemoryOffsetData);
            mTaskWriteDataMessage.execute();
        }
        @Override
        protected ActionStatus doInBackground(Void... param) {
            ActionStatus result = ActionStatus.ACTION_FAILED;

            if (mMessageData != null ) {
                UIHelper.displayCircularProgressBar(ReadFragmentActivity.this, getString(R.string.please_wait));
                try {
                    if (UIHelper.isAType4Tag(mNFCTag)) {
                        // Tag type 4
                        int fileId = UIHelper.getType4FileIdFromArea(mRetrievedAreaID);
                        if (mWritePassword != null && mNFCTag instanceof STType4Tag) {
                            ((STType4Tag) mNFCTag).writeBytes(fileId,mMemoryOffsetData, mMessageData, mWritePassword);
                        } else {
                            ((Type4Tag) mNFCTag).writeBytes(fileId,mMemoryOffsetData, mMessageData);
                        }
                        result = ActionStatus.ACTION_SUCCESSFUL;
                    } else if (UIHelper.isAType5Tag(mNFCTag)){
                        // Type 5
                        // retrieve the AreaID
                        mRetrievedAreaID = getAreaIdFromAddressInBytesForType5Tag(mStartAddress+mMemoryOffsetData);
                        if (mRetrievedAreaID == -1) {
                            // An issue occured retrieving AreaId from Address
                            // Address is probably invalid or write overlap capacity
                            snackBarUiThreadWithMessage(getString(R.string.invalid_value));
                        } else {
                            mNFCTag.writeBytes(mStartAddress+mMemoryOffsetData, mMessageData);
                            result = ActionStatus.ACTION_SUCCESSFUL;
                        }
                    } else if (UIHelper.isAType2Tag(mNFCTag)){
                        // Type 2
                        // retrieve the AreaID
                        mRetrievedAreaID = getAreaIdFromAddressInBytesForType2Tag(mStartAddress+mMemoryOffsetData);
                        if (mRetrievedAreaID == -1) {
                            // An issue occured retrieving AreaId from Address
                            // Address is probably invalid or write overlap capacity
                            snackBarUiThreadWithMessage(getString(R.string.invalid_value));
                        } else {
                            mNFCTag.writeBytes(mStartAddress+mMemoryOffsetData, mMessageData);
                            result = ActionStatus.ACTION_SUCCESSFUL;

                        }
                    } else {
                        // Tag not yet handled
                        snackBarUiThreadWithMessage(getString(R.string.invalid_value));
                        Log.e(TAG, "Tag type not Handled");

                    }
                    if (result == ActionStatus.ACTION_SUCCESSFUL) {
                        mWritePassword = null;
                        UIHelper.invalidateCache(mNFCTag);
                    }
                    UIHelper.dismissCircularProgressBar();

                }catch (STException e) {
                    UIHelper.dismissCircularProgressBar();

                    switch (e.getError()) {
                        case WRONG_SECURITY_STATUS:
                        case ISO15693_BLOCK_PROTECTED:
                        case ISO15693_BLOCK_IS_LOCKED:
                            result = ActionStatus.TAG_WRITE_PROTECTED;
                            break;

                        case TAG_NOT_IN_THE_FIELD:
                            result = TAG_NOT_IN_THE_FIELD;
                            break;

                        default:
                            e.printStackTrace();
                            break;
                    }
                }

            } else {
                // define error
            }
            return result;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(ActionStatus actionStatus) {
            UIHelper.dismissCircularProgressBar();
            switch (actionStatus) {
                case ACTION_SUCCESSFUL:
                    showToast(R.string.tag_updated);
                    startSmartTagRead();
                    break;

                case TAG_WRITE_PROTECTED:
                    showToast(R.string.write_permission);
                    if(UIHelper.isAType5Tag(mNFCTag))  {
                        // Type 5
                        // Check that targeted bytes not in several areas
                        if (!isTargetedBytesInOneArea(mStartAddress+mMemoryOffsetData, mMessageData.length)) {
                            // display information message that write on two areas protected....
                            snackBarUiThreadWithMessage(getString(R.string.write_to_several_area_report));
                            return;
                        }
                    }
                    mIsAreaProtectedInWrite = true;
                    showWritePasswordDialog(mRetrievedAreaID);
                    break;

                case TAG_NOT_IN_THE_FIELD:
                    showToast(R.string.tag_not_in_the_field);
                    break;

                case ACTION_FAILED:
                default:
                    showToast(R.string.command_failed);
                    break;
            }
        }
        private void snackBarUiThreadWithMessage(final String message){
            runOnUiThread (new Thread(new Runnable() {
                public void run() {
                    // inform user that a read will be performed
                    Snackbar snackbar = Snackbar.make(mChildView , "", Snackbar.LENGTH_LONG);
                    snackbar.setText(message);
                    // Need this to solve issue displaying text in White
                    View view = snackbar.getView();
                    TextView tv = (TextView) view.findViewById(com.google.android.material.R.id.snackbar_text);
                    tv.setTextColor(getResources().getColor(R.color.red));
                    snackbar.setActionTextColor(getResources().getColor(R.color.red));
                    snackbar.show();
                }
            }));
        }
    }

    private int convertItemToBytesUnit(int value) {
        return value * NBR_OF_BYTES_PER_RAW;
    }

    @Override
    public void onClick(View v) {
        if (startSmartTagReadGetParametersFromUI()) {
            startSmartTagRead();
        }
    }

    private boolean startSmartTagReadGetParametersFromUI () {
        Boolean ret = true;
        try {
            if (mUnitInBytes) {
                mStartAddress = Integer.parseInt(mStartAddressEditText.getText().toString());
            } else {
                int valInBlock = Integer.parseInt(mStartAddressEditText.getText().toString());
                mStartAddress = convertItemToBytesUnit(valInBlock);
            }
        } catch (Exception e) {
            STLog.e("Bad Start Address" + e.getMessage());
            showToast(R.string.bad_start_address);
            ret = false;
        }

        try {
            if (mUnitInBytes) {
                mNumberOfBytes = Integer.parseInt(mNbrOfBytesEditText.getText().toString());
            } else {
                int valInBlock = Integer.parseInt(mNbrOfBytesEditText.getText().toString());
                mNumberOfBytes = convertItemToBytesUnit(valInBlock);
            }
        } catch (Exception e) {
            STLog.e("Bad Numbers of Bytes" + e.getMessage());
            showToast(R.string.bad_number_of_bytes);
            ret = false;
        }

        // Hide Soft Keyboard
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }

        return ret;
    }

    private void startSmartTagRead () {
        if (getTag() instanceof Type5Tag) {
            startType5ReadingAndDisplaying(mStartAddress, mNumberOfBytes);
        } else if (getTag() instanceof Type2Tag) {
            startType2ReadingAndDisplaying(mStartAddress, mNumberOfBytes);
        } else if (getTag() instanceof Type4Tag) {
            // by defaut - read first area
            startType4ReadingAndDisplaying(getTag(), mAreaId);
        }
    }

    private int getAreaIdFromAddressInBytesForType5Tag(int address) {
        int ret = AREA1;
        if (getTag() instanceof MultiAreaInterface && getTag() instanceof STType5Tag) {
            MultiAreaInterface tag = (MultiAreaInterface) getTag();
            try {
                ret = tag.getAreaFromByteAddress(address);
            } catch (STException e) {
                ret = -1;
            }
        }
        return ret;
    }

    private int getAreaIdFromAddressInBytesForType2Tag(int address) {
        // only one AREA for this type of Tag for time being
        int ret = AREA1;
        return ret;
    }

    private boolean isTargetedBytesInOneArea(int address, int numberOfBytes) {
        boolean ret = true;
        if (getTag() instanceof MultiAreaInterface && getTag() instanceof STType5Tag) {
            MultiAreaInterface tag = (MultiAreaInterface) getTag();
            try {
                ret = (tag.getAreaFromByteAddress(address) == tag.getAreaFromByteAddress(address + numberOfBytes -1));
            } catch (STException e) {
                // bad address or address and length over capacity
                ret = false;
            }
        }
        return ret;
    }
    private void startType5ReadingAndDisplaying (int startAddress, int numberOfBytes) {
        mStartAddress = startAddress;
        mNumberOfBytes = numberOfBytes;
        // Start retrieving information with an area id not initialised
        Snackbar snackbar = Snackbar.make(mChildView, "", Snackbar.LENGTH_LONG);
        snackbar.setAction(getString(R.string.reading_x_bytes_starting_y_address, mNumberOfBytes, mStartAddress), this);

        snackbar.setActionTextColor(getResources().getColor(R.color.white));
        snackbar.show();

        mContentView = new ContentViewAsync(getTag(), -1);
        mContentView.execute();

    }

    private void startType2ReadingAndDisplaying (int startAddress, int numberOfBytes) {
        mStartAddress = startAddress;
        mNumberOfBytes = numberOfBytes;
        // Start retrieving information with an area id not initialised
        Snackbar snackbar = Snackbar.make(mChildView, "", Snackbar.LENGTH_LONG);
        snackbar.setAction(getString(R.string.reading_x_bytes_starting_y_address, mNumberOfBytes, mStartAddress), this);

        snackbar.setActionTextColor(getResources().getColor(R.color.white));
        snackbar.show();

        mContentView = new ContentViewAsync(getTag(), -1);
        mContentView.execute();

    }
    private void startDisplaying (byte[] data) {
        mContentView = new ContentViewAsync(data);
        mContentView.execute();
    }
    private void startType4ReadingAndDisplaying(NFCTag tag, int area) {
        mContentView = new ContentViewAsync(tag, area);
        mContentView.execute();
    }

    public void onPause() {
        if (mContentView != null)
                mContentView.cancel(true);

        super.onPause();
    }

    class CustomListAdapter extends BaseAdapter {

        byte[] mBuffer;

        public CustomListAdapter(byte[] buffer) {

            mBuffer = buffer;
        }

        //get read_list_items count
        @Override
        public int getCount() {
            try {
                return Helper.divisionRoundedUp(mBuffer.length, NBR_OF_BYTES_PER_RAW);
            } catch (STException e) {
                e.printStackTrace();
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
        public View getView(int pos, View convertView, ViewGroup parent) {
            View listItem = convertView;
            String data;
            Byte myByte;
            int address;
            char char1 = ' ';
            char char2 = ' ';
            char char3 = ' ';
            char char4 = ' ';
            String byte1Str = "  ";
            String byte2Str = "  ";
            String byte3Str = "  ";
            String byte4Str = "  ";

            // The data are now read by Byte but we will still format the display by raw of 4 Bytes

            // Get the 4 Bytes to display on this raw
            address = pos * NBR_OF_BYTES_PER_RAW;
            if(address < mBuffer.length) {
                myByte = mBuffer[address];
                byte1Str = Helper.convertByteToHexString(myByte).toUpperCase();
                char1 = getChar(myByte);
            }

            address = pos * NBR_OF_BYTES_PER_RAW + 1;
            if(address < mBuffer.length) {
                myByte = mBuffer[address];
                byte2Str = Helper.convertByteToHexString(myByte).toUpperCase();
                char2 = getChar(myByte);
            }

            address = pos * NBR_OF_BYTES_PER_RAW + 2;
            if(address < mBuffer.length) {
                myByte = mBuffer[address];
                byte3Str = Helper.convertByteToHexString(myByte).toUpperCase();
                char3 = getChar(myByte);
            }

            address = pos * NBR_OF_BYTES_PER_RAW + 3;
            if(address < mBuffer.length) {
                myByte = mBuffer[address];
                byte4Str = Helper.convertByteToHexString(myByte).toUpperCase();
                char4 = getChar(myByte);
            }

            if (listItem == null) {
                //set the main ListView's layout
                listItem = getLayoutInflater().inflate(R.layout.read_fragment_item, parent, false);
            }

            TextView addresssTextView = (TextView) listItem.findViewById(R.id.addrTextView);
            TextView hexValuesTextView = (TextView) listItem.findViewById(R.id.hexValueTextView);
            TextView asciiValueTextView = (TextView) listItem.findViewById(R.id.asciiValueTextView);

            String startAddress = String.format("%s %3d: ", getResources().getString(R.string.addr), mStartAddress + pos * NBR_OF_BYTES_PER_RAW);
            addresssTextView.setText(startAddress);

            data = String.format("%s %s %s %s", byte1Str, byte2Str, byte3Str, byte4Str);
            hexValuesTextView.setText(data);

            data = String.format("  %c%c%c%c", char1, char2, char3, char4);
            asciiValueTextView.setText(data);

            return listItem;
        }
    }

    private char getChar(byte myByte) {
        char myChar = ' ';

        if(myByte > 0x20) {
            myChar = (char) (myByte & 0xFF);
        }

        return myChar;
    }

}

