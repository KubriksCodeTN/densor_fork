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

#ifndef __HUMID_I2C_H_
#define __HUMID_I2C_H_

#include <stdint.h>
#include <stdbool.h>
#include "humid_dft.h"

#if HUMID_CHIP_HTU21D
    #define HUMID_I2C_SLAVE_ADDRESS 0x40
#else /* HUMID_CHIP_SHTC3 */
    #define HUMID_I2C_SLAVE_ADDRESS 0x70
#endif

/* ------------------------------------------------------------------------- */

/** Initialize the HW or the SW module to perform I2C. */
void Humid_I2cInit(void);

/**
 * De-initialize the HW or the SW module to perform I2C.
 */
void Humid_I2cDeInit(void);

/**
 * Read from the humidity sensor.
 * @param pData : pointer to an array to store the requested bytes
 * @param count : the number of bytes to read. @c pData must have at least this size.
 * @return success. A failure likely indicates a NACK.
 */
bool Humid_I2cRead(uint8_t * pData, unsigned int count);

/**
 * Write to the humidity sensor.
 * @param pData : pointer to an array containing bytes to write
 * @param count : the number of bytes to write. @c pData must have at least this size.
 * @return success. A failure likely indicates a NACK.
 */
bool Humid_I2cWrite(uint8_t * pData, unsigned int count);

#endif
