/**
 ******************************************************************************
 * @file           : tmp117.h
 * @brief          : Header for tmp117.c file.
 *                   This file contains a barebone tmp117 driver.
 ******************************************************************************
 */

// #pragma once // non standard I guess?

#ifndef __TMP117_H__
#define __TMP117_H__

#include "stm32l0xx_hal.h"
#include <stdint.h>

/**
 * @brief TMP117 I2C address.
 *
 * @note If TMP117_I2C_ADDR is not defined at compile time,
 *       the default address is (0x48.
 *       you can use the -D directive to override it.
 *       This would not work with multiple TMP117s but
 *       it's good enough for now.
 *       NB this is the unshifted addr
 */
#ifndef TMP117_ADDR
#define TMP117_ADDR 0x48
#endif

/**
 * sensor registers
 */
// config register
#define TMP117_CONFIG_REG	   0x01

// temp result register
#define TMP117_TEMP_RES_REG    0x00
// temp high limit register
#define TMP117_THIGH_LIM_REG   0x02
// temp low limit register
#define TMP117_TLOW_LIM_REG	   0x03

// EEPROM unlock register
#define TMP117_EEPROM_UL_REG   0x04
// EEPROM1 register
#define TMP117_EEPROM1_REG	   0x05
// EEPROM2 register
#define TMP117_EEPROM2_REG	   0x06
// EEPROM3 register
#define TMP117_EEPROM3_REG	   0x08

// device ID register
#define TMP117_DEVICE_ID_REG   0x0F
// temp offset register
#define TMP117_TEMP_OFFSET_REG 0x07

/**
 * NB: if needed, with these 2 primitives the driver could be easily extended for
 * some QOL functions such as setConfig or getTemp or maybe some EEPROM checks
 * but the usage in this firmware they're not needed :^)
 */

/**
 * @brief write a 16-bit register
 *
 * @param i2c I2C handler
 * @param reg_addr register address
 * @param reg_val register value to write (big endian conversion handled internally).
 * @return TMP117_OK on success, otherwise an error code
 */
HAL_StatusTypeDef tmp117WriteReg(I2C_HandleTypeDef* i2c, uint8_t reg_addr, uint16_t reg_val);

/**
 * @brief read a 16-bit register value
 *
 * @param i2c I2C handler
 * @param reg_addr register address
 * @param reg_val[out] read register value (needs to be at least 2 size)
 * @return TMP117_OK on success, otherwise an error code
 *
 * @note reg_value[out] must point to a buffer with at least 2 available bytes allocated
 *       (dest_buf[0] = MSB, dest_buf[1] = LSB), if reading temperature registers,
 *       you can cast int16_t to preserve negative values in 2's complement,
 *       floating-point scaling (78125e-3) should be offloaded to the host/app.
*/
HAL_StatusTypeDef tmp117ReadReg(I2C_HandleTypeDef* i2c, uint8_t reg_addr, uint8_t* reg_val);

#endif
