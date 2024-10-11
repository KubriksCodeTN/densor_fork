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

package com.st.st25nfc.generic.util;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Build;
import android.os.Looper;
import androidx.appcompat.app.AlertDialog;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.st.st25android.AndroidReaderInterface;
import com.st.st25nfc.R;
import com.st.st25nfc.densor.DataViewFragment;
import com.st.st25nfc.densor.DensorSettingsFragment;
import com.st.st25nfc.densor.TimeSyncFragment;
import com.st.st25nfc.generic.MainActivity;
import com.st.st25nfc.generic.RawReadWriteFragment;
import com.st.st25nfc.generic.STFragment;
import com.st.st25nfc.generic.TagInfoFragment;
import com.st.st25nfc.generic.ndef.NDEFEditorFragment;
import com.st.st25nfc.type5.CCFileType5Fragment;
import com.st.st25nfc.type5.SysFileType5Fragment;
import com.st.st25sdk.About;
import com.st.st25sdk.NFCTag;

import com.st.st25sdk.STException;
import com.st.st25sdk.type2.STType2PasswordInterface;
import com.st.st25sdk.type2.Type2Tag;
import com.st.st25sdk.type4a.Type4Tag;
import com.st.st25sdk.type5.STType5PasswordInterface;
import com.st.st25sdk.type5.Type5Tag;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;
import java.util.Set;

import static com.st.st25sdk.STException.STExceptionCode.BAD_PARAMETER;


public class UIHelper {

    static final String TAG = "UIHelper";

    static private android.app.AlertDialog mProgressAlertDialog = null;
    static private Boolean mDisplayCircularProgressBar = false;

