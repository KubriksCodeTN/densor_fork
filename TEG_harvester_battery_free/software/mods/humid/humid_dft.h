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
 * @defgroup MODS_NSS_HUMID_DFT Diversity settings
 * @ingroup MODS_NSS_HUMID
 * These 'defines' capture the diversity settings of the module. The displayed
 * values refer to the default settings. To override the default settings,
 * place the defines with their desired values in the board board_sel.h header
 * file. The compiler will pick up your defines before parsing this file.
 * @{
 */

#ifndef __HUMID_DFT_H_
#define __HUMID_DFT_H_

#ifndef HUMID_CHIP_HTU21D
    /**
     * Sensor chip selection for the
     * <em>HTU21D(F) RH/T SENSOR IC - Digital Relative Humidity sensor with Temperature output</em> from
     * <em>TYCO ELECTRONICS</em>.
     * This option is mutually exclusive with #HUMID_CHIP_SHTC3: exactly one must be non-zero.
     * Set this to a non-zero value if the board has this sensor stuffed and if the firmware must address this sensor.
     * @note only the functionality to read out the humidity is used.
     */
    #define HUMID_CHIP_HTU21D 1
#endif

#ifndef HUMID_CHIP_SHTC3
    /**
     * Sensor chip selection for the
     * <em>SHTC3 - Humidity and Temperature Sensor IC</em> from
     * <em>SENSIRION</em>
     * This option is mutually exclusive with #HUMID_CHIP_HTU21D: exactly one must be non-zero.
     * Set this to a non-zero value if the board has this sensor stuffed and if the firmware must address this sensor.
     * @note only the functionality to read out the humidity is used.
     */
    #define HUMID_CHIP_SHTC3 0
#endif

#if HUMID_CHIP_HTU21D && HUMID_CHIP_SHTC3
    #error Only one of HUMID_CHIP_HTU21D and HUMID_CHIP_SHTC3 may be set to a non-zero value.
#endif
#if !HUMID_CHIP_HTU21D && !HUMID_CHIP_SHTC3
    #error One of HUMID_CHIP_HTU21D and HUMID_CHIP_SHTC3 must be set to a non-zero value.
#endif

#ifndef HUMID_I2C_HW
    /**
     * Bus interface selection. This option is mutually exclusive with #HUMID_I2C_BB: exactly one must be defined.
     * When set to a non-zero value the I2C HW block will be used by this module for I2C communication.
     */
    #define HUMID_I2C_HW 1
#endif

#ifndef HUMID_I2C_BB
    /**
     * Bus interface selection. This option is mutually exclusive with #HUMID_I2C_HW: exactly one must be defined.
     * When set to a non-zero value the i2cbbm module will be used by this module for I2C communication.
     * @note When this define is made, the module i2cbbm must also be included in the project.
     * @note When this define is made, the pins used for communication are to be defined in the i2cbbm module.
     */
    #define HUMID_I2C_BB 0
#endif

#if HUMID_I2C_HW && HUMID_I2C_BB
    #error Only one of HUMID_I2C_HW and HUMID_I2C_BB may be set to a non-zero value.
#endif
#if !HUMID_I2C_HW && !HUMID_I2C_BB
    #error One of HUMID_I2C_HW and HUMID_I2C_BB must be set to a non-zero value.
#endif

#ifndef HUMID_POWER_PIN
    /**
     * The IOCON pin number that is used to power the humidity sensor. This pin on the NHS3100 is connected with the
     * supply pin VDD on the humidity sensor.
     */
    #define HUMID_POWER_PIN 6
#endif

#if !HUMID_PULLUP_COUNT && !defined(HUMID_PULLUPS)
    /**
     * The number of GPIO's that are used as pull-up for the I2C lines.
     * Must match the length of #HUMID_PULLUPS.
     * @note When #HUMID_I2C_BB is defined, you can define #I2CBBM_PULLUP_COUNT and #I2CBBM_PULLUPS instead.
     */
    #define HUMID_PULLUP_COUNT 0
    /**
     * The GPIO's used as pull-up for the I2C lines.
     * Refers to an array of type uint32_t with size #HUMID_PULLUP_COUNT.
     * @note There is no need to list the GPIO's used for the clock and data lines here. If these pins have pull-up
     *  capabilities, it will be used regardless of what is listed here.
     * @note When #HUMID_I2C_BB is defined, you can define #I2CBBM_PULLUP_COUNT and #I2CBBM_PULLUPS instead.
     */
    #define HUMID_PULLUPS NULL
#elif HUMID_PULLUP_COUNT && defined(HUMID_PULLUPS)
    /* OK */
#else
    #error Both HUMID_PULLUP_COUNT and HUMID_PULLUPS must be both defined or undefined.
    #error Define HUMID_PULLUPS as an array of type uint32_t and size HUMID_PULLUP_COUNT.
#endif

#endif /** @} */
