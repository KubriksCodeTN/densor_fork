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

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.st.st25sdk.Helper;
import com.st.st25nfc.R;
import com.st.st25sdk.STException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class FillMemoryFragmentActivity extends STFragmentActivity
        implements STFragment.STFragmentListener, View.OnClickListener {

    // Set here the Toolbar to use for this activity
    private ST25Menu mMenu;
    private int toolbar_res = R.menu.toolbar_empty;

    // Address at which we will write the data
    static private int mDestinationByteAddress;

    // Data to write to memory
    private byte[] mBuffer;

    private EditText mDestinationOffsetEditText;
    private TextView mSourceFileTextView;

    // The data are now read by Byte but we will still format the display by raw of 4 Bytes
    private final  int NBR_OF_BYTES_PER_RAW = 4;

    private static final String TAG = "FillMemory";
    private ListView lv;
    private Handler mHandler;
    private CustomListAdapter mAdapter;
    private Thread mThread;
    private Uri mSelectedfileUri;

    private Button mSelectSourceFileButton;
    private FloatingActionButton mFloatingActionButton;

    private final int READ_PERMISSION = 2;


    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.default_layout);

        // Inflate content of FrameLayout
        FrameLayout frameLayout=(FrameLayout) findViewById(R.id.frame_content);
        View childView = getLayoutInflater().inflate(R.layout.fragment_fill_memory, null);
        frameLayout.addView(childView);

        if (super.getTag() == null) {
            showToast(R.string.invalid_tag);
            goBackToMainActivity();
            return;
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.fill_memory_from_file);

        // add back arrow to toolbar
        if (getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        mFloatingActionButton = (FloatingActionButton) findViewById(R.id.fab);
        mFloatingActionButton.setOnClickListener(this);

        mSourceFileTextView = (TextView) findViewById(R.id.sourceFileTextView);
        mSelectedfileUri = null;

        mDestinationOffsetEditText = (EditText) findViewById(R.id.destinationOffsetEditText);
        mDestinationOffsetEditText.setText(Helper.convertIntToHexFormatString(mDestinationByteAddress));

        mHandler = new Handler();

        showDownloadDirectoryContent();

        mSelectSourceFileButton = (Button) findViewById(R.id.selectSourceFileButton);
        mSelectSourceFileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = getString(R.string.select_a_file);

                Intent intent = new Intent()
                        .setType("*/*")
                        .setAction(Intent.ACTION_GET_CONTENT);

                // Special intent for Samsung file manager
                Intent sIntent = new Intent("com.sec.android.app.myfiles.PICK_DATA");
                sIntent.addCategory(Intent.CATEGORY_DEFAULT);

                Intent chooserIntent;
                if (getPackageManager().resolveActivity(sIntent, 0) != null){
                    // it is device with samsung file manager
                    chooserIntent = Intent.createChooser(sIntent, message);
                    chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] { intent});
                }
                else {
                    chooserIntent = Intent.createChooser(intent, message);
                }

                startActivityForResult(chooserIntent, 0);
            }
        });

        // For Android version >= 10, it is no more necessary to ask for this permission
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_PERMISSION);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult (int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch(requestCode) {
            case READ_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                    showToast(R.string.cannot_continue_read_without_permission);
                    // Disable the action buttons
                    mSelectSourceFileButton.setEnabled(false);
                    mFloatingActionButton.setEnabled(false);
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==0 && resultCode==RESULT_OK) {
            if (data != null) {
                mSelectedfileUri = data.getData(); //The uri with the location of the file
                if (mSelectedfileUri != null) {
                    mSourceFileTextView.setText(mSelectedfileUri.getPath());
                } else {
                    String filename = data.getDataString();
                    if (filename != null) {
                        mSourceFileTextView.setText(Uri.parse(filename).getPath());
                    }
                }
            }
        }
    }

    private void showDownloadDirectoryContent() {
        File downloadFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (!downloadFolder.exists()) {
            downloadFolder.mkdir();
        }

        /* Show the list of files present in the Downloads directory */
        Log.v(TAG, "Files and folders present in Download directory:");
        File[] filesList = downloadFolder.listFiles();
        if(filesList != null) {
            for (File file : filesList) {
                Log.v(TAG, file.getName() );
            }
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

    /**
     * Parses the NdefSTMessage Message from the intent and prints to the TextView
     */
    TextView textView;

    void processIntent(Intent intent) {
        Log.d(TAG, "Process Intent");
    }

    class ContentView implements Runnable {
        public void run() {
            byte buffer[] = null;
            boolean writingOk = false;
            lv = (ListView) findViewById(R.id.writeBlockListView);

            try {
                getTag().writeBytes(mDestinationByteAddress, mBuffer);
                writingOk = true;
                // Re-read the data and display them
                buffer = getTag().readBytes(mDestinationByteAddress, mBuffer.length);
                // Warning: readBytes() may return less bytes than requested
                if (buffer != null && buffer.length != mBuffer.length) {
                    showToast(R.string.error_during_read_operation, buffer.length);
                }

            } catch (STException e) {
                if (e.getMessage() != null) {
                    Log.e(TAG, e.getMessage());
                } else {
                    Log.e(TAG, "Command failed");
                }
                if (!writingOk) {
                    showToast(R.string.error_while_writing_data, e.toString());
                } else {
                    showToast(R.string.error_while_reading_data, e.toString());
                }
            }

            if (buffer != null) {
                mAdapter = new CustomListAdapter(buffer);


                if (mHandler != null && lv != null) {
                    mHandler.post(new Runnable() {
                        public void run() {
                            lv.setAdapter(mAdapter);
                        }
                    });
                }
            }
        }
    }


    @Override
    public void onClick(View v) {

        // Hide Soft Keyboard
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }

        if(mSelectedfileUri == null) {
            showToast(R.string.please_select_source_file);
            return;
        }

        InputStream inputStream;
        try {
            inputStream = getContentResolver().openInputStream(mSelectedfileUri);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        } catch (SecurityException se) {
            se.printStackTrace();
            return;
        }

        try {
            int fileSize = inputStream.available();

            mBuffer = new byte[fileSize];
            inputStream.read(mBuffer);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            showToast(R.string.source_file_not_found);
            return;
        } catch (IOException e) {
            e.printStackTrace();
            showToast(R.string.failed_to_read_source_file);
            return;
        }

        mThread = new Thread(new ContentView());

        try {
            mDestinationByteAddress = Helper.convertHexStringToInt(mDestinationOffsetEditText.getText().toString());
        } catch (STException e) {
            e.printStackTrace();
            return;
        }

        Snackbar snackbar = Snackbar.make(v, "", Snackbar.LENGTH_LONG);

        snackbar.setAction("Writing " + mBuffer.length + " Bytes of data at offset" + mDestinationByteAddress, this);
        snackbar.setActionTextColor(getResources().getColor(R.color.white));

        snackbar.show();
        mThread.start();
    }

    public void onPause() {
        if (mThread != null)
            try {
                mThread.join();
            } catch (InterruptedException e) {
                Log.e(TAG, "Issue joining thread");
            }
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

            String startAddress = String.format("%s %3d: ", getResources().getString(R.string.addr), mDestinationByteAddress + pos * NBR_OF_BYTES_PER_RAW);
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

