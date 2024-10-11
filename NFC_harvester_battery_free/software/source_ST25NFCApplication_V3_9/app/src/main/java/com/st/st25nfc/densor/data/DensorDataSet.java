package com.st.st25nfc.densor.data;

import android.util.Log;
import com.st.st25nfc.densor.DensorCommon;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import org.apache.commons.lang3.ArrayUtils;

/**
 * The object to hold a data dump of Densor readings.
 */
public class DensorDataSet {
    /**
     * The size in bytes of all readings per channel.
     */
    public static int[] dataSizes = {2, 2, 2, 2, 2, 1}; //temp, pd, future1, future2, accel, vdda
    /**
     * List containing all seperate samples.
     */
    private ArrayList<DensorDataSample> samples;
    /**
     * Interval in seconds between the samples.
     */
    private int interval;
    /**
     * The startup delay used before the first sample in seconds.
     */
    private int startupDelay;
    /**
     * The time at which the Densor was started.
     */
    private int startTime;
    /**
     * List containing the timestamp per sample.
     */
    private ArrayList<Integer> timestamps;

    /**
     * Decodes a given number int BCD to decimal.
     *
     * @param bcd The int in BCD.
     * @return The int in decimal.
     */
    public static int bdcDecode(int bcd) {
        return (((bcd & 0b01110000) >> 4) * 10) + (bcd & 0b00001111);
    }

    /**
     * Encodes a given int from decimal to BCD.
     *
     * @param val The int in decimal.
     * @return The int in BCD.
     */
    public static byte bcdEncode(int val) {
        byte ls = (byte) (val % 10);
        byte ms = (byte) (((val - (val % 10)) / 10) << 4);
        return (byte) (ms | ls);
    }

    /**
     * Creates the list of timestamps per sample, given the start time, the sample interval and amount of samples.
     *
     * @param firstReadTime The time at which the Densor was started in seconds epoch.
     * @param interval The interval in seconds between samples.
     * @param samples The amount of samples.
     * @return A list of timestamps per sample in seconds epoch.
     */
    private static ArrayList<Integer> createTimestamps(int firstReadTime, int interval, int samples) {
        ArrayList<Integer> out = new ArrayList<>();
        for (int i = 0; i < samples; i++) {
            out.add(firstReadTime + (interval * i));
        }

        return out;
    }

    /**
     * Creates a dataset given data loaded from the Densor. Will deserialize the data array into separate readings.
     *
     * @param data - An array of bytes, loaded from the Densor, containing all readings.
     * @param interval - The interval between samples in BCD.
     * @param startupDelay - The startup delay in seconds between the start of the Densor and the first reading.
     * @param sensorState - Byte describing which sensors where enabled and which disabled.
     * @param startTime - The time at which the Densor was started in seconds epoch.
     */
    public DensorDataSet(byte[] data, int interval, int startupDelay, byte sensorState, int startTime) {

        this.interval = bdcDecode(interval);
        this.interval *= ((interval * 0b10000000) > 0) ? 60 : 1;
        this.startupDelay = bdcDecode(startupDelay);
        this.startTime = startTime;
        this.samples = dataDecorder(data, sensorState);
        int firstReadTime = startTime + startupDelay;
        this.timestamps = createTimestamps(firstReadTime, this.interval, this.samples.size());
    }

    /**
     * Deserializes a given byte array into separate samples with converted readings.
     *
     * @param data The array to deserialize.
     * @param sensorState Byte describing which sensors are enabled and which disabled.
     * @return A list of samples.
     */
    private static ArrayList<DensorDataSample> dataDecorder(byte[] data, byte sensorState) {
        // Determine which sensors are turned on.
        int pointer = 0;
        boolean tempEnabled = (DensorCommon.tempEnableMask & sensorState) != 0;
        boolean pdEnabled = (DensorCommon.pdEnableMask & sensorState) != 0;
        boolean future1Enabled = (DensorCommon.future1EnableMask & sensorState) != 0;
        boolean future2Enabled = (DensorCommon.future2EnableMask & sensorState) != 0;
        boolean accelEnabled = (DensorCommon.accelEnableMask & sensorState) != 0;

        // Calculate the size of a sample based on which sensors are enabled.
        int dataSampleSize = 0;
        dataSampleSize += tempEnabled ? dataSizes[0] : 0;
        dataSampleSize += pdEnabled ? dataSizes[1] : 0;
        dataSampleSize += future1Enabled ? 5 * dataSizes[2] : 0;
        dataSampleSize += future2Enabled ? dataSizes[3] : 0;
        dataSampleSize += accelEnabled ? 3 * dataSizes[4] : 0;
        // In case pd is enabled but temp not, vdda is added separately. Otherwise vdda is embedded in temp
        dataSampleSize += (pdEnabled && !tempEnabled) ? dataSizes[5] : 0;

        ArrayList<DensorDataSample> out = new ArrayList<>();

        // For every full sample, decode the sample from the raw bytes.
        while (pointer + dataSampleSize <= data.length) {
            Float temp = 0.f;
            Integer pd = 0;
            Integer[] future1 = {0, 0, 0, 0, 0};
            Integer future2 = 0;
            Float[] accel = {0.f, 0.f, 0.f};
            Float vdda = 0.f;

            // Convert the temperature reading, if enabled.
            if (tempEnabled) {
                byte[] buff = Arrays.copyOfRange(data, pointer, pointer + dataSizes[0]);
                ArrayUtils.reverse(buff);
                Integer inttemp = (int) ByteBuffer.wrap(buff).getShort();
                vdda = ((float) ((inttemp & 0x0F) + 18)) / 10;
                inttemp = (inttemp >> 4);
                temp = ((inttemp.floatValue() / 16.f) + 25.f);
                pointer += dataSizes[0];

            // If temp is not enabled, but pd is enabled, retrieve VDDA. If temp is enabled, pd can use vdda whether it is enabled or not.
            } else if (pdEnabled) {
                vdda = ((float) (data[pointer] + 18)) / 10;
                pointer += 1;
            }
            // Convert the photo-diode reading, if enabled.
            if (pdEnabled) {
                byte[] buff = Arrays.copyOfRange(data, pointer, pointer + dataSizes[1]);
                ArrayUtils.reverse(buff);
                pd = Short.toUnsignedInt(ByteBuffer.wrap(buff).getShort());
                pointer += dataSizes[1];
            }
            // Convert the future1 readings, if enabled.
            if (future1Enabled) {
                future1 = new Integer[5];

                for (int i = 0; i < 5; i++) {
                    byte[] buff = Arrays.copyOfRange(data, pointer, pointer + dataSizes[2]);
                    ArrayUtils.reverse(buff);
                    int t = Short.toUnsignedInt(ByteBuffer.wrap(buff).getShort());
                    pointer += dataSizes[2];
                    future1[i] = t;
                }
            }
            // Convert the future2 reading, if enabled.
            if (future2Enabled) {
                byte buff[] = Arrays.copyOfRange(data, pointer, pointer + dataSizes[3]);
                future2 = Short.toUnsignedInt(ByteBuffer.wrap(buff).getShort());
                pointer += dataSizes[3];
            }
            // Convert the accelerometer readings, if enabled.
            if (accelEnabled) {
                accel = new Float[3];

                for (int j = 0; j < 3; j++) {
                    byte buff[] = Arrays.copyOfRange(data, pointer, pointer + dataSizes[4]);
                    ArrayUtils.reverse(buff);
                    int a = (int) ByteBuffer.wrap(buff).getShort();
                    pointer += dataSizes[4];
                    accel[j] = (float) a / 16384;
                }
            }

            // Add the sample to the list of samples.
            out.add(new DensorDataSample(temp, pd, future1, future2, accel, vdda));
        }

        return out;
    }

