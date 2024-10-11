#include <spi.h>
#include "board.h"

/**
 * @brief Configure the SPI bus
 */
void SPI_Init(){
	/* Set PIO2 as a input without pull-up, a different pin is used instead */
    Chip_IOCON_SetPinConfig(NSS_IOCON, IOCON_PIO0_2, IOCON_FUNC_0 | IOCON_RMODE_INACT);

    /* Unground the pins used for SCLK */
    Chip_IOCON_UngroundAnabus(NSS_IOCON, (1 << (IOCON_ANA0_2 - IOCON_ANA0_0)));
    Chip_IOCON_UngroundAnabus(NSS_IOCON, (1 << (IOCON_ANA0_3 - IOCON_ANA0_0)));

    /* Init the MOSI, MISO and SCLK pins*/
    Chip_IOCON_SetPinConfig(NSS_IOCON, IOCON_PIO0_6, IOCON_FUNC_1);
    Chip_IOCON_SetPinConfig(NSS_IOCON, IOCON_PIO0_8, IOCON_FUNC_1);
    Chip_IOCON_SetPinConfig(NSS_IOCON, IOCON_PIO0_9, IOCON_FUNC_1);

    /* Raise the system clock. Keep the software defined limits in mind*/
//    Chip_Clock_System_SetClockFreq(200000);

    /* Start the SPI Module */
    Chip_SSP_Init(NSS_SSP0);
    Chip_SSP_SetMaster(NSS_SSP0, true);
    /* Mode 3 is supported by both the IMU and Memory used in V200 and V300 Densors */
    Chip_SSP_SetFormat(NSS_SSP0, SSP_BITS_8, SSP_FRAME_FORMAT_SPI, SSP_CLOCK_MODE3);
    Chip_SSP_SetBitRate(NSS_SSP0, 100000);
    Chip_SSP_Enable(NSS_SSP0);

}

/**
 * @brief DeInit the SPI bus
 */
void SPI_DeInit(){
	/* Disable and deinit the SPI driver */
	Chip_SSP_Disable(NSS_SSP0);
	Chip_SSP_DeInit(NSS_SSP0);

	/* Return the system clock to the regular speed*/
    Chip_Clock_System_SetClockFreq(REG_SYSTEMCLOCK);

    /* Return all SPI related pins to their default state */
    Chip_IOCON_SetPinConfig(NSS_IOCON, IOCON_PIO0_2, IOCON_FUNC_0 | BOARD_PIO2_PULL);
    Chip_IOCON_SetPinConfig(NSS_IOCON, IOCON_PIO0_6, IOCON_FUNC_0 | BOARD_PIO6_PULL);
    Chip_IOCON_SetPinConfig(NSS_IOCON, IOCON_PIO0_8, IOCON_FUNC_0 | BOARD_PIO8_PULL);
    Chip_IOCON_SetPinConfig(NSS_IOCON, IOCON_PIO0_9, IOCON_FUNC_0 | BOARD_PIO9_PULL);

    /* Ground the pins used for SCLK */
    Chip_IOCON_GroundAnabus(NSS_IOCON, (1 << (IOCON_ANA0_2 - IOCON_ANA0_0)));
    Chip_IOCON_GroundAnabus(NSS_IOCON, (1 << (IOCON_ANA0_3 - IOCON_ANA0_0)));
}


/**
 * @brief Write to the SPI bus
 */
void SPI_Write(uint8_t cs_pin, uint8_t *wdata, uint8_t wlen){
	Chip_GPIO_SetPinOutLow(NSS_GPIO, 0, cs_pin);		/* Select a chip */

	Chip_SSP_WriteFrames_Blocking(NSS_SSP0, wdata, wlen);
	Chip_GPIO_SetPinOutHigh(NSS_GPIO, 0, cs_pin);		/* Release the chip */
}

/**
 * @brief Write-then-Read from the SPI bus
 */
void SPI_Write_Read(uint8_t cs_pin, uint8_t *wdata, uint8_t wlen, uint8_t *rdata, uint8_t rlen){
	Chip_GPIO_SetPinOutLow(NSS_GPIO, 0, cs_pin);		/* Select a chip */

	Chip_SSP_WriteFrames_Blocking(NSS_SSP0, wdata, wlen);
	Chip_SSP_ReadFrames_Blocking(NSS_SSP0, rdata, rlen);

	Chip_GPIO_SetPinOutHigh(NSS_GPIO, 0, cs_pin);		/* Release the chip */
}

