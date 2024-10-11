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

#include "chip.h"
#include "../accel_dft.h"
#include "../accel_internal.h"

#ifdef ACCEL_CHIP_FXLS8972CF
#include "fxls8972cf_reg.h"

typedef struct REGVAL_S {
    uint8_t reg;
    uint8_t val;
} REGVAL_T;

const uint8_t SLEEP_ODR_MAPPING[] = {
    REG_SENS_CONFIG3_SLEEP_ODR_1_563_HZ, /* ACCEL_ODR_1HZ */
    REG_SENS_CONFIG3_SLEEP_ODR_3_125_HZ, /* ACCEL_ODR_3HZ */
    REG_SENS_CONFIG3_SLEEP_ODR_6_25_HZ,  /* ACCEL_ODR_6HZ */
    REG_SENS_CONFIG3_SLEEP_ODR_12_5_HZ,  /* ACCEL_ODR_12HZ */
    REG_SENS_CONFIG3_SLEEP_ODR_25_HZ,    /* ACCEL_ODR_25HZ */
    REG_SENS_CONFIG3_SLEEP_ODR_50_HZ,    /* ACCEL_ODR_50HZ */
    REG_SENS_CONFIG3_SLEEP_ODR_100_HZ,   /* ACCEL_ODR_100HZ */
    REG_SENS_CONFIG3_SLEEP_ODR_200_HZ,   /* ACCEL_ODR_200HZ */
    REG_SENS_CONFIG3_SLEEP_ODR_400_HZ,   /* ACCEL_ODR_400HZ */
};

const uint8_t WAKE_ODR_MAPPING[] = {
    REG_SENS_CONFIG3_WAKE_ODR_1_563_HZ, /* ACCEL_ODR_1HZ */
    REG_SENS_CONFIG3_WAKE_ODR_3_125_HZ, /* ACCEL_ODR_3HZ */
    REG_SENS_CONFIG3_WAKE_ODR_6_25_HZ,  /* ACCEL_ODR_6HZ */
    REG_SENS_CONFIG3_WAKE_ODR_12_5_HZ,  /* ACCEL_ODR_12HZ */
    REG_SENS_CONFIG3_WAKE_ODR_25_HZ,    /* ACCEL_ODR_25HZ */
    REG_SENS_CONFIG3_WAKE_ODR_50_HZ,    /* ACCEL_ODR_50HZ */
    REG_SENS_CONFIG3_WAKE_ODR_100_HZ,   /* ACCEL_ODR_100HZ */
    REG_SENS_CONFIG3_WAKE_ODR_200_HZ,   /* ACCEL_ODR_200HZ */
    REG_SENS_CONFIG3_WAKE_ODR_400_HZ,   /* ACCEL_ODR_400HZ */
};

static bool Program(REGVAL_T * pPair, int count);
static int GetThresholdSettings(REGVAL_T * pPair, int index, ACCEL_FSMR_T fsmr, int level);
static int GetGenericSettings(REGVAL_T * pPair, int index, ACCEL_FSMR_T fsmr, ACCEL_ODR_T odr, bool motion,
                              bool orientation);
#if ACCEL_TILT_ENABLE
static int GetOrientationSettings(REGVAL_T * pPair, int index, int debounceCount);
#endif
#if ACCEL_SHOCK_ENABLE || ACCEL_SHAKE_ENABLE || ACCEL_VIBRATION_ENABLE
static int GetMotionSettings(REGVAL_T * pPair, int index, int debounceCount);
#endif
static ACCEL_FSMR_T DetermineFsmr(bool shock, bool shake, bool vibration, bool tilt);
static ACCEL_ODR_T DetermineOdr(bool shock, bool shake, bool vibration, bool tilt);
static ACCEL_AXES_FLAGS_T GetAxesFlags(void);

/* ------------------------------------------------------------------------- */

