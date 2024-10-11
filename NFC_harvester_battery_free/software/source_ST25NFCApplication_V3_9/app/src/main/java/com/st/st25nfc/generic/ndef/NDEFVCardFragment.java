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
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.st.st25nfc.R;
import com.st.st25nfc.generic.util.ContactHelper;
import com.st.st25sdk.ndef.NDEFMsg;
import com.st.st25sdk.ndef.VCardRecord;
import com.theartofdev.edmodo.cropper.CropImage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

public class NDEFVCardFragment extends NDEFRecordFragment {

    final static String TAG = "NDEFVCardFragment";

    private View mView;
    private int mSeekPhotoCurPos;

    private TextView mPhotoSizeTextView;
    private TextView mMaxNdefSizeTextView;
    private EditText mContactAddressEditText;
    private EditText mContactNameEditText;
    private EditText mContactEmailEditText;
    private EditText mContactNumberEditText;
    private EditText mContactWebsiteEditText;
    private ImageView mPhotoImageView;
    private CheckBox mPhotoCheckBox;
    private Button mCapturePhotoButton;
    private Button mGetContactButton;
    private SeekBar mPhotoQualitySeekBar;

    private int mMaxNdefSizeInBytes;

    private Bitmap mSelectedPhoto;      // Selected picture (in 256x256 resolution) without compression
    private String mEncodedPhoto = "";  // Photo after string encoding
    private int mEncodedPhotoSize;

    private VCardRecord mVCardRecord;
    private int mAction;

    private final int PICK_CONTACT = 1;
    private final int PICK_IMAGE = 2;
    private final int PICTURE_CROP = CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE;

    private final int mDefaultPhotoDisplayHSize = 256;
    private final int mDefaultPhotoDisplayWSize = 256;

    private ActivityResultContracts.RequestMultiplePermissions mMultiplePermissionsContract;
    private ActivityResultLauncher<String[]> mMultiplePermissionLauncher;

