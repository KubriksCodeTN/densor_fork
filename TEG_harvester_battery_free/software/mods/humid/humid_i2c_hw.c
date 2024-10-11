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

#include <string.h>
#include "board.h"
#include "humid_i2c.h"

#if HUMID_I2C_HW

#ifdef I2C_IRQHANDLER_HUMID
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

void Humid_I2cInit(void)
{
    /* Configure PIOs for I2C operation. */
    Chip_IOCON_SetPinConfig(NSS_IOCON, IOCON_PIO0_4, IOCON_FUNC_1);
    Chip_IOCON_SetPinConfig(NSS_IOCON, IOCON_PIO0_5, IOCON_FUNC_1);

    /* Setup I2C block. */
    Chip_SysCon_Peripheral_AssertReset(SYSCON_PERIPHERAL_RESET_I2C0);
    Chip_SysCon_Peripheral_DeassertReset(SYSCON_PERIPHERAL_RESET_I2C0);
    Chip_I2C_Init(I2C0);
    Chip_I2C_SetClockRate(I2C0, 100000);
    Chip_I2C_SetMasterEventHandler(I2C0, Chip_I2C_EventHandler);
    NVIC_EnableIRQ(I2C0_IRQn);
}

void Humid_I2cDeInit(void)
{
    NVIC_DisableIRQ(I2C0_IRQn);
    Chip_I2C_DeInit(I2C0);
    Chip_SysCon_Peripheral_AssertReset(SYSCON_PERIPHERAL_RESET_I2C0);
}

bool Humid_I2cRead(uint8_t * pData, unsigned int count)
{
    I2C_XFER_T xfer;
    xfer.slaveAddr = HUMID_I2C_SLAVE_ADDRESS;
    xfer.txBuff = NULL;
    xfer.txSz = 0;
    xfer.rxBuff = pData;
    xfer.rxSz = (int)count;

    I2C_STATUS_T status = Chip_I2C_MasterTransfer(I2C0, &xfer);
    return status == I2C_STATUS_DONE;
}

bool Humid_I2cWrite(uint8_t * pData, unsigned int count)
{
    I2C_XFER_T xfer;
    xfer.slaveAddr = HUMID_I2C_SLAVE_ADDRESS;
    xfer.txBuff = pData;
    xfer.txSz = (int)count;
    xfer.rxBuff = 0;
    xfer.rxSz = 0;

    I2C_STATUS_T status = Chip_I2C_MasterTransfer(I2C0, &xfer);
    return status == I2C_STATUS_DONE;
}

#endif