static bool Program(REGVAL_T * pPair, int count)
{
    bool success = true;
    for (int i = 0; i < count; i++) {
        success &= Accel_I2cWriteReg(pPair[i].reg, pPair[i].val);
    }
    return success;
}

static int GetThresholdSettings(REGVAL_T * pPair, int index, ACCEL_FSMR_T fsmr, int level)
{
    /* LSB/g. See $9.4 Mechanical characteristics in FXLS8972CF data sheet */
    const int nominalSensitivity[] = {1024 /* ACCEL_FSMR_2G */,
                                      512 /* ACCEL_FSMR_4G */,
                                      256 /* ACCEL_FSMR_8G */,
                                      128 /* ACCEL_FSMR_16G */};
    int count = (level * nominalSensitivity[fsmr]) / 1000;
    if (count >= (1 << 12)) {
        /* Sensor Data Change Detection uses a 12-bit value, spread over two registers - two for the lower and two for
         * the upper threshold.
         */
        count = (1 << 12) - 1;
    }

    /* Upper threshold. */
    pPair[index].reg = REG_SDCD_UTHS_LSB;
    pPair[index].val = (uint8_t)(count & 0xFF);
    index++;
    pPair[index].reg = REG_SDCD_UTHS_MSB;
    pPair[index].val = (uint8_t)((count >> 8) & 0x0F);
    index++;

    /* Lower threshold. */
    count = -count;
    pPair[index].reg =  REG_SDCD_LTHS_LSB;
    pPair[index].val = (uint8_t)(count & 0xFF);
    index++;
    pPair[index].reg = REG_SDCD_LTHS_MSB;
    pPair[index].val = (uint8_t)((count >> 8) & 0x0F);
    index++;

    return index;
}

static int GetGenericSettings(REGVAL_T * pPair, int index, ACCEL_FSMR_T fsmr, ACCEL_ODR_T odr, bool motion,
                              bool orientation)
{
    pPair[index].reg = REG_SENS_CONFIG1;
    pPair[index].val = REG_SENS_CONFIG1_RST;
    index++;

    /* ODRs. */
    pPair[index].reg = REG_SENS_CONFIG3;
    pPair[index].val = SLEEP_ODR_MAPPING[odr] | WAKE_ODR_MAPPING[odr];
    index++;

    /* Open drain IRQ. */
    pPair[index].reg = REG_SENS_CONFIG4;
    pPair[index].val = REG_SENS_CONFIG4_INT_PP_OD_OPEN_DRAIN;
    index++;

//    /* Vector magnitude calculation. */
//    pPair[index].reg = REG_SENS_CONFIG5;
//    pPair[index].val = REG_SENS_CONFIG5_VECM_EN;
//    index++;

    /* Enable interrupts. */
    pPair[index].reg = REG_INT_EN;
    pPair[index].val = REG_INT_EN_BOOT_DIS;
    if (motion) {
        pPair[index].val |= REG_INT_EN_SDCD_OT_EN | REG_INT_EN_SDCD_BUF_EN;
    }
    if (orientation) {
        pPair[index].val |= REG_INT_EN_ORIENT_EN;
    }
    index++;

    /* Setting full range measurement scale. */
    pPair[index].reg = REG_SENS_CONFIG1;
    pPair[index].val = (uint8_t)(fsmr << REG_SENS_CONFIG1_FSR_BITPOS);
    index++;

    return index;
}

#if ACCEL_TILT_ENABLE
static int GetOrientationSettings(REGVAL_T * pPair, int index, int debounceCount)
{
    /* Clear de-bounce counter on orient.
     * Orient enabled.
     */
    pPair[index].reg = REG_ORIENT_CONFIG;
    pPair[index].val = REG_ORIENT_CONFIG_ORIENT_ENABLE | REG_ORIENT_CONFIG_ORIENT_DBCNTM;
    index++;

    /* Debounce. */
    pPair[index].reg = REG_ORIENT_DBCOUNT;
    pPair[index].val = (uint8_t)debounceCount;
    index++;

    /* Threshold & hysteresis */
    pPair[index].reg = REG_ORIENT_THS;
    pPair[index].val = 0xF8; /* 1Fh: threshold angle 88.2 degrees, 000b: Landscape-to-Portrait trip angle 45 degrees. */
    index++;

    pPair[index].reg = REG_ORIENT_BF_ZCOMP;
    pPair[index].val = 0x47; /* default value for ORIENT_BKFR, ORIENT_ZLOCK to max value. */
    index++;

    return index;
}
#endif

