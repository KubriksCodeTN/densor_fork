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

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;

import com.st.st25nfc.generic.util.UIHelper;
import com.st.st25sdk.Helper;
import com.st.st25nfc.R;
import com.st.st25sdk.STException;
import com.st.st25sdk.ndef.BtRecord;
import com.st.st25sdk.ndef.NDEFMsg;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

public class NDEFBtFragment extends NDEFRecordFragment {

    final static String TAG = "NDEFBtFragment";


    private View mView;
    private boolean mBtPermissionGranted = false;
    private BluetoothAdapter mBtAdapter = null;
    Set<BluetoothDevice> mPairedDevices = null;

    private ArrayList<String> mDeviceListName;
    private ArrayList<String> mDeviceListMacAddr;

    private ActivityResultContracts.RequestMultiplePermissions mMultiplePermissionsContract;
    private ActivityResultLauncher<String[]> mMultiplePermissionLauncher;
    private BtRecord mBtRecord;
    private int mAction;

    private EditText mDeviceNameEditText;
    private EditText mAddrByte5EditText;
    private EditText mAddrByte4EditText;
    private EditText mAddrByte3EditText;
    private EditText mAddrByte2EditText;
    private EditText mAddrByte1EditText;
    private EditText mAddrByte0EditText;
    private Button mPrevConnectedDevicesButton;
    private TextView mBtDeviceClassTextView;
    private TextView mBtUuidClassTextView;
    private TextView mBtUuidClassListTextView;

    public static NDEFBtFragment newInstance(Context context) {
        NDEFBtFragment f = new NDEFBtFragment();
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

        View view = inflater.inflate(R.layout.fragment_ndef_bt, container, false);
        mView = view;

        Bundle bundle = getArguments();
        if (bundle == null) {
            Log.e(TAG, "Fatal error! Arguments are missing!");
            return null;
        }

        NDEFMsg ndefMsg = (NDEFMsg) bundle.getSerializable(NDEFRecordFragment.NDEFKey);
        int recordNbr = bundle.getInt(NDEFRecordFragment.RecordNbrKey);
        mBtRecord = (BtRecord) ndefMsg.getNDEFRecord(recordNbr);


        mMultiplePermissionsContract = new ActivityResultContracts.RequestMultiplePermissions();
        mMultiplePermissionLauncher = registerForActivityResult(mMultiplePermissionsContract, new ActivityResultCallback<Map<String, Boolean>>() {
            @Override
            public void onActivityResult(Map<String, Boolean> result) {
                Log.d(TAG, "Multiple permissions result: " + result);

                // Iterate through all the results
                for (Map.Entry<String, Boolean> entry : result.entrySet()) {
                    String permission = entry.getKey();
                    boolean granted = entry.getValue();

                    Log.d(TAG, "Key: " + permission + ", value: " + granted);
                    if (!granted) {
                        showToast(R.string.bt_permission_not_granted);
                        mBtPermissionGranted = false;
                        return;
                    }
                }

                // All the permissions are granted
                mBtPermissionGranted = true;
                getBondedDevices();
            }
        });

        initFragmentWidgets();

        mAction = bundle.getInt(NDEFEditorFragment.EditorKey);
        if(mAction == NDEFEditorFragment.VIEW_NDEF_RECORD) {
            // We are displaying an existing record. By default it is not editable
            ndefRecordEditable(false);

        } else {
            // We are adding a new TextRecord or editing an existing record
            ndefRecordEditable(true);
        }

        return mView;
    }

