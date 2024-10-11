#include "vext.h"
#include "acc.h"
#include "board.h"

/**
 * @brief Function for enabling the External supply rail 'VEXT' to power sensors
 *
 * This function uses a CS resistor to pre-charge the caps on VEXT
 */
void VEXT_On()
{
	/* Un-ground the Analog pad connected to VEXT */
    Chip_IOCON_UngroundAnabus(NSS_IOCON, (1 << (IOCON_ANA0_5 - IOCON_ANA0_0)));
    Chip_IOCON_SetPinConfig(NSS_IOCON, IOCON_ANA0_5, IOCON_FUNC_0 | IOCON_RMODE_INACT);

    /* Set VEXT as an input without pull-ups or pull-downs */
    Chip_IOCON_SetPinConfig(NSS_IOCON, IOCON_PIO0_7, IOCON_FUNC_0 | IOCON_RMODE_INACT);
    Chip_GPIO_SetPinDIRInput(NSS_GPIO, 0, IOCON_PIO0_7);

    /* Set the pin connected to CS_MEM as an input without pull-ups or pull-downs */
    Chip_IOCON_SetPinConfig(NSS_IOCON, IOCON_PIO0_5, IOCON_FUNC_0 | IOCON_RMODE_INACT);
    Chip_GPIO_SetPinDIRInput(NSS_GPIO, 0, IOCON_PIO0_5);

    /* Set ACC Chip Select High */
	Chip_GPIO_SetPinDIROutput(NSS_GPIO, 0, ACC_CS_PIN);
	Chip_GPIO_SetPinOutHigh(NSS_GPIO, 0, ACC_CS_PIN);

    /* Configure CS_MEM as output an high to start pre-charge */
    /* Can be left high, since CS is active low */
    Chip_GPIO_SetPinDIROutput(NSS_GPIO, 0, IOCON_PIO0_3);
    Chip_GPIO_SetPinOutHigh(NSS_GPIO, 0, IOCON_PIO0_3);

    /* Wait for the precharge time */
	Chip_Clock_System_BusyWait_ms(10);

    /* Set VEXT as an output and turn it on */
    Chip_IOCON_SetPinConfig(NSS_IOCON, IOCON_PIO0_7, IOCON_FUNC_0 | IOCON_DDRIVE_ULTRAHIGH);
    Chip_GPIO_SetPinDIROutput(NSS_GPIO, 0, IOCON_PIO0_7);
    Chip_GPIO_SetPinOutHigh(NSS_GPIO, 0, IOCON_PIO0_7);
}

/**
 * @brief Function for disabling the External supply rail 'VEXT' to power sensors
 *
 * This function a specific pattern to quickly discharge the VEXT caps
 */
void VEXT_Off(void)
{
	/* Set as input to allow for discharging via pulls? */
	Chip_GPIO_SetPinDIRInput(NSS_GPIO, 0, IOCON_PIO0_7);

	/* Wait for the caps to discharge */
	Chip_Clock_System_BusyWait_ms(5);

    Chip_IOCON_SetPinConfig(NSS_IOCON, IOCON_PIO0_5, IOCON_FUNC_0 | BOARD_PIO5_PULL);

    /* Set CS_MEM as input */
	Chip_IOCON_SetPinConfig(NSS_IOCON, IOCON_PIO0_3, IOCON_FUNC_0 | BOARD_PIO3_PULL);
	Chip_GPIO_SetPinDIRInput(NSS_GPIO, 0, IOCON_PIO0_3);

	/* Set ACC Chip Select as input*/
	Chip_IOCON_SetPinConfig(NSS_IOCON, ACC_CS_PIN, IOCON_FUNC_0 | BOARD_PIO3_PULL);
	Chip_GPIO_SetPinDIRInput(NSS_GPIO, 0, ACC_CS_PIN);

	/* Set VEXT as a output and low */
	Chip_GPIO_SetPinDIROutput(NSS_GPIO, 0, IOCON_PIO0_7);
    Chip_GPIO_SetPinOutLow(NSS_GPIO, 0, IOCON_PIO0_7);

	/* Ground Analog Pin 5*/
	Chip_IOCON_GroundAnabus(NSS_IOCON, (1 << (IOCON_ANA0_5 - IOCON_ANA0_0)));
}