#if ACCEL_SHOCK_ENABLE || ACCEL_SHAKE_ENABLE || ACCEL_VIBRATION_ENABLE
static int GetMotionSettings(REGVAL_T * pPair, int index, int debounceCount)
{
    /* XYZ OT enable and OT latch. */
    pPair[index].reg = REG_SDCD_CONFIG1;
    pPair[index].val = REG_SDCD_CONFIG1_Z_OT_EN | REG_SDCD_CONFIG1_Y_OT_EN | REG_SDCD_CONFIG1_X_OT_EN
            | REG_SDCD_CONFIG1_OT_ENE;
    index++;

    /* Clear debounce counter on OT.
     * Absolute mode.
     * SDCD enabled.
     */
    pPair[index].reg = REG_SDCD_CONFIG2;
//    pPair[index].val = REG_SDCD_CONFIG2_OT_DBCTM | REG_SDCD_CONFIG2_REF_UPDM_ABSOLUTE | REG_SDCD_CONFIG2_SDCD_EN
//            | REG_SDCD_CONFIG2_SDCD_MODE;
    pPair[index].val = REG_SDCD_CONFIG2_OT_DBCTM | REG_SDCD_CONFIG2_REF_UPDM_ABSOLUTE | REG_SDCD_CONFIG2_SDCD_EN;
    index++;

    /* Debounce count. */
    pPair[index].reg = REG_SDCD_OT_DBCNT;
    pPair[index].val = (uint8_t)debounceCount;
    index++;

    return index;
}
#endif

static ACCEL_FSMR_T DetermineFsmr(bool shock, bool shake, bool vibration, bool tilt)
{
    ASSERT(shock + shake + vibration <= 1);
    ACCEL_FSMR_T fsmr = ACCEL_FSMR_8G;
    if (shock | shake | vibration) {
        fsmr = (ACCEL_SHOCK_FSMR * shock) + (ACCEL_SHAKE_FSMR * shake) + (ACCEL_VIBRATION_FSMR * vibration);
    }
    else if (tilt) {
        fsmr = ACCEL_TILT_FSMR * tilt;
    }
    return fsmr;
}

static ACCEL_ODR_T DetermineOdr(bool shock, bool shake, bool vibration, bool tilt)
{
    ASSERT(shock + shake + vibration <= 1);
    ACCEL_ODR_T odr = ACCEL_ODR_100HZ;
    if (shock | shake | vibration) {
        odr = (ACCEL_SHOCK_ODR * shock) + (ACCEL_SHAKE_ODR * shake) + (ACCEL_VIBRATION_ODR * vibration);
    }
    else if (tilt) {
        odr = ACCEL_TILT_ODR * tilt;
    }
    return odr;
}

#if ACCEL_SHOCK_ENABLE
bool Accel_ProgramShock(int amplitude)
{
    REGVAL_T pair[8];
    int count = GetMotionSettings(pair, 0, 0);
    count = GetThresholdSettings(pair, count, ACCEL_SHOCK_FSMR, amplitude);
    ASSERT(count <= 8);
    bool success = Program(pair, count);
    return success;
}
#endif

#if ACCEL_SHAKE_ENABLE
bool Accel_ProgramShake(int amplitude)
{
    REGVAL_T pair[8];
    const int waitTime = 10; /* fixed 10 millisecond debounce time, as per AN12004 $3.1 Shake event detection */
    ACCEL_ODR_T odr = DetermineOdr(false, false, true, false /* don't care */);
    int debounce = Accel_TimerCalculateDebounce(odr, waitTime);
    int count = GetMotionSettings(pair, 0, debounce);
    count = GetThresholdSettings(pair, count, ACCEL_SHAKE_FSMR, amplitude);
    ASSERT(count <= 8);
    bool success = Program(pair, count);
    return success;
}
#endif