    private void initFragmentWidgets() {
        mDeviceNameEditText = mView.findViewById(R.id.ndef_fragment_bt_device_name);

        mAddrByte5EditText = mView.findViewById(R.id.addrByte5EditText);
        mAddrByte4EditText = mView.findViewById(R.id.addrByte4EditText);
        mAddrByte3EditText = mView.findViewById(R.id.addrByte3EditText);
        mAddrByte2EditText = mView.findViewById(R.id.addrByte2EditText);
        mAddrByte1EditText = mView.findViewById(R.id.addrByte1EditText);
        mAddrByte0EditText = mView.findViewById(R.id.addrByte0EditText);

        mBtDeviceClassTextView = mView.findViewById(R.id.btDeviceClassTextView);
        mBtUuidClassTextView = mView.findViewById(R.id.btUuidClassTextView);
        mBtUuidClassListTextView = mView.findViewById(R.id.btUuidClassListTextView);

        mPrevConnectedDevicesButton = mView.findViewById(R.id.prevConnectedDevicesButton);

        mPrevConnectedDevicesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                displayPreviouslyConnectedDevices();
            }
        });

        BluetoothManager manager = (BluetoothManager) getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
        if(manager == null) {
            throw new NullPointerException("Cannot get BluetoothManager");
        } else {
            mBtAdapter = manager.getAdapter();
        }

        setContent();
    }


    private void askPermissions(ActivityResultLauncher<String[]> multiplePermissionLauncher) {
        String[] permissions;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            permissions = new String[] {
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
            };
        } else {
            permissions = new String[] {
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            };
        }

        if (!hasPermissions(permissions)) {
            Log.d(TAG, "Launching multiple contract permission launcher for ALL required permissions");
            multiplePermissionLauncher.launch(permissions);
        } else {
            Log.d(TAG, "All permissions are already granted");
            mBtPermissionGranted = true;
            getBondedDevices();
        }
    }

    private boolean hasPermissions(String[] permissions) {
        if (permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(getActivity(), permission) != PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Permission is not granted: " + permission);
                    return false;
                }
                Log.d(TAG, "Permission already granted: " + permission);
            }
            return true;
        }
        return false;
    }

    /**
     * Get the list of bonded devices
     */
    private void getBondedDevices() {
        mPairedDevices = mBtAdapter.getBondedDevices();
    }

    private byte[] convertBTAddressStringToByteArray(String btAddress) throws STException {
        byte[] addr = Helper.convertHexStringToByteArray(btAddress.replaceAll(":", ""));

        if ((addr == null) || (addr.length != 6)) {
            throw new STException(STException.STExceptionCode.CMD_FAILED);
        }

        return addr;
    }

    private String convertBTAddressByteArrayToString(byte[] btAddress) throws STException {
        if ((btAddress == null) || (btAddress.length != 6)) {
            throw new STException(STException.STExceptionCode.BAD_PARAMETER);
        }

        String txt = String.format("%02x", btAddress[0]).toUpperCase() + ":" +
                     String.format("%02x", btAddress[1]).toUpperCase() + ":" +
                     String.format("%02x", btAddress[2]).toUpperCase() + ":" +
                     String.format("%02x", btAddress[3]).toUpperCase() + ":" +
                     String.format("%02x", btAddress[4]).toUpperCase() + ":" +
                     String.format("%02x", btAddress[5]).toUpperCase();

        return txt;
    }

    private byte[] getBTAddressFromUI() {
        byte[] addr = new byte[6];
        addr[5] = (byte) Integer.parseInt(mAddrByte5EditText.getText().toString(), 16);                 // This is the MSB
        addr[4] = (byte) Integer.parseInt(mAddrByte4EditText.getText().toString(), 16);
        addr[3] = (byte) Integer.parseInt(mAddrByte3EditText.getText().toString(), 16);                 // This is the MSB
        addr[2] = (byte) Integer.parseInt(mAddrByte2EditText.getText().toString(), 16);
        addr[1] = (byte) Integer.parseInt(mAddrByte1EditText.getText().toString(), 16);
        addr[0] = (byte) Integer.parseInt(mAddrByte0EditText.getText().toString(), 16);                 // This is the LSB

        return addr;
    }

    private void setUIBTAddress(byte[] addr) throws STException {
        if ((addr == null) || (addr.length != 6)) {
            throw new STException(STException.STExceptionCode.BAD_PARAMETER);
        }

        mAddrByte5EditText.setText(String.format("%02x", addr[5]).toUpperCase());
        mAddrByte4EditText.setText(String.format("%02x", addr[4]).toUpperCase());
        mAddrByte3EditText.setText(String.format("%02x", addr[3]).toUpperCase());
        mAddrByte2EditText.setText(String.format("%02x", addr[2]).toUpperCase());
        mAddrByte1EditText.setText(String.format("%02x", addr[1]).toUpperCase());
        mAddrByte0EditText.setText(String.format("%02x", addr[0]).toUpperCase());

    }

    /**
     * The content from the NDEF Record is displayed in the Fragment
     */
    public void setContent() {
        String deviceName = mBtRecord.getBTDeviceName();
        mDeviceNameEditText.setText(deviceName);

        try {
            byte[] addr = mBtRecord.getBTDeviceMacAddr();
            setUIBTAddress(addr);
        } catch (STException e) {
            e.printStackTrace();
        }

        byte[] deviceClass = mBtRecord.getBTDeviceClass();
        mBtDeviceClassTextView.setText(Helper.convertByteArrayToHexString(deviceClass));

        byte uuidClass = mBtRecord.getBtUuidClass();
        mBtUuidClassTextView.setText(String.format("0x%02x", uuidClass));

        byte[] uuidClassList = mBtRecord.getBtUuidClassList();
        mBtUuidClassListTextView.setText(Helper.convertByteArrayToHexString(uuidClassList));
    }

    /**
     * The content from the fragment is saved into the NDEF Record
     */
    @Override
    public void updateContent() {
        String deviceName;
        if (mDeviceNameEditText.getText() != null) {
            deviceName = mDeviceNameEditText.getText().toString();
        } else {
            deviceName = "Device name unknown";
        }

        byte[] btAddr = getBTAddressFromUI();

        mBtRecord.setBTDeviceName(deviceName);

        if (mBtPermissionGranted) {
            if (mBtAdapter.getName().equals(deviceName)) {
                byte[] serviceClass = {(byte) 0x0C, (byte) 0x02, (byte) 0x40}; // Device class / major class / minor class little endian coding
                mBtRecord.setBTDeviceClass(serviceClass);
                byte uuidClass = (byte) 0x03; // uiid Service class 16-bit complete.
                byte[] uuid = {(byte) 0x1E, (byte) 0x11, (byte) 0x0B, (byte) 0x11}; // HFP A2DP litlle endian coding
                mBtRecord.setBTUuidClassList(uuid);
                mBtRecord.setBtUuidClass(uuidClass);

            } else {
                try {
                    BluetoothDevice remoteDevice = mBtAdapter.getRemoteDevice(btAddr);
                    if (remoteDevice != null) {
                        BluetoothClass deviceClass = remoteDevice.getBluetoothClass();
                        if (deviceClass != null) {
                            int Cod = deviceClass.hashCode();
                            byte[] buff = {(byte) (Cod & 0xFF), (byte) ((Cod & 0xFF00) >> 8), (byte) ((Cod & 0xFF0000) >> 16)};
                            mBtRecord.setBTDeviceClass(buff);
                            ParcelUuid[] uiids = remoteDevice.getUuids();
                            // only handle a 16 bit class uuid - full list -
                            byte[] uuid = new byte[uiids.length * 2];
                            for (int i = 0; i < uiids.length; i++) {
                                long value = (uiids[i].getUuid().getMostSignificantBits() & 0x0000FFFF00000000L) >>> 32;
                                uuid[2 * i + 1] = (byte) ((value & 0xFF00) >> 8);
                                uuid[2 * i] = (byte) (value & 0xFF);
                            }
                            byte uuidClass = (byte) 0x03; // uiid Service class 16-bit complete.
                            mBtRecord.setBTUuidClassList(uuid);
                            mBtRecord.setBtUuidClass(uuidClass);
                        }
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        mBtRecord.setBTDeviceMacAddr(btAddr);
    }


    public void ndefRecordEditable(boolean editable) {

        mDeviceNameEditText.setFocusable(editable);
        mDeviceNameEditText.setFocusableInTouchMode(editable);
        mDeviceNameEditText.setClickable(editable);

        mAddrByte5EditText.setFocusable(editable);
        mAddrByte5EditText.setFocusableInTouchMode(editable);
        mAddrByte5EditText.setClickable(editable);

        mAddrByte4EditText.setFocusable(editable);
        mAddrByte4EditText.setFocusableInTouchMode(editable);
        mAddrByte4EditText.setClickable(editable);

        mAddrByte3EditText.setFocusable(editable);
        mAddrByte3EditText.setFocusableInTouchMode(editable);
        mAddrByte3EditText.setClickable(editable);

        mAddrByte2EditText.setFocusable(editable);
        mAddrByte2EditText.setFocusableInTouchMode(editable);
        mAddrByte2EditText.setClickable(editable);

        mAddrByte1EditText.setFocusable(editable);
        mAddrByte1EditText.setFocusableInTouchMode(editable);
        mAddrByte1EditText.setClickable(editable);

        mAddrByte0EditText.setFocusable(editable);
        mAddrByte0EditText.setFocusableInTouchMode(editable);
        mAddrByte0EditText.setClickable(editable);

        mPrevConnectedDevicesButton.setClickable(editable);
        mPrevConnectedDevicesButton.setVisibility(editable ? View.VISIBLE : View.GONE);

        if(editable) {
            // Ask BT permissions
            askPermissions(mMultiplePermissionLauncher);
        } else {
            // The Fragment is no more editable. Reload its content
            setContent();
        }
    }

    public void displayPreviouslyConnectedDevices() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());

        //alertDialogBuilder.setTitle(getString(R.string.add_a_device));

        // inflate XML content
        View dialogView = getLayoutInflater().inflate(R.layout.fragment_dialog_with_spinner, null);

        TextView titleTextView = dialogView.findViewById(R.id.titleTextView);
        Spinner bondedDevicesSpinner = dialogView.findViewById(R.id.mySpinner);

        titleTextView.setText(R.string.please_select_a_device);

        alertDialogBuilder
                .setCancelable(false)
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        dialog.cancel();
                    }
                })
                .setPositiveButton("Continue", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        try {
                            int position = bondedDevicesSpinner.getSelectedItemPosition();

                            String deviceName = mDeviceListName.get(position);
                            mDeviceNameEditText.setText(deviceName);

                            String macAddr = mDeviceListMacAddr.get(position);
                            byte[] addr = convertBTAddressStringToByteArray(macAddr);
                            setUIBTAddress(addr);

                        } catch (STException e) {
                            e.printStackTrace();
                        }
                        dialog.cancel();
                    }
                });


        alertDialogBuilder.setView(dialogView);

        mDeviceListName = new ArrayList<String>();
        mDeviceListMacAddr = new ArrayList<String>();

        // Add the current android phone to the list
        mDeviceListName.add(mBtAdapter.getName());
        mDeviceListMacAddr.add(mBtAdapter.getAddress());

        // Add the bonded devices
        if (mPairedDevices.size() > 0) {
            for (BluetoothDevice device : mPairedDevices) {
                int type = device.getType();
                if ((type == BluetoothDevice.DEVICE_TYPE_CLASSIC) || (type == BluetoothDevice.DEVICE_TYPE_DUAL)) {
                    if (device.getName() != null) {
                        mDeviceListName.add(device.getName());
                        mDeviceListMacAddr.add(device.getAddress());
                    }
                }
            }
        }

        final ArrayAdapter<String> myArrayAdapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_dropdown_item_1line, mDeviceListName);
        bondedDevicesSpinner.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        bondedDevicesSpinner.setAdapter(myArrayAdapter);

        // Check if the device name indicated in the tag is one of the devices available. If yes, select it
        String deviceName = mBtRecord.getBTDeviceName();
        if ((deviceName != null) && (deviceName.length() > 0)) {
            int pos = UIHelper.findItemPositionInStringArray(mDeviceListName.toArray(new String[0]), deviceName);
            if (pos >= 0) {
                bondedDevicesSpinner.setSelection(pos);
            }
        }

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();

        alertDialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(R.color.st_light_blue));
        alertDialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.st_light_blue));
    }

}


