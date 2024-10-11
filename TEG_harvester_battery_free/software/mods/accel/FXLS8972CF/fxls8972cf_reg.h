/*
 * Copyright 2018 NXP
 * This software is owned or controlled by NXP and may only be used strictly
 * in accordance with the applicable license terms.  By expressly accepting
 * such terms or by downloading, installing, activating and/or otherwise using
 * the software, you are agreeing that you have read, and that you agree to
 * comply with and are bound by, such license terms.  If you do not agree to
 * be bound by the applicable license terms, then you may not retain, install,
 * activate or otherwise use the software.
 */

#ifndef __FXLS8972CF__REG_H_
#define __FXLS8972CF__REG_H_

#ifdef ACCEL_CHIP_FXLS8972CF

#define REG_INT_STATUS          0x00
    #define REG_INT_STATUS_SRC_ASLP         (1<<1)
    #define REG_INT_STATUS_SRC_ORIENT       (1<<2)
    #define REG_INT_STATUS_SRC_SDCD_WT      (1<<3)
    #define REG_INT_STATUS_SRC_SDCD_OT      (1<<4)
    #define REG_INT_STATUS_SRC_BUF          (1<<5)

#define REG_TEMP_OUT            0x01
#define REG_VECM_LSB            0x02
#define REG_VECM_MSB            0x03
#define REG_OUT_X_LSB           0x04
#define REG_OUT_X_MSB           0x05
#define REG_OUT_Y_LSB           0x06
#define REG_OUT_Y_MSB           0x07
#define REG_OUT_Z_LSB           0x08
#define REG_OUT_Z_MSB           0x09
#define REG_RESERVED_REG1       0x0A

#define REG_BUF_STATUS          0x0B
    #define REG_BUF_STATUS_BUF_OVF      (1<<6)
    #define REG_BUF_STATUS_BUF_WMRK     (1<<7)

#define REG_BUF_X_LSB           0x0C
#define REG_BUF_X_MSB           0x0D
#define REG_BUF_Y_LSB           0x0E
#define REG_BUF_Y_MSB           0x0F
#define REG_BUF_Z_LSB           0x10
#define REG_BUF_Z_MSB           0x11
#define REG_PROD_REV            0x12
#define REG_WHO_AM_I            0x13

#define REG_SYS_MODE            0x14
    #define REG_SYS_MODE_MASK       (0x3<<0)
    #define REG_SYS_MODE_STANDBY    (0x0<<0)
    #define REG_SYS_MODE_WAKE       (0x1<<0)
    #define REG_SYS_MODE_SLEEP      (0x2<<0)

#define REG_SENS_CONFIG1        0x15
    #define REG_SENS_CONFIG1_ACTIVE         (1<<0)
    #define REG_SENS_CONFIG1_FSR_MASK 0x06
    #define REG_SENS_CONFIG1_FSR_BITPOS 1
    #define REG_SENS_CONFIG1_FSR_2G         (0x0<<1)
    #define REG_SENS_CONFIG1_FSR_4G         (0x1<<1)
    #define REG_SENS_CONFIG1_FSR_8G         (0x2<<1)
    #define REG_SENS_CONFIG1_FSR_16G        (0x3<<1)
    /* Register values (12 bit 2s complement range) for different
     * g ranges. 1g is 512.
     *      2g ---> -1023 to 1023
     *      4g ---> -2047 to 2047
     *      8g ---> -4095 to 4095
     *      16g --> -8191 to 8191
     * When setting lower and upper limits consider current range g value.
     */
    #define REG_SENS_CONFIG1_RST            (1<<7)

#define REG_SENS_CONFIG2        0x16