#if ACCEL_VIBRATION_ENABLE
bool Accel_ProgramVibration(int amplitude)
{
    REGVAL_T pair[8];
    int count = GetMotionSettings(pair, 0, 0);
    count = GetThresholdSettings(pair, count, ACCEL_VIBRATION_FSMR, amplitude);
    ASSERT(count <= 8);
    bool success = Program(pair, count);
    return success;
}
#endif

#if ACCEL_TILT_ENABLE
bool Accel_ProgramTilt(bool shock, bool shake, bool vibration, int waitTime)
{
    ACCEL_ODR_T odr = DetermineOdr(shock, shake, vibration, true);
    int debounce = Accel_TimerCalculateDebounce(odr, waitTime);
    REGVAL_T pair[8];
    int count = GetOrientationSettings(pair, 0, debounce);
    ASSERT(count <= 8);
    bool success = Program(pair, count);
    return success;
}
#endif

/* ------------------------------------------------------------------------- */

void Accel_SetThresholds(int level)
{
    ACCEL_FSMR_T fsmr = Accel_Disable();
    REGVAL_T pair[8];
    int count = GetThresholdSettings(pair, 0, fsmr, level);
    Program(pair, count);
    Accel_Enable();
}

static ACCEL_AXES_FLAGS_T GetAxesFlags(void)
{
    ACCEL_AXES_FLAGS_T flags = {.x = 0, .y = 0, .z = 0};
    uint8_t value = Accel_I2cReadReg(REG_SDCD_INT_SRC1); /* Reading it clears the pending interrupt flag. */
    if (value & REG_SDCD_INT_SRC1_X_OT_EF) {
        flags.x = (value & REG_SDCD_INT_SRC1_X_OT_POL) ? 1 : -1;
    }
    if (value & REG_SDCD_INT_SRC1_Y_OT_EF) {
        flags.y = (value & REG_SDCD_INT_SRC1_Y_OT_POL) ? 1 : -1;
    }
    if (value & REG_SDCD_INT_SRC1_Z_OT_EF) {
        flags.z = (value & REG_SDCD_INT_SRC1_Z_OT_POL) ? 1 : -1;
    }
    return flags;
}

