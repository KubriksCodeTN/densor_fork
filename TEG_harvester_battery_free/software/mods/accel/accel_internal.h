/*
 * Copyright 2018-2019 NXP
 * This software is owned or controlled by NXP and may only be used strictly
 * in accordance with the applicable license terms.  By expressly accepting
 * such terms or by downloading, installing, activating and/or otherwise using
 * the software, you are agreeing that you have read, and that you agree to
 * comply with and are bound by, such license terms.  If you do not agree to
 * be bound by the applicable license terms, then you may not retain, install,
 * activate or otherwise use the software.
 */

#ifndef __ACCEL_INTERNAL_H_
#define __ACCEL_INTERNAL_H_

#include <stdint.h>
#include <stdbool.h>
#include "accel.h"

#define ACCEL_BUFFER_SIZE 32

/**
 * Full Scale Measurement Rate.
 * Maximum acceleration the accelerometer can measure when monitoring for shocks.
 * Unit: ACCEL_FSMR_T
 */
#define ACCEL_SHOCK_FSMR ACCEL_FSMR_16G

/**
 * Output Data Rate.
 * Sampling rate of the accelerometer when monitoring for shocks.
 * Unit: ACCEL_ODR_T
 */
#define ACCEL_SHOCK_ODR ACCEL_ODR_100HZ

/**
 * Full Scale Measurement Rate.
 * Maximum acceleration the accelerometer can measure when monitoring for shakes.
 * Unit: ACCEL_FSMR_T
 */
#define ACCEL_SHAKE_FSMR ACCEL_FSMR_8G

/**
 * Output Data Rate.
 * Sampling rate of the accelerometer when monitoring for shakes.
 * Unit: ACCEL_ODR_T
 */
#define ACCEL_SHAKE_ODR ACCEL_ODR_25HZ

/**
 * Full Scale Measurement Rate.
 * Maximum acceleration the accelerometer can measure when monitoring for vibrations.
 * Unit: ACCEL_FSMR_T
 */
#define ACCEL_VIBRATION_FSMR ACCEL_FSMR_4G

/**
 * Output Data Rate.
 * Sampling rate of the accelerometer when monitoring for vibrations.
 * Unit: ACCEL_ODR_T
 */
#define ACCEL_VIBRATION_ODR ACCEL_ODR_50HZ

/**
 * Full Scale Measurement Rate.
 * Maximum acceleration the accelerometer can measure when monitoring for tilts.
 * Unit: ACCEL_FSMR_T
 */
#define ACCEL_TILT_FSMR ACCEL_FSMR_2G

/**
 * Output Data Rate.
 * Sampling rate of the accelerometer when monitoring for tilts.
 * Unit: ACCEL_ODR_T
 */
#define ACCEL_TILT_ODR ACCEL_ODR_12HZ

/* ------------------------------------------------------------------------- */

/**
 * FSMR rates.
 */
typedef enum ACCEL_FSMR
{
    ACCEL_FSMR_2G,
    ACCEL_FSMR_4G,
    ACCEL_FSMR_8G,
    ACCEL_FSMR_16G,
} ACCEL_FSMR_T;

/**
 * ODR rates.
 * @note not all possible rates are listed here.
 */
typedef enum ACCEL_ODR
{
    ACCEL_ODR_1HZ, /**< 1563 Hz */
    ACCEL_ODR_3HZ, /**< 3125 Hz */
    ACCEL_ODR_6HZ, /**< 6250 Hz */
    ACCEL_ODR_12HZ, /**< 12500 Hz */
    ACCEL_ODR_25HZ, /**< 25000 Hz */
    ACCEL_ODR_50HZ, /**< 50000 Hz */
    ACCEL_ODR_100HZ, /**< 100000 Hz */
    ACCEL_ODR_200HZ, /**< 200000 Hz */
    ACCEL_ODR_400HZ, /**< 400000 Hz */

    /**
     * Number of possible Output Data Rates. Not to be used as a valid ODR value.
     * Use this in for loops or to define array sizes.
     */
    ACCEL_ODR_COUNT
} ACCEL_ODR_T;