    /**
     * Function indicating if the current thread is the UI Thread
     *
     * @return
     */
    public static boolean isUIThread() {
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            // On UI thread.
            return true;
        } else {
            // Not on UI thread.
            return false;
        }
    }

    /**
     * Print an error message if this function is called from a non UI Thread.
     * This verification is useful when updating some UI elements like ListView.
     */
    public static void checkIfUiThread() {
        if (!isUIThread()) {
            Log.e(TAG, "Current thread is not the UI Thread!");
        }
    }

    // Identifiers of STFragments. Each id correspond to a STFragment
    public enum STFragmentId {
        // Generic Fragments
        TAG_INFO_FRAGMENT_ID,
        TAG_INFO_TYPE2_FRAGMENT_ID,
        CC_FILE_TYPE5_FRAGMENT_ID,
        CC_FILE_TYPE4_FRAGMENT_ID,
        CC_FILE_TYPE2_FRAGMENT_ID,
        SYS_FILE_TYP5_FRAGMENT_ID,
        SYS_FILE_M24SR_FRAGMENT_ID,
        SYS_FILE_ST25TA_HIGH_DENSITY_FRAGMENT_ID,
        SYS_FILE_ST25TA_FRAGMENT_ID,
        RAW_DATA_FRAGMENT_ID,
        NDEF_DETAILS_FRAGMENT_ID,

        // M24SR Fragments
        M24SR_NDEF_DETAILS_FRAGMENT_ID,
        M24SR_EXTRA_FRAGMENT_ID,

        // ST25TV Fragments
        ST25TV_CONFIG_FRAGMENT_ID,

        // NDEF Fragments
        NDEF_MULTI_RECORD_FRAGMENT_ID,
        NDEF_SMS_FRAGMENT_ID,
        NDEF_TEXT_FRAGMENT_ID,
        NDEF_URI_FRAGMENT_ID,

        // Densor Fragments
        DENSOR_TIMESYNC_ID,
        DENSOR_SETTINGS_ID,
        DENSOR_DATA_VIEW
    }

    /**
     * This function instantiate a STFragment from its STFragmentId
     *
     * @param context
     * @param stFragmentId
     * @return
     */
    public static STFragment getSTFragment(Context context, STFragmentId stFragmentId) {
        STFragment fragment = null;

        switch (stFragmentId) {
            // Generic Fragments
            case TAG_INFO_FRAGMENT_ID:
                fragment = TagInfoFragment.newInstance(context);
                break;
            case CC_FILE_TYPE5_FRAGMENT_ID:
                fragment = CCFileType5Fragment.newInstance(context);
                break;
            case SYS_FILE_TYP5_FRAGMENT_ID:
                fragment = SysFileType5Fragment.newInstance(context);
                break;
            case RAW_DATA_FRAGMENT_ID:
                fragment = RawReadWriteFragment.newInstance(context);
                break;
            case NDEF_DETAILS_FRAGMENT_ID:
                fragment = NDEFEditorFragment.newInstance(context);
                break;
            case DENSOR_TIMESYNC_ID:
                fragment = TimeSyncFragment.newInstance(context);
                break;
            case DENSOR_SETTINGS_ID:
                fragment = DensorSettingsFragment.newInstance(context);
                break;
            case DENSOR_DATA_VIEW:
                fragment = DataViewFragment.newInstance(context);
                break;
            default:
                Log.e(TAG, "Invalid stFragmentId: " + stFragmentId);
                break;

        }

        return fragment;
    }


    // Convert the area number into an area name
    public static String getAreaName(int area) {
        String areaName = getApplicationResources().getString(R.string.area_number_to_name) + area;
        return areaName;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    public static boolean isAType4Tag(NFCTag tag) {
        if (tag instanceof Type4Tag) {
            return true;
        } else {
            return false;
        }
    }
    public static boolean isAType2Tag(NFCTag tag) {
        if (tag instanceof Type2Tag) {
            return true;
        } else {
            return false;
        }
    }
    public static boolean isAType5Tag(NFCTag tag) {
        if (tag instanceof Type5Tag) {
            return true;
        } else {
            return false;
        }
    }

    public static void invalidateCache(NFCTag tag) {
        if (tag instanceof Type4Tag) {
            Type4Tag type4Tag = (Type4Tag) tag;
            type4Tag.invalidateCache();
        } else if (tag instanceof Type5Tag) {
            Type5Tag type5Tag = (Type5Tag) tag;
            type5Tag.invalidateCache();
        } else if (tag instanceof Type2Tag) {
            Type2Tag type2Tag = (Type2Tag) tag;
            type2Tag.invalidateCache();
        } else {
            Log.e(TAG, "Tag not supported yet!");
        }
    }

    /**
     * Function returning the Type4 fileId corresponding to an Area.*
     * @param area
     */
    public static int getType4FileIdFromArea(int area) {
        return area;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    public static void resetTag(NfcAdapter nfcAdapter, NFCTag nfcTag) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            System.out.println("=== resetTag ===");
            AndroidReaderInterface androidReaderInterface = (AndroidReaderInterface) nfcTag.getReaderInterface();
            Tag androidTag = androidReaderInterface.getAndroidTag();
            int debounceMs = 100;
            nfcAdapter.ignore(androidTag, debounceMs, null,null);
        } else {
            Log.e(TAG, "resetTag() not supported yet on Android versions lower than Android 7.0");
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    public static void displayAboutDialogBox(Context context) {

        //set up dialog
        final Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.application_version_dialog);
        dialog.setTitle(context.getResources().getString(R.string.version_dialog_header));
        dialog.setCancelable(true);

        String versionName = "???";
        try {
            versionName = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }


        //set up text
        String message;
        message = context.getResources().getString(R.string.app_version_v) + versionName;

        TextView text = (TextView) dialog.findViewById(R.id.versionTextView);
        text.setText(message);

        message = "";
        message = message + "ST25SDK" + ": " + About.getFullVersion() + "\n";

        Set<String> productFeatures = About.getExtraFeatureList();
        if (String.valueOf(productFeatures).length() == 2) {
            // Public version
            // SDK return []
            message = message + context.getResources().getString(R.string.public_version);
            text = (TextView) dialog.findViewById(R.id.featuresTextView);
            text.setText(message);

        } else {
            // Features with NDA version
            message = message + context.getResources().getString(R.string.product_features) + ": " + About.getExtraFeatureList();
            text = (TextView) dialog.findViewById(R.id.featuresTextView);
            text.setText(message);
        }


        message = context.getResources().getString(R.string.app_description);
        text = (TextView) dialog.findViewById(R.id.TextView02);
        text.setText(message);

        //set up image view
        ImageView img = (ImageView) dialog.findViewById(R.id.versionImageView);
        img.setImageResource(R.drawable.logo_st25_transp);

        //set up button
        Button button = (Button) dialog.findViewById(R.id.Button01);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        //now that the dialog is set up, it's time to show it
        dialog.show();

        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        int width = metrics.widthPixels;
        int height = metrics.heightPixels;
        dialog.getWindow().setLayout((95 * width) / 100, (95 * height) / 100);
    }

    public static Resources getApplicationResources() {
        return MainActivity.mResources;
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////

    public static String convertInputStreamToString(InputStream inputStream) {
        Scanner scanner = new Scanner(inputStream).useDelimiter("\\A");

        return scanner.hasNext() ? scanner.next() : "";
    }

    public static void displayCircularProgressBar(final Activity activity, final String message) {
        mDisplayCircularProgressBar = true;

        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    mProgressAlertDialog = new android.app.AlertDialog.Builder(activity).create();

                    // inflate XML content
                    View dialogView = activity.getLayoutInflater().inflate(R.layout.fragment_display_progress_bar, null);
                    if (dialogView != null && mProgressAlertDialog != null) {
                        mProgressAlertDialog.setView(dialogView);
                        TextView mMessageTextView = (TextView) dialogView.findViewById(R.id.messageTextView);
                        mMessageTextView.setText(message);
                        synchronized(mDisplayCircularProgressBar) {
                            // NB: A request to dismiss the Circular ProgressBar may have happened in the meantime
                            if (mDisplayCircularProgressBar) {
                                // show the progressBar
                                mProgressAlertDialog.show();
                            }
                        }
                    }
                }
            });
        }
    }

    public static void dismissCircularProgressBar() {

        synchronized(mDisplayCircularProgressBar) {
            mDisplayCircularProgressBar = false;

            if (mProgressAlertDialog != null ) {
                if (mProgressAlertDialog.isShowing()) {
                    mProgressAlertDialog.dismiss();
                }
                mProgressAlertDialog = null;
            }
        }
    }

    public static int getConfigurationPasswordNumber(NFCTag nfcTag) {
        int passwordNumber = -1;

        try {
            if (nfcTag instanceof STType5PasswordInterface) {
                passwordNumber = ((STType5PasswordInterface) nfcTag).getConfigurationPasswordNumber();
            } else if (nfcTag instanceof STType2PasswordInterface) {
                passwordNumber = ((STType2PasswordInterface) nfcTag).getConfigurationPasswordNumber();
            } else {
                Log.e(TAG, "Tag not supported!");
            }

        } catch (STException e) {
            e.printStackTrace();
        }

        return passwordNumber;
    }

    public static void displayMessage(final Activity activity, int resource_id) {
        Resources resources = activity.getResources();
        if(resources == null) {
            Log.e(TAG, "Failed to retrieve resource!");
        }
        String message = resources.getString(resource_id);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(activity);

        // set dialog message
        alertDialogBuilder
                .setMessage(message)
                .setCancelable(true)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        dialog.cancel();
                    }
                });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();

        alertDialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setTextColor(resources.getColor(R.color.st_light_blue));
    }

    /**
     * Allocate a buffer of a requested size containing 01234567890123...etc
     * This is mainly used for debugging.
     *
     * @param dataLength
     * @return
     */
    static public byte[] allocateAndInitData(int dataLength) {
        char[] alphabet = "0123456789".toCharArray();

        byte[] data = new byte[dataLength];

        for(int i=0; i<dataLength; i++) {
            data[i] = (byte) alphabet[i % 10];
        }

        return data;
    }

    /**
     * Read the data present in an inputStream.
     * The read is done by batches of 1000 bytes max.
     *
     * @param inputStream
     * @return
     * @throws IOException
     */
    static public byte[] readInputStream(InputStream inputStream) throws IOException, STException {

        if ((inputStream == null) || (inputStream.available() == 0)) {
            throw new STException(BAD_PARAMETER);
        }

        int remaining = inputStream.available();

        byte[] buffer = new byte[remaining];
        int writeOffset = 0;

        while (remaining > 0) {
            int len = (remaining > 1000) ? 1000 : remaining;

            int dataRead = inputStream.read(buffer, writeOffset, len);

            writeOffset += dataRead;

            remaining = inputStream.available();
        }

        return buffer;
    }

    public static int findItemPositionInStringArray(String[] array, String item) {
        if ((array == null) || (array.length == 0)) {
            Log.e(TAG, "Empty array!");
            return 0;
        }

        for (int i=0; i < array.length; i++) {
            if (array[i].equals(item)) {
                return i;
            }
        }

        Log.e(TAG, "Item not found!");
        return -1;
    }

}