    /**
     * Returns the list of temperature readings of this dataset.
     *
     * @return The list of temperatures.
     */
    public Float[] getTemp() {
        ArrayList<Float> out = new ArrayList<>();

        for (DensorDataSample sample : samples) {
            out.add(sample.getTemp());
        }
        return out.toArray(new Float[samples.size()]);
    }

    /**
     * Returns the list of photo-diode readings of this dataset.
     *
     * @return The list of photo-diode readings.
     */
    public Integer[] getPd() {
        ArrayList<Integer> out = new ArrayList<>();

        for (DensorDataSample sample : samples) {
            out.add(sample.getPd());
        }
        return out.toArray(new Integer[samples.size()]);
    }

    /**
     * Returns the list of future1 readings of this dataset.
     *
     * @return The list of future1 readings.
     */
    public Integer[][] getFuture1() {
        Integer[][] out = new Integer[5][samples.size()];
        int i = 0;
        for (DensorDataSample sample : samples) {
            Integer[] t = sample.getFuture1();
            Log.w("DDS", "data:" + t[0] + ", " + t[1] + ", " + t[2] + ", " + t[3] + ", " + t[4]);
            out[0][i] = t[0];
            out[1][i] = t[1];
            out[2][i] = t[2];
            out[3][i] = t[3];
            out[4][i] = t[4];
            i++;
        }
        return out;
    }

    /**
     * Returns the list of future2 readings of this dataset.
     *
     * @return The list of future2 readings.
     */
    public Integer[] getFuture2() {
        Integer[] out = {};
        for (DensorDataSample sample : samples) {
            out = ArrayUtils.addAll(out, sample.getFuture2());
        }
        return out;
    }

    /**
     * Returns the list of accelerometer readings of this dataset.
     *
     * @return The list of accelerometer readings.
     */
    public Float[][] getAccel() {
        Float[][] out = new Float[3][samples.size()];
        int i = 0;
        for (DensorDataSample sample : samples) {
            Float[] in = sample.getAccel();
            out[0][i] = in[0];
            out[1][i] = in[1];
            out[2][i] = in[2];
            i++;
        }
        return out;
    }

    /**
     * Returns the list of supply voltage readings of this dataset.
     *
     * @return The list of supply voltage readings.
     */
    public Float[] getVdda() {
        Float[] out = {};
        for (DensorDataSample sample : samples) {
            out = ArrayUtils.addAll(out, sample.getVdda());
        }
        return out;
    }

    /**
     * Returns the list of samples of this dataset.
     *
     * @return The list of samples.
     */
    public ArrayList<DensorDataSample> getSamples() {
        return samples;
    }

    /**
     * Returns the interval between samples in seconds used for this dataset.
     *
     * @return The interval between samples in seconds.
     */
    public int getInterval() {
        return interval;
    }

    /**
     * The startup delay in seconds between the start of the Densor and the first reading used for this dataset.
     *
     * @return The startup delay in seconds.
     */
    public int getStartupDelay() {
        return startupDelay;
    }

    /**
     * The start time in seconds epoch of the Densor for this dataset.
     *
     * @return The start time in seconds epoch.
     */
    public int getStartTime() {
        return startTime;
    }

    /**
     * Returns the list of timestamps for the dataset.
     *
     * @return The list of timestamps.
     */
    public ArrayList<Integer> getTimestamps() {
        return timestamps;
    }
}