ACCEL_INTERRUPT_STATUS_T Accel_GetInterruptStatus(void) {
    ACCEL_INTERRUPT_STATUS_T status = {
#if ACCEL_SHOCK_ENABLE || ACCEL_SHAKE_ENABLE || ACCEL_VIBRATION_ENABLE
                                       .thresholdsExceeded = false,
                                       .flags.x = 0,
                                       .flags.y = 0,
                                       .flags.z = 0,
#endif
#if ACCEL_TILT_ENABLE
                                       .orientationChanged = false,
                                       .tiltStatus = ACCEL_TILT_STATUS_UNKNOWN,
#endif
                                       .timerExpired = false};

    uint8_t regIntStatus = Accel_I2cReadReg(REG_INT_STATUS);

#if ACCEL_SHOCK_ENABLE || ACCEL_SHAKE_ENABLE || ACCEL_VIBRATION_ENABLE
    status.thresholdsExceeded = (regIntStatus & REG_INT_STATUS_SRC_SDCD_OT);
    if (status.thresholdsExceeded) {
        status.flags = GetAxesFlags();
    }
    if (regIntStatus & REG_INT_STATUS_SRC_BUF) {
        uint8_t regBufStatus = Accel_I2cReadReg(REG_BUF_STATUS);

        /* Set BUF_FLUSH bit to clear buf status. No need to switch to standby mode to change this bit.
         * The rest of the register content will not be effected.
         */
        (void)Accel_I2cWriteReg(REG_BUF_CONFIG2, REG_BUF_CONFIG2_BUF_FLUSH);

        if (regBufStatus & REG_BUF_STATUS_BUF_WMRK) {
            status.timerExpired = Accel_TimerCb();
        }
    }
#endif
#if ACCEL_TILT_ENABLE
    status.orientationChanged = (regIntStatus & REG_INT_STATUS_SRC_ORIENT);
    uint8_t regOrientStatus = Accel_I2cReadReg(REG_ORIENT_STATUS);
    /* Disregard if the lockout bit is set, except when this is the first orientation interrupt. */
    if (status.orientationChanged && (!(regOrientStatus & REG_ORIENT_STATUS_LO))) {
        switch (regOrientStatus & REG_ORIENT_STATUS_LAPO_MASK) {
            default:
                ASSERT(false);
                /* fallthrough */
            case REG_ORIENT_STATUS_LAPO_PORTRAIT_UP:
                status.tiltStatus |= ACCEL_TILT_STATUS_PORTRAIT_UP;
                break;
            case REG_ORIENT_STATUS_LAPO_PORTRAIT_DOWN:
                status.tiltStatus |= ACCEL_TILT_STATUS_PORTRAIT_DOWN;
                break;
            case REG_ORIENT_STATUS_LAPO_LANDSCAPE_RIGHT:
                status.tiltStatus |= ACCEL_TILT_STATUS_LANDSCAPE_RIGHT;
                break;
            case REG_ORIENT_STATUS_LAPO_LANDSCAPE_LEFT:
                status.tiltStatus |= ACCEL_TILT_STATUS_LANDSCAPE_LEFT;
                break;
        }
        if (regOrientStatus & REG_ORIENT_STATUS_BAFRO_BACK) {
            status.tiltStatus |= ACCEL_TILT_STATUS_BACK;
        }
        else {
            status.tiltStatus |= ACCEL_TILT_STATUS_FRONT;
        }
    }
#endif
    return status;
}

void Accel_SetWatermark(int watermark)
{
    (void)Accel_Disable();

    if (watermark >= ACCEL_BUFFER_SIZE) {
        watermark = ACCEL_BUFFER_SIZE - 1;
    }
    (void)Accel_I2cWriteReg(REG_BUF_CONFIG2, (uint8_t)watermark);
    (void)Accel_I2cWriteReg(REG_BUF_CONFIG1, REG_BUF_CONFIG1_BUF_MODE_STREAM);

    Accel_Enable();
}

void Accel_Reset(bool shock, bool shake, bool vibration, bool tilt)
{
    (void)Accel_Disable();

    ASSERT(shock + shake + vibration <= 1);
    ACCEL_FSMR_T fsmr = DetermineFsmr(shock, shake, vibration, tilt);
    ACCEL_ODR_T odr = DetermineOdr(shock, shake, vibration, tilt);

    REGVAL_T pair[8];
    int count = GetGenericSettings(pair, 0, fsmr, odr, shock | shake | vibration, tilt);
    ASSERT(count <= 8);
    Program(pair, count);
}

ACCEL_FSMR_T Accel_Disable(void)
{
    uint8_t value = Accel_I2cReadReg(REG_SENS_CONFIG1);
    (void)Accel_I2cWriteReg(REG_SENS_CONFIG1, (uint8_t)(value & (~REG_SENS_CONFIG1_ACTIVE)));
    ACCEL_FSMR_T fsmr = (value & REG_SENS_CONFIG1_FSR_MASK) >> REG_SENS_CONFIG1_FSR_BITPOS;
    return fsmr;
}

void Accel_Enable(void)
{
    uint8_t value = Accel_I2cReadReg(REG_SENS_CONFIG1);
    (void)Accel_I2cWriteReg(REG_SENS_CONFIG1, value | REG_SENS_CONFIG1_ACTIVE);
}
#endif /* ACCEL_CHIP_FXLS8972CF */
