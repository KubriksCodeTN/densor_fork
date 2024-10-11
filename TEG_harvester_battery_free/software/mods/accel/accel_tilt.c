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

/**
 * @file
 * This module provides tilt detection functionalities by using an accelerometer sensor chip.
 *
 * @par Tilt_algorithm
 *  Tilt (orientation) is a static measurement. The force of gravity is used as an input to determine the
 *  orientation of an object calculating the degree of tilt. The accelerometer will experience acceleration in the
 *  range from -1g to +1g through 180° of tilt (sine wave) on each axis.
 *  Accelerometer sensors will measure the following orientation detections:
 *  - XY axes:
 *      - Portrait Up (PU)                              PU x=0g y=-1g
 *      - Portrait Down (PD)                            +-------+
 *      - Landscape Left (LL)                    pin1-->|o      |
 *      - Landscape Right (LR)                          |       |
 *      .                                               |       |
 *                                                      +-------+
 *                                        +-------+         |          +-------+
 *                                        |       |         |          |      o|
 *                         LL x=-1g, y=0g |       |---------+----------|       | LR x=+1g, y=0g
 *                                        |o      |         |          |       |
 *                                        +-------+         |          +-------+
 *                                                      +-------+
 *                                                      |       |
 *                                                      |       |
 *                                                      |      o|
 *                                                      +-------+
 *                                                      PD x=0g, y=+1g
 *
 *      The criteria switching from one XY detection to the other are defined by the following
 *      parameters:
 *      - Threshold angle (TA): the angle at which the detection trips. Different trip angles can be defined but we use
 *          45° as a fix default value.
 *      - Hysteresis angle (HA): A very slight movement can cause the device to flicker slightly above and then
 *          below the threshold angle that could make the detections jump back and forth. This is avoided by
 *          adding the hysteresis angle around the trip threshold. A fixed default value of 15° will be used.
 *       @note Tilting measurement is using the gravity. Hence if the device is laid on a flat surface (F Z=0°) or
 *          (B Z=180°), there is no way to detect any changes on XY axes as the gravity on these axes will be 0 when the
 *          device is rotated around Z axis. Therefore a minimum angle on Z-axis called the “Z-lockout” angle is
 *          required (will add some g to either on X, Y or both) to detect correct XY settings. The application should
 *          be aware of this fact that if device is placed on a flat surface, we cannot detect any rotation changes on
 *          XY axes.
 *  - Z axis:
 *      - Front (F)                                    BtoF  90°  FtoB
 *      - Back (B)                                       \   |   /
 *      .                                                 \  |  /
 *                                                         \ | /
 *                                     z=-1g (B) 180° ------------- 0° (F) z=+1g
 *                                                         / | \
 *                                                        /  |  \
 *                                                       /   |   \
 *                                                     BtoF 270°  FtoB
 *
 *      Back or front orientation is detected using these fixed values:
 *       BtoF                FtoB
 *       Z<75° and Z>285°    Z>105° and Z<255°   (15° difference)
 *
 *  For both Z and XY measurements the accelerometer chip itself will calculate the angles, based on the acceleration
 *  data points. A sample angle exceeding the specified limits will create an internal event. If sufficient consecutive
 *  events are generated, an interrupt to the MCU - the NHS3xx IC - will be generated.
 *  The time after which the interrupt to MCU is generated depends on the ODR (Output Data Rate, the sampling
 *  rate of the accelerometer chip) and this counter. A fixed value
 *          A fixed value (1) as a macro is defined in source code.
 */
#include <string.h>
#include "chip.h"
#include "accel.h"
#include "accel_internal.h"

#if ACCEL_TILT_ENABLE

typedef struct TILT_S {
    ACCEL_TILT_STATUS_T status;

    const ACCEL_TILT_PARAMS_T * pParams;
} TILT_T;

static TILT_T sTilt;

/* ------------------------------------------------------------------------- */

bool Accel_Tilt_Start(bool shock, bool shake, bool vibration)
{
    sTilt.status = ACCEL_TILT_STATUS_UNKNOWN;
    bool success = Accel_ProgramTilt(shock, shake, vibration, sTilt.pParams->waitTime);
    return success;
}

void Accel_Tilt_Stop(void)
{
    /* void */
}

void Accel_Tilt_SetParams(ACCEL_TILT_PARAMS_T * pTilt)
{
    sTilt.pParams = pTilt;
}

void Accel_Tilt_Detect(const ACCEL_INTERRUPT_STATUS_T status)
{
    if (status.orientationChanged) {
        int oldPortraitStatus = sTilt.status & (ACCEL_TILT_STATUS_PORTRAIT_UP | ACCEL_TILT_STATUS_PORTRAIT_DOWN);
        int oldLandscapeStatus = sTilt.status & (ACCEL_TILT_STATUS_LANDSCAPE_RIGHT | ACCEL_TILT_STATUS_LANDSCAPE_LEFT);
        int oldFrontStatus = sTilt.status & (ACCEL_TILT_STATUS_FRONT | ACCEL_TILT_STATUS_BACK);
        int newPortraitStatus = status.tiltStatus & (ACCEL_TILT_STATUS_PORTRAIT_UP | ACCEL_TILT_STATUS_PORTRAIT_DOWN);
        int newLandscapeStatus = status.tiltStatus & (ACCEL_TILT_STATUS_LANDSCAPE_RIGHT | ACCEL_TILT_STATUS_LANDSCAPE_LEFT);
        int newFrontStatus = status.tiltStatus & (ACCEL_TILT_STATUS_FRONT | ACCEL_TILT_STATUS_BACK);
        if ((oldPortraitStatus != newPortraitStatus)
                || (oldLandscapeStatus != newLandscapeStatus)
                || (oldFrontStatus != newFrontStatus)) {
            Accel_Cb(ACCEL_MODE_TILT, ACCEL_STATE_END);
            sTilt.status = status.tiltStatus;
        }
    }
}

#endif
