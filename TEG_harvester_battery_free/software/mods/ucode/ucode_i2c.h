/*
 * Copyright 2017-2018 NXP
 * This software is owned or controlled by NXP and may only be used strictly
 * in accordance with the applicable license terms.  By expressly accepting
 * such terms or by downloading, installing, activating and/or otherwise using
 * the software, you are agreeing that you have read, and that you agree to
 * comply with and are bound by, such license terms.  If you do not agree to
 * be bound by the applicable license terms, then you may not retain, install,
 * activate or otherwise use the software.
 */

#ifndef __UCODE_I2C_H_
#define __UCODE_I2C_H_

#include <stdint.h>
#include <stdbool.h>

/** Initialize the HW or the SW module to perform I2C. */
void Ucode_I2cInit(void);

/**
 * De-initialize the HW or the SW module to perform I2C.
 */
void Ucode_I2cDeInit(void);

/**
 * Read 1 word from the UCode at the the given memory address offset.
 * @param address The location in the UHF EEPROM to read from. The address includes the two-bit value indicating the
 *  memory bank value.
 * @param pWord The location to store the retrieved word.
 * @return success. A failure likely indicates a NACK.
 */
bool Ucode_I2cRead(uint16_t address, uint16_t * pWord);

/**
 * Write 1 word to the UCode at the the given memory address offset.
 * @param address The location in the UHF EEPROM to write to. The address includes the two-bit value indicating the
 *  memory bank value.
 * @param word The value to write.
 * @return success. A failure likely indicates a NACK.
 */
bool Ucode_I2cWrite(uint16_t address, uint16_t word);

#endif
