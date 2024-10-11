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
 * Acceleration module needs timer support. Normally we can use HW timers here for that purpose. But we don't want to
 * add an extra HW dependency and also not allocate any HW timer(s). Hence we will use FXLS8972CF's buffer interrupt
 * mechanism to obtain timing information. FXLS8972CF can be programmed to generate interrupt when certain number of
 * samples are collected (watermark interrupt) based on ODR.
 * The time calculation:
 *      time in millisecond = (1000/ODR_Hz) * watermark_value
 */

#include "chip.h"
#include "accel.h"
#include "accel_internal.h"
#include "accel/FXLS8972CF/fxls8972cf_reg.h"

/* Calculation macros. */
#define ODR_AND_MS_TO_CNT(odr,ms)       (ms/ODR_TO_MS[odr])
#define ODR_AND_CNT_TO_MS(odr,cnt)      (cnt*ODR_TO_MS[odr])

/* ------------------------------------------------------------------------- */

static int sTimerCount;
static int sTimerLimit;

bool Accel_TimerCb(void)
{
    bool timeoutElapsed = false;
    /* Check if we reach the limit. */
    sTimerCount++;
    if (sTimerCount > sTimerLimit) {
        timeoutElapsed = true;

        (void)Accel_Disable();
        (void)Accel_I2cWriteReg(REG_BUF_CONFIG1, REG_BUF_CONFIG1_BUF_MODE_DISABLED);
        (void)Accel_I2cWriteReg(REG_BUF_CONFIG2, 0);
        Accel_Enable();
        sTimerCount = 0;
        sTimerLimit = 0;
    }
    return timeoutElapsed;
}

void Accel_TimerSet(ACCEL_ODR_T odr, unsigned int time)
{
    /* count = sampling rate in Hz * time in seconds
     * count = (sampling rate in milli-Hz * time in milli-seconds) / (1000 * 1000)
     */
    int numSamples = Accel_TimerCalculateDebounce(odr, (int)time);
    sTimerLimit = numSamples / ACCEL_BUFFER_SIZE + 1;
    uint8_t watermark = (uint8_t)(numSamples / sTimerLimit);
    if (watermark < ACCEL_BUFFER_SIZE - 1) {
        watermark++;
    }
    sTimerCount = 0;

    /* Set BUF_FLUSH bit to clear buffer status. No need to switch to standby mode to change this bit.
     * The rest of the register content will not be effected. This is required to reset the buffer counter.
     */
    (void)Accel_I2cWriteReg(REG_BUF_CONFIG2, REG_BUF_CONFIG2_BUF_FLUSH);

    /* Set watermark and enable buffer mode as stream. This will create an interrupt when number of samples reaches
     * watermark value.
     */
    Accel_SetWatermark(watermark);
}

int Accel_TimerCalculateDebounce(ACCEL_ODR_T odr, int duration)
{
    /* Provides the exact sampling rate of a given ODR setting, in milli-Hertz. */
    const int odr2hz[ACCEL_ODR_COUNT] = {
        1563 /* ACCEL_ODR_1HZ */,
        3125 /* ACCEL_ODR_3HZ */,
        6250 /* ACCEL_ODR_6HZ */,
        12500 /* ACCEL_ODR_12HZ */,
        25000 /* ACCEL_ODR_25HZ */,
        50000 /* ACCEL_ODR_50HZ */,
        100000 /* ACCEL_ODR_100HZ */,
        200000 /* ACCEL_ODR_200HZ */,
        400000 /* ACCEL_ODR_400HZ */,
    };

    int debounce = (odr2hz[odr] * duration) / (1000 * 1000);
    if (debounce <= 0) {
        debounce = duration ? 1 : 0;
    }
    else if (debounce > 0xFF) {
        debounce = 0xFF;
    }
    return debounce;
}
