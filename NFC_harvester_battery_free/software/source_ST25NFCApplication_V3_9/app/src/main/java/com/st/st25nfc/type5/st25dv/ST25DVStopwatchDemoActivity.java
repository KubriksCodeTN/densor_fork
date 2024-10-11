package com.st.st25nfc.type5.st25dv;

import android.app.AlertDialog;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.st.st25nfc.R;
import com.st.st25nfc.generic.MainActivity;
import com.st.st25nfc.generic.ST25Menu;
import com.st.st25nfc.generic.STFragmentActivity;
import com.st.st25nfc.generic.util.UIHelper;
import com.st.st25sdk.STException;
import com.st.st25sdk.STLog;
import com.st.st25sdk.ftmprotocol.FtmCommands;
import com.st.st25sdk.type5.st25dv.ST25DVTag;

import java.util.Arrays;

import static com.st.st25nfc.type5.st25dv.ST25DVStopwatchDemoActivity.ActionStatus.*;
import static com.st.st25sdk.STException.STExceptionCode.BAD_PARAMETER;
import static com.st.st25sdk.ftmprotocol.FtmCommands.FTM_CMD_GET_BOARD_INFO;


public class ST25DVStopwatchDemoActivity extends STFragmentActivity implements NavigationView.OnNavigationItemSelectedListener {
    final static String TAG = "StopwatchDemo";
    private ST25DVTag mST25DVTag;
    private FtmCommands mFtmCommands;
    private Handler mHandler;

    enum Action {
        GET_BOARD_INFO
    };

    enum ActionStatus {
        ACTION_SUCCESSFUL,
        ACTION_FAILED,
        TAG_NOT_IN_THE_FIELD,
        CONFIG_PASSWORD_NEEDED,
        ERROR_DURING_PWD_PRESENTATION,
        NO_RESPONSE
    };

    private Chronometer mStopwatch;
    private boolean mContinue = true;

    private byte mBoardName;

    private static final int BOARD_MB1283 = 0x00;
    private static final int MB1283_MINIMUM_VERSION_MAJOR_NBR = 1;
    private static final int MB1283_MINIMUM_VERSION_MINOR_NBR = 2;
    private static final int MB1283_MINIMUM_VERSION_PATCH_NBR = 0;

    private static final int BOARD_MB1396 = 0x01;
    private static final int MB1396_MINIMUM_VERSION_MAJOR_NBR = 2;
    private static final int MB1396_MINIMUM_VERSION_MINOR_NBR = 1;
    private static final int MB1396_MINIMUM_VERSION_PATCH_NBR = 0;

    private byte mVersionMajor;
    private byte mVersionMinor;
    private byte mVersionPatch;
    private boolean mIsFwUpdateNeeded;
    private boolean mBoardInfoCollected = false;
    private boolean mMaterialNeededMsgDisplayed = false;

