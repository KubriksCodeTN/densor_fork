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
 * This module provides motion and tilt (orientation) detection functionalities by using an accelerometer sensor chip.
 * The module supports different accelerometer chips and the selection can be done as a diversity setting;
 * for example: ACCEL_CHIP_FXLS8972CF.
 * A common API is exposed for all accelerometer chips. All implementation details should be hidden
 * in the chip specific folder such as FXLS8972CF.
 *
 * @par Technical notes
 *  An upper and lower threshold values in milli gravity (mg) can be defined to create user defined clipping regions.
 *  A time factor can also be added before a clipped sample create an motion event (interrupt). A number of consecutive
 *  clipped samples that will generate the interrupt will be defined in a debounce counter. The total duration has
 *  a dependency on Output Data Rate (ODR) as this defines the sample rate.
 *  The debounce duration is defined as:
 *       DebounceDuration= DebounceCount*(1000/ODR(in Hz)) milliseconds
 *  Debounce count can be tweaked by the application depending on the use case.
 *
 *  Motion detection is dependent on the following parameters:
 *  - Output Data Rate (ODR) in Hz: This parameter determines the sample rate of the accelerometer sensor readings
 *       on each axis (x, y and z) in mg (milli gravity). For example 3Hz ODR means, 3 samples from each axis will
 *       be collected per second, or a sample will be taken each 333ms. Similarly 400Hz ODR means, 400 samples per
 *       second, or a sample will be taken each 2.5ms.
 *       The possible ODR rates are:
 *           1Hz, 3Hz, 6Hz, 12Hz, 25Hz, 50Hz, 100Hz, 200Hz and 400Hz.
 *       If a specific chip is not supporting the selected ODR rate, the closest value shall be used. The set values
 *       shall not cause any motion event miss.
 *  - Full Scale Measurement Rate (FSMR) in g (gravity). This parameter will define the accelerometer measurement
 *       scale range. The possible values are (plus/minus) 2, 4, 8 and 16.
 *  - Lower and Upper thresholds: These parameters define the limits if a sample measurement is within the limits
 *       (with-in threshold, WT) or outside the limits (out-of-threshold, OT). The chip can be programmed to
 *       generate an event (interrupt) when the sample is outside or inside the threshold limits.
 *       Threshold values will define FSMR given above.
 *  - De-bounce count: The chip will internally count the consecutive internal events taken from
 *       each sample (based on the above thresholds). This parameter defines the counter value that will create an
 *       interrupt to MCU if the number of consecutive samples above(OT)/below(WT) thresholds exceed it. Overall
 *       duration that the interrupt to MCU will be generated depends on ODR and this counter. The formula is as given
 *       above:
 *           DebounceDuration= DebounceCount*(1000/ODR) ms (Measurement duration in API)
 *
 *  To reduce current consumption, sensor sampling can operate in two modes; Wake and optional Sleep. Normally sensor
 *  will be operating in Sleep mode with low ODR and debounce count (low current). When debounce counter reaches
 *  the limit, Wake mode may be invoked depending on the motion mode where higher ODR and debounce values are used
 *  (high current) for better accuracy. Wake mode can be enabled for operations that require to measure samples for
 *  a short duration motion swings with higher ODR rate than Sleep mode.
 *
 *  The device will operate in Wake mode by default if Sleep is not used.
 *
 *  Example (using wake mode only for simplicity):
 *       OT/WT: OT
 *       ODR: 3Hz
 *       Lower Threshold: -2000g
 *       Upper Threshold: +2000mg
 *       De-bounce Count: 3
 *       DebounceDuration=3*(1000/3)=1000ms
 *
 *       Samples during 2 seconds (6 samples - mg)
 *       -----------------------------------------
 *       -2800, -2300, -2050, -1800, -1500, -1200
 *           Will generate MCU interrupt as first 3 consecutive samples are OTed.
 *       1400, 1800, 2200, 2300, 2500, 2700
 *           Will generate MCU interrupt as samples 2,3 and 4 are consecutively OTed.
 *       -2300, -2100, -400, 400, 2100, 2300
 *           Will NOT generate MCU interrupt as no 3 consecutive samples are OTed.
 */
#include <string.h>
#include "chip.h"
#include "accel.h"
#include "accel_internal.h"

/* ------------------------------------------------------------------------- */

typedef struct WORKSPACE_S {
    ACCEL_MODE_T mode;
    ACCEL_SHOCK_PARAMS_T shock; /**< A copy of the shock parameters as given in the call to #Accel_SetParams. */
    ACCEL_SHAKE_PARAMS_T shake; /**< A copy of the shake parameters as given in the call to #Accel_SetParams. */
    ACCEL_VIBRATION_PARAMS_T vibration; /**< A copy of the vibration parameters as given in the call to #Accel_SetParams. */
    ACCEL_TILT_PARAMS_T tilt; /**< A copy of the tilt parameters as given in the call to #Accel_SetParams. */
} WORKSPACE_T;

/* ------------------------------------------------------------------------- */

static WORKSPACE_T * spWorkspace;

