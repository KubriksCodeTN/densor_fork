package com.st.st25nfc.densor;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.CatmullRomInterpolator;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;
import com.st.st25nfc.densor.data.DensorDataSet;
import com.st.st25nfc.generic.STFragment;
import com.st.st25nfc.generic.STFragmentActivity;
import com.st.st25sdk.NFCTag;
import com.st.st25nfc.R;
import com.st.st25sdk.STException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Fragment to load and show data from a Densor.
 */
public class DataViewFragment extends STFragment implements View.OnClickListener{

    /**
     * Text view to display status updates like errors or successful downloads.
     */
    private TextView statusView;
    /**
     * Handler to update update fragment from child threads.
     */
    private Handler handler;
    /**
     * Thread used to download data from the Densor.
     */
    private Thread actionThread;
    /**
     * Holder to keep a loaded dataset.
     */
    private DensorDataSet data;
    /**
     * Plot to show a temperature trace from a loaded dataset.
     */
    private XYPlot tempPlt;
    /**
     * Plot to show a photo-diode trace from a loaded dataset.
     */
    private XYPlot pdPlt;
    /**
     * Plot to show a future1 traces from a loaded dataset.
     */
    private XYPlot future1Plt;
    /**
     * Plot to show a future2 trace from a loaded dataset.
     */
    private XYPlot future2Plt;
    /**
     * Plot to show a accelerometer traces from a loaded dataset.
     */
    private XYPlot accelPlt;
    /**
     * Plot to show a supply voltage trace from a loaded dataset.
     */
    private XYPlot vddaPlt;
    /**
     * Button to download a dataset from a Densor.
     */
    private Button downloadButton;

    /**
     * Creates a new instance of this fragment.
     *
     * @param context Context to which this fragment should be connected.
     *
     * @return The new data view fragment
     */
    public static DataViewFragment newInstance(Context context) {
        DataViewFragment f = new DataViewFragment();

        // Set the title of this fragment
        f.setTitle(context.getResources().getString(R.string.data_view));

        return f;
    }

    /**
     * Creates a new data view fragment.
     */
    public DataViewFragment() {
    }

    /**
     * Called to do initial creation of the data view fragment.
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
     * @return the user interface view for the data view fragment.
     */
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final View view = inflater.inflate(R.layout.fragment_densor_data_view, container, false);
        mView = view;

        statusView = (TextView) view.findViewById(R.id.densor_data_view_status);

        handler = new Handler();

        tempPlt = (XYPlot) mView.findViewById(R.id.densor_data_view_temp_plt);
        pdPlt = (XYPlot) mView.findViewById(R.id.densor_data_view_pd_plt);
        future1Plt = (XYPlot) mView.findViewById(R.id.densor_data_view_future1_plt);
        future2Plt = (XYPlot) mView.findViewById(R.id.densor_data_view_future2_plt);
        accelPlt = (XYPlot) mView.findViewById(R.id.densor_data_view_accel_plt);
        vddaPlt = (XYPlot) mView.findViewById(R.id.densor_data_view_vdda_plt);

        downloadButton = (Button) mView.findViewById(R.id.densor_data_view_download);
        downloadButton.setOnClickListener(this);

