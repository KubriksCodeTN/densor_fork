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
#include "accel.h"
#include "accel_internal.h"

#define I2C_ADDR (0x30>>1)

#ifdef ACCEL_I2C_HW
#ifdef I2C_IRQHANDLER_ACCEL
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
#endif

void Accel_I2cInit(void)
{
#ifdef ACCEL_I2C_HW
    Chip_IOCON_SetPinConfig(NSS_IOCON, IOCON_PIO0_4, IOCON_FUNC_1 | IOCON_I2CMODE_STDFAST);
    Chip_IOCON_SetPinConfig(NSS_IOCON, IOCON_PIO0_5, IOCON_FUNC_1 | IOCON_I2CMODE_STDFAST);
    Chip_SysCon_Peripheral_AssertReset(SYSCON_PERIPHERAL_RESET_I2C0);
    Chip_SysCon_Peripheral_DeassertReset(SYSCON_PERIPHERAL_RESET_I2C0);
    Chip_I2C_Init(I2C0);
    Chip_I2C_SetClockRate(I2C0, 100000); /* 100k */
    Chip_I2C_SetMasterEventHandler(I2C0, Chip_I2C_EventHandler);
    NVIC_EnableIRQ(I2C0_IRQn);
#endif
}

void Accel_I2cDeInit(void)
{
#ifdef ACCEL_I2C_HW
    NVIC_DisableIRQ(I2C0_IRQn);
    Chip_I2C_DeInit(I2C0);
    Chip_SysCon_Peripheral_AssertReset(SYSCON_PERIPHERAL_RESET_I2C0);
#endif
}

uint8_t Accel_I2cReadReg(uint8_t reg)
{
    uint8_t val;
    if (Chip_I2C_MasterCmdRead(I2C0, I2C_ADDR, &reg, &val, 1) != 1) {
        val = 0;
    }
    return val;
}

bool Accel_I2cWriteReg(uint8_t reg, uint8_t val)
{
    uint8_t buf[2] = {reg, val};
    bool success = Chip_I2C_MasterSend(I2C0, I2C_ADDR, buf, 2) == 2;
    return success;
}
