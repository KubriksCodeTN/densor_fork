#include <string.h>
#include <stdlib.h>

#include "board.h"

#include "ndeft2t/ndeft2t.h"
#include "tmeas/tmeas.h"
#include "storage/storage.h"

#include "IMU.h"
#include "NFC_console.h"
#include "memory.h"


int main(void);
void App_FieldStatusCb(bool isPresent);

static volatile bool sMsgAvailable = false;
static volatile bool sMsgRead = false;
static volatile bool sFieldPresent = true;

static bool doMeaserment = false;
static volatile int temperature = 0;
static volatile bool tmeasDone = false;

void ResetISR(void)
{
    Board_Init();

    /* Configure GPIO 9 as input */
    Chip_IOCON_SetPinConfig(NSS_IOCON, IOCON_PIO0_9, IOCON_FUNC_0 | IOCON_RMODE_PULLDOWN);
    Chip_GPIO_SetPortDIRInput(NSS_GPIO, 0,  NSS_GPIOn_PINMASK(9));

    while ((Chip_GPIO_GetPinState(NSS_GPIO, 0, 9))) {
        ; /* Wait for as long as GPIO 9 is high */
        /* This allows for reprogramming even if the device goes straight to power-down */
    }

    /* Configure GPIO9 as a LED */
    LED_Init();

    Startup_VarInit();
    main();
}


void PIO0_0_IRQHandler(void)
{
    Chip_SysCon_StartLogic_ClearStatus(SYSCON_STARTSOURCE_PIO0_0);
}

void I2C0_IRQHandler(void)
{
    if (Chip_I2C_IsMasterActive(I2C0)) {
        Chip_I2C_MasterStateHandler(I2C0);
    }
    else {
        Chip_I2C_SlaveStateHandler(I2C0);
    }
}

void PIO0_IRQHandler(void)
{
	uint32_t states = Chip_GPIO_GetRawInts(NSS_GPIO, 0);
	Chip_GPIO_ClearInts(NSS_GPIO, 0, states);
}


void TSEN_IRQHandler(void)
{
    if ((Chip_TSen_Int_GetRawStatus(NSS_TSEN) & TSEN_INT_MEASUREMENT_RDY)) {
        Chip_TSen_Int_ClearRawStatus(NSS_TSEN, TSEN_INT_MEASUREMENT_RDY);
        /* Store the temperature results in a variable */
		temperature = Chip_TSen_GetValue(NSS_TSEN);
		temperature = Chip_TSen_NativeToCelsius(temperature, 10);
		tmeasDone = true;
    }
}

void RTC_IRQHandler(void)
{
    RTC_INT_T status = Chip_RTC_Int_GetRawStatus(NSS_RTC);
    Chip_RTC_Int_ClearRawStatus(NSS_RTC, status);

    if (status & RTC_INT_WAKEUP) {
        Chip_RTC_Int_ClearRawStatus(NSS_RTC, RTC_INT_WAKEUP);
		/* Always reload the RTC. Should be overwritten in main, but this ensures the count-down is always set */
		Chip_RTC_Wakeup_SetReload(NSS_RTC, 10);
    }
}

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

/* Function to turn on the external sensors */
void VEXT_On()
{
	/* Un-ground the Analog pad connected to VACC */
    Chip_IOCON_UngroundAnabus(NSS_IOCON, (1 << (IOCON_ANA0_5 - IOCON_ANA0_0)));

    /* Set VACC as an output and turn it on */
    Chip_GPIO_SetPinDIROutput(NSS_GPIO, 0, IOCON_PIO0_7);
    Chip_GPIO_SetPinOutHigh(NSS_GPIO, 0, IOCON_PIO0_7);

    /* Set PIO4 as a input without pull-up/down (INACT) */
	Chip_GPIO_SetPortDIRInput(NSS_GPIO, 0, IOCON_PIO0_4);
    Chip_IOCON_SetPinConfig(NSS_IOCON, IOCON_PIO0_4, IOCON_FUNC_0 | IOCON_RMODE_INACT);

    /* Set VMEM as an output and turn it on */
    Chip_GPIO_SetPinDIROutput(NSS_GPIO, 0, IOCON_PIO0_3);
    Chip_GPIO_SetPinOutHigh(NSS_GPIO, 0, IOCON_PIO0_3);
}

/* Function to turn off the external sensors */
void VEXT_Off(void)
{
	/* Turn off VACC */
	Chip_GPIO_SetPinOutLow(NSS_GPIO, 0, IOCON_PIO0_7);
	/* Configure VACC as a input */
	Chip_IOCON_SetPinConfig(NSS_IOCON, IOCON_PIO0_7, IOCON_FUNC_0 | BOARD_PIO7_PULL);
	/* Ground Analog Pin 5*/
	Chip_IOCON_GroundAnabus(NSS_IOCON, (1 << (IOCON_ANA0_5 - IOCON_ANA0_0)));

	/* Turn off VMEM*/
	Chip_GPIO_SetPinOutLow(NSS_GPIO, 0, IOCON_PIO0_3);
	/* Configure as an input */
    Chip_IOCON_SetPinConfig(NSS_IOCON, IOCON_PIO0_3, IOCON_FUNC_0 | BOARD_PIO3_PULL);
    /* Set PIO4 to its default state */
    Chip_IOCON_SetPinConfig(NSS_IOCON, IOCON_PIO0_4, IOCON_FUNC_0 | BOARD_PIO4_PULL);
}


