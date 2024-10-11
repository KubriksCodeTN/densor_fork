#include <string.h>
#include <stdlib.h>

#include "board.h"

#include "ndeft2t/ndeft2t.h"
#include "tmeas/tmeas.h"
#include "storage/storage.h"
#include "adcdac_nss.h"

#include "nfc.h"
#include "adc.h"

#include "vext.h"
#include "spi.h"
#include "acc.h"
#include "fram.h"

#include "memory.h"


int main(void);
void App_FieldStatusCb(bool isPresent);

static volatile bool sMsgAvailable = false;
static volatile bool sMsgRead = false;
static volatile bool sFieldPresent = false;

/* Used in the temperature measurement interrupt */
static volatile int temperature = 0;
static volatile bool tmeasDone = false;

/**
 * @brief Reset ISR. Called when the reset pin is lowered
 */
void ResetISR(void)
{
	Board_Init(); /* Configure the IO, NFC, EEPROM and the RTC */

	/* Wait for as long as WAKE-UP is low */
	/* This allows for reprogramming even if the device goes straight to power-down - DO NOT CHANGE */
    while (!(NSS_GPIO->DATA[1 << 0]))

    /* Initialize the memory */
    Startup_VarInit();

    /* Run the main code */
    main();
}

/**
 * @brief Temperature ISR. Called when the temperature sensor conversion is done
 */
void TSEN_IRQHandler(void)
{
    if ((Chip_TSen_Int_GetRawStatus(NSS_TSEN) & TSEN_INT_MEASUREMENT_RDY)) {
    	/* Clear the interrupt flag */
        Chip_TSen_Int_ClearRawStatus(NSS_TSEN, TSEN_INT_MEASUREMENT_RDY);
        /* Store the temperature results in a variable */
		temperature = Chip_TSen_GetValue(NSS_TSEN);
		/* Convert the result to degrees */
		temperature = Chip_TSen_NativeToCelsius(temperature, 10);
		/* Set the flag to indicate the measurement is done */
		tmeasDone = true;
    }
}

/**
 * @brief RTC ISR. Called on a RTC interrupt
 */
void RTC_IRQHandler(void)
{
    /* Retrieve the interrupt */
	RTC_INT_T status = Chip_RTC_Int_GetRawStatus(NSS_RTC);
	/* and clear it*/
    Chip_RTC_Int_ClearRawStatus(NSS_RTC, status);

    /* Clear the Wake-Up interrupt (again?) */
    if (status & RTC_INT_WAKEUP) {
        Chip_RTC_Int_ClearRawStatus(NSS_RTC, RTC_INT_WAKEUP);
		/* Always reload the RTC. Should be overwritten in main, but this ensures the count-down is always set */
		Chip_RTC_Wakeup_SetReload(NSS_RTC, 10);
    }
}

/**
 * @brief NFC related callbacks
 */
void App_FieldStatusCb(bool status)
{
    sFieldPresent = status; /* Handled in main loop */
}

void App_MsgAvailableCb(void)
{
    sMsgAvailable = true; /* Handled in main loop */
}

void App_MsgReadCb(void)
{
	sMsgRead = true;
}


