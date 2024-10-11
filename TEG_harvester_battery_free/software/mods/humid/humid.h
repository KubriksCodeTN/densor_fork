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

#ifndef __HUMID_H_
#define __HUMID_H_

/**
 * @defgroup MODS_NSS_HUMID humid: Humidity sensor module
 * @ingroup MODS_NSS
 *
 * This module abstracts away an external humidity sensor, and provides a high level API to easily measure the humidity.
 * The API ensures an easy way to instruct the humidity sensor to gauge the current humidity. The API is non-blocking,
 * enabling parallel sensing of multiple environmental factors. See #HUMID_RESET_WAIT_TIME and #HUMID_MEASURE_WAIT_TIME
 *
 * @note The module is prepared to allow to change the type of the external humidity sensor without affecting its API
 *  or functionality. The chosen external humidity sensors would then be selected as a diversity setting. <em>This is
 *  currently not implemented: only once external humidity sensor is currently supported: see #HUMID_CHIP_HTU21D</em>.
 *
 * @warning This module assumes it is the only block accessing the external humidity sensor. Any other access through
 *  other unrelated code, both during, before or after the use of this module, may result in unpredictable behavior.
 *
 * @par Diversity
 *  This module supports diversity, such as the choice between the supported humidity sensors, and the bus interface
 *  selection - which allows a more flexible layout, as the humidity sensor can be connected to any two available pins.
 *  Check @ref MODS_NSS_HUMID_DFT for all diversity parameters.
 *
 * @{
 */

#include "humid/humid_dft.h"

/** The time to wait after calling #Humid_Init, in milli-seconds, to ensure the humidity sensor is ready to use. */
#define HUMID_RESET_WAIT_TIME 15 /* ms. The maximum time per the datasheet to reach the idle state. */

/** The time to wait between the calls #Humid_Measure and #Humid_Get, in milli-seconds. */
#define HUMID_MEASURE_WAIT_TIME 16 /* ms. The maximum measuring time per the datasheet at maximum resolution. */

/* ------------------------------------------------------------------------- */

/**
 * This function must be the first function to call in this module after going to deep power down or power-off power
 * save mode. Not calling this function will result in a hang.
 * @pre The I2C driver is not in use: #Chip_I2C_Init has not yet been called, or #Chip_I2C_DeInit was the last call to
 *  this driver.
 * @post The I2C driver is in exclusive use by the humid module
 * @post When this call returns, #HUMID_RESET_WAIT_TIME ms must be waited before any other call may be attempted.
 *  This is @b not checked for.
 */
void Humid_Init(void);

/**
 * De-initializes the humidity sensor and the I2C driver.
 * @post The I2C driver is not claimed anymore.
 * @post All pins listed in #HUMID_PULLUPS as reconfigured as output, outputting low.
 * @post The humidity sensor is powered off.
 */
void Humid_DeInit(void);

/**
 * Soft-resets the humidity sensor.
 * @post When this call returns, #HUMID_RESET_WAIT_TIME ms must be waited before any other call may be attempted.
 *  This is @b not checked for.
 */
void Humid_Reset(void);

/**
 * Start a humidity measurement.
 * @post When this call returns, #HUMID_MEASURE_WAIT_TIME ms must be waited before the result can be retrieved via
 *  #Humid_Get - this is @b not checked for.
 */
void Humid_Measure(void);

/**
 * @pre #Humid_Measure has been called #HUMID_MEASURE_WAIT_TIME ms or longer ago.
 * Read out the measured humidity data.
 * @return the relative humidity in deci-percentages. e.g. 548 to indicate a relative humidity of 54.8%
 */
unsigned int Humid_Get(void);

#endif /** @} */
