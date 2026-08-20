/**
 ******************************************************************************
 * @file           : tmp117.c
 * @brief          : This file contains the implementation of a barebone
 * 					 tmp117 driver functions.
 ******************************************************************************
 */

#include "tmp117.h"

HAL_StatusTypeDef tmp117WriteReg(I2C_HandleTypeDef* i2c, uint8_t reg_addr, uint16_t reg_val) {
	uint8_t buff[2];
	buff[0] = (reg_val >> 8);
	buff[1] = (reg_val & 0xFF);

	// transmit the data to the sensor
	HAL_StatusTypeDef ret = HAL_I2C_Mem_Write(i2c, TMP117_ADDR << 1, reg_addr, I2C_MEMADD_SIZE_8BIT, buff, 2, 100);
	return ret;
}

HAL_StatusTypeDef tmp117ReadReg(I2C_HandleTypeDef* i2c, uint8_t reg_addr, uint8_t* reg_val) {
	HAL_StatusTypeDef ret_val = HAL_I2C_Mem_Read(i2c, TMP117_ADDR << 1, reg_addr, I2C_MEMADD_SIZE_8BIT, reg_val, 2, 100);

	return ret_val;
}