int main(void)
{
    /* Configure the NDEF interface; A message MUST be generated within 15 ms after startup*/
	NDEFT2T_Init();
    /* Store the Wake-Up reason. This is lost after RTC setup, so must be done here */
    PMU_DPD_WAKEUPREASON_T wakeupReason = Chip_PMU_PowerMode_GetDPDWakeupReason();

    /* Set up the wake-up pin - Can wake the chip form DPD */
    NVIC_EnableIRQ(PIO0_0_IRQn); /* PIO0_0_IRQHandler is called when this interrupt fires. */
    Chip_SysCon_StartLogic_SetEnabledMask(SYSCON_STARTSOURCE_PIO0_0);
    Chip_PMU_SetWakeupPinEnabled(true);

    /* Configure the RTC for wake up */
    Chip_RTC_Wakeup_SetControl(NSS_RTC, RTC_WAKEUPCTRL_ENABLE | RTC_WAKEUPCTRL_AUTO);
    Chip_SysCon_StartLogic_SetEnabledMask(SYSCON_STARTSOURCE_RTC);
    Chip_SysCon_StartLogic_ClearStatus(SYSCON_STARTSOURCE_RTC);
    Chip_RTC_Int_SetEnabledMask(NSS_RTC, RTC_INT_WAKEUP);
    NVIC_EnableIRQ(RTC_IRQn);

    /* --- DO NOT ADD CODE BEFORE THIS STATEMENT --- */

    /* Check if the MCU is powered by NFC. If so, start the NFC console */
	while((Chip_PMU_GetStatus() << 7) && 1)
	{
		generate_ndef_eeprom();
		while (1) {
			if (sFieldPresent) { /* Update the NDEF message once when there is an NFC field */
				generate_ndef_eeprom();
			}
			while (sFieldPresent) {
				if (sMsgAvailable) {
					sMsgAvailable = false;
					ParseCommandFromNDEF(true);
					generate_ndef_eeprom();
				}
				Chip_Clock_System_BusyWait_ms(10);
			}
		}
	}

    /* If the system is powered by VDDBAT, check if the enable bit is set */
    uint8_t enable_bit = 0;
    Chip_EEPROM_Read(NSS_EEPROM, EEPROM_ENABLE_ADDR, &enable_bit, sizeof(uint8_t));

    /* If the chip is disabled, only run the NFC console and do not perform a measurement */
    while(!enable_bit){
    	if (sFieldPresent) { /* Update the NDEF message once when there is an NFC field */
    					GenerateNdef_Text();
    				}
    				while (sFieldPresent) {
    					if (sMsgAvailable) {
    						sMsgAvailable = false;
    						ParseCommandFromNDEF(false);
    						GenerateNdef_Text();
    					}
    					Chip_Clock_System_BusyWait_ms(10);
    				}
    }

	/* Check if the VDD is above the threshold. The threshold is stored (compressed) in the EEPROM */
    int vdd_threshold = 0;
    Chip_EEPROM_Read(NSS_EEPROM, EEPROM_VOLT_ADDR, &vdd_threshold, sizeof(uint8_t));

    vdd_threshold = vdd_threshold << 3;
    vdd_threshold += 1800;

    /* Perform a check on the threshold and decide to use the default instead */
    if (vdd_threshold < 1800 || vdd_threshold > 3300) vdd_threshold = DEFAULT_VDD_THRESHOLD;

    int vddbat = measure_VDD();
	while (vddbat < vdd_threshold && vddbat > 1600)
	{
		/* If Wake-Up is pressed (low), skip the sleep */
		if (!Chip_GPIO_GetPinState(NSS_GPIO, 0, 0)) break;

		/* Sleep for 10 seconds in the voltage is to low */
		Chip_RTC_Wakeup_SetReload(NSS_RTC, 10);
		Chip_PMU_PowerMode_EnterDeepPowerDown(false);
		while(1){}; /* System does not resume here. Unreachable. */
	}

	/* Read the measurement enable bits from EEPROM */
    uint8_t measure_enable_bit = 0;
	Chip_EEPROM_Read(NSS_EEPROM, EEPROM_MEAS_EN_ADDR, &measure_enable_bit, sizeof(uint8_t));
	bool temp_meas_enabled = (measure_enable_bit >> 0) & 1;
	bool acc_meas_enabled  = (measure_enable_bit >> 1) & 1;

	int orientation = 0;
	temperature = 0;
	tmeasDone = false;

	/* Start a temperature measurement in with an interrupt */
	if(temp_meas_enabled)
	{
		/* Enable the temperature sensor */
	   	Chip_TSen_Init(NSS_TSEN);
	   	/* A 10 bit measurement takes around 26 ms; With an interrupt the ACC can be read in the mean time */
		Chip_TSen_SetResolution(NSS_TSEN, TSEN_10BITS);
		/* Enable the interrupt */
		Chip_TSen_Int_SetEnabledMask(NSS_TSEN, TSEN_INT_MEASUREMENT_RDY);
		NVIC_EnableIRQ(TSEN_IRQn);
		/* Start the measurement */
		Chip_TSen_Start(NSS_TSEN);
	}

	/* Start determining the orientation with the IMU */
	if(acc_meas_enabled)
	{
		/* Enable power to the ACC */
		VEXT_On();
		/* Start the SPI bus */
		SPI_Init();
		/* Configure the ACC driver and read its 6-axis orientation */
		acc_init();
		orientation = acc_read_orientation();
		/* Shut-down the SPI and power down the IMU */
		SPI_DeInit();
		VEXT_Off();
	}

	if(temp_meas_enabled)
	{
		/* Wait for the measurement to complete and shut down the sensor afterwards */
		while (!tmeasDone){} /* Wait until the measurement has finished. */
		Chip_TSen_DeInit(NSS_TSEN);
	}

	/* Store the result in the EEPROM memory */
	add_measurement(temperature, orientation);

	/* Go to deep-power - down */
	uint8_t pdown_time = 10;
	Chip_EEPROM_Read(NSS_EEPROM, EEPROM_PDOWN_ADDR, &pdown_time, sizeof(uint8_t));

	/* If pdown_time is zero, do not sleep and stay active */
	if(pdown_time == 0) while(1){};

	Chip_RTC_Wakeup_SetReload(NSS_RTC, pdown_time);
	Chip_PMU_PowerMode_EnterDeepPowerDown(false);
	while(1){};

    return 0;
}