int main(void)
{
	Board_Init();
    NDEFT2T_Init();
//    PMU_DPD_WAKEUPREASON_T wakeupReason = Chip_PMU_PowerMode_GetDPDWakeupReason();
//
//    /* This wait call is added to protect the device from bricking.
//     * After a reset, their is a 2 second window to connect the SWD-probe, even if the device goes to Deep-Powerdown */
////    Chip_Clock_System_BusyWait_ms(2000);
//
//    /* Set up the wake-up pin */
//    NVIC_EnableIRQ(PIO0_0_IRQn); /* PIO0_0_IRQHandler is called when this interrupt fires. */
//    Chip_SysCon_StartLogic_SetEnabledMask(SYSCON_STARTSOURCE_PIO0_0);
//    Chip_PMU_SetWakeupPinEnabled(true);
//    /* DO NOT ADD CODE BEFORE THIS STATEMENT */
//
//    doMeaserment = false;
//    switch (wakeupReason) {
//		case PMU_DPD_WAKEUPREASON_NONE:
//	        /*PMU_DPD_WAKEUPREASON_NONE: POR or RESETN pin asserted - (re-)setup and (re-)initialize */
//			Chip_Clock_System_BusyWait_ms(2000);
//     	break;
//        case PMU_DPD_WAKEUPREASON_RTC:
//            /* PMU_DPD_WAKEUPREASON_RTC: Waking up from Deep Power Down due to RTC down counter expired */
//        	doMeaserment = true;
//        	break;
//        case PMU_DPD_WAKEUPREASON_WAKEUPPIN:
//            /* PMU_DPD_WAKEUPREASON_WAKEUPPIN: Waking up from Deep Power Down due to Wakeup pin toggled low */
//        	Chip_Clock_System_BusyWait_ms(2000);
//        	break;
//        case PMU_DPD_WAKEUPREASON_NFCPOWER:
//            /* PMU_DPD_WAKEUPREASON_NFCPOWER: Waking up from Deep Power Down due to the presence of an NFC field */
//        	Chip_Clock_System_BusyWait_ms(2000);
//        	break;
//        default:
//            break;
//    }

    /* Configure the RTC for wake up */
    Chip_RTC_Wakeup_SetControl(NSS_RTC, RTC_WAKEUPCTRL_ENABLE | RTC_WAKEUPCTRL_AUTO);
    Chip_SysCon_StartLogic_SetEnabledMask(SYSCON_STARTSOURCE_RTC);
    Chip_SysCon_StartLogic_ClearStatus(SYSCON_STARTSOURCE_RTC);
    Chip_RTC_Int_SetEnabledMask(NSS_RTC, RTC_INT_WAKEUP);
    NVIC_EnableIRQ(RTC_IRQn);

    /* Un-ground TX*/
    Chip_IOCON_UngroundAnabus(NSS_IOCON, (1 << (IOCON_ANA0_1 - IOCON_ANA0_0)));
    /* Remove the pull-down from TX */
    Chip_IOCON_SetPinConfig(NSS_IOCON, IOCON_PIO0_1, IOCON_FUNC_0 | IOCON_RMODE_INACT);
    Chip_IOCON_SetPinConfig(NSS_IOCON, IOCON_PIO0_6, IOCON_FUNC_0 | IOCON_RMODE_INACT);

    Chip_IOCON_UngroundAnabus(NSS_IOCON, (1 << (IOCON_ANA0_2 - IOCON_ANA0_0)));
    Chip_IOCON_UngroundAnabus(NSS_IOCON, (1 << (IOCON_ANA0_3 - IOCON_ANA0_0)));
    Chip_IOCON_UngroundAnabus(NSS_IOCON, (1 << (IOCON_ANA0_5 - IOCON_ANA0_0)));

//    I2C_DeInit();
    VEXT_Off();

    VEXT_On();
    SPI_Init();






    IMU_read_orientation();


    while(1){}

    if(doMeaserment){
    	/* Do a 10-bit temperature measurement */
    	tmeasDone = false;
		Chip_TSen_Init(NSS_TSEN);
		Chip_TSen_SetResolution(NSS_TSEN, TSEN_10BITS);
		Chip_TSen_Int_SetEnabledMask(NSS_TSEN, TSEN_INT_MEASUREMENT_RDY);
		NVIC_EnableIRQ(TSEN_IRQn);
		Chip_TSen_Start(NSS_TSEN);

    	/* Get orientation */
    	int orientation = IMU_read_orientation();

		while (!tmeasDone) {
			; /* Wait until the measurement has finished. */
		}
		Chip_TSen_DeInit(NSS_TSEN);

		/* Log it to EEPROM */
		uint16_t data = (orientation << 12) | temperature;
		Memory_Init();
		Memory_Write(data);
		Memory_DeInit();


    	/* Go to deep-power - down */
		 Chip_RTC_Wakeup_SetReload(NSS_RTC, 10);
//		 Chip_Clock_System_BusyWait_ms(2000); /* Safety - so the debugger can break in */
		 Chip_PMU_PowerMode_EnterDeepPowerDown(false);
		 while(1); /* System does not resume here. Unreachable. */
    }

    while (1) {
    	if (sFieldPresent) { /* Update the NDEF message once when there is an NFC field */
			GenerateNdef_Text();
    	}
		while (sFieldPresent) {
			if (sMsgAvailable) {
				sMsgAvailable = false;
				ParseNdef();
				GenerateNdef_Text();
			}
			Chip_Clock_System_BusyWait_ms(10);
		}
    }

    return 0;
}