/** Indicates which axis caused the trigger when a motion interrupt occurred. */
typedef struct ACCEL_AXES_FLAGS {
    int x; /**< @c -1: below lower threshold; @c 0: within range; @c +1: above upper threshold. */
    int y; /**< @c -1: below lower threshold; @c 0: within range; @c +1: above upper threshold. */
    int z; /**< @c -1: below lower threshold; @c 0: within range; @c +1: above upper threshold. */
} ACCEL_AXES_FLAGS_T;

/**
 * Indicates the orientation possibilities of the acceleration sensor.
 */
typedef enum ACCEL_TILT_STATUS {
    ACCEL_TILT_STATUS_UNKNOWN = 0,

    /**
     * Looking at the front side of the accelerometer sensor, pin1 is the highest pin on the left.
     *          +-----+
     * pin1 --> |o    |
     *          |     |
     *          +-----+
     */
    ACCEL_TILT_STATUS_PORTRAIT_UP = 0x01,

    /**
     * Looking at the front side of the accelerometer sensor, pin1 is the lowest pin on the right.
     *          +-----+
     *          |     |
     *          |    o| <-- pin1
     *          +-----+
     */
    ACCEL_TILT_STATUS_PORTRAIT_DOWN = 0x02,

    /**
     * Looking at the front side of the accelerometer sensor, pin1 is the highest pin on the right.
     *          +-----+
     *          |    o| <-- pin1
     *          |     |
     *          +-----+
     */
    ACCEL_TILT_STATUS_LANDSCAPE_RIGHT = 0x04,

    /**
     * Looking at the front side of the accelerometer sensor, pin1 is the lowest pin on the left.
     *          +-----+
     *          |     |
     * pin1 --> |o    |
     *          +-----+
     */
    ACCEL_TILT_STATUS_LANDSCAPE_LEFT = 0x08,

    /**
     * Looking at the accelerometer sensor, the front side is visible from above.
     */
    ACCEL_TILT_STATUS_FRONT = 0x10,

    /**
     * Looking at the accelerometer sensor, the back side is visible from above.
     */
    ACCEL_TILT_STATUS_BACK = 0x20,

    /** Number of statuses. Not to be used as a possible mode. Use this in for loops or to define array sizes. */
    ACCEL_TILT_STATUS_COUNT = 6
} ACCEL_TILT_STATUS_T;

/**
 * Provides information about the current state of the accelerometer. To be refreshed when an interrupt is raised by
 * the accelerometer.
 * The information is assembled from multiple registers and translated into a non-accelerometer-specific format
 */
typedef struct ACCEL_INTERRUPT_STATUS_S {
#if ACCEL_SHOCK_ENABLE || ACCEL_SHAKE_ENABLE || ACCEL_VIBRATION_ENABLE
    /**
     * @c true when an acceleration higher than the upper level, or lower than the upper level has been detected.
     */
    bool thresholdsExceeded;

    /**
     * If @c thresholdsExceeded is @c true, this structure lists which axes were experiencing the measured acceleration.
     */
    ACCEL_AXES_FLAGS_T flags;
#endif

#if ACCEL_TILT_ENABLE
    /**
     * @c true when an orientation change has been detected, given the programmed constraints.
     */
    bool orientationChanged;

    /**
     * If @c orientationChanged is @c true, this field lists the new orientation as a bitmask of OR'd enumerations
     * of @c ACCEL_TILT_STATUS_T.
     */
    int tiltStatus;
#endif

    /**
     * @c true when the accelerometer has sampled acceleration values for the configured time duration.
     * @see Accel_TimerSet
     */
    bool timerExpired;
} ACCEL_INTERRUPT_STATUS_T;

/* ------------------------------------------------------------------------- */

