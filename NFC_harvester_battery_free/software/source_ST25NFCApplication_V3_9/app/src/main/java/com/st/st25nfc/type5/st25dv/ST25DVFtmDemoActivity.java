package com.st.st25nfc.type5.st25dv;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.st.st25nfc.R;
import com.st.st25nfc.generic.ST25Menu;
import com.st.st25nfc.generic.STFragmentActivity;
import com.st.st25nfc.generic.util.UIHelper;
import com.st.st25sdk.Helper;
import com.st.st25sdk.STException;
import com.st.st25sdk.STLog;
import com.st.st25sdk.ftmprotocol.FtmCommands;
import com.st.st25sdk.ftmprotocol.FtmProtocol;
import com.st.st25sdk.type5.st25dv.ST25DVTag;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;

import static com.st.st25nfc.type5.st25dv.ST25DVFtmDemoActivity.Action.*;
import static com.st.st25nfc.type5.st25dv.ST25DVFtmDemoActivity.ActionStatus.*;
import static com.st.st25sdk.STException.STExceptionCode.BAD_PARAMETER;
import static com.st.st25sdk.ftmprotocol.FtmCommands.*;

public class ST25DVFtmDemoActivity extends STFragmentActivity implements NavigationView.OnNavigationItemSelectedListener, FtmProtocol.TransferProgressionListener {

    static final String TAG = "DV_DemoActivity";

    private ST25DVTag mST25DVTag;
    private FtmCommands mFtmCommands;

    private Action mCurrentAction;

    private ProgressBar mProgressBar;
    private Chronometer mTimer;
    private TextView mTransmittedBytesTextView;
    private TextView mReceivedBytesTextView;
    private ImageView mPictureImageView;
    private TextView mPictureSizeTextView;
    private TextView mBoardNameTextView;
    private TextView mErrorRecoveryTextView;
    private boolean mMaterialNeededMsgDisplayed = false;
    private boolean mBoardInfoCollected = false;
    private boolean mCancelCurrentTransfer = false;
    private boolean mIsErrorRecoveryEnabled = true;

    // Picture to send
    private Bitmap mSelectedPicture;
    private Bitmap mCompressedBitmap;
    private byte[] mCompressedImageRawData;

    private Handler mHandler;

    private int mNbrOfBytesToSend;
    private byte[] mPassword;

    private byte mBoardId;

    private static final int BOARD_MB1283 = 0x00;
    private static final int MB1283_MINIMUM_VERSION_MAJOR_NBR = 1;
    private static final int MB1283_MINIMUM_VERSION_MINOR_NBR = 2;
    private static final int MB1283_MINIMUM_VERSION_PATCH_NBR = 0;

    private static final int BOARD_MB1396 = 0x01;
    private static final int MB1396_MINIMUM_VERSION_MAJOR_NBR = 2;
    private static final int MB1396_MINIMUM_VERSION_MINOR_NBR = 1;
    private static final int MB1396_MINIMUM_VERSION_PATCH_NBR = 0;

    // Other boards supported
    private static final int BOARD_STEVAL_SMARTAG2 = 0x02;
    private static final int BOARD_STEVAL_MKBOXPRO = 0x03;
    private static final int BOARD_STEVAL_STWINBX1 = 0x04;


    private byte mVersionMajor;
    private byte mVersionMinor;
    private byte mVersionPatch;
    private boolean mIsFwUpdateNeeded;



    private InputStream mFirmwareInputStream;

    // Resolution of the Discovery Kit display:
    private static final int MAX_PICTURE_WIDTH = 320;
    private static final int MAX_PICTURE_HEIGHT = 240;

    private static final int MAX_PICTURE_SIZE = 100*1024;

    private final int PICK_IMAGE = 2;
    private final int SELECT_FIRMWARE = 3;

    enum Action {
        IDLE,
        GET_BOARD_INFO,
        ENABLE_MAILBOX,
        SEND_PICTURE,
        READ_PICTURE,
        FIRMWARE_UPGRADE,
        SEND_DATA,
        READ_DATA
    };

