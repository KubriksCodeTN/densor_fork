package com.st.st25nfc.densor;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.widget.SwitchCompat;

import com.st.st25nfc.R;
import com.st.st25nfc.densor.data.DensorDataSet;
import com.st.st25nfc.generic.STFragment;
import com.st.st25nfc.generic.STFragmentActivity;
import com.st.st25sdk.NFCTag;
import com.st.st25sdk.STException;

/**
 * Fragment to update settings from a connected Densor.
 */
public class DensorSettingsFragment extends STFragment implements View.OnClickListener {

    /**
     * Radiogroup to set if the input interval is in seconds or minutes.
     */
    private RadioGroup secOrMin;
    /**
     * Input to set the interval between samples.
     */
    private EditText intervalInput;
    /**
     * Switch to enable the RC clock for the RTC. If not set, the RTC will be set to use the (external) XT clock.
     */
    private SwitchCompat rcSE;
    /**
     * Switch to enable or disable the temperature sensor.
     */
    private SwitchCompat tempSE;
    /**
     * Switch to enable or disable the photo-diode sensor.
     */
    private SwitchCompat pdSE;
    /**
     * Switch to enable or disable the future1 sensor.
     */
    private SwitchCompat future1SE;
    /**
     * Switch to enable or disable the future2.
     */
    private SwitchCompat future2SE;
    /**
     * Switch to enable or disable the accelerometer.
     */
    private SwitchCompat accelSE;
    /**
     * Input to set the startup delay in minutes.
     */
    private EditText startupDelayInput;
    /**
     * Button to set the given settings to the Densor.
     */
    private Button updateButton;
    /**
     * Text view to display status updates like errors or successful uploads.
     */
    private TextView statusView;
    /**
     * Holder for the thread that attempts to update the settings on the Densor.
     */
    private Thread actionThread;
    /**
     * Handler to update variables from the action thread.
     */
    private Handler handler;

    /**
     * Creates a new instance of this fragment.
     *
     * @param context Context to which this fragment should be connected.
     *
     * @return The new Densor settings fragment.
     */
    public static DensorSettingsFragment newInstance(Context context) {
        DensorSettingsFragment f = new DensorSettingsFragment();

        // Set the title of this fragment
        f.setTitle(context.getResources().getString(R.string.densor_settings));

        return f;
    }

    /**
     * Creates a new densor settings fragment.
     */
    public DensorSettingsFragment() {
    }

    /**
     * Called to do initial creation of the densor settings fragment.
     *
     * @param savedInstanceState If the fragment is being re-created from
     * a previous saved state, this is the state.
     */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * Called to have the fragment instantiate its user interface view.
     *
     * @param inflater The LayoutInflater object that can be used to inflate
     * any views in the fragment,
     * @param container If non-null, this is the parent view that the fragment's
     * UI should be attached to.  The fragment should not add the view itself,
     * but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     *
     * @return the user interface view for the Densor settings fragment.
     */
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final View view = inflater.inflate(R.layout.fragment_densor_settings, container, false);
        mView = view;

        secOrMin = view.findViewById(R.id.densor_settings_som);
        intervalInput = view.findViewById(R.id.densor_settings_int_in);

        rcSE = view.findViewById(R.id.densor_settings_se_rc);
        tempSE = view.findViewById(R.id.densor_settings_se_temp);
        pdSE = view.findViewById(R.id.densor_settings_se_pd);
        future1SE = view.findViewById(R.id.densor_settings_se_future1);
        future2SE = view.findViewById(R.id.densor_settings_se_future2);
        accelSE = view.findViewById(R.id.densor_settings_se_accel);

        startupDelayInput = view.findViewById(R.id.densor_settings_startup_delay);

        updateButton = view.findViewById(R.id.densor_settings_update);
        updateButton.setOnClickListener(this);

        statusView = view.findViewById(R.id.densor_settings_status);

        handler = new Handler();