#define REG_SENS_CONFIG3        0x17
    /* Low Power Mode (LPM) ODR values. We will be using LPM always. */
    #define REG_SENS_CONFIG3_SLEEP_ODR_1_563_HZ     (0xB<<0)    /* 0.60 uA */
    #define REG_SENS_CONFIG3_SLEEP_ODR_3_125_HZ     (0xA<<0)    /* 0.65 uA */
    #define REG_SENS_CONFIG3_SLEEP_ODR_6_25_HZ      (0x9<<0)    /* 0.85 uA */
    #define REG_SENS_CONFIG3_SLEEP_ODR_12_5_HZ      (0x8<<0)    /* 1.20 uA */
    #define REG_SENS_CONFIG3_SLEEP_ODR_25_HZ        (0x7<<0)    /* 1.70 uA */
    #define REG_SENS_CONFIG3_SLEEP_ODR_50_HZ        (0x6<<0)    /* 2.90 uA */
    #define REG_SENS_CONFIG3_SLEEP_ODR_100_HZ       (0x5<<0)    /* 5.20 uA */
    #define REG_SENS_CONFIG3_SLEEP_ODR_200_HZ       (0x4<<0)    /* 11 uA */
    #define REG_SENS_CONFIG3_SLEEP_ODR_400_HZ       (0x3<<0)    /* 20 uA */
    #define REG_SENS_CONFIG3_WAKE_ODR_1_563_HZ      (0xB<<4)    /* 0.60 uA */
    #define REG_SENS_CONFIG3_WAKE_ODR_3_125_HZ      (0xA<<4)    /* 0.65 uA */
    #define REG_SENS_CONFIG3_WAKE_ODR_6_25_HZ       (0x9<<4)    /* 0.85 uA */
    #define REG_SENS_CONFIG3_WAKE_ODR_12_5_HZ       (0x8<<4)    /* 1.20 uA */
    #define REG_SENS_CONFIG3_WAKE_ODR_25_HZ         (0x7<<4)    /* 1.70 uA */
    #define REG_SENS_CONFIG3_WAKE_ODR_50_HZ         (0x6<<4)    /* 2.90 uA */
    #define REG_SENS_CONFIG3_WAKE_ODR_100_HZ        (0x5<<4)    /* 5.20 uA */
    #define REG_SENS_CONFIG3_WAKE_ODR_200_HZ        (0x4<<4)    /* 11 uA */
    #define REG_SENS_CONFIG3_WAKE_ODR_400_HZ        (0x3<<4)    /* 20 uA */

#define REG_SENS_CONFIG4        0x18
    #define REG_SENS_CONFIG4_INT_PP_OD_OPEN_DRAIN   (1<<1)
    #define REG_SENS_CONFIG4_WAKE_ORIENT            (1<<4)
    #define REG_SENS_CONFIG4_WAKE_SDCD_OT           (1<<5)
    #define REG_SENS_CONFIG4_WAKE_SDCD_WT           (1<<6)


#define REG_SENS_CONFIG5        0x19
    #define REG_SENS_CONFIG5_VECM_EN      (1<<4)

#define REG_WAKE_IDLE_LSB       0x1A
#define REG_WAKE_IDLE_MSB       0x1B
#define REG_SLEEP_IDLE_LSB      0x1C
#define REG_SLEEP_IDLE_MSB      0x1D
#define REG_ASLP_COUNT_LSB      0x1E
#define REG_ASLP_COUNT_MSB      0x1F

#define REG_INT_EN              0x20
    #define REG_INT_EN_WAKE_OUT_EN      (1<<0)
    #define REG_INT_EN_BOOT_DIS         (1<<1)
    #define REG_INT_EN_ASLP_EN          (1<<2)
    #define REG_INT_EN_ORIENT_EN        (1<<3)
    #define REG_INT_EN_SDCD_WT_EN       (1<<4)
    #define REG_INT_EN_SDCD_OT_EN       (1<<5)
    #define REG_INT_EN_SDCD_OT_EN       (1<<5)
    #define REG_INT_EN_SDCD_BUF_EN      (1<<6)

#define REG_INT_PIN_SEL         0x21
#define REG_OFF_X               0x22
#define REG_OFF_Y               0x23
#define REG_OFF_Z               0x24
#define REG_RESERVED_REG2       0x25

#define REG_BUF_CONFIG1         0x26
    #define REG_BUF_CONFIG1_BUF_MODE_DISABLED   (0x0<<5)
    #define REG_BUF_CONFIG1_BUF_MODE_STREAM     (0x1<<5)
    #define REG_BUF_CONFIG1_BUF_MODE_STOP       (0x2<<5)
    #define REG_BUF_CONFIG1_BUF_MODE_TRIGGER    (0x3<<5)