/**
 * Dummy variables to test whether the define ACCEL_WORKSPACE_SIZE is set correct.
 * If not in sync, one of the dummy variables will have a negative array size and the compiler will raise an error
 * similar to:
 *   ../src/memory.c:71:13: error: size of array 'sTestValuesOfEvent.' is negative
 */
static char sTestWorkspaceSize[(ACCEL_WORKSPACE_SIZE == sizeof(WORKSPACE_T)) ? 1 : - 1] __attribute__((unused));

/* ------------------------------------------------------------------------- */

WEAK void Accel_Cb(ACCEL_MODE_T mode, ACCEL_STATE_T state)
{
    (void)mode; /* suppress [-Wunused-parameter]: argument is on purpose not used in the dummy weak implementation */
    (void)state; /* suppress [-Wunused-parameter]: argument is on purpose not used in the dummy weak implementation */
    ASSERT(false); /* May not be hit: the application must override this weak implementation. */
}

/* ------------------------------------------------------------------------- */

void Accel_Init(void * pWorkspace)
{
    ASSERT(pWorkspace != NULL);
    spWorkspace = (WORKSPACE_T *)pWorkspace;
    Accel_SetParams(&spWorkspace->shock, &spWorkspace->shake, &spWorkspace->vibration, &spWorkspace->tilt);
}

void Accel_DeInit(void)
{
    /* void */
}

void Accel_SetParams(const ACCEL_SHOCK_PARAMS_T * pShock, const ACCEL_SHAKE_PARAMS_T * pShake,
                     const ACCEL_VIBRATION_PARAMS_T * pVibration, const ACCEL_TILT_PARAMS_T * pTilt)
{
#if ACCEL_SHOCK_ENABLE
    if ((pShock != NULL) /* Check for highly improbable values. */
            && (pShock->amplitude >= 3000)
            && (pShock->waitTime > 0)
            && (pShock->ringingAmplitude >= 1500)
            && (pShock->ringingAmplitude <= pShock->amplitude)
            && (pShock->ringingDuration >= pShock->waitTime)
            && (pShock->ringingCount < 6)) {
        spWorkspace->shock = *pShock;
    }
    else {
        spWorkspace->shock.amplitude = ACCEL_SHOCK_DEFAULT_AMPLITUDE;
        spWorkspace->shock.waitTime = ACCEL_SHOCK_DEFAULT_WAITTIME;
        spWorkspace->shock.ringingAmplitude = ACCEL_SHOCK_DEFAULT_RINGING_AMPLITUDE;
        spWorkspace->shock.ringingCount = ACCEL_SHOCK_DEFAULT_RINGING_COUNT;
        spWorkspace->shock.ringingDuration = ACCEL_SHOCK_DEFAULT_RINGING_DURATION;
    }
    Accel_Shock_SetParams(&spWorkspace->shock);
#else
    (void)pShock; /* suppress [-Wunused-parameter]: functionality is not available, thus ignore. */
#endif

#if ACCEL_SHAKE_ENABLE
    if ((pShake != NULL) /* Check for highly improbable values. */
            && (pShake->amplitude >= 1500)
            && (pShake->amplitude < 6000)
            && (pShake->count >= 4)
            && (pShake->duration > 300)) {
        spWorkspace->shake = *pShake;
    }
    else {
        spWorkspace->shake.amplitude = ACCEL_SHAKE_DEFAULT_AMPLITUDE;
        spWorkspace->shake.count = ACCEL_SHAKE_DEFAULT_COUNT;
        spWorkspace->shake.duration = ACCEL_SHAKE_DEFAULT_DURATION;
    }
    Accel_Shake_SetParams(&spWorkspace->shake);
#else
    (void)pShake; /* suppress [-Wunused-parameter]: functionality is not available, thus ignore. */
#endif

#if ACCEL_VIBRATION_ENABLE
    if ((pVibration != NULL) /* Check for highly improbable values. */
            && (pVibration->amplitude > 100)
            && (pVibration->frequency > 5)
            && (pVibration->duration > 0)) {
        spWorkspace->vibration = *pVibration;
    }
    else {
        spWorkspace->vibration.amplitude = ACCEL_VIBRATION_DEFAULT_AMPLITUDE;
        spWorkspace->vibration.frequency = ACCEL_VIBRATION_DEFAULT_FREQUENCY;
        spWorkspace->vibration.duration = ACCEL_VIBRATION_DEFAULT_DURATION;
    }
    Accel_Vibration_SetParams(&spWorkspace->vibration);
#else
    (void)pVibration; /* suppress [-Wunused-parameter]: functionality is not available, thus ignore. */
#endif

#if ACCEL_TILT_ENABLE
    if ((pTilt != NULL) /* Check for highly improbable values. */
            && (pTilt->waitTime > 0)) {
        spWorkspace->tilt = *pTilt;
    }
    else {
        spWorkspace->tilt.waitTime = ACCEL_TILT_DEFAULT_WAITTIME;
    }
    Accel_Tilt_SetParams(&spWorkspace->tilt);
#else
    (void)pTilt; /* suppress [-Wunused-parameter]: functionality is not available, thus ignore. */
#endif
}

