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
 * @defgroup MODS_NSS_ACCEL_DFT Diversity settings
 * @ingroup MODS_NSS_ACCEL
 *  These 'defines' capture the diversity settings of the module. The displayed values refer to the default
 *  settings. To override the default settings, place the defines with their desired values in the board
 *  board_sel.h header file. The compiler will pick up your defines before parsing this file.
 *
 * @{
 */
#ifndef __ACCEL_DFT_H_
#define __ACCEL_DFT_H_

/** Defines the (currently only) supported accelerometer sensor. */
#define ACCEL_CHIP_FXLS8972CF

/** Defines the (currently only) supported I2C driver. */
#define ACCEL_I2C_HW

/* ------------------------------------------------------------------------- */

#ifndef ACCEL_SHOCK_ENABLE
    /**
     * When defined to a non-zero value, shock detection can be enabled via #Accel_Start.
     * When undefined or 0, all shock configurations and requests for enabling or disabling them are silently ignored.
     */
    #define ACCEL_SHOCK_ENABLE 1
#endif

#ifndef ACCEL_SHAKE_ENABLE
    /**
     * When defined to a non-zero value, shake detection can be enabled via #Accel_Start.
     * When undefined or 0, all shake configurations and requests for enabling or disabling them are silently ignored.
     */
    #define ACCEL_SHAKE_ENABLE 1
#endif

#ifndef ACCEL_VIBRATION_ENABLE
    /**
     * When defined to a non-zero value, vibration detection can be enabled via #Accel_Start.
     * When undefined or 0, all vibration configurations and requests for enabling or disabling them are silently ignored.
     */
    #define ACCEL_VIBRATION_ENABLE 1
#endif

#ifndef ACCEL_TILT_ENABLE
    /**
     * When defined to a non-zero value, tilt detection can be enabled via #Accel_Start.
     * When undefined or 0, all tilt configurations and requests for enabling or disabling them are silently ignored.
     */
    #define ACCEL_TILT_ENABLE 1
#endif

#endif /** @} */
