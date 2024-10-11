#include "ADC.h"
#include "board.h"
#include "adcdac_nss.h"

/**
 * @brief: Function for turning on VMEAS
 */
void VMEAS_On(){
    /* Unground the pin connected to MEAS_VDD */
    Chip_IOCON_UngroundAnabus(NSS_IOCON, (1 << (IOCON_ANA0_1 - IOCON_ANA0_0)));
    Chip_IOCON_SetPinConfig(NSS_IOCON, IOCON_ANA0_1, IOCON_FUNC_0 | IOCON_RMODE_INACT);

    /* Set MEAS_VDD as a output */
    Chip_GPIO_SetPinDIROutput(NSS_GPIO, 0, VMEAS_ENABLE_PIN);
    Chip_GPIO_SetPinOutHigh(NSS_GPIO, 0, VMEAS_ENABLE_PIN);
}


void VMEAS_Off(){
	/* Turn off VMEAS and set as a input with its default pull-resistor */
    Chip_GPIO_SetPinOutLow(NSS_GPIO, 0, VMEAS_ENABLE_PIN);
    Chip_GPIO_SetPinDIRInput(NSS_GPIO, 0, VMEAS_ENABLE_PIN);
    Chip_IOCON_SetPinConfig(NSS_IOCON, VMEAS_ENABLE_PIN, IOCON_FUNC_0 | BOARD_PIO1_PULL);

    /* Ground the Analog pin */
    Chip_IOCON_GroundAnabus(NSS_IOCON, (1 << (IOCON_ANA0_1 - IOCON_ANA0_0)));
}

int measure_VDD(){
	VMEAS_On();

	/* Wait for VMEAS to settle */
	Chip_Clock_System_BusyWait_ms(10);

	/* Initialize the ADCDAC*/
	Chip_ADCDAC_Init(NSS_ADCDAC0);

	/* Configure the analog pin and set the AMUX */
	Chip_IOCON_SetPinConfig(NSS_IOCON, VMEAS_ADC_PIN, IOCON_FUNC_1);
	Chip_ADCDAC_SetMuxADC(NSS_ADCDAC0, VMEAS_ADC_PIN - 11);

	/* Set the input range as 'NARROW' (0-1V) */
	Chip_ADCDAC_SetInputRangeADC(NSS_ADCDAC0, ADCDAC_INPUTRANGE_NARROW);
	/* Set the ADC up for a single measurement */
	Chip_ADCDAC_SetModeADC(NSS_ADCDAC0, ADCDAC_SINGLE_SHOT);
	/* Start the ADC */
	Chip_ADCDAC_StartADC(NSS_ADCDAC0);

	while (!(Chip_ADCDAC_ReadStatus(NSS_ADCDAC0) & ADCDAC_STATUS_ADC_DONE)) {
		; /* Wait until measurement completes*/
	}

	/* Retrieve the ADC value*/
	int adc_value = Chip_ADCDAC_GetValueADC(NSS_ADCDAC0);
	/* Convert ADC value to a voltage */
	int adc_voltage = ((float) adc_value / 4096) * 1000;
	/* Convert ADC voltage to VDD */
	int vdd_voltage = adc_voltage * (( (float) VMEAS_R1 + (float) VMEAS_R2)/ (float) VMEAS_R2);

	VMEAS_Off();

	return vdd_voltage;
}


