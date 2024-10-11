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
#include "ucode_i2c.h"
#include "ucode_dft.h"

#if UCODE_I2C_HW

#ifdef I2C_IRQHANDLER_UCODE
void I2C0_IRQHandler(void)
{
    if (Chip_I2C_IsMasterActive(I2C0)) {
        Chip_I2C_MasterStateHandler(I2C0);
    }
    else {
        Chip_I2C_SlaveStateHandler(I2C0);
    }
}
#endif

/* ------------------------------------------------------------------------- */

void Ucode_I2cInit(void)
{
    /* Configure PIOs for I2C operation. */
    Chip_IOCON_SetPinConfig(NSS_IOCON, IOCON_PIO0_4, IOCON_FUNC_1);
    Chip_IOCON_SetPinConfig(NSS_IOCON, IOCON_PIO0_5, IOCON_FUNC_1);

    /* Setup I2C block. */
    Chip_SysCon_Peripheral_DeassertReset(SYSCON_PERIPHERAL_RESET_I2C0);
    Chip_I2C_Init(I2C0);
    Chip_I2C_SetClockRate(I2C0, 100000);
    Chip_I2C_SetMasterEventHandler(I2C0, Chip_I2C_EventHandler);
    NVIC_EnableIRQ(I2C0_IRQn);
}

void Ucode_I2cDeInit(void)
{
    NVIC_DisableIRQ(I2C0_IRQn);
    Chip_I2C_DeInit(I2C0);
    Chip_SysCon_Peripheral_AssertReset(SYSCON_PERIPHERAL_RESET_I2C0);
}

bool Ucode_I2cRead(uint16_t address, uint16_t * pWord)
{
    uint8_t data[2];
    data[0] = (uint8_t)((address >> 8) & 0xFF);
    data[1] = (uint8_t)(address & 0xFF);

    I2C_XFER_T xfer;
    xfer.slaveAddr = UCODE_I2C_SLAVE_ADDRESS;
    xfer.txBuff = data;
    xfer.txSz = sizeof(data);
    xfer.rxBuff = data;
    xfer.rxSz = sizeof(data);

    /* The UCode-I2C timing constraints are unknown, but need quite some time to handle a previous transaction and be
     * prepared for the next: up to a few ms (perhaps the time required by the EEPROM in the UCode-I2C to cache the
     * data?), but it is unknown what the variation is and where it depends upon.
     * To be sure, we try multiple times, each time waiting some time; in the end, attempts have been made for a period
     * longer than 10 msec.
     */
    I2C_STATUS_T status;
    int tries = 11; /* Any reasonable small value should do. */
    do {
        status = Chip_I2C_MasterTransfer(I2C0, &xfer);
        if (status == I2C_STATUS_DONE) {
            tries = 0;
        }
        else {
            tries--;
            Chip_Clock_System_BusyWait_ms(3); /* Just ensure that this number * total tries > 10 msec. */
        }
    } while (tries);

    *pWord = (uint16_t)((data[0] << 8) | data[1]);
    return status == I2C_STATUS_DONE;
}

bool Ucode_I2cWrite(uint16_t address, uint16_t word)
{
    uint8_t data[4];
    data[0] = (uint8_t)((address >> 8) & 0xFF);
    data[1] = (uint8_t)(address & 0xFF);
    data[2] = (uint8_t)((word >> 8) & 0xFF);
    data[3] = (uint8_t)(word & 0xFF);

    I2C_XFER_T xfer;
    xfer.slaveAddr = UCODE_I2C_SLAVE_ADDRESS;
    xfer.txBuff = data;
    xfer.txSz = sizeof(data);
    xfer.rxBuff = 0;
    xfer.rxSz = 0;

    /* The UCode-I2C timing constraints are unknown, but need quite some time to handle a previous transaction and be
     * prepared for the next: around 5 ms (likely the time required by the EEPROM in the UCode-I2C to flush the data),
     * but it is unknown what the variation is and where it depends upon.
     * To be sure, we try multiple times, each time waiting some time; in the end, attempts have been made for a period
     * longer than 10 msec.
     */
    I2C_STATUS_T status;
    int tries = 11; /* Any reasonable small value should do. */
    do {
        status = Chip_I2C_MasterTransfer(I2C0, &xfer);
        if (status == I2C_STATUS_DONE) {
            tries = 0;
        }
        else {
            tries--;
            Chip_Clock_System_BusyWait_ms(3); /* Just ensure that this number * total tries > 10 msec. */
        }
    } while (tries);
    return status == I2C_STATUS_DONE;
}

#endif
