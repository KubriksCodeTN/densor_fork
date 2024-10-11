package com.st.st25nfc.densor;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.st.st25nfc.R;
import com.st.st25nfc.generic.STFragment;
import com.st.st25nfc.generic.STFragmentActivity;
import com.st.st25nfc.generic.WriteFragmentActivity;
import com.st.st25sdk.NFCTag;
import com.st.st25sdk.STException;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;

public class TimeSyncFragment extends STFragment implements View.OnClickListener {

    /**
     * Button used to sync the current seconds epoch to a connected Densor.
     */
    private Button syncButton;
    /**
     * Button used to wipe the data segment of a connected Densor.
     */
    private Button wipeButton;
    /**
     * Button used to put the Densor into charge mode.
     */
    private Button chargeButton;
    /**
     * Button used to overwrite the memory pointer on a connected Densor to the start of the data segment.
     */
    private Button deleteButton;
    /**
     * Button used to wipe and time sync a connected Densor in one go.
     */
    private Button completeStartButton;
    /**
     * Text view to display status updates like errors or successful downloads.
     */
    private TextView statusView;
    /**
     * Thread used for all communication between the app and a connected Densor.
     */
    private Thread actionThread;
    /**
     * Handler to access variables from the actionThread.
     */
    private Handler handler;

    /**
     * Creates a new instance of this fragment.
     *
     * @param context Context to which this fragment should be connected.
     *
     * @return The new time sync fragment
     */
    public static TimeSyncFragment newInstance(Context context) {
        TimeSyncFragment f = new TimeSyncFragment();

        // Set the title of this fragment
        f.setTitle(context.getResources().getString(R.string.time_sync));

        return f;
    }

    /**
     * Creates a new time sync fragment.
     */
    public TimeSyncFragment() {
    }

    /**
     * Called to do initial creation of the time sync fragment.
     *
     * @param savedInstanceState If the fragment is being re-created from
     * a previous saved state, this is the state.
     */
    public void onCreate(Bundle savedInstanceState) { super.onCreate(savedInstanceState); }

    /**
     * Called to have the fragment instantiate its user interface view.
     *
     * @param inflater The LayoutInflater object that can be used to inflate
     * any views in the fragment.
     * @param container If non-null, this is the parent view that the fragment's
     * UI should be attached to.  The fragment should not add the view itself,
     * but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     *
     * @return the user interface view for the time sync fragment.
     */
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the time sync layout.
        final View view = inflater.inflate(R.layout.fragment_densor_time_sync, container, false);
        mView = view;

        // Obtain all buttons and the status view.
        syncButton = view.findViewById(R.id.densor_time_sync_button);
        wipeButton = view.findViewById(R.id.densor_wipe);
        chargeButton = view.findViewById(R.id.densor_charge);
        deleteButton = view.findViewById(R.id.densor_delete);
        statusView = view.findViewById(R.id.densor_time_sync_status);
        completeStartButton =  view.findViewById(R.id.densor_complete_start);

        // Set the on-click callback for the sync button
        syncButton.setOnClickListener(this);
        // Set the on-click callback for the wipe button
        wipeButton.setOnClickListener(v -> {
            if (actionThread != null)
                try {
                    actionThread.join();
                } catch (InterruptedException e) {
                    Log.e("TimeSync", "Issue joining thread");
                }

            actionThread = new Thread(new WipeAction());
            actionThread.start();
        });

        // Set the on-click callback for the delete button
        deleteButton.setOnClickListener(v -> {
            if (actionThread != null)
                try {
                    actionThread.join();
                } catch (InterruptedException e) {
                    Log.e("TimeSync", "Issue joining thread");
                }

            actionThread = new Thread(new DeleteAction());
            actionThread.start();
        });

        // Set the on-click callback for the charge button
        chargeButton.setOnClickListener(v -> {
            if (actionThread != null)
                try {
                    actionThread.join();
                } catch (InterruptedException e) {
                    Log.e("TimeSync", "Issue joining thread");
                }

            actionThread = new Thread(new ChargeAction());
            actionThread.start();
        });

