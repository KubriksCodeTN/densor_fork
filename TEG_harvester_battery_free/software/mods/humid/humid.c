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

#include "board.h"
#include "humid.h"
#include "humid_i2c.h"

#if HUMID_PULLUP_COUNT
static uint32_t sPullups[UCODE_PULLUP_COUNT] = HUMID_PULLUPS;
#endif

void Humid_Init(void)
{
    Humid_I2cInit();

#if HUMID_PULLUP_COUNT
    /* Configure pull-ups. */
    uint32_t pinMask = 0;
    for (int i = 0; i < HUMID_PULLUP_COUNT; i++) {
        Chip_IOCON_SetPinConfig(NSS_IOCON, sPullups[i], IOCON_FUNC_0 | IOCON_RMODE_PULLUP);
        pinMask |= (uint32_t)NSS_GPIOn_PINMASK(sPullups[i]);
    }
    if (pinMask) {
        Chip_GPIO_SetPortDIRInput(NSS_GPIO, 0, pinMask);
    }
#endif

    /* Power up humidity sensor. Use the pull-up to slowly charge the external capacitor. */
    Chip_IOCON_SetPinConfig(NSS_IOCON, HUMID_POWER_PIN, IOCON_FUNC_0 | IOCON_RMODE_PULLUP);
    Chip_Clock_System_BusyWait_ms(5); /* OK for HTU21D, NOT checked for SHTC3 */
    Chip_GPIO_SetPinDIROutput(NSS_GPIO, 0, HUMID_POWER_PIN);
    Chip_GPIO_SetPinState(NSS_GPIO, 0, HUMID_POWER_PIN, true);
}

void Humid_DeInit(void)
{
    Humid_I2cDeInit();

#if HUMID_PULLUP_COUNT
    /* Re-configure pull-ups. */
    uint32_t pinMask = 0;
    for (int i = 0; i < HUMID_PULLUP_COUNT; i++) {
        Chip_IOCON_SetPinConfig(NSS_IOCON, sPullups[i], IOCON_FUNC_0 | IOCON_RMODE_INACT);
        pinMask |= (uint32_t)NSS_GPIOn_PINMASK(sPullups[i]);
    }
    Chip_GPIO_SetPortDIROutput(NSS_GPIO, 0, pinMask);
    Chip_GPIO_SetPortOutLow(NSS_GPIO, 0, pinMask);
#endif

    Chip_GPIO_SetPinDIRInput(NSS_GPIO, 0, HUMID_POWER_PIN);
    Chip_IOCON_SetPinConfig(NSS_IOCON, HUMID_POWER_PIN, IOCON_FUNC_0 | IOCON_RMODE_PULLDOWN);
}