#define REG_BUF_CONFIG2         0x27
    #define REG_BUF_CONFIG2_BUF_FLUSH       (1<<7)

#define REG_ORIENT_STATUS       0x28
    #define REG_ORIENT_STATUS_BAFRO_BACK            (1<<0)
    #define REG_ORIENT_STATUS_LAPO_MASK             (0x3<<1)
    #define REG_ORIENT_STATUS_LAPO_PORTRAIT_UP      (0x0<<1)
    #define REG_ORIENT_STATUS_LAPO_PORTRAIT_DOWN    (0x1<<1)
    #define REG_ORIENT_STATUS_LAPO_LANDSCAPE_RIGHT  (0x2<<1)
    #define REG_ORIENT_STATUS_LAPO_LANDSCAPE_LEFT   (0x3<<1)
    #define REG_ORIENT_STATUS_LO                    (1<<6)
    #define REG_ORIENT_STATUS_NEW_ORIENT            (1<<7)

#define REG_ORIENT_CONFIG       0x29
    #define REG_ORIENT_CONFIG_ORIENT_ENABLE     (1<<6)
    #define REG_ORIENT_CONFIG_ORIENT_DBCNTM     (1<<7)

#define REG_ORIENT_DBCOUNT      0x2A
#define REG_ORIENT_BF_ZCOMP     0x2B
#define REG_ORIENT_THS          0x2C

#define REG_SDCD_INT_SRC1       0x2D
    #define REG_SDCD_INT_SRC1_Z_OT_POL  (1<<0)
    #define REG_SDCD_INT_SRC1_Z_OT_EF   (1<<1)
    #define REG_SDCD_INT_SRC1_Y_OT_POL  (1<<2)
    #define REG_SDCD_INT_SRC1_Y_OT_EF   (1<<3)
    #define REG_SDCD_INT_SRC1_X_OT_POL  (1<<4)
    #define REG_SDCD_INT_SRC1_X_OT_EF   (1<<5)
    #define REG_SDCD_INT_SRC1_OT_EA     (1<<7)

#define REG_SDCD_INT_SRC2       0x2E

#define REG_SDCD_CONFIG1        0x2F
    #define REG_SDCD_CONFIG1_Z_WT_EN    (1<<0)
    #define REG_SDCD_CONFIG1_Y_WT_EN    (1<<1)
    #define REG_SDCD_CONFIG1_X_WT_EN    (1<<2)
    #define REG_SDCD_CONFIG1_Z_OT_EN    (1<<3)
    #define REG_SDCD_CONFIG1_Y_OT_EN    (1<<4)
    #define REG_SDCD_CONFIG1_X_OT_EN    (1<<5)
    #define REG_SDCD_CONFIG1_WT_ENE     (1<<6)
    #define REG_SDCD_CONFIG1_OT_ENE     (1<<7)

#define REG_SDCD_CONFIG2        0x30
    #define REG_SDCD_CONFIG2_SDCD_MODE          (1<<1)
    #define REG_SDCD_CONFIG2_WT_DBCTM           (1<<3)
    #define REG_SDCD_CONFIG2_OT_DBCTM           (1<<4)
    #define REG_SDCD_CONFIG2_REF_UPDM_RELATIVE  (0x2<<5)
    #define REG_SDCD_CONFIG2_REF_UPDM_ABSOLUTE  (0x3<<5)
    #define REG_SDCD_CONFIG2_SDCD_EN            (1<<7)

#define REG_SDCD_OT_DBCNT       0x31
#define REG_SDCD_WT_DBCNT       0x32
#define REG_SDCD_LTHS_LSB       0x33
#define REG_SDCD_LTHS_MSB       0x34
#define REG_SDCD_UTHS_LSB       0x35
#define REG_SDCD_UTHS_MSB       0x36
#define REG_SELF_TEST_CONFIG1   0x37
#define REG_SELF_TEST_CONFIG2   0x38

#endif

#endif