void Accel_GetParams(ACCEL_SHOCK_PARAMS_T * pShock, ACCEL_SHAKE_PARAMS_T * pShake,
                     ACCEL_VIBRATION_PARAMS_T * pVibration, ACCEL_TILT_PARAMS_T * pTilt)
{
    if (pShock != NULL) {
        *pShock = spWorkspace->shock;
    }
    if (pShake != NULL) {
        *pShake = spWorkspace->shake;
    }
    if (pVibration != NULL) {
        *pVibration = spWorkspace->vibration;
    }
    if (pTilt != NULL) {
        *pTilt = spWorkspace->tilt;
    }
}

int Accel_Start(int mode)
{
    /** @todo Currently, multiple calls will fail. */
    ASSERT(spWorkspace->mode == ACCEL_MODE_NONE);
    /** @todo Currently, wrong OR'ing is not correctly prevented. */
    ASSERT(((mode & ACCEL_MODE_SHOCK) == ACCEL_MODE_SHOCK)
           + ((mode & ACCEL_MODE_SHAKE) == ACCEL_MODE_SHAKE)
           + ((mode & ACCEL_MODE_VIBRATION) == ACCEL_MODE_VIBRATION) <= 1);
    Accel_I2cInit();

    Accel_Reset(mode & ACCEL_MODE_SHOCK,
                mode & ACCEL_MODE_SHAKE,
                mode & ACCEL_MODE_VIBRATION,
                mode & ACCEL_MODE_TILT);

#if ACCEL_SHOCK_ENABLE
    if (mode & ACCEL_MODE_SHOCK) {
        if (Accel_Shock_Start()) {
            spWorkspace->mode |= ACCEL_MODE_SHOCK;
        }
    }
#endif

#if ACCEL_SHAKE_ENABLE
    if (mode & ACCEL_MODE_SHAKE) {
        if (Accel_Shake_Start()) {
            spWorkspace->mode |= ACCEL_MODE_SHAKE;
        }
    }
#endif

#if ACCEL_VIBRATION_ENABLE
    if (mode & ACCEL_MODE_VIBRATION) {
        if (Accel_Vibration_Start()) {
            spWorkspace->mode |= ACCEL_MODE_VIBRATION;
        }
    }
#endif

#if ACCEL_TILT_ENABLE
    if (mode & ACCEL_MODE_TILT) {
        if (Accel_Tilt_Start(mode & ACCEL_MODE_SHOCK,
                             mode & ACCEL_MODE_SHAKE,
                             mode & ACCEL_MODE_VIBRATION)) {
            spWorkspace->mode |= ACCEL_MODE_TILT;
        }
    }
#endif

    Accel_Enable();
    Accel_I2cDeInit();
    return spWorkspace->mode;
}

int Accel_Stop(int mode)
{
    mode &= spWorkspace->mode;
    if (mode) {
        spWorkspace->mode &= ~mode;
        Accel_I2cInit();
        Accel_Reset(spWorkspace->mode & ACCEL_MODE_SHOCK,
                        spWorkspace->mode & ACCEL_MODE_SHAKE,
                        spWorkspace->mode & ACCEL_MODE_VIBRATION,
                        spWorkspace->mode & ACCEL_MODE_TILT);

#if ACCEL_SHOCK_ENABLE
        if (mode & ACCEL_MODE_SHOCK) {
            Accel_Shock_Stop();
        }
#endif
#if ACCEL_SHAKE_ENABLE
        if (mode & ACCEL_MODE_SHAKE) {
            Accel_Shake_Stop();
        }
#endif
#if ACCEL_VIBRATION_ENABLE
        if (mode & ACCEL_MODE_VIBRATION) {
            Accel_Vibration_Stop();
        }
#endif
#if ACCEL_TILT_ENABLE
        if (mode & ACCEL_MODE_TILT) {
            Accel_Tilt_Stop();
        }
#endif

        Accel_I2cDeInit();
    }
    return spWorkspace->mode;
}

bool Accel_Check(void)
{
    Accel_I2cInit();
    const ACCEL_INTERRUPT_STATUS_T status = Accel_GetInterruptStatus();
    bool isHandled = false;

#if ACCEL_SHOCK_ENABLE
    isHandled |= status.thresholdsExceeded | status.timerExpired;
    if (spWorkspace->mode & ACCEL_MODE_SHOCK) {
        Accel_Shock_Detect(status);
    }
#endif

#if ACCEL_SHAKE_ENABLE
    isHandled |= status.thresholdsExceeded | status.timerExpired;
    if (spWorkspace->mode & ACCEL_MODE_SHAKE) {
        Accel_Shake_Detect(status);
    }
#endif

#if ACCEL_VIBRATION_ENABLE
    isHandled |= status.thresholdsExceeded | status.timerExpired;
    if (spWorkspace->mode & ACCEL_MODE_VIBRATION) {
        Accel_Vibration_Detect(status);
    }
#endif

#if ACCEL_TILT_ENABLE
    isHandled |= status.orientationChanged | status.timerExpired;
    if (spWorkspace->mode & ACCEL_MODE_TILT) {
        Accel_Tilt_Detect(status);
    }
#endif

    Accel_I2cDeInit();
    return isHandled;
}