/**
 * To be called whenever the watermark level has been reached. Used for timing purposes.
 * @return @c true when the timeout value as set in #Accel_TimerSet has elapsed, @c false otherwise.
 */
bool Accel_TimerCb(void);
void Accel_TimerSet(ACCEL_ODR_T odr, unsigned int timeout);
/**
 * Converts a time in a number of acceleration date samples.
 * @param odr Output Data rate of the accelerometer. The frequency at which X-, Y-, Z-values are sampled.
 * @param duration The time to convert. In milliseconds.
 * @return A number of samples. It takes the accelerometer @em approximately @c duration milliseconds to sample this
 *  amount of data. The inaccuracy stems from multiple factors:
 *  - calculations are rounded down.
 *  - if @c duration is bigger than 0, returnvalue is at least 1
 *  - the returnvalue is clipped at 0xFF
 */
int Accel_TimerCalculateDebounce(ACCEL_ODR_T odr, int duration);

void Accel_I2cInit(void);
void Accel_I2cDeInit(void);
uint8_t Accel_I2cReadReg(uint8_t reg);
bool Accel_I2cWriteReg(uint8_t reg, uint8_t val);

/* ------------------------------------------------------------------------- */

#if ACCEL_SHOCK_ENABLE
bool Accel_Shock_Start(void);
void Accel_Shock_Stop(void);
void Accel_Shock_SetParams(ACCEL_SHOCK_PARAMS_T * pVibration);
void Accel_Shock_Detect(const ACCEL_INTERRUPT_STATUS_T status);
#endif

#if ACCEL_SHAKE_ENABLE
bool Accel_Shake_Start(void);
void Accel_Shake_Stop(void);
void Accel_Shake_SetParams(ACCEL_SHAKE_PARAMS_T * pVibration);
void Accel_Shake_Detect(const ACCEL_INTERRUPT_STATUS_T status);
#endif

#if ACCEL_VIBRATION_ENABLE
bool Accel_Vibration_Start(void);
void Accel_Vibration_Stop(void);
void Accel_Vibration_SetParams(ACCEL_VIBRATION_PARAMS_T * pVibration);
void Accel_Vibration_Detect(const ACCEL_INTERRUPT_STATUS_T status);
#endif

#if ACCEL_TILT_ENABLE
bool Accel_Tilt_Start(bool shock, bool shake, bool vibration);
void Accel_Tilt_Stop(void);
void Accel_Tilt_SetParams(ACCEL_TILT_PARAMS_T * pTilt);
void Accel_Tilt_Detect(const ACCEL_INTERRUPT_STATUS_T status);
#endif

/* ------------------------------------------------------------------------- */

/**
 * - Disables the accelerometer
 * - Writes the given threshold values
 * - Enables the accelerometer
 * @pre The accelerometer has been programmed before by calling #Accel_ProgramShock, #Accel_ProgramShake or
 *  #Accel_ProgramVibration. Specifically, the FSMR setting is read out and used to translate the @c level
 *  parameter to the correct register values.
 * @param level : the threshold value, in milli-g. Configures detection of accelerations of
 *  less than @c -level and higher than @c +level.
 */
void Accel_SetThresholds(int level);

/**
 * - Disables the accelerometer
 * - Writes the given watermark value
 * - Enables the accelerometer
 * @param watermark : the number of samples to buffer before an interrupt is raised. If a value greater than
 *  #ACCEL_BUFFER_SIZE - 1 is given, the value is clipped.
 */
void Accel_SetWatermark(int watermark);

ACCEL_INTERRUPT_STATUS_T Accel_GetInterruptStatus(void);

bool Accel_ProgramShock(int amplitude);
bool Accel_ProgramShake(int amplitude);
bool Accel_ProgramVibration(int amplitude);
bool Accel_ProgramTilt(bool shock, bool shake, bool vibration, int waitTime);

void Accel_Reset(bool shock, bool shake, bool vibration, bool tilt);
ACCEL_FSMR_T Accel_Disable(void);
void Accel_Enable(void);

#endif