        initView();
        return (View) view;
    }

    /**
     * Called when this fragment is first connected to its context.
     *
     * @param context The context this fragment is connected to.
     */
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    /**
     * Called when the Fragment is no longer resumed.
     */
    @Override
    public void onPause() {
        super.onPause();
    }

    /**
     * Called when the fragment is visible to the user and actively running.
     */
    @Override
    public void onResume() {
        super.onResume();
    }

    /**
     * On-click listener callback for the update button.
     *
     * @param v The view that was clicked.
     */
    @Override
    public void onClick(View v) {
        if (actionThread != null)
            try {
                actionThread.join();
            } catch (InterruptedException e) {
                Log.e("TimeSync", "Issue joining thread");
            }

        actionThread = new Thread(new SettingsAction());
        actionThread.start();

    }

    /**
     * Tasks used to update the settings on the Densor.
     */
    class SettingsAction implements Runnable {
        /**
         * Upload the given settings to the Densor.
         */
        public void run() {
            // Check if the Densor is still connected.
            NFCTag tag = ((STFragmentActivity) requireActivity()).getTag();
            if (tag == null) {
                if (handler != null && statusView != null) {
                    handler.post(new Runnable() {
                        public void run() {
                            statusView.setText(String.format("Error: %s", "unable to write to tag!"));
                        }
                    });
                }
                showToast(R.string.invalid_tag);
                return;
            }

            // Retrieve the input interval, check if it is an integer.
            int intervalInt;
            try {
                intervalInt = Integer.parseInt(intervalInput.getText().toString());
            } catch (Exception e) {
                if (handler != null && statusView != null) {
                    handler.post(new Runnable() {
                        public void run() {
                            statusView.setText(String.format("Incorrect interval input: %s", e.getMessage()));
                        }
                    });
                }
                showToast(R.string.settings_update_failed);
                return;
            }

            // Check if the interval is in the allowed range.
            if (intervalInt > 59 | intervalInt < 0) {
                if (handler != null && statusView != null) {
                    handler.post(new Runnable() {
                        public void run() {
                            statusView.setText(String.format("Incorrect interval input: %s", "out of bounds! ([0,59])"));
                        }
                    });
                }
                showToast(R.string.settings_update_failed);
                return;
            }

            // Convert the interval to BCD. Append if the interval is in seconds or minutes.
            byte somSelector = (secOrMin.getCheckedRadioButtonId() == R.id.densor_settings_som_sec) ? 0x0 : (byte) 0b10000000;
            byte interval = DensorDataSet.bcdEncode(intervalInt);
            interval = (byte) (somSelector | interval);

            // Create the sensor enabled byte.
            byte rcEnabled = (rcSE.isChecked()) ? DensorCommon.rcEnableMask : 0;
            byte tempEnabled = (tempSE.isChecked()) ? DensorCommon.tempEnableMask : 0;
            byte pdEnabled = (pdSE.isChecked()) ? DensorCommon.pdEnableMask : 0;
            byte future1Enabled = (future1SE.isChecked()) ? DensorCommon.future1EnableMask : 0;
            byte future2Enabled = (future2SE.isChecked()) ? DensorCommon.future2EnableMask : 0;
            byte accelEnabled = (accelSE.isChecked()) ? DensorCommon.accelEnableMask : 0;

            byte sensorEnable = (byte) (rcEnabled | tempEnabled | pdEnabled | future1Enabled | future2Enabled | accelEnabled);

            // Check if the input startup delay is an integer.
            int startupDelay;
            try {
                startupDelay = Integer.parseInt(startupDelayInput.getText().toString());
            } catch (Exception e) {
                if (handler != null && statusView != null) {
                    handler.post(new Runnable() {
                        public void run() {
                            statusView.setText(String.format("Incorrect startup delay input: %s", e.getMessage()));
                        }
                    });
                }
                showToast(R.string.settings_update_failed);
                return;
            }

            // Check if the startup delay is in the allowed range.
            if (startupDelay > 59 | startupDelay < 0) {
                if (handler != null && statusView != null) {
                    handler.post(new Runnable() {
                        public void run() {
                            statusView.setText(String.format("Incorrect startup delay input: %s", "out of bounds! ([0, 59])"));
                        }
                    });
                }
                showToast(R.string.settings_update_failed);
                return;
            }

            // Convert the startup delay to BCD, append the startup delay enable bit if a startup delay is set.
            byte sudSelector = (startupDelay == 0) ? 0x0 : (byte) 0b10000000;
            byte sud = DensorDataSet.bcdEncode(startupDelay);
            sud = (byte) (sudSelector | sud);

            // Write the settings one by one to the Densor.
            try {
                tag.writeBytes(DensorCommon.rtcInvertalRegister, new byte[] {interval});
                tag.writeBytes(DensorCommon.sensorStateRegister, new byte[] {sensorEnable});
                tag.writeBytes(DensorCommon.startupDelayRegister, new byte[] {sud});
            } catch (STException e) {
                if (handler != null && statusView != null) {
                    handler.post(new Runnable() {
                        public void run() {
                            statusView.setText(String.format("Error: %s", e.getMessage()));
                        }
                    });
                }

                showToast(R.string.settings_update_failed);
                return;
            }

            // Update the status view to report the successful update.
            if (handler != null && statusView != null) {
                handler.post(new Runnable() {
                    public void run() {
                        statusView.setText("Settings updated!");
                    }
                });
            }
            showToast(R.string.settings_update_success);
        }
    }
}