    enum ActionStatus {
        ACTION_SUCCESSFUL,
        ACTION_FAILED,
        TAG_NOT_IN_THE_FIELD,
        CONFIG_PASSWORD_NEEDED,
        ERROR_DURING_PWD_PRESENTATION,
        NO_RESPONSE
    };


    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.default_layout);

        // Inflate content of FrameLayout
        FrameLayout frameLayout=(FrameLayout) findViewById(R.id.frame_content);
        View childView = getLayoutInflater().inflate(R.layout.st25dv_ftm_demo, null);
        frameLayout.addView(childView);

        mST25DVTag = (ST25DVTag) super.getTag();
        if (mST25DVTag == null) {
            showToast(R.string.invalid_tag);
            goBackToMainActivity();
            return;
        }

        // For test without traces
        //RFReaderInterface rfReaderInterface = mST25DVTag.getReaderInterface();
        //rfReaderInterface.setTransceiveMode(SILENT);

        mFtmCommands = new FtmCommands(mST25DVTag);

        mFtmCommands.setMinTimeInMsBetweenConsecutiveCmds(80);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        mMenu = ST25Menu.newInstance(super.getTag());
        NavigationView navigationView = (NavigationView) findViewById(R.id.navigation_view);
        navigationView.setNavigationItemSelectedListener(this);
        mMenu.inflateMenu(navigationView);

        mHandler = new Handler();

        mBoardNameTextView = findViewById(R.id.boardNameTextView);

        Button sendPictureToTagButton = findViewById(R.id.sendPictureToTagButton);
        sendPictureToTagButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                selectPictureDialogBox();
            }
        });

        Button receivePictureFromTagButton = findViewById(R.id.receivePictureFromTagButton);
        receivePictureFromTagButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                pleaseSelectAPictureOnDiscovery();
            }
        });

        Button firmwareUpdateDemoButton = findViewById(R.id.firmwareUpdateDemoButton);
        firmwareUpdateDemoButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                chooseFirmware();
            }
        });

        Button sendDataToTagButton = findViewById(R.id.sendDataToTagButton);
        sendDataToTagButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                selectDataSizeDialogBox();
            }
        });

        Button receiveDataFromTagButton = findViewById(R.id.receiveDataFromTagButton);
        receiveDataFromTagButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                pleaseSelectDataSizeOnDiscovery();
            }
        });

        ImageView stopImageView = findViewById(R.id.stopImageView);
        stopImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCancelCurrentTransfer = true;
                mFtmCommands.cancelCurrentTransfer();
            }
        });

        mErrorRecoveryTextView = findViewById(R.id.errorRecoveryTextView);
        Button updateButton = findViewById(R.id.updateButton);
        updateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayErrorRecoverySelectionPopup();
            }
        });

        toolbar.setTitle(mST25DVTag.getName());

        mProgressBar = findViewById(R.id.progressBar);
        mTimer = findViewById(R.id.st25DvChronometer);
        mTransmittedBytesTextView = findViewById(R.id.transmittedBytesTextView);
        mReceivedBytesTextView = findViewById(R.id.receivedBytesTextView);

        displayMaterialNeededAlert();
    }

    @Override
    public void onResume() {
        super.onResume();

        if(mMaterialNeededMsgDisplayed) {
            if (mCurrentAction == IDLE) {
                executeAsynchronousAction(Action.GET_BOARD_INFO);
            }
        }
    }

    @Override
    public void onBackPressed() {
        mCancelCurrentTransfer = true;
        mFtmCommands.cancelCurrentTransfer();
        finish();
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        return mMenu.selectItem(this, item);
    }

    private void displayMaterialNeededAlert() {
        AlertDialog.Builder builder = new AlertDialog.Builder(ST25DVFtmDemoActivity.this);
        final AlertDialog dialog = builder.create();

        // set title
        //alertDialog.setTitle(getString(R.string.XXX));

        dialog.setCancelable(false);

        // inflate XML content
        View dialogView = getLayoutInflater().inflate(R.layout.st25dv_demo_material_needed, null);
        dialog.setView(dialogView);

        TextView board1TextView = dialogView.findViewById(R.id.board1TextView);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            board1TextView.setText(Html.fromHtml(getString(R.string.st25dv_discovery_kit), Html.FROM_HTML_MODE_COMPACT));
        } else {
            board1TextView.setText(Html.fromHtml(getString(R.string.st25dv_discovery_kit)));
        }
        Linkify.addLinks(board1TextView, Linkify.ALL);
        board1TextView.setMovementMethod(LinkMovementMethod.getInstance());

        TextView board2TextView = dialogView.findViewById(R.id.board2TextView);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            board2TextView.setText(Html.fromHtml(getString(R.string.st25dv64kc_disco_kit), Html.FROM_HTML_MODE_COMPACT));
        } else {
            board2TextView.setText(Html.fromHtml(getString(R.string.st25dv64kc_disco_kit)));
        }
        Linkify.addLinks(board2TextView, Linkify.ALL);
        board2TextView.setMovementMethod(LinkMovementMethod.getInstance());

        Button continueButton = dialogView.findViewById(R.id.continueButton);
        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.cancel();
                mMaterialNeededMsgDisplayed = true;
                executeAsynchronousAction(Action.GET_BOARD_INFO);
            }
        });

        // show the dialog box
        dialog.show();

    }

    private void displayMb1396FwUpgradeNeededAlert() {
        AlertDialog.Builder builder = new AlertDialog.Builder(ST25DVFtmDemoActivity.this);
        final AlertDialog dialog = builder.create();

        // set title
        //alertDialog.setTitle(getString(R.string.XXX));

        dialog.setCancelable(false);

        // inflate XML content
        View dialogView = getLayoutInflater().inflate(R.layout.st25dv_demo_fw_upgrade_needed, null);
        dialog.setView(dialogView);

        TextView msgTextView = dialogView.findViewById(R.id.msgTextView);
        msgTextView.setText(getResources().getString(R.string.firmware_update_needed_mb1396,
                MB1396_MINIMUM_VERSION_MAJOR_NBR,
                MB1396_MINIMUM_VERSION_MINOR_NBR,
                MB1396_MINIMUM_VERSION_PATCH_NBR));

        TextView fwLinkTextView = dialogView.findViewById(R.id.fwLinkTextView);
        String html = "<a href=\"https://www.st.com/content/st_com/en/products/embedded-software/st25-nfc-rfid-software/stsw-st25dv002.html\">stsw-st25dv002</a>";
        fwLinkTextView.setText(Html.fromHtml(html));
        fwLinkTextView.setMovementMethod(LinkMovementMethod.getInstance());

        Button continueButton = dialogView.findViewById(R.id.continueButton);
        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.cancel();
                mMaterialNeededMsgDisplayed = true;
            }
        });

        // show the dialog box
        dialog.show();
    }

    private void displayMb1283FwUpgradeNeededAlert() {
        AlertDialog.Builder builder = new AlertDialog.Builder(ST25DVFtmDemoActivity.this);
        final AlertDialog dialog = builder.create();

        // set title
        //alertDialog.setTitle(getString(R.string.XXX));

        dialog.setCancelable(false);

        // inflate XML content
        View dialogView = getLayoutInflater().inflate(R.layout.st25dv_demo_fw_upgrade_needed, null);
        dialog.setView(dialogView);

        TextView msgTextView = dialogView.findViewById(R.id.msgTextView);
        msgTextView.setText(getResources().getString(R.string.firmware_update_needed_mb1283,
                MB1283_MINIMUM_VERSION_MAJOR_NBR,
                MB1283_MINIMUM_VERSION_MINOR_NBR,
                MB1283_MINIMUM_VERSION_PATCH_NBR));

        TextView fwLinkTextView = dialogView.findViewById(R.id.fwLinkTextView);
        String html = "<a href=\"https://www.st.com/content/st_com/en/products/embedded-software/st25-nfc-rfid-software/stsw-st25dv001.html\">stsw-st25dv001</a>";

        fwLinkTextView.setText(Html.fromHtml(html));
        fwLinkTextView.setMovementMethod(LinkMovementMethod.getInstance());

        Button continueButton = dialogView.findViewById(R.id.continueButton);
        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.cancel();
                mMaterialNeededMsgDisplayed = true;
            }
        });

        // show the dialog box
        dialog.show();
    }

    private void displayBoardNameAndCheckVersion() {
        switch(mBoardId) {
            case BOARD_MB1283:
                mBoardNameTextView.setText(String.format("%s (FW ver. %d.%d.%d)",
                        "MB1283",
                        mVersionMajor,
                        mVersionMinor,
                        mVersionPatch));

                mIsFwUpdateNeeded = false;
                if (mVersionMajor < MB1283_MINIMUM_VERSION_MAJOR_NBR) {
                    mIsFwUpdateNeeded = true;
                } else if (mVersionMajor == MB1283_MINIMUM_VERSION_MAJOR_NBR) {
                    if (mVersionMinor < MB1283_MINIMUM_VERSION_MINOR_NBR) {
                        mIsFwUpdateNeeded = true;
                    } else if (mVersionMinor == MB1283_MINIMUM_VERSION_MINOR_NBR) {
                        if (mVersionPatch < MB1283_MINIMUM_VERSION_PATCH_NBR) {
                            mIsFwUpdateNeeded = true;
                        }
                    }
                }

                if (mIsFwUpdateNeeded) {
                    displayMb1283FwUpgradeNeededAlert();
                }
                break;

            case BOARD_MB1396:
                mBoardNameTextView.setText(String.format("%s (FW ver. %d.%d.%d)",
                        "MB1396",
                        mVersionMajor,
                        mVersionMinor,
                        mVersionPatch));

                mIsFwUpdateNeeded = false;
                if (mVersionMajor < MB1396_MINIMUM_VERSION_MAJOR_NBR) {
                    mIsFwUpdateNeeded = true;
                } else if (mVersionMajor == MB1396_MINIMUM_VERSION_MAJOR_NBR) {
                    if (mVersionMinor < MB1396_MINIMUM_VERSION_MINOR_NBR) {
                        mIsFwUpdateNeeded = true;
                    } else if (mVersionMinor == MB1396_MINIMUM_VERSION_MINOR_NBR) {
                        if (mVersionPatch < MB1396_MINIMUM_VERSION_PATCH_NBR) {
                            mIsFwUpdateNeeded = true;
                        }
                    }
                }

                if (mIsFwUpdateNeeded) {
                    displayMb1396FwUpgradeNeededAlert();
                }
                break;

            case BOARD_STEVAL_SMARTAG2:
                mBoardNameTextView.setText(String.format("%s (FW ver. %d.%d.%d)",
                        "STEVAL_SMARTAG2",
                        mVersionMajor,
                        mVersionMinor,
                        mVersionPatch));
                break;

            case BOARD_STEVAL_MKBOXPRO:
                mBoardNameTextView.setText(String.format("%s (FW ver. %d.%d.%d)",
                        "STEVAL_MKBOXPRO",
                        mVersionMajor,
                        mVersionMinor,
                        mVersionPatch));
                break;

            case BOARD_STEVAL_STWINBX1:
                mBoardNameTextView.setText(String.format("%s (FW ver. %d.%d.%d)",
                        "STEVAL_STWINBX1",
                        mVersionMajor,
                        mVersionMinor,
                        mVersionPatch));
                break;

            default:
                mBoardNameTextView.setText("Uknown");
                Log.e(TAG, "Unknown board Id: " + mBoardId);
                break;
        }

    }

    private void pleaseSelectAPictureOnDiscovery() {
        androidx.appcompat.app.AlertDialog.Builder alertDialogBuilder = new androidx.appcompat.app.AlertDialog.Builder(ST25DVFtmDemoActivity.this);

        // set title
        //alertDialogBuilder.setTitle(getString(R.string.confirmation_needed));

        // set dialog message
        alertDialogBuilder
                .setMessage(getString(R.string.please_select_a_picture_on_discovery))
                .setCancelable(true)

                .setPositiveButton(getString(R.string.ok),new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        dialog.cancel();
                        executeAsynchronousAction(Action.READ_PICTURE);
                    }
                });

        // create alert dialog
        androidx.appcompat.app.AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.st_light_blue));

    }

    private void displayMailboxAlert() {
        androidx.appcompat.app.AlertDialog.Builder alertDialogBuilder = new androidx.appcompat.app.AlertDialog.Builder(ST25DVFtmDemoActivity.this);

        // set title
        //alertDialogBuilder.setTitle(getString(R.string.confirmation_needed));

        // set dialog message
        alertDialogBuilder
                .setMessage(getString(R.string.mailbox_not_enable_do_you_want_to_enable_it))
                .setCancelable(true)

                .setPositiveButton(getString(R.string.Enable_mailbox),new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        dialog.cancel();
                        executeAsynchronousAction(Action.ENABLE_MAILBOX);
                    }
                })
                .setNegativeButton(getString(R.string.cancel),new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        dialog.cancel();
                    }
                });

        // create alert dialog
        androidx.appcompat.app.AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();

        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(R.color.st_light_blue));
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.st_light_blue));

    }

    private void selectPictureDialogBox() {
        AlertDialog.Builder builder = new AlertDialog.Builder(ST25DVFtmDemoActivity.this);
        final AlertDialog dialog = builder.create();

        // set title
        //alertDialog.setTitle(getString(R.string.XXX));

        dialog.setCancelable(false);

        // inflate XML content
        View dialogView = getLayoutInflater().inflate(R.layout.st25dv_select_picture, null);
        dialog.setView(dialogView);

        final RadioButton demoPictureRadioButton = dialogView.findViewById(R.id.demoPictureRadioButton);
        mPictureImageView = dialogView.findViewById(R.id.pictureImageView);
        mPictureSizeTextView = dialogView.findViewById(R.id.pictureSizeTextView);

        RadioGroup pictureSelectionRadioGroup = dialogView.findViewById(R.id.pictureSelectionRadioGroup);
        pictureSelectionRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId)
            {
                switch(checkedId)
                {
                    case R.id.demoPictureRadioButton:
                        int imageId = R.drawable.st4970_shutterstock;
                        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), imageId);
                        setSelectedPicture(bitmap);
                        break;
                    case R.id.pickupPictureRadioButton:
                        selectImageInPhoneMemory();
                        break;
                }
            }
        });

        demoPictureRadioButton.setChecked(true);

        Button cancelButton = dialogView.findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.cancel();
            }
        });

        Button sendButton = dialogView.findViewById(R.id.sendButton);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.cancel();
                executeAsynchronousAction(Action.SEND_PICTURE);
            }
        });

        // show the dialog box
        dialog.show();
    }

    private void displayPictureSize(int sizeInBytes) {
        mPictureSizeTextView.setText(getString(R.string.picture_size_in_bytes, sizeInBytes));
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
        if (getPackageManager().resolveActivity(sIntent, 0) != null){
            // it is device with samsung file manager
            chooserIntent = Intent.createChooser(sIntent, message);
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] { intent});
        }
        else {
            chooserIntent = Intent.createChooser(intent, message);
        }

        startActivityForResult(chooserIntent, PICK_IMAGE);
    }

    public int pxToDp(int px) {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int dp = Math.round(px / (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
        return dp;
    }

    public int dpToPx(int dp) {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int px = Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
        return px;
    }

    private void setSelectedPicture(Bitmap bitmap) {
        int destinationWidth;
        int destinationHeight;

        if (bitmap == null) {
            Log.e(TAG, "Invalid bitmap!");
            return;
        }

        int pictureWidth =  bitmap.getWidth();
        int pictureHeight = bitmap.getHeight();

        // Reduce picture resolution
        if (pictureHeight > pictureWidth) {
            if (pictureHeight > MAX_PICTURE_HEIGHT) {
                destinationHeight = MAX_PICTURE_HEIGHT;
                destinationWidth = (pictureWidth * MAX_PICTURE_HEIGHT) / pictureHeight;
                mSelectedPicture = changePictureResolution(bitmap, destinationWidth, destinationHeight);
            } else {
                // Picture is unchanged
                mSelectedPicture = bitmap;
            }
        } else {
            if (pictureWidth > MAX_PICTURE_WIDTH) {
                destinationHeight = (pictureHeight * MAX_PICTURE_WIDTH) / pictureWidth;
                destinationWidth = MAX_PICTURE_WIDTH;
                mSelectedPicture = changePictureResolution(bitmap, destinationWidth, destinationHeight);
            } else {
                // Picture is unchanged
                mSelectedPicture = bitmap;
            }
        }

        if (mSelectedPicture != null) {
            adjustPictureCompression();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case PICK_IMAGE:
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null) {
                        try {
                            Uri pictureUri = data.getData();
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), pictureUri);
                            setSelectedPicture(bitmap);

                        } catch (IOException e) {
                            e.printStackTrace();
                            showToast(R.string.command_failed);
                        }
                    } else {
                        showToast(R.string.command_failed);
                    }
                }
                break;

            case SELECT_FIRMWARE:
                if(resultCode==RESULT_OK) {
                    Uri selectedfileUri = data.getData(); //The uri with the location of the file

                    try {
                        mFirmwareInputStream = getContentResolver().openInputStream(selectedfileUri);
                    } catch (FileNotFoundException e) {
                        showToast(R.string.failled_to_open_file);
                        e.printStackTrace();
                        return;
                    }

                    String filePath = selectedfileUri.getPath();
                    int fileSize = 0;
                    try {
                        fileSize = mFirmwareInputStream.available();
                    } catch (IOException e) {
                        showToast(R.string.failed_to_red_file_size);
                        e.printStackTrace();
                        return;
                    }

                    presentPassword();
                }
                break;

            default:
                break;
        }
    }

    public byte[] compressImage(Bitmap bitmap) {
        byte[] data;

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        // Compress image for max quality
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);

        data = byteArrayOutputStream.toByteArray();
        return data;
    }


    /**
     * This function will automatically adjust the compression to get a picture fitting in the memory
     */
    private void adjustPictureCompression() {

        for(int quality=100; quality>0; quality--) {

            mCompressedImageRawData = compressBitmap(mSelectedPicture, quality);
            if (mCompressedImageRawData.length < MAX_PICTURE_SIZE) {
                // We have found a compression allowing to get the requested size
                mPictureImageView.setImageBitmap(mSelectedPicture);
                displayPictureSize(mCompressedImageRawData.length);
                return;
            }
        }

        Log.e(TAG, "Failed to compress the picture to the requested size!");
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
    private byte[] compressBitmap(Bitmap bitmap, int quality) {

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);

        byte[] data = outputStream.toByteArray();

        mCompressedBitmap = BitmapFactory.decodeByteArray(data, 0, data.length);

        return data;
    }

    /**
     * Change quality of selected photo.
     * @param quality  Hint to the compressor, 0-100. 0 meaning compress for
     *                 small size, 100 meaning compress for max quality. Some
     *                 formats, like PNG which is lossless, will ignore the
     *                 quality setting
     */
    private void changePhotoQuality(int quality) {
        if (mSelectedPicture != null) {





        }
    }

    private void reducePictureSize(Bitmap bitmap) {

    }

    private Bitmap changePictureResolution(Bitmap bitmap, int newWidth, int newHeight) {
        Bitmap newBitmap = ThumbnailUtils.extractThumbnail(bitmap, newWidth, newHeight);
        return newBitmap;
    }

    @Override
    public void transmissionProgress(int transmittedBytes, int acknowledgedBytes, int totalSize) {
        //Log.d(TAG, "transmissionProgress currentSize: " + acknowledgedBytes + "  totalSize: " + totalSize);

        int progress = (acknowledgedBytes * 100) / totalSize;
        int secondaryProgress = (transmittedBytes * 100) / totalSize;

        runOnUiThread(new Runnable() {
            public void run() {
                mTransmittedBytesTextView.setText(String.format("%d / %d bytes", acknowledgedBytes, totalSize));

                switch (mCurrentAction) {
                    case FIRMWARE_UPGRADE:
                    case SEND_PICTURE:
                    case SEND_DATA:
                        mProgressBar.setProgress(progress);
                        mProgressBar.setSecondaryProgress(secondaryProgress);
                        break;
                    default:
                        break;
                }
            }
        });

    }

    @Override
    public void receptionProgress(int receivedBytes, int acknowledgedBytes, int totalSize) {
        //Log.d(TAG, "receptionProgress currentSize: " + acknowledgedBytes + "  totalSize: " + totalSize);

        int progress = (acknowledgedBytes * 100) / totalSize;
        int secondaryProgress = (receivedBytes * 100) / totalSize;

        runOnUiThread(new Runnable() {
            public void run() {
                mReceivedBytesTextView.setText(String.format("%d / %d bytes", acknowledgedBytes, totalSize));

                switch (mCurrentAction) {
                    case READ_PICTURE:
                    case READ_DATA:
                        mProgressBar.setProgress(progress);
                        mProgressBar.setSecondaryProgress(secondaryProgress);
                        break;
                    default:
                        break;
                }
            }
        });

    }

    private void stopTimer() {
        runOnUiThread(new Runnable() {
            public void run() {
                mTimer.stop();
            }
        });
    }

    private void startTimer() {
        runOnUiThread(new Runnable() {
            public void run() {
                mTimer.start();
            }
        });
    }

    private void selectDataSizeDialogBox() {
        final AlertDialog alertDialog = new AlertDialog.Builder(ST25DVFtmDemoActivity.this).create();

        alertDialog.setTitle("");

        // inflate XML content
        View dialogView = getLayoutInflater().inflate(R.layout.fragment_enter_data_size, null);
        alertDialog.setView(dialogView);

        EditText numberOfBytesEditText = dialogView.findViewById(R.id.numberOfBytesEditText);

        Button sendButton = dialogView.findViewById(R.id.sendButton);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    mNbrOfBytesToSend = Integer.parseInt(numberOfBytesEditText.getText().toString());
                    if (mNbrOfBytesToSend > 0) {
                        alertDialog.cancel();
                        executeAsynchronousAction(Action.SEND_DATA);
                    }
                } catch (Exception e) {
                    STLog.e("Bad Value" + e.getMessage());
                }
            }
        });

        // show it
        alertDialog.show();
    }

    private void pleaseSelectDataSizeOnDiscovery() {
        androidx.appcompat.app.AlertDialog.Builder alertDialogBuilder = new androidx.appcompat.app.AlertDialog.Builder(ST25DVFtmDemoActivity.this);

        // set title
        //alertDialogBuilder.setTitle(getString(R.string.confirmation_needed));

        // set dialog message
        alertDialogBuilder
                .setMessage(getString(R.string.please_select_a_data_size_on_discovery))
                .setCancelable(true)

                .setPositiveButton(getString(R.string.ok),new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        dialog.cancel();
                        executeAsynchronousAction(Action.READ_DATA);
                    }
                });

        // create alert dialog
        androidx.appcompat.app.AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.st_light_blue));

    }

    private void displayErrorRecoverySelectionPopup() {
        final AlertDialog alertDialog = new AlertDialog.Builder(ST25DVFtmDemoActivity.this).create();

        // set title
        alertDialog.setTitle(R.string.please_select_a_mode);

        // inflate XML content
        View dialogView = getLayoutInflater().inflate(R.layout.error_recovery_mode_selection_popup, null);
        alertDialog.setView(dialogView);

        final RadioButton errorRecoveryEnabledRadioButton = (RadioButton) dialogView.findViewById(R.id.errorRecoveryEnabledRadioButton);
        final RadioButton errorRecoveryDisabledRadioButton = (RadioButton) dialogView.findViewById(R.id.errorRecoveryDisabledRadioButton);

        if (mIsErrorRecoveryEnabled) {
            errorRecoveryEnabledRadioButton.setChecked(true);
        } else {
            errorRecoveryDisabledRadioButton.setChecked(true);
        }

        Button continueButton = (Button) dialogView.findViewById(R.id.continueButton);
        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Leave the dialog box
                alertDialog.dismiss();

                if (errorRecoveryEnabledRadioButton.isChecked()) {
                    mIsErrorRecoveryEnabled = true;
                    mErrorRecoveryTextView.setText(R.string.error_recovery_enabled);
                } else {
                    mIsErrorRecoveryEnabled = false;
                    mErrorRecoveryTextView.setText(R.string.error_recovery_disabled);
                }
            }
        });

        // show it
        alertDialog.show();
    }

    /**
     * Display an AlertDialog to choose between:
     * - the demo FW embedded in the APK
     * - a FW present in phone's memory storage
     */
    private void chooseFirmware() {
        final AlertDialog alertDialog = new AlertDialog.Builder(ST25DVFtmDemoActivity.this).create();

        // set title
        alertDialog.setTitle(R.string.firmware_update_message);

        // inflate XML content
        View dialogView = getLayoutInflater().inflate(R.layout.st25dv_firmware_selection_popup, null);
        alertDialog.setView(dialogView);

        final RadioButton embeddedFirmwareMb1283RadioButton = dialogView.findViewById(R.id.embeddedFirmwareMb1283RadioButton);
        final RadioButton embeddedFirmwareMb1396RadioButton = dialogView.findViewById(R.id.embeddedFirmwareMb1396RadioButton);
        final RadioButton memoryStorageFirmwareRadioButton = dialogView.findViewById(R.id.memoryStorageFirmwareRadioButton);

        switch(mBoardId) {
            case BOARD_MB1283:
                embeddedFirmwareMb1283RadioButton.setChecked(true);
                break;
            case BOARD_MB1396:
                embeddedFirmwareMb1396RadioButton.setChecked(true);
                break;
            default:
                memoryStorageFirmwareRadioButton.setChecked(true);
                break;
        }

        Button continueButton = (Button) dialogView.findViewById(R.id.continueButton);
        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (embeddedFirmwareMb1283RadioButton.isChecked()) {
                    selectEmbeddedFirmwareForMb1283();
                } else if (embeddedFirmwareMb1396RadioButton.isChecked()) {
                    selectEmbeddedFirmwareForMb1396();
                } else {
                    String message = getString(R.string.please_select_firmware_file);

                    // Pick up FW in phone's memory storage
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

                    startActivityForResult(chooserIntent, SELECT_FIRMWARE);
                }

                // Leave the dialog box
                alertDialog.dismiss();
            }
        });

        // show it
        alertDialog.show();
    }

    public void displayPicture(byte[] buffer) {
        class DisplayPicture implements Runnable {

            byte[] mBuffer;

            public DisplayPicture(byte[] buffer) {
                mBuffer = buffer;
            }

            @Override
            public void run() {
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(ST25DVFtmDemoActivity.this);
                final AlertDialog dialog = alertDialogBuilder.create();

                View v = getLayoutInflater().inflate(R.layout.display_bitmap, null);

                Bitmap bitmap = BitmapFactory.decodeByteArray(mBuffer, 0, mBuffer.length);

                ImageView imageView = (ImageView) v.findViewById(R.id.bitmapImageView);
                int bitmapWidth = bitmap.getWidth();
                int bitmapHeight = bitmap.getHeight();
                int newWidth = 800;
                Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, bitmapHeight * newWidth / bitmapWidth, true);

                imageView.setImageBitmap(resizedBitmap);

                alertDialogBuilder.setView(v);

                alertDialogBuilder.show();
            }
        }
        mHandler.post(new DisplayPicture(buffer));
    }


    private void presentPassword() {

        View promptView = getLayoutInflater().inflate(R.layout.present_firmware_password, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setView(promptView);
        final EditText passwordEditText = (EditText) promptView.findViewById(R.id.user_input);

        alertDialogBuilder.setCancelable(false).setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                String password = passwordEditText.getText().toString();
                mPassword =  Helper.convertHexStringToByteArray(password);
                executeAsynchronousAction(Action.FIRMWARE_UPGRADE);
            }
        }).setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();

        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(R.color.st_light_blue));
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.st_light_blue));
    }

    private void selectEmbeddedFirmwareForMb1283() {
        mFirmwareInputStream = ST25DVFtmDemoActivity.class.getResourceAsStream("/assets/ST25DVDemo_FwUpgrd_MB1283.bin");

        String filePath = "Embedded demo Firmware for MB1283";

        int fileSize = 0;
        try {
            fileSize = mFirmwareInputStream.available();
        } catch (IOException e) {
            showToast(R.string.failed_to_red_file_size);
            e.printStackTrace();
            return;
        }

        presentPassword();
    }

    private void selectEmbeddedFirmwareForMb1396() {
        mFirmwareInputStream = ST25DVFtmDemoActivity.class.getResourceAsStream("/assets/ST25DVDemo_FwUpgrd_MB1396.bin");

        String filePath = "Embedded demo Firmware for MB1396";

        int fileSize = 0;
        try {
            fileSize = mFirmwareInputStream.available();
        } catch (IOException e) {
            showToast(R.string.failed_to_red_file_size);
            e.printStackTrace();
            return;
        }

        presentPassword();
    }

    private void parseBoardInfo(byte[] response) throws STException {
        if ((response == null) || (response.length != 4)) {
            throw new STException(BAD_PARAMETER);
        }

        mBoardId = response[0];
        mVersionMajor = response[1];
        mVersionMinor = response[2];
        mVersionPatch = response[3];
        mBoardInfoCollected = true;
    }

    private byte[] getRandomData(int size) {
        SecureRandom random = new SecureRandom();
        byte[] randomData = new byte[size];
        random.nextBytes(randomData);
        return randomData;
    }

    private void executeAsynchronousAction(Action action) {
        Log.d(TAG, "Starting background action " + action);
        mCurrentAction = action;
        new myAsyncTask(action).execute();
    }

    private class myAsyncTask extends AsyncTask<Void, Void, ActionStatus> {
        Action mAction;
        private boolean mIsMailboxEnabled;
        private byte[] mDataReceived;

        public myAsyncTask(Action action) {
            mAction = action;
        }

        @Override
        protected ActionStatus doInBackground(Void... param) {
            ActionStatus result;
            mCancelCurrentTransfer = false;

            try {
                switch (mAction) {
                    case GET_BOARD_INFO:
                        mIsMailboxEnabled = mST25DVTag.isMailboxEnabled(true);
                        if (mIsMailboxEnabled) {
                            // Retrieve board name and FW version
                            byte[] data = new byte[] { 0x00, 0x00 };
                            mDataReceived = mFtmCommands.sendCmdAndWaitForCompletion(FTM_CMD_GET_BOARD_INFO, data,false, true, null, FtmCommands.SHORT_FTM_TIME_OUT_IN_MS);
                            parseBoardInfo(mDataReceived);
                        }
                        result = ACTION_SUCCESSFUL;
                        break;
                    case ENABLE_MAILBOX:
                        mST25DVTag.enableMailbox();
                        result = ACTION_SUCCESSFUL;
                        break;
                    case READ_DATA:
                        startTimer();
                        byte cmdId = mIsErrorRecoveryEnabled ? FTM_CMD_READ_DATA : FTM_CMD_READ_DATA_NO_ERROR_RECOVERY;
                        mDataReceived = mFtmCommands.sendCmdAndWaitForCompletion(cmdId, null, mIsErrorRecoveryEnabled, true, ST25DVFtmDemoActivity.this, FtmCommands.DEFAULT_FTM_TIME_OUT_IN_MS);
                        result = ACTION_SUCCESSFUL;
                        break;
                    case SEND_DATA:
                        startTimer();
                        // Send some random data of the requested length (mNbrOfBytesToSend)
                        if (mNbrOfBytesToSend == 1) {
                            mDataReceived = mFtmCommands.sendCmdAndWaitForCompletion(FTM_CMD_SEND_DATA, null, mIsErrorRecoveryEnabled, true, ST25DVFtmDemoActivity.this, FtmCommands.DEFAULT_FTM_TIME_OUT_IN_MS);
                        } else {
                            // There will be one byte of command and (mNbrOfBytesToSend-1) bytes of random data
                            byte[] randomData = getRandomData(mNbrOfBytesToSend-1);
                            mDataReceived = mFtmCommands.sendCmdAndWaitForCompletion(FTM_CMD_SEND_DATA, randomData, mIsErrorRecoveryEnabled, true, ST25DVFtmDemoActivity.this, FtmCommands.DEFAULT_FTM_TIME_OUT_IN_MS);
                        }
                        result = ACTION_SUCCESSFUL;
                        break;
                    case READ_PICTURE:
                        startTimer();
                        cmdId = mIsErrorRecoveryEnabled ? FTM_CMD_READ_PICTURE : FTM_CMD_READ_PICTURE_NO_ERROR_RECOVERY;
                        mDataReceived = mFtmCommands.sendCmdAndWaitForCompletion(cmdId,null, mIsErrorRecoveryEnabled, true, ST25DVFtmDemoActivity.this, FtmCommands.DEFAULT_FTM_TIME_OUT_IN_MS);
                        result = ACTION_SUCCESSFUL;
                        break;
                    case SEND_PICTURE:
                        startTimer();
                        mDataReceived = mFtmCommands.sendCmdAndWaitForCompletion(FTM_CMD_SEND_PICTURE, mCompressedImageRawData, mIsErrorRecoveryEnabled, true, ST25DVFtmDemoActivity.this, FtmCommands.DEFAULT_FTM_TIME_OUT_IN_MS);
                        result = ACTION_SUCCESSFUL;
                        break;
                    case FIRMWARE_UPGRADE:
                        // Send the password
                        mDataReceived = mFtmCommands.sendCmdAndWaitForCompletion(FTM_CMD_SEND_PASSWORD, mPassword,mIsErrorRecoveryEnabled, true, ST25DVFtmDemoActivity.this, FtmCommands.DEFAULT_FTM_TIME_OUT_IN_MS);
                        // Command successful
                        // Send the firmware
                        startTimer();
                        byte[] fwData = UIHelper.readInputStream(mFirmwareInputStream);
                        mDataReceived = mFtmCommands.sendCmdAndWaitForCompletion(FTM_CMD_FW_UPGRADE, fwData,mIsErrorRecoveryEnabled, true, ST25DVFtmDemoActivity.this, FtmCommands.DEFAULT_FTM_TIME_OUT_IN_MS);
                        result = ACTION_SUCCESSFUL;
                        break;

                    default:
                        result = ACTION_FAILED;
                        break;
                }

            } catch (STException e) {
                switch (e.getError()) {
                    case CONFIG_PASSWORD_NEEDED:
                        result = ActionStatus.CONFIG_PASSWORD_NEEDED;
                        break;

                    case TAG_NOT_IN_THE_FIELD:
                        result = TAG_NOT_IN_THE_FIELD;
                        break;

                    case RFREADER_NO_RESPONSE:
                        result = NO_RESPONSE;
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

            stopTimer();
            mCurrentAction = IDLE;

            switch(actionStatus) {
                case ACTION_SUCCESSFUL:
                    switch (mAction) {
                        case GET_BOARD_INFO:
                            if (mIsMailboxEnabled) {
                                displayBoardNameAndCheckVersion();
                            } else {
                                displayMailboxAlert();
                            }
                            break;
                        case ENABLE_MAILBOX:
                            showToast(R.string.Mailbox_enabled);
                            executeAsynchronousAction(Action.GET_BOARD_INFO);
                            break;

                        case READ_PICTURE:
                            displayPicture(mDataReceived);
                            break;
                        case READ_DATA:
                        case FIRMWARE_UPGRADE:
                        case SEND_PICTURE:
                        case SEND_DATA:
                            showToast(R.string.command_successful);
                            break;
                    }
                    break;

                case CONFIG_PASSWORD_NEEDED:
                    //displayConfigurationPasswordDialogBox();
                    break;

                case ERROR_DURING_PWD_PRESENTATION:
                    showToast(R.string.error_during_password_presentation);
                    break;

                case ACTION_FAILED:
                    if (!mCancelCurrentTransfer) {
                        showToast(R.string.command_failed);
                    }
                    break;

                case NO_RESPONSE:
                    if (!mCancelCurrentTransfer) {
                        if (mAction == Action.GET_BOARD_INFO) {
                            UIHelper.displayMessage(ST25DVFtmDemoActivity.this, R.string.failed_to_retrieve_board_name);
                        } else {
                            showToast(R.string.no_response_received);
                        }
                    }
                    break;

                case TAG_NOT_IN_THE_FIELD:
                    if (!mCancelCurrentTransfer) {
                        if (mAction == Action.GET_BOARD_INFO) {
                            UIHelper.displayMessage(ST25DVFtmDemoActivity.this, R.string.please_put_the_phone_on_the_discovery_board);
                        } else {
                            showToast(R.string.tag_not_in_the_field);
                        }
                    }
                    break;
            }

            return;
        }
    }


}