    public static NDEFVCardFragment newInstance(Context context) {
        NDEFVCardFragment f = new NDEFVCardFragment();
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

        View view = inflater.inflate(R.layout.fragment_ndef_vcard, container, false);
        mView = view;

        Bundle bundle = getArguments();
        if (bundle == null) {
            Log.e(TAG, "Fatal error! Arguments are missing!");
            return null;
        }

        NDEFMsg ndefMsg = (NDEFMsg) bundle.getSerializable(NDEFRecordFragment.NDEFKey);
        int recordNbr = bundle.getInt(NDEFRecordFragment.RecordNbrKey);
        mVCardRecord = (VCardRecord) ndefMsg.getNDEFRecord(recordNbr);
        mMaxNdefSizeInBytes = bundle.getInt(NDEFEditorFragment.MaxNdefSizeKey);

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
                        showToast(R.string.cannot_continue_without_read_contacts_permission);
                        return;
                    }
                }

                // All the permissions are granted
                pickContact();
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

        String text = getResources().getString(R.string.max_ndef_size) + " " + mMaxNdefSizeInBytes + " " + getResources().getString(R.string.bytes);
        mMaxNdefSizeTextView.setText(text);

        return view;
    }

    private void askPermissions(ActivityResultLauncher<String[]> multiplePermissionLauncher) {
        String[] permissions = new String[] { Manifest.permission.READ_CONTACTS };

        if (!hasPermissions(permissions)) {
            Log.d(TAG, "Launching multiple contract permission launcher for ALL required permissions");
            multiplePermissionLauncher.launch(permissions);
        } else {
            Log.d(TAG, "All permissions are already granted");
            pickContact();
        }
    }

    private void pickContact() {
        startActivityForResult(new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI), PICK_CONTACT);
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
     * This function will automatically adjust the compression to get a photo fitting in the memory
     */
    private void adjustPhotoCompression() {

        for(int quality=100; quality>0; quality--) {
            changePhotoQuality(quality);

            if (mEncodedPhotoSize < mMaxNdefSizeInBytes) {
                mPhotoQualitySeekBar.setProgress(quality);
                return;
            }
        }
    }

    /**
     * Change quality of selected photo.
     * @param quality  Hint to the compressor, 0-100. 0 meaning compress for
     *                 small size, 100 meaning compress for max quality. Some
     *                 formats, like PNG which is lossless, will ignore the
     *                 quality setting
     */
    private void changePhotoQuality(int quality) {
        if (mSelectedPhoto != null) {
            mEncodedPhoto = "";

            Bitmap compressedPhoto = compressPhoto(mSelectedPhoto, quality);

            mPhotoImageView.setImageBitmap(compressedPhoto);
            mPhotoCheckBox.setChecked(true);

            String text = getResources().getString(R.string.photo_size) + " " + mEncodedPhotoSize + " " + getResources().getString(R.string.bytes);
            mPhotoSizeTextView.setText(text);

            if (mEncodedPhotoSize > mMaxNdefSizeInBytes) {
                mPhotoSizeTextView.setTextColor(getResources().getColor(R.color.red));
            } else {
                mPhotoSizeTextView.setTextColor(getResources().getColor(R.color.st_dark_blue));
            }

        }
    }

    /**
     * Compress the given Bitmap.
     * @param bitmap
     * @param quality  Hint to the compressor, 0-100. 0 meaning compress for
     *                 small size, 100 meaning compress for max quality. Some
     *                 formats, like PNG which is lossless, will ignore the
     *                 quality setting
     * @return
     */
    private Bitmap compressPhoto(Bitmap bitmap, int quality) {

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);

        byte[] b = outputStream.toByteArray();

        Bitmap compressedPhoto = BitmapFactory.decodeByteArray(b, 0, b.length);

        mEncodedPhoto = Base64.encodeToString(b, Base64.DEFAULT);
        mEncodedPhotoSize = mEncodedPhoto.length();

        return compressedPhoto;
    }

    private void initFragmentWidgets() {

        mContactAddressEditText = (EditText) mView.findViewById(R.id.edit_contact_address);
        mContactNameEditText = (EditText) mView.findViewById(R.id.edit_contact_name);
        mContactEmailEditText = (EditText) mView.findViewById(R.id.edit_contact_email);
        mContactNumberEditText = (EditText) mView.findViewById(R.id.edit_contact_number);
        mContactWebsiteEditText = (EditText) mView.findViewById(R.id.edit_contact_website);
        mPhotoImageView = (ImageView) mView.findViewById(R.id.photoView);
        mPhotoCheckBox = (CheckBox) mView.findViewById(R.id.capture_photo_checkbox);
        mCapturePhotoButton = (Button) mView.findViewById(R.id.capturePhotoButton);
        mGetContactButton = (Button) mView.findViewById(R.id.getContactButton);
        mPhotoQualitySeekBar = (SeekBar) mView.findViewById(R.id.vcard_photo_quality_slider);
        mPhotoSizeTextView = (TextView) mView.findViewById(R.id.photoSizeTextView);
        mMaxNdefSizeTextView = (TextView) mView.findViewById(R.id.maxNdefSizeTextView);

        mCapturePhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //captureFrame();
                selectImageInPhoneMemory();
            }
        });

        mGetContactButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getContact();
            }
        });

        mSeekPhotoCurPos = 80;
        mPhotoQualitySeekBar.setProgress((int) mSeekPhotoCurPos);

        mPhotoQualitySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mSeekPhotoCurPos = progress;
                changePhotoQuality(mSeekPhotoCurPos);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        setContent();
    }

    /**
     * The content from the fragment is saved into the NDEF Record
     */
    @Override
    public void updateContent() {
        String contactAddress = mContactAddressEditText.getText().toString();
        String contactName = mContactNameEditText.getText().toString();
        String contactEmail = mContactEmailEditText.getText().toString();
        String contactNumber = mContactNumberEditText.getText().toString();
        String contactWebsite = mContactWebsiteEditText.getText().toString();

        mVCardRecord.setSPAddr(contactAddress);
        mVCardRecord.setName(contactName);
        mVCardRecord.setEmail(contactEmail);
        mVCardRecord.setNumber(contactNumber);
        mVCardRecord.setWebSite(contactWebsite);

        if (!mEncodedPhoto.isEmpty() && mPhotoCheckBox.isChecked()) {
            mVCardRecord.setPhoto(mEncodedPhoto);
        } else {
            mVCardRecord.setPhoto(null);
        }
    }

    /**
     * The content from the NDEF Record is displayed in the Fragment
     */
    public void setContent() {
        String address = mVCardRecord.getStructPostalAddr();
        mContactAddressEditText.setText(address);

        String name = mVCardRecord.getFormattedName();
        if (name == null) {
            name = mVCardRecord.getName();
        }
        mContactNameEditText.setText(name);

        String email = mVCardRecord.getEmail();
        mContactEmailEditText.setText(email);

        String number = mVCardRecord.getNumber();
        mContactNumberEditText.setText(number);

        String webSite = mVCardRecord.getWebSiteAddr();
        mContactWebsiteEditText.setText(webSite);

        String photoString = mVCardRecord.getPhoto();
        if (photoString != null) {
            try {
                byte[] decodedString = Base64.decode(photoString, Base64.DEFAULT);
                mSelectedPhoto = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                if (mSelectedPhoto != null) {
                    mPhotoImageView.setImageBitmap(mSelectedPhoto);
                    mPhotoCheckBox.setChecked(true);

                    mEncodedPhoto = photoString;
                    mEncodedPhotoSize = mEncodedPhoto.length();

                    String text = getResources().getString(R.string.photo_size) + " " + mEncodedPhotoSize + " " + getResources().getString(R.string.bytes);
                    mPhotoSizeTextView.setText(text);
                }
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                showToast(R.string.invalid_photo);
            }
        }
    }

    public void ndefRecordEditable(boolean editable) {
        mContactNameEditText.setClickable(editable);
        mContactNameEditText.setFocusable(editable);
        mContactNameEditText.setFocusableInTouchMode(editable);

        mContactNumberEditText.setClickable(editable);
        mContactNumberEditText.setFocusable(editable);
        mContactNumberEditText.setFocusableInTouchMode(editable);

        mContactEmailEditText.setClickable(editable);
        mContactEmailEditText.setFocusable(editable);
        mContactEmailEditText.setFocusableInTouchMode(editable);

        mContactAddressEditText.setClickable(editable);
        mContactAddressEditText.setFocusable(editable);
        mContactAddressEditText.setFocusableInTouchMode(editable);

        mContactWebsiteEditText.setClickable(editable);
        mContactWebsiteEditText.setFocusable(editable);
        mContactWebsiteEditText.setFocusableInTouchMode(editable);

        mPhotoQualitySeekBar.setEnabled(editable);
        mCapturePhotoButton.setEnabled(editable);
        mGetContactButton.setEnabled(editable);
        mPhotoCheckBox.setEnabled(editable);

        if(!editable) {
            // The Fragment is no more editable. Reload its content
            setContent();
        }
    }

    public void getContact() {
        askPermissions(mMultiplePermissionLauncher);
    }

    private void performCrop(Uri pictureUri) {
        try {
            CropImage.activity(pictureUri)
                    .setFixAspectRatio(true)
                    .setAspectRatio(1, 1)
                    .start(getContext(), this);

        } catch (ActivityNotFoundException anfe) {
            showToast(R.string.device_doesnt_support_crop_feature);
        }
    }

    private void selectImageInPhoneMemory() {
        String message = getString(R.string.select_picture);

        // Pick up FW in phone's memory storage
        Intent intent = new Intent()
                .setType("image/*")
                .setAction(Intent.ACTION_GET_CONTENT);

        // Special intent for Samsung file manager
        Intent sIntent = new Intent("com.sec.android.app.myfiles.PICK_DATA");
        sIntent.addCategory(Intent.CATEGORY_DEFAULT);

        Intent chooserIntent;
        if (getContext().getPackageManager().resolveActivity(sIntent, 0) != null){
            // it is device with samsung file manager
            chooserIntent = Intent.createChooser(sIntent, message);
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] { intent});
        }
        else {
            chooserIntent = Intent.createChooser(intent, message);
        }

        startActivityForResult(chooserIntent, PICK_IMAGE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case PICK_IMAGE:
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null) {
                        Uri pictureUri = data.getData();
                        performCrop(pictureUri);
                    } else {
                        showToast(R.string.device_doesnt_support_capturing);
                    }
                }
                break;

            case PICTURE_CROP:
                try {
                    CropImage.ActivityResult result = CropImage.getActivityResult(data);
                    if (resultCode == Activity.RESULT_OK) {
                        Uri imageUri = result.getUri();

                        Bitmap photo = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), imageUri);
                        if (photo != null) {
                            // Reduce picture resolution to 256x256
                            mSelectedPhoto = changePictureResolution(photo, mDefaultPhotoDisplayWSize, mDefaultPhotoDisplayHSize);
                            if (mSelectedPhoto != null) {
                                adjustPhotoCompression();
                            }
                        }

                    } else {
                        showToast(R.string.crop_failed);
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                    showToast(R.string.crop_failed);
                }
                break;

            case PICK_CONTACT: {
                if (resultCode == Activity.RESULT_OK) {
                    Uri uri = data.getData();
                    ContactHelper contactHelper = new ContactHelper(uri, getActivity().getContentResolver());
                    String id = contactHelper.getId();

                    String address = contactHelper.retrieveContactStructurePostAddr(id);
                    String name = contactHelper.getDisplayName(id);
                    String email = contactHelper.retrieveContactEmail(id);
                    String number = contactHelper.retrieveContactNumber(id);
                    String addressWebsite = contactHelper.retrieveContactWebSite(id);
                    Bitmap photo = contactHelper.retrieveContactPhoto(id);

                    mContactAddressEditText.setText(address);
                    mContactNameEditText.setText(name);
                    mContactEmailEditText.setText(email);
                    mContactNumberEditText.setText(number);
                    mContactWebsiteEditText.setText(addressWebsite);

                    if (photo != null) {
                        mSelectedPhoto = photo;
                        adjustPhotoCompression();
                    }
                }

                break;

            }
        }
    }

    public Bitmap changePictureResolution(Bitmap bitmap, int newWidth, int newHeight) {
        Bitmap newBitmap = ThumbnailUtils.extractThumbnail(bitmap, newWidth, newHeight);
        return newBitmap;
    }

}