        // Set the on-click callback for the complete-start button
        completeStartButton.setOnClickListener(v -> {
            if (actionThread != null)
                try {
                    actionThread.join();
                } catch (InterruptedException e) {
                    Log.e("TimeSync", "Issue joining thread");
                }

            actionThread = new Thread(new CompleteStartAction());
            actionThread.start();
        });

        // Create a new handler.
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
        if (actionThread != null)
            try {
                actionThread.join();
            } catch (InterruptedException e) {
                Log.e("TimeSync", "Issue joining thread");
            }
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
     * On-click listener callback for the time sync button.
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

        actionThread = new Thread(new TimeSyncAction());
        actionThread.start();
    }

    /**
     * Task used to sync the current seconds epoch to the Densor.
     */
    class TimeSyncAction implements Runnable {
        /**
         * Write the current seconds epoch time to a connected Densor.
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

            // Get the current epoch time in milliseconds and convert it to seconds.
            int currentEpoch = (int) (System.currentTimeMillis() / 1000);
            byte[] buffer = ByteBuffer.allocate(4).putInt(currentEpoch).array();

            // Write the seconds epoch to the start time register.
            try {
                tag.writeBytes(DensorCommon.timeRegister, buffer);
            } catch (STException e) {
                if (handler != null && statusView != null) {
                    handler.post(new Runnable() {
                        public void run() {
                            statusView.setText(String.format("Error: %s", e.getMessage()));
                        }
                    });
                }
                showToast(R.string.time_sync_failed);
                return;
            }

            // Update the status view to report the successful update.
            if (handler != null && statusView != null) {
                handler.post(new Runnable() {
                    public void run() {
                        statusView.setText(String.format("Time synced! %s", currentEpoch));
                    }
                });
            }
            showToast(R.string.time_sync_success);

        }
    }

    /**
     * Task that writes the address of the data segment to the memory pointer register.
     */
    class DeleteAction implements Runnable {
        /**
         * Writes the address of the data segment to the memory pointer register.
         */
        public void run() {
            // Check if the Densor is still connected.
            NFCTag tag = ((STFragmentActivity) requireActivity()).getTag();
            if (tag == null) {
                if (handler != null && statusView != null) {
                    handler.post(() -> statusView.setText(String.format("Error: %s", "unable to write to tag!")));
                }
                showToast(R.string.invalid_tag);
                return;
            }

            // Write the start address of the data segment to the memory pointer.
            try {
                byte[] buffer = new byte[2];
                buffer[0] = DensorCommon.dataStartAddr;
                tag.writeBytes(DensorCommon.memoryPointerRegister, buffer);
            } catch (STException e) {
                if (handler != null && statusView != null) {
                    handler.post(() -> statusView.setText(String.format("Error: %s", e.getMessage())));
                }
                showToast(R.string.delete_fail);
                return;
            }

            // Update the status view to report the successful download.
            if (handler != null && statusView != null) {
                handler.post(() -> statusView.setText("Memory deleted!"));
            }
            showToast(R.string.delete_success);

        }
    }

    /**
     * Task to wipe all written memory on a connected Densor.
     */
    class WipeAction implements Runnable {
        /**
         * Wipes all written memory on a connected Densor.
         */
        public void run() {
            // Checks if the Densor is still connected.
            NFCTag tag = ((STFragmentActivity) requireActivity()).getTag();
            if (tag == null) {
                if (handler != null && statusView != null) {
                    handler.post(() -> statusView.setText(String.format("Error: %s", "unable to write to tag!")));
                }
                showToast(R.string.invalid_tag);
                return;
            }

            int prevAddr = 0;
            try {
                // Downloads the memory pointer from the Densor. Loads an entire (aligned) block.
                int memPointFstBlckAddr = (DensorCommon.memoryPointerRegister / DensorCommon.blockSize) * DensorCommon.blockSize;
                int memPointSndBlckAddr = ((DensorCommon.memoryPointerRegister + 1) / DensorCommon.blockSize) * DensorCommon.blockSize;
                int relMemPointAddr = DensorCommon.memoryPointerRegister - memPointFstBlckAddr;

                byte[] registers = tag.readBytes(memPointFstBlckAddr, DensorCommon.blockSize * 2); // Read the blocks containing the memory pointer.

                int memoryPointer = (((int) (registers[relMemPointAddr + 1] & 0xff)) << 8) | (registers[relMemPointAddr] & 0xff);
                Log.i("TimeSync", "Mem pointer at: " + memoryPointer + ". memPointBlockStart at: " + memPointFstBlckAddr);
                int finalBlockAddr = ((memoryPointer / DensorCommon.blockSize) * DensorCommon.blockSize) + DensorCommon.blockSize; // Get the address until which to wipe.

                if (handler != null && statusView != null) {
                    int tempfba = finalBlockAddr;
                    handler.post(() -> statusView.setText(String.format("Mem Pointer %d\r\nBytes wiped: %d/%d", memoryPointer, 0, tempfba)));
                }

                // Wipes end of block containing the memory pointer
                registers[relMemPointAddr] = DensorCommon.dataStartAddr;
                Arrays.fill(registers, relMemPointAddr + 1, registers.length, (byte) 0x0);
                tag.writeBytes(memPointFstBlckAddr, registers);

                // For all block that contain written data, wipe it.
                int currAddr = memPointSndBlckAddr + DensorCommon.blockSize;
                prevAddr = currAddr;
                byte[] buffer = new byte[DensorCommon.blockSize * DensorCommon.blockClusterSize];
                int memSize = tag.getMemSizeInBytes();
                finalBlockAddr = Math.min(finalBlockAddr, memSize);

                while (currAddr <= finalBlockAddr && currAddr <= memSize) {
                    if (currAddr + (DensorCommon.blockSize * DensorCommon.blockClusterSize) > finalBlockAddr) {
                        buffer = new byte[finalBlockAddr - currAddr];
                    }
                    tag.writeBytes(currAddr, buffer);
                    if (handler != null && statusView != null) {
                        int temp = currAddr;
                        int tempfba = finalBlockAddr;
                        handler.post(() -> statusView.setText(String.format("Mem Pointer %d\r\nBytes wiped: %d/%d", memoryPointer, temp, tempfba)));
                    }
                    prevAddr = currAddr + buffer.length;
                    currAddr += (DensorCommon.blockSize * DensorCommon.blockClusterSize);

                    Thread.sleep(10);
                }
            } catch (STException e) {
                if (handler != null && statusView != null) {
                    int temp = prevAddr - (DensorCommon.blockSize * DensorCommon.blockClusterSize);
                    handler.post(() -> statusView.setText(String.format("Error: %s\r\nWiped: %d", e.getMessage(), temp)));
                }
                showToast(R.string.wipe_fail);
                return;
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            // Update the status view to report the successful wipe.
            if (handler != null && statusView != null) {
                int temp = prevAddr;
                handler.post(() -> statusView.setText(String.format("Memory wiped!\r\nWiped: %d", temp)));
            }
            showToast(R.string.wipe_success);

        }
    }

    /**
     * Task to put a connected Densor into charge mode.
     */
    class ChargeAction implements Runnable {
        /**
         * Put a connected Densor into charge mode.
         */
        public void run() {
            // Check if the Densor is still connected.
            NFCTag tag = ((STFragmentActivity) requireActivity()).getTag();
            if (tag == null) {
                if (handler != null && statusView != null) {
                    handler.post(() -> statusView.setText(String.format("Error: %s", "unable to write to tag!")));
                }
                showToast(R.string.invalid_tag);
                return;
            }

            // Write two zero bytes to the two MSBs of the start time register.
            try {
                byte[] buffer = new byte[2];
                tag.writeBytes(DensorCommon.timeRegister, buffer);
            } catch (STException e) {
                if (handler != null && statusView != null) {
                    handler.post(() -> statusView.setText(String.format("Error: %s", e.getMessage())));
                }
                showToast(R.string.charge_start_failed);
                return;
            }

            // Update the status view to report the successful charge update.
            if (handler != null && statusView != null) {
                handler.post(() -> statusView.setText("Started charging!"));
            }
            showToast(R.string.started_charging);

        }
    }

    /**
     * Task that first wipes a connected Densor and starts it when finished wiping.
     */
    class CompleteStartAction implements Runnable {
        /**
         * Consecutively wipes and starts a connected Densor.
         */
        public void run() {
            WipeAction wipe = new WipeAction();
            wipe.run();
            TimeSyncAction tsa = new TimeSyncAction();
            tsa.run();
        }
    }
}