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

#include "chip.h"
#include "humid.h"
#include "humid_i2c.h"

#if HUMID_CHIP_HTU21D

/** The 8-bit commands supported by the HTU21D. Fetched from HPP845E131R4ENG_DS_HPC199_6_A6.pdf */
typedef enum COMMMAND {
    COMMAND_SOFT_RESET = 0xFE,
    COMMAND_READ_USER_REGISTER = 0xE7,
    COMMAND_WRITE_USER_REGISTER = 0xE6,
    COMMAND_TRIGGER_HUMIDITY_MEASUREMENT = 0xF5,
    COMMAND_TRIGGER_TEMPERATURE_MEASUREMENT = 0xF3,
    COMMAND_TRIGGER_HUMIDITY_MEASUREMENT_HOLD_MASTER = 0xE5,
    COMMAND_TRIGGER_TEMPERATURE_MEASUREMENT_HOLD_MASTER = 0xE3,

    COMMAND_COUNT = 7 /** Number of possible commands. Not to be used as a possible command. Use this in for loops or to define array sizes. */
} COMMMAND_T;

/**
 * Maximum waiting time between issuing #COMMAND_TRIGGER_HUMIDITY_MEASUREMENT and retrieving the result.
 * @{
 */
#define HUMIDITY_WAIT_TIME_12BITS 16 /* ms */
#define HUMIDITY_WAIT_TIME_11BITS 8 /* ms */
#define HUMIDITY_WAIT_TIME_10BITS 5 /* ms */
#define HUMIDITY_WAIT_TIME_8BITS 3 /* ms */
/** @} */
#if HUMID_MEASURE_WAIT_TIME != HUMIDITY_WAIT_TIME_12BITS
    #error Unexpected value for HUMID_MEASURE_WAIT_TIME
#endif

static unsigned int Convert(int raw);

/* ------------------------------------------------------------------------- */

static unsigned int Convert(int raw)
{
    /* Formula: RH = -6 + 125 * (raw / 2**16)
     * Reworking gives: deci-relative humidity = -60 + (10 * 125 * raw) >> 16
     * Overflow cannot occur:
     * - raw max value (corresponding to 100% humidity): 55574.528
     * - max intermediate value: 0x4240000
     */
    int drh = -60 + ((10 * 125 * (int)raw) >> 16);
    if (drh < 0) {
        drh = 0;
    } else if (drh > 1000) {
        drh = 1000;
    }
    return (unsigned int)drh;
}

/* ------------------------------------------------------------------------- */

void Humid_Reset(void)
{
    uint8_t command = COMMAND_SOFT_RESET;
    Humid_I2cWrite(&command, 1);
}

void Humid_Measure(void)
{
    /* COMMAND_WRITE_USER_REGISTER is not needed: default measurement resolution (bits 7, 0) is 00 -> 12 bits. */
    uint8_t command = COMMAND_TRIGGER_HUMIDITY_MEASUREMENT;
    Humid_I2cWrite(&command, 1);
}

unsigned int Humid_Get(void)
{
    uint8_t response[3];
    Humid_I2cRead(response, 3);

    int raw = ((int)response[0] << 8) | ((int)response[1] & 0xFC);
    unsigned int drh = Convert(raw);
    return drh;
}

#endif
