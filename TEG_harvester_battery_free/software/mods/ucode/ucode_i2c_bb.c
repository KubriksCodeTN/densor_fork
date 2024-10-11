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

#include <string.h>
#include "board.h"
#include "i2cbbm/i2cbbm.h"
#include "ucode_i2c.h"
#include "ucode_dft.h"

#if UCODE_I2C_BB

#if UCODE_I2C_SLAVE_ADDRESS != I2CBBM_DEFAULT_I2C_ADDRESS
    #warning UCODE_I2C_SLAVE_ADDRESS has a different value than I2CBBM_DEFAULT_I2C_ADDRESS
    #warning The value of UCODE_I2C_SLAVE_ADDRESS will not be used.
#endif

void Ucode_I2cInit(void)
{
    I2cbbm_Init();
}

void Ucode_I2cDeInit(void)
{
    I2cbbm_DeInit();
}

bool Ucode_I2cRead(uint16_t address, uint16_t * pWord)
{
    uint8_t data[2];
    data[0] = (uint8_t)((address >> 8) & 0xFF);
    data[1] = (uint8_t)(address & 0xFF);

    /* The UCode-I2C timing constraints are unknown, but need quite some time to handle a previous transaction and be
     * prepared for the next: up to a few ms (perhaps the time required by the EEPROM in the UCode-I2C to cache the
     * data?), but it is unknown what the variation is and where it depends upon.
     * To be sure, we try multiple times, each time waiting some time; in the end, attempts have been made for a period
     * longer than 10 msec.
     */
    int retval;
    int tries = 11; /* Any reasonable small value should do. */
    do {
        retval = I2cbbm_WriteRead(data, sizeof(data), (uint8_t *)pWord, sizeof(uint16_t));
        if (retval == sizeof(uint16_t)) {
            tries = 0;
        }
        else {
            tries--;
            Chip_Clock_System_BusyWait_ms(3); /* Just ensure that this number * total tries > 10 msec. */
        }
    } while (tries);

    *pWord = (*pWord >> 8) | (uint16_t)(*pWord << 8); /* Swap bytes */
    return retval == sizeof(uint16_t);
}

bool Ucode_I2cWrite(uint16_t address, uint16_t word)
{
    uint8_t data[4];
    data[0] = (uint8_t)((address >> 8) & 0xFF);
    data[1] = (uint8_t)(address & 0xFF);
    data[2] = (uint8_t)((word >> 8) & 0xFF);
    data[3] = (uint8_t)(word & 0xFF);

    /* The UCode-I2C timing constraints are unknown, but need quite some time to handle a previous transaction and be
     * prepared for the next: around 5 ms (likely the time required by the EEPROM in the UCode-I2C to flush the data?),
     * but it is unknown what the variation is and where it depends upon.
     * To be sure, we try multiple times, each time waiting some time; in the end, attempts have been made for a total
     * period longer than 10 msec.
     */
    int retval;
    int tries = 11; /* Any reasonable small value should do. */
    do {
        retval = I2cbbm_Write(data, sizeof(data));
        if (retval == sizeof(data)) {
            tries = 0;
        }
        else {
            tries--;
            Chip_Clock_System_BusyWait_ms(3); /* Just ensure that this number * total tries > 10 msec. */
        }
    } while (tries);

    return retval == sizeof(data);
}

#endif
