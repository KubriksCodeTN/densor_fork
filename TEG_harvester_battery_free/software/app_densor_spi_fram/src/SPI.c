#include "SPI.h"
#include "board.h"


void SPI_Init(){
	/* Set PIO2 as a input without pull-up, PIO5 is used as CS instead */
    Chip_IOCON_SetPinConfig(NSS_IOCON, IOCON_PIO0_2, IOCON_FUNC_0 | IOCON_RMODE_INACT);

    /* Unground the pins used for SCLK */
    Chip_IOCON_UngroundAnabus(NSS_IOCON, (1 << (IOCON_ANA0_2 - IOCON_ANA0_0)));
    Chip_IOCON_UngroundAnabus(NSS_IOCON, (1 << (IOCON_ANA0_3 - IOCON_ANA0_0)));

    /* Init the MOSI, MISO and SCLK pins*/
    Chip_IOCON_SetPinConfig(NSS_IOCON, IOCON_PIO0_6, IOCON_FUNC_1);
    Chip_IOCON_SetPinConfig(NSS_IOCON, IOCON_PIO0_8, IOCON_FUNC_1);
    Chip_IOCON_SetPinConfig(NSS_IOCON, IOCON_PIO0_9, IOCON_FUNC_1);

    /* Configure the CS pin */
    Chip_GPIO_SetPinDIROutput(NSS_GPIO, 0, CS_PIN);
    Chip_GPIO_SetPinOutHigh(NSS_GPIO, 0, CS_PIN);

    /* Raise the system clock. Keep the software defined limits in mind*/
    Chip_Clock_System_SetClockFreq(200000);

    /* Start the SPI Module */
    Chip_SSP_Init(NSS_SSP0);
    Chip_SSP_SetMaster(NSS_SSP0, true);
    /* Mode 3 is supported by both the IMU and Memory used in V200 and V300 Densors */
    Chip_SSP_SetFormat(NSS_SSP0, SSP_BITS_8, SSP_FRAME_FORMAT_SPI, SSP_CLOCK_MODE3);
    Chip_SSP_SetBitRate(NSS_SSP0, 100000);
    Chip_SSP_Enable(NSS_SSP0);

}

void SPI_DeInit(){
	/* Disable and deinit the SPI driver */
	Chip_SSP_Disable(NSS_SSP0);
	Chip_SSP_DeInit(NSS_SSP0);

	/* Return the system clock to the regular speed*/
    Chip_Clock_System_SetClockFreq(REG_SYSTEMCLOCK);

    /* Configure the CS pin */
    Chip_GPIO_SetPinDIRInput(NSS_GPIO, 0, CS_PIN);
    Chip_IOCON_SetPinConfig(NSS_IOCON, CS_PIN, IOCON_FUNC_0 | IOCON_RMODE_PULLDOWN);

    /* Return all SPI related pins to their default state */
    Chip_IOCON_SetPinConfig(NSS_IOCON, IOCON_PIO0_2, IOCON_FUNC_0 | BOARD_PIO2_PULL);
    Chip_IOCON_SetPinConfig(NSS_IOCON, IOCON_PIO0_6, IOCON_FUNC_0 | BOARD_PIO6_PULL);
    Chip_IOCON_SetPinConfig(NSS_IOCON, IOCON_PIO0_8, IOCON_FUNC_0 | BOARD_PIO8_PULL);
    Chip_IOCON_SetPinConfig(NSS_IOCON, IOCON_PIO0_9, IOCON_FUNC_0 | BOARD_PIO9_PULL);

    /* Ground the pins used for SCLK */
    Chip_IOCON_GroundAnabus(NSS_IOCON, (1 << (IOCON_ANA0_2 - IOCON_ANA0_0)));
    Chip_IOCON_GroundAnabus(NSS_IOCON, (1 << (IOCON_ANA0_3 - IOCON_ANA0_0)));
}



void SPI_Write(uint8_t *wdata, uint8_t wlen){
	Chip_GPIO_SetPinOutLow(NSS_GPIO, 0, CS_PIN);		/* Select a chip */
	Chip_SSP_WriteFrames_Blocking(NSS_SSP0, wdata, wlen);
	Chip_GPIO_SetPinOutHigh(NSS_GPIO, 0, CS_PIN);		/* Release the chip */
}

void SPI_Write_Read(uint8_t *wdata, uint8_t wlen, uint8_t *rdata, uint8_t rlen){
	Chip_GPIO_SetPinOutLow(NSS_GPIO, 0, CS_PIN);		/* Select a chip */
	Chip_SSP_WriteFrames_Blocking(NSS_SSP0, wdata, wlen);
	Chip_SSP_ReadFrames_Blocking(NSS_SSP0, rdata, rlen);
	Chip_GPIO_SetPinOutHigh(NSS_GPIO, 0, CS_PIN);		/* Release the chip */
}

