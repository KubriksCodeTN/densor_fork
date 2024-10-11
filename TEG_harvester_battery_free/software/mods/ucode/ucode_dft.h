/*
 * Copyright 2016-2019 NXP
 * This software is owned or controlled by NXP and may only be used strictly
 * in accordance with the applicable license terms.  By expressly accepting
 * such terms or by downloading, installing, activating and/or otherwise using
 * the software, you are agreeing that you have read, and that you agree to
 * comply with and are bound by, such license terms.  If you do not agree to
 * be bound by the applicable license terms, then you may not retain, install,
 * activate or otherwise use the software.
 */

#ifndef __UCODE_DFT_H_
#define __UCODE_DFT_H_

/**
 * @defgroup MODS_NSS_UCODE_DFT Diversity Settings
 * @ingroup MODS_NSS_UCODE
 * The UCode-I2C module module allows the application to select certain behavior through the use diversity
 * flags in the form of defines. Sensible defaults are chosen; to deviate, simply define the relevant flags and message
 * ids in your application before including this module.
 *
 * @par HW
 *  - Flags that influence the HW used by the module
 *      - #UCODE_I2C_HW
 *      - #UCODE_I2C_BB
 *
 * @par PIOs
 *  - Flags to select which PIOs are reserved for use by this module:
 *      - #UCODE_I2C_POWER_PIN
 *      - #UCODE_PULLUP_COUNT
 *      - #UCODE_PULLUPS
 *
 * @par Behavior
 *  - Flags that influence the behavior of the module
 *      - #UCODE_I2C_SLAVE_ADDRESS
 *      - #UCODE_EVENT_PRISTINE
 *
 * @{
 */

#ifndef UCODE_I2C_HW
    /**
     * Bus interface selection. This option is mutually exclusive with #UCODE_I2C_BB: exactly one must be non-zero.
     * When set to a non-zero value the I2C HW block will be used by this module for I2C communication.
     */
    #define UCODE_I2C_HW 1
#endif

#ifndef UCODE_I2C_BB
    /**
     * Bus interface selection. This option is mutually exclusive with #UCODE_I2C_HW: exactly one must be non-zero.
     * When set to a non-zero value the i2cbbm module will be used by this module for I2C communication.
     * @note When this define is made, the module i2cbbm must also be included in the project.
     */
    #define UCODE_I2C_BB 0
#endif

#if UCODE_I2C_HW && UCODE_I2C_BB
    #error Only one of UCODE_I2C_HW and UCODE_I2C_BB may be set to a non-zero value.
#endif
#if !UCODE_I2C_HW && !UCODE_I2C_BB
    #error One of UCODE_I2C_HW and UCODE_I2C_BB must be set to a non-zero value.
#endif

#ifndef UCODE_I2C_POWER_PIN
    /**
     * The IOCON pin number that is used to power the Ucode-I2C. This pin on the NHS3100 is connected with the
     * supply pin 4 (VDD) on the Ucode-I2C.
     */
    #define UCODE_I2C_POWER_PIN 6
#endif

#ifndef UCODE_I2C_SLAVE_ADDRESS
    /**
     * The 8-bit device select code for the Ucode-I2C must be @c 0b1010xxxy, where @c xxx can be modified via RF, and
     * @c y indicates Read/Write direction.
     * This define sets the 7-bit I2C slave address of the Ucode-I2C and must therefore be in the range
     * @code [0b1010000, 0b1010111] @endcode
     * This value is by default (right after production of the Ucode-I2C) @c 0x51, but the value of the three LSBs
     * (@c xxx) can be changed via the RF interface.
     */
    #define UCODE_I2C_SLAVE_ADDRESS 0x51
#endif

#if !UCODE_PULLUP_COUNT && !defined(UCODE_PULLUPS)
    /**
     * The number of GPIO's that are used as pull-up for the I2C lines.
     * Must match the length of #UCODE_PULLUPS.
     */
    #define UCODE_PULLUP_COUNT 0
    /**
     * The GPIO's used as pull-up for the I2C lines.
     * Refers to an array of type uint32_t with size #UCODE_PULLUP_COUNT.
     * @note There is no need to list the GPIO's used for the clock and data lines here. If these pins have pull-up
     *  capabilities, it will be used regardless of what is listed here.
     */
    #define UCODE_PULLUPS NULL
#elif UCODE_PULLUP_COUNT && defined(UCODE_PULLUPS)
    /* OK */
#else
    #error Both UCODE_PULLUP_COUNT and UCODE_PULLUPS must be both defined or undefined.
    #error Define UCODE_PULLUPS as an array of type uint32_t and size UCODE_PULLUP_COUNT.
#endif

#ifndef UCODE_EVENT_PRISTINE
    /**
     * Each event is represented by a single bit set and is application defined. The module only enforces the existence
     * of one event that indicates the application is running and the tag is ready for use. This bit is set at first
     * run or after reset by calling #Ucode_Reset.
     */
    #define UCODE_EVENT_PRISTINE 1
#endif

#endif /** @} */
