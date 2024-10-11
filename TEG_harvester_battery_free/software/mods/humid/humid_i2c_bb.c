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

#include <string.h>
#include "board.h"
#include "i2cbbm/i2cbbm.h"
#include "humid_i2c.h"
#include "humid_dft.h"

#if HUMID_I2C_BB

void Humid_I2cInit(void)
{
    I2cbbm_Init();
    I2cbbm_SetAddress(HUMID_I2C_SLAVE_ADDRESS);
}

void Humid_I2cDeInit(void)
{
    I2cbbm_DeInit();
}

bool Humid_I2cRead(uint8_t * pData, unsigned int count)
{
    int retval = I2cbbm_Read(pData, count);
    return retval == (int)count;
}

bool Humid_I2cWrite(uint8_t * pData, unsigned int count)
{
    int retval = I2cbbm_Write(pData, count);
    return retval == (int)count;
}

#endif
