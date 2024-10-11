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

#ifndef __ACCEL_PARAMS_H_
#define __ACCEL_PARAMS_H_

#include <stdbool.h>
#include <stdint.h>

/**
 * @addtogroup MODS_NSS_ACCEL accel: Accelerometer sensor module
 * @{
 */

#pragma pack(push, 1)

/** Defines the parameter values that characterize a shock. */
typedef struct ACCEL_SHOCK_PARAMS_S {
    /**
     * Define the minimal threshold value to watch for. This threshold is applied to each axis: X, Y and Z.
     * When an acceleration is detected which is - in absolute value - above this threshold value, the NHS31xx IC is
     * notified and it is checked whether this constitutes a valid shock.
     * The number listed must be an absolute positive number, expressed in milli-g.
     */
    uint16_t amplitude;

    /**
     * Define the time to wait after detecting an initial shock. All acceleration events after detecting the initial
     * shock are ignored for this duration. After this time has passed, the 'ringing' effect is looked for.
     * The number listed must be an absolute positive number, expressed in milli-seconds.
     */
    uint16_t waitTime;

    /**
     * When a shock occurs, after the first peak, a number of smaller shocks are expected, in a so-called 'ringing'
     * effect. Define here the minimal threshold value above which ringing is detected.
     * The number listed must be an absolute positive number less than @c amplitude, expressed in milli-g.
     */
    uint16_t ringingAmplitude;

    /**
     * When a shock occurs, after the first peak, a number of smaller shocks are expected, in a so-called 'ringing'
     * effect. Define here the minimal number of smaller shocks to detect.
     * The number listed must be an absolute positive number less than 10.
     */
    uint8_t ringingCount;

    /**
     * Define the total duration to look for the 'ringing' effect. This time window starts when @c waitTime
     * expires. At least @c ringingShockCount smaller shocks are expected in this time window.
     * The number listed must be equal to or larger than @c waitTime, expressed in milli-seconds.
     */
    uint16_t ringingDuration;
} ACCEL_SHOCK_PARAMS_T;

/** Defines the parameter values that characterize a shake. */
typedef struct ACCEL_SHAKE_PARAMS_S {
    /**
     * Define the minimal threshold value to watch for. This threshold is applied to each axis: X, Y and Z.
     * When accelerations are detected which are - in absolute value - less than this value, the NHS31xx IC is not
     * involved: the data is immediately discarded by the accelerometer.
     * The number listed must be an absolute positive number, expressed in milli-g.
     */
    uint16_t amplitude;

    /**
     * Define the minimal number of back-and-forth movements to detect before classifying the motion as a shake.
     * The number listed must be an absolute positive number.
     */
    uint8_t count;

    /**
     * Define the maximal duration in which the shake must be completed before it is reported.
     * When the product is shaked the sufficient number of times after a longer duration, it is considered as a
     * 'short-lived heavy vibration' kind of motion, and not a - typically human-induced - shake.
     * The number listed must be an absolute positive number, expressed in milliseconds.
     */
    uint16_t duration;
} ACCEL_SHAKE_PARAMS_T;

/** Defines the parameter values that characterize a vibration. */
typedef struct ACCEL_VIBRATION_PARAMS_S {
    /**
     * Define the minimal threshold value to watch for. This threshold is applied to each axis: X, Y and Z.
     * When accelerations are detected which are - in absolute value - less than this value, the NHS31xx IC is not
     * involved: the data is immediately discarded by the accelerometer.
     * The number listed must be an absolute positive number, expressed in milli-g.
     */
    uint16_t amplitude;

    /**
     * Define the minimal frequency before a vibration is to be reported.
     * The accelerometer is sampling continually, at a rate suitable to catch the vibration to monitor for.
     * The samples are filtered per the given threshold value @c amplitude. The filtered samples occur
     * at a rate which increases when the vibration intensifies. The value given here sets the minimum frequency in
     * which the application is interested.
     * The number listed must be an absolute positive number, expressed in Hertz.
     */
    uint8_t frequency;

    /**
     * Define the minimal duration of the vibration before it is reported.
     * When the product or machine, which the NHS31xx solution is monitoring, is manipulated, short-lived vibrations
     * can occur. These do not necessarily indicate a defect.
     * The number listed here sets a duration: only when the filtered accelerations are sampled at a high-enough rate
     * for a long enough time, the application is notified.
     * The number listed must be an absolute positive number, expressed in seconds.
     */
    uint16_t duration;
} ACCEL_VIBRATION_PARAMS_T;

/** Defines the parameter values that characterize a tilt. */
typedef struct ACCEL_TILT_PARAMS_S {
    /**
     * Define the time to wait after detecting an orientation change. Only when the orientation change is stable for
     * this amount of time, it is reported as an event.
     * The number listed must be an absolute positive number, expressed in milli-seconds.
     * @note The maximum possible wait time is dependent on the sampling rate of the accelerometer. When the chosen
     *  wait time cannot be met, it is set to the maximum value it can wait.
     */
    uint16_t waitTime;
} ACCEL_TILT_PARAMS_T;

#pragma pack(pop)

#endif /** @} */
