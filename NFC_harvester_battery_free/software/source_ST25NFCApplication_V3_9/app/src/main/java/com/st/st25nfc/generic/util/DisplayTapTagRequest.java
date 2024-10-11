package com.st.st25nfc.generic.util;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.widget.RadioButton;
import android.widget.TextView;

import com.st.st25nfc.R;
import com.st.st25nfc.generic.STFragmentActivity;
import com.st.st25sdk.NFCTag;
import com.st.st25sdk.STException;
import com.st.st25sdk.type5.st25tvc.ST25TVCTag;

public class DisplayTapTagRequest {

    public interface TapTagDialogBoxListener {
        void tapTagDialogBoxClosed();
    }

    static final String TAG = "DisplayTapTagRequest";

    static private android.app.AlertDialog mTapTagRequestDialog = null;
    static private Boolean mDisplayTapTagRequestDialog = false;
    static private TapTagDialogBoxListener mTapTagDialogBoxListener;

    // NB: On Android 7 and upper versions, the "Tap tag" request is handled automatically
    public static void run(final STFragmentActivity stFragmentActivity, final NFCTag nfcTag, final String message) {
        run(stFragmentActivity, nfcTag, message, null);
    }

    public static void run(final STFragmentActivity stFragmentActivity, final NFCTag nfcTag, final String message, TapTagDialogBoxListener tapTagDialogBoxListener) {
        mDisplayTapTagRequestDialog = true;
        mTapTagDialogBoxListener = tapTagDialogBoxListener;

        stFragmentActivity.runOnUiThread(new Runnable() {
            public void run() {

                stFragmentActivity.registerTapTagListener(new STFragmentActivity.TagTapedListener() {
                    @Override
                    public void tagTaped(boolean tagChanged) {
                        Log.e(TAG, "tagChanged: " + tagChanged);
                        if (!tagChanged) {
                            // The same tag has been taped so the configuration has been updated and
                            // the cache should be refreshed
                            updateCache(stFragmentActivity, nfcTag);
                            stFragmentActivity.showToast(R.string.tag_updated);
                        }

                        if (mTapTagRequestDialog != null) {
                            mTapTagRequestDialog.cancel();
                            mTapTagRequestDialog = null;
                        }

                        if (mTapTagDialogBoxListener != null) {
                            mTapTagDialogBoxListener.tapTagDialogBoxClosed();
                        }
                    }
                });

                // NB: On Android 7 and upper versions, the "Tap tag" request is handled automatically
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(stFragmentActivity);
                    UIHelper.resetTag(nfcAdapter, nfcTag);
                } else {
                    DisplayTapTagDialog(stFragmentActivity, nfcTag, message);
                }

            }
        });
    }

    private static void DisplayTapTagDialog(final STFragmentActivity stFragmentActivity, final NFCTag nfcTag, final String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(stFragmentActivity);
        mTapTagRequestDialog = builder.create();

        mTapTagRequestDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                stFragmentActivity.unRegisterTapTagListener();
            }
        });

        // set title
        //alertDialog.setTitle(getString(R.string.XXX));

        // inflate XML content
        View dialogView = stFragmentActivity.getLayoutInflater().inflate(R.layout.tap_tag_request, null);
        mTapTagRequestDialog.setView(dialogView);

        TextView msgTextView = (TextView) dialogView.findViewById(R.id.msgTextView);
        msgTextView.setText(message);

        // show the dialog box
        mTapTagRequestDialog.show();
    }

    static private void updateCache(final STFragmentActivity stFragmentActivity, final NFCTag nfcTag) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    if (nfcTag instanceof ST25TVCTag) {
                        ST25TVCTag mST25TVCTag = (ST25TVCTag) nfcTag;

                        // WARNING: System file should be updated because the memory size depends of SIG_ANDEF value
                        mST25TVCTag.getSystemFile().updateCache();

                        mST25TVCTag.invalidateNdefCache();
                    }

                } catch (STException e) {
                    e.printStackTrace();
                    stFragmentActivity.showToast(R.string.error_while_reading_the_tag);
                }
            }
        }).start();
    }
}