    public static final byte FTM_STOPWATCH = 0x03;


    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.default_layout);

        // Inflate content of FrameLayout
        FrameLayout frameLayout=(FrameLayout) findViewById(R.id.frame_content);
        View childView = getLayoutInflater().inflate(R.layout.chronometer_demo_content_st25dv, null);
        frameLayout.addView(childView);

        if (super.getTag() == null) {
            showToast(R.string.invalid_tag);
            goBackToMainActivity();
            return;
        }

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

        mStopwatch = (Chronometer) findViewById(R.id.st25DvChronometer);

        Button startButton = (Button) findViewById(R.id.startChronoButton);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mStopwatch.start();
            }
        });

        Button stopButton = (Button) findViewById(R.id.stopChronoButton);
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mStopwatch.stop();
            }
        });

        Button resumeButton = (Button) findViewById(R.id.resumeChronoButton);
        resumeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mStopwatch.resume();
            }
        });

        Button pauseButton = (Button) findViewById(R.id.pauseChronoButton);
        pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mStopwatch.pause();
            }
        });

        mHandler = new Handler();
        mST25DVTag = (ST25DVTag) MainActivity.getTag();
        mFtmCommands = new FtmCommands(mST25DVTag);

        mFtmCommands.setMinTimeInMsBetweenConsecutiveCmds(0);   // To reach the max throughput

        startStopWatchThread();

        displayMaterialNeededAlert();
    }

    @Override
    public void onResume() {
        super.onResume();

        if(mMaterialNeededMsgDisplayed) {
            if (mStopwatch.getState() != Chronometer.STATE.STARTED) {
                executeAsynchronousAction(Action.GET_BOARD_INFO);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopStopWatchThread();
    }


    private void displayMaterialNeededAlert() {
        AlertDialog.Builder builder = new AlertDialog.Builder(ST25DVStopwatchDemoActivity.this);
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

    private void startStopWatchThread() {
        Log.d(TAG, "Starting StopWatch Thread");

        new Thread(new Runnable() {
            public void run() {
                byte[] lastData = new byte[3];

                while (mContinue) {
                    try {
                        byte[] newData = getStopWatchData();
                        if (!Arrays.equals(newData, lastData)) {
                            // StopWatch data has changed. Send it to the Discovery board
                            mFtmCommands.sendCmdAndWaitForCompletion(FTM_STOPWATCH, newData, false, false, null, FtmCommands.SHORT_FTM_TIME_OUT_IN_MS);

                            System.arraycopy(newData, 0, lastData, 0,lastData.length);

                            // Sleep before sending new StopWatch data
                            //sleep_in_ms(10);
                        } else {
                            // StopWatch data are the same
                            sleep_in_ms(10);
                        }
                    } catch (InterruptedException | STException e) {
                        e.printStackTrace();
                        sleep_in_ms(20);
                    }
                }

                Log.d(TAG, "Stopping StopWatch Thread");
            }
        }).start();
    }

    private void sleep_in_ms(int time_in_ms) {
        try {
            Thread.sleep(time_in_ms);
            //SystemClock.sleep(time_in_ms);
        } catch (InterruptedException e) {
            STLog.e(e.getMessage());
        }
    }

    private void stopStopWatchThread() {
        mContinue = false;
    }

    public byte[] getStopWatchData() {
        byte[] data = new byte[3];
        long time = mStopwatch.getTimeElapsed();

        int remaining = (int) (time % (3600 * 1000));

        int minutes = (int) (remaining / (60 * 1000));
        remaining = (int) (remaining % (60 * 1000));

        int seconds = (int) (remaining / 1000);
        remaining = (int) (remaining % (1000));

        int hundredsseconds = (int) (((int) time % 1000) / 10);

        data[0] = (byte) (minutes & 0xFF);
        data[1] = (byte) (seconds  & 0xFF);
        data[2] = (byte) (hundredsseconds & 0xFF);

        return data;
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        return mMenu.selectItem(this, item);
    }

    private void parseBoardInfo(byte[] response) throws STException {
        if ((response == null) || (response.length != 4)) {
            throw new STException(BAD_PARAMETER);
        }

        mBoardName = response[0];
        mVersionMajor = response[1];
        mVersionMinor = response[2];
        mVersionPatch = response[3];
        mBoardInfoCollected = true;
    }

    private void checkFirmwareVersion() {
        if (mBoardName == BOARD_MB1396) {
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

        } else {
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
        }
    }

    private void displayMb1396FwUpgradeNeededAlert() {
        AlertDialog.Builder builder = new AlertDialog.Builder(ST25DVStopwatchDemoActivity.this);
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
        AlertDialog.Builder builder = new AlertDialog.Builder(ST25DVStopwatchDemoActivity.this);
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

    private void executeAsynchronousAction(Action action) {
        Log.d(TAG, "Starting background action " + action);
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
            switch(actionStatus) {
                case ACTION_SUCCESSFUL:
                    switch (mAction) {
                        case GET_BOARD_INFO:
                            if (mIsMailboxEnabled) {
                                checkFirmwareVersion();
                            } else {
                                UIHelper.displayMessage(ST25DVStopwatchDemoActivity.this, R.string.please_enable_mailbox);
                            }
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
                    showToast(R.string.command_failed);
                    break;

                case NO_RESPONSE:
                    if (mAction == Action.GET_BOARD_INFO) {
                        UIHelper.displayMessage(ST25DVStopwatchDemoActivity.this, R.string.failed_to_retrieve_board_name);
                    } else {
                        showToast(R.string.no_response_received);
                    }
                    break;

                case TAG_NOT_IN_THE_FIELD:
                    if (mAction == Action.GET_BOARD_INFO) {
                        UIHelper.displayMessage(ST25DVStopwatchDemoActivity.this, R.string.please_put_the_phone_on_the_discovery_board);
                    } else {
                        showToast(R.string.tag_not_in_the_field);
                    }
                    break;
            }

            return;
        }
    }


}
