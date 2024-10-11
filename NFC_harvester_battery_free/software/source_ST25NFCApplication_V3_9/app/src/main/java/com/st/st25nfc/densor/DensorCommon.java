package com.st.st25nfc.densor;

/**
 * All common variables for the Denspr
 */
public class DensorCommon {

    // Register addresses
    /**
     * Address of the sensor state register.
     */
    public static byte sensorStateRegister = 0x0;
    /**
     * Address of the start time register.
     */
    public static byte timeRegister = 0x01;
    /**
     * Address of the sample interval register.
     */
    public static byte rtcInvertalRegister = 0x5;
    /**
     * Address of the startup delay register.
     */
    public static byte startupDelayRegister = 0x06;
    /**
     * Address of the memory pointer register.
     */
    public static byte memoryPointerRegister = 0x07;
    /**
     * Address of the data segment.
     */
    public static byte dataStartAddr = 0x09;

    /**
     * Mask for the RC enable bit. If set to 1, RTC used the RC crystal, if set to 0, RTC used the (external) XT crystal
     */
    public static byte rcEnableMask = (byte) 0b10000000;
    /**
     * Mask for the temperature enable bit.
     */
    public static byte tempEnableMask = 0b00100000;
    /**
     * Mask for the photo-diode enable bit.
     */
    public static byte pdEnableMask = 0b00001000;
    /**
     * Mask for the future1 enable bit.
     */
    public static byte future1EnableMask = 0b00000100;
    /**
     * Mask for the barometer enable bit.
     */
    public static byte future2EnableMask = 0b00000010;
    /**
     * Mask for the accelerometer enable bit.
     */
    public static byte accelEnableMask = 0b00000001;
    /**
     * Size of a block in bytes while reading in blocks.
     */
    public static byte blockSize = 4;
    /**
     * Amount of blocks that may be written or read at the same time while using block read or write.
     */
    public static byte blockClusterSize = 50;
}