        initView();
        return (View) view;
    }

    /**
     * On-click listener callback for the download button.
     *
     * @param v The view that was clicked.
     */
    @Override
    public void onClick(View v) {
        if (actionThread != null)
            try {
                actionThread.join();
            } catch (InterruptedException e) {
                Log.e("Dataview", "Issue joining thread");
            }

        actionThread = new Thread(new DataViewAction());
        actionThread.start();
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

    // TODO: Remove
//    /**
//     * Task used to fill the graphs in this fragment on a separate thread.
//     */
//    private class FillViewTask extends STFragment.FillViewTask {
//
//        /**
//         * Creates a new fill view task. Will fill the graphs of this fragment.
//         */
//        public FillViewTask() {
//        }
//
//        @Override
//        protected Integer doInBackground(NFCTag... param) {
//            return 0;
//        }
//
//
//        @Override
//        protected void onPostExecute(Integer result) {
//
//            if (actionThread != null) {
//                try {
//                    actionThread.join();
//                } catch (InterruptedException e) {
//                    Log.e("DataViewFragment", "Issue joining thread");
//                }
//            }
//
//        }
//    }

    /**
     * Tasks used to download data from a connected Densor, deserialize it and plot it on the view.
     */
    class DataViewAction implements Runnable {
        /**
         * Download data from a connected Densor, deserialize it and plot it on the view.
         */
        public void run() {
            // Check if a tag is connected.
            NFCTag tag = ((STFragmentActivity) requireActivity()).getTag();
            if (tag == null) {
                if (handler != null && statusView != null) {
                    handler.post(new Runnable() {
                        public void run() {
                            statusView.setText(String.format("Error: %s", "unable to read tag!"));
                        }
                    });
                }
                showToast(R.string.invalid_tag);
                return;
            }

            // Download all registers.
            byte[] registers;

            try {
                registers = tag.readBytes(0x0, DensorCommon.dataStartAddr);
            } catch (STException e) {
                if (handler != null && statusView != null) {
                    handler.post(new Runnable() {
                        public void run() {
                            statusView.setText(String.format("Error: %s", e.getMessage()));
                        }
                    });
                }

                showToast(R.string.reading_tag_failed);
                return;
            }

            // Split and convert the downloaded registers, obtain the memory pointer.
            byte sensorState = registers[DensorCommon.sensorStateRegister];
            byte[] timeRegisterBuff = {
                    registers[DensorCommon.timeRegister],
                    registers[DensorCommon.timeRegister + 1],
                    registers[DensorCommon.timeRegister + 2],
                    registers[DensorCommon.timeRegister + 3]
            };
            int timestamp = ByteBuffer.wrap(timeRegisterBuff).getInt();
            byte rtcInterval = registers[DensorCommon.rtcInvertalRegister];
            byte startupDelay = registers[DensorCommon.startupDelayRegister];
            int memoryPointer = (((int) (registers[DensorCommon.memoryPointerRegister + 1] & 0xff)) << 8) | (registers[DensorCommon.memoryPointerRegister] & 0xff);

            // Download all data till the memory pointer
            byte[] dataBuff;

            try {
                dataBuff = tag.readBytes(DensorCommon.dataStartAddr, memoryPointer - DensorCommon.dataStartAddr); // Load all registers
            } catch (STException e) {
                if (handler != null && statusView != null) {
                    handler.post(new Runnable() {
                        public void run() {
                            statusView.setText(String.format("Error: %s", e.getMessage()));
                        }
                    });
                }

                showToast(R.string.reading_tag_failed);
                return;
            }

            // Deserialize the loaded data.
            DensorDataSet ds = new DensorDataSet(dataBuff, (int) rtcInterval, (int) startupDelay, sensorState, timestamp);

            // Update the status view to report the successful download.
            if (handler != null) {
                handler.post(new Runnable() {
                    public void run() {
                        statusView.setText(String.format("Loaded data, starting at: %s", timestamp));
                        data = ds;
                    }
                });
            }

            // Log all loaded sensor data.
            Log.i("Dataview", "Dataset:");
            Log.i("Dataview", "temp:" + Arrays.toString(ds.getTemp()));
            Log.i("Dataview", "pd:" + Arrays.toString(ds.getPd()));
            Log.i("Dataview", "accel:[[" + Arrays.toString(ds.getAccel()[0]) + "],[" + Arrays.toString(ds.getAccel()[1]) + "],[" + Arrays.toString(ds.getAccel()[1]) + "]]");
            Log.i("Dataview", "vdda:" + Arrays.toString(ds.getVdda()));

            showToast(R.string.read_success);

            // Update the graphs in the view.
            if (mView != null) {
                // Clear all graphs.
                tempPlt.clear();
                pdPlt.clear();
                future1Plt.clear();
                future2Plt.clear();
                accelPlt.clear();
                vddaPlt.clear();

                // Get the time range.
                Number leftBound = (Number) (ds.getTimestamps().get(0) - 60);
                Number rightBound = (Number) (ds.getTimestamps().get(ds.getTimestamps().size() - 1) + 60);

                LineAndPointFormatter series1Format =
                        new LineAndPointFormatter(getContext(), R.xml.line_point_formatter_with_labels);
                series1Format.setInterpolationParams(
                        new CatmullRomInterpolator.Params(10, CatmullRomInterpolator.Type.Centripetal));

                // Plot the temperature graph.
                XYSeries tempSeries = new SimpleXYSeries(
                        ds.getTimestamps(), Arrays.asList(ds.getTemp()), "Temperature");
                tempPlt.addSeries(tempSeries, series1Format);
                tempPlt.setDomainBoundaries(leftBound, rightBound, BoundaryMode.FIXED);

                // Plot the photo-diode graph.
                XYSeries pdSeries = new SimpleXYSeries(
                        ds.getTimestamps(), Arrays.asList(ds.getPd()), "Photodiode");
                pdPlt.addSeries(pdSeries, series1Format);
                pdPlt.setDomainBoundaries(leftBound, rightBound, BoundaryMode.FIXED);


                // Plot the future1 graph.
                XYSeries future11Series = new SimpleXYSeries(
                        ds.getTimestamps(), Arrays.asList(ds.getFuture1()[0]), "future1 1");
                XYSeries future12Series = new SimpleXYSeries(
                        ds.getTimestamps(), Arrays.asList(ds.getFuture1()[1]), "future1 2");
                XYSeries future13Series = new SimpleXYSeries(
                        ds.getTimestamps(), Arrays.asList(ds.getFuture1()[2]), "future1 3");
                XYSeries future14Series = new SimpleXYSeries(
                        ds.getTimestamps(), Arrays.asList(ds.getFuture1()[3]), "future1 4");
                XYSeries future15Series = new SimpleXYSeries(
                        ds.getTimestamps(), Arrays.asList(ds.getFuture1()[4]), "future1 5");
                future1Plt.addSeries(future11Series, series1Format);
                future1Plt.addSeries(future12Series, series1Format);
                future1Plt.addSeries(future13Series, series1Format);
                future1Plt.addSeries(future14Series, series1Format);
                future1Plt.addSeries(future15Series, series1Format);
                future1Plt.setDomainBoundaries(leftBound, rightBound, BoundaryMode.FIXED);

                // Plot the future2 graph.
                XYSeries future2Series = new SimpleXYSeries(
                        ds.getTimestamps(), Arrays.asList(ds.getFuture2()), "future 2");
                future2Plt.addSeries(future2Series, series1Format);
                future2Plt.setDomainBoundaries(leftBound, rightBound, BoundaryMode.FIXED);

                XYSeries accelXSeries = new SimpleXYSeries(
                        ds.getTimestamps(), Arrays.asList(ds.getAccel()[0]), "Accel x");
                XYSeries accelYSeries = new SimpleXYSeries(
                        ds.getTimestamps(), Arrays.asList(ds.getAccel()[1]), "Accel y");
                XYSeries accelZSeries = new SimpleXYSeries(
                        ds.getTimestamps(), Arrays.asList(ds.getAccel()[2]), "Accel z");

                accelPlt.addSeries(accelXSeries, series1Format);
                accelPlt.addSeries(accelYSeries, series1Format);
                accelPlt.addSeries(accelZSeries, series1Format);
                tempPlt.setDomainBoundaries(leftBound, rightBound, BoundaryMode.FIXED);

                // Plot the supply voltage graph.
                XYSeries vddaSeries = new SimpleXYSeries(
                        ds.getTimestamps(), Arrays.asList(ds.getVdda()), "VDDA");
                vddaPlt.addSeries(vddaSeries, series1Format);
                vddaPlt.setRangeBoundaries(1.7, 3.4, BoundaryMode.FIXED);
                vddaPlt.setDomainBoundaries(leftBound, rightBound, BoundaryMode.FIXED);

                // Draw all graphs.
                tempPlt.redraw();
                pdPlt.redraw();
                future1Plt.redraw();
                future2Plt.redraw();
                accelPlt.redraw();
                vddaPlt.redraw();

            }

        }
    }

    // TODO: Remove!
    @Override
    public void fillView() {
//        new FillViewTask().execute(myTag);
    }
}
