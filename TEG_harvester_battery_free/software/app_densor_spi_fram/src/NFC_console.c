#include "NFC_console.h"
#include "board.h"
#include "IMU.h"
#include "memory.h"
#include "tmeas/tmeas.h"
#include "ndeft2t/ndeft2t.h"

uint8_t sName[MAX_TEXT_PAYLOAD] = "Dental Sensor";
uint8_t sResponse[MAX_TEXT_PAYLOAD] = "No Response";
uint8_t sTime[MAX_TEXT_PAYLOAD] = "-";

const char * tempResponseString = "Current temperature: ___._C";


void GenerateNdef_Text(void)
{

	itoa(Chip_RTC_Time_GetValue(NSS_RTC), sTime, 10);
    uint8_t instance[NDEFT2T_INSTANCE_SIZE];
    uint8_t buffer[NFC_SHARED_MEM_BYTE_SIZE ];
    NDEFT2T_CREATE_RECORD_INFO_T nameRecordInfo = {.pString = (uint8_t *)"en" /* language code */,
                                                   .shortRecord = true,
                                                   .uriCode = 0 /* don't care */};
    NDEFT2T_CREATE_RECORD_INFO_T commandRecordInfo = {.pString = (uint8_t *)"en" /* language code */,
                                                   .shortRecord = true,
                                                   .uriCode = 0 /* don't care */};
    NDEFT2T_CREATE_RECORD_INFO_T timeRecordInfo = {.pString = (uint8_t *)"en" /* language code */,
                                                   .shortRecord = true,
                                                   .uriCode = 0 /* don't care */};

    NDEFT2T_CreateMessage(instance, buffer, NFC_SHARED_MEM_BYTE_SIZE, true);
    if (NDEFT2T_CreateTextRecord(instance, &nameRecordInfo)) {
        if (NDEFT2T_WriteRecordPayload(instance, sName, strlen(sName))) {
            NDEFT2T_CommitRecord(instance);
        }
    }
    if (NDEFT2T_CreateTextRecord(instance, &commandRecordInfo)) {
        if (NDEFT2T_WriteRecordPayload(instance, sResponse, strlen(sResponse))) {
            NDEFT2T_CommitRecord(instance);
        }
    }
    if (NDEFT2T_CreateTextRecord(instance, &timeRecordInfo)) {
        if (NDEFT2T_WriteRecordPayload(instance, sTime, strlen(sTime))) {
            NDEFT2T_CommitRecord(instance);
        }
    }

    NDEFT2T_CommitMessage(instance); /* Copies the generated message to NFC shared memory. */
}

/** Parses the NDEF message in the NFC shared memory, and copies the TEXT and MIME payloads. */
void ParseNdef(void)
{
    uint8_t instance[NDEFT2T_INSTANCE_SIZE];
    uint8_t buffer[NFC_SHARED_MEM_BYTE_SIZE];
    NDEFT2T_PARSE_RECORD_INFO_T recordInfo;
    int len = 0;
    uint8_t *pData = NULL;

    if (NDEFT2T_GetMessage(instance, buffer, NFC_SHARED_MEM_BYTE_SIZE))
    {
        while (NDEFT2T_GetNextRecord(instance, &recordInfo) != false)
        {
            pData = (uint8_t *)NDEFT2T_GetRecordPayload(instance, &len);
            switch (recordInfo.type)
            {
                case NDEFT2T_RECORD_TYPE_TEXT:
                    if ((size_t)len <= MAX_TEXT_PAYLOAD)
                    {
                    	/* Find the space. This indicates if their is a argument to the command*/
                    	uint8_t* seperator_ptr = memchr(pData,' ', len);
                    	uint8_t *pCommand = pData;
                    	uint8_t *pArgument = NULL;
						if(seperator_ptr != NULL)
						{
							int seperator_index = (seperator_ptr-pData);
							pArgument = seperator_ptr + 1;
							pData[seperator_index] = '\0';
						}
						pData[len] = '\0';

						/* LED control */
						if((memcmp (pCommand, "led", 3) == 0))
						{
							if(memcmp (pArgument, "on", 2) == 0)	LED_On(LED_RED);
							else									LED_Off(LED_RED);

						}
						/* Blink / LED-test */
						if((memcmp (pCommand, "blink", 5) == 0))
						{
							for(int i = 0; i < 5; i++)
							{
								LED_On(LED_RED);
								Chip_Clock_System_BusyWait_ms(250);
								LED_Off(LED_RED);
								Chip_Clock_System_BusyWait_ms(250);
							}
						}
						/* Temperature measurement */
						else if((memcmp (pCommand, "temp", 4) == 0))
						{
							Chip_TSen_Init(NSS_TSEN);
							Chip_TSen_SetResolution(NSS_TSEN, TSEN_10BITS);
							Chip_TSen_Start(NSS_TSEN);
							while (!(Chip_TSen_ReadStatus(NSS_TSEN, NULL) & TSEN_STATUS_MEASUREMENT_DONE)) {
								; /* Wait until the measurement has finished. */
							}
							int native = Chip_TSen_GetValue(NSS_TSEN);
							Chip_TSen_DeInit(NSS_TSEN);
							int value = Chip_TSen_NativeToCelsius(native, 1000); /* in milli-celsius */
							value = value/100; /* Convert to deci-celcius*/

							/* Interrupt version */
							//    Chip_TSen_Init(NSS_TSEN);
							//    Chip_TSen_SetResolution(NSS_TSEN, TSEN_12BITS);
							//    Chip_TSen_Int_SetEnabledMask(NSS_TSEN, TSEN_INT_MEASUREMENT_RDY);
							//    NVIC_EnableIRQ(TSEN_IRQn);
							//    Chip_TSen_Start(NSS_TSEN);
							//    Chip_PMU_PowerMode_EnterSleep();
							//
							//	int native = Chip_TSen_GetValue(NSS_TSEN);
							//	Chip_TSen_DeInit(NSS_TSEN);
							//	int value = Chip_TSen_NativeToCelsius(native, 1000); /* in milli-celsius */

							/* Format the response in a human readible format */
							strcpy(sResponse, tempResponseString);
							DeciValue2String(value, &sResponse[25]);
						}
						/* Configure the RTC */
						else if((memcmp (pCommand, "rtc", 3) == 0))
						{
							if (pArgument == NULL) return;
							Chip_RTC_Time_SetValue(NSS_RTC, atoi(pArgument));
						}
						/* Sleep for x seconds */
						else if((memcmp (pCommand, "sleep", 5) == 0))
						{
							 /* Indicate going to sleep*/
							 LED_On(LED_RED);
							 Chip_Clock_System_BusyWait_ms(500);
							 LED_Off(LED_RED);
							 Chip_Clock_System_BusyWait_ms(500);

							 /* Determine the sleep time */
							 int sleep_time = 10;
							 if (pArgument != NULL)
							 {
								 sleep_time = atoi(pArgument);
								 if (sleep_time > 120) sleep_time = 120;
								 if (sleep_time < 1) sleep_time = 1;
							 }

							 /* Configure the RTC for wake-up */
							 Chip_RTC_Wakeup_SetReload(NSS_RTC, sleep_time);
							 Chip_PMU_PowerMode_EnterSleep();

							 /* Indicate the chip is awake again */
							 LED_On(LED_RED);
							 Chip_Clock_System_BusyWait_ms(500);
							 LED_Off(LED_RED);
						}
						/* Deep-Sleep for x seconds */
						else if((memcmp (pCommand, "dsleep", 6) == 0) || (memcmp (pCommand, "deep-sleep", 10) == 0))
						{
							 /* Indicate going to sleep*/
							 LED_On(LED_RED);
							 Chip_Clock_System_BusyWait_ms(500);
							 LED_Off(LED_RED);
							 Chip_Clock_System_BusyWait_ms(500);

							 /* Determine the sleep time */
							 int sleep_time = 10;
							 if (pArgument != NULL)
							 {
								 sleep_time = atoi(pArgument);
								 if (sleep_time > 120) sleep_time = 120;
								 if (sleep_time < 1) sleep_time = 1;
							 }

							 /* Configure the RTC for wake-up */
							 Chip_RTC_Wakeup_SetReload(NSS_RTC, sleep_time);
							 Chip_PMU_PowerMode_EnterDeepSleep();

							 /* Indicate the chip is awake again */
							 LED_On(LED_RED);
							 Chip_Clock_System_BusyWait_ms(500);
							 LED_Off(LED_RED);
						}
						else if((memcmp (pCommand, "pdown", 5) == 0) || (memcmp (pCommand, "power-down", 10) == 0))
						{
							 LED_On(LED_RED);
							 Chip_Clock_System_BusyWait_ms(500);
							 LED_Off(LED_RED);
							 Chip_Clock_System_BusyWait_ms(500);

							 /* Determine the sleep time */
							 int sleep_time = 10;
							 if (pArgument != NULL)
							 {
								 sleep_time = atoi(pArgument);
								 if (sleep_time > 120) sleep_time = 120;
								 if (sleep_time < 1) sleep_time = 1;
							 }

							 /* Configure the RTC for wake-up */
							 Chip_RTC_Wakeup_SetReload(NSS_RTC, sleep_time);

							 Chip_Clock_System_BusyWait_ms(2000); /* Safety - so the debugger can break in */
							 Chip_PMU_PowerMode_EnterDeepPowerDown(false);
							 while(1); /* System does not resume here. Unreachable. */
						}
						/* Off */
						else if((memcmp (pCommand, "off", 3) == 0))
						{
							 LED_On(LED_RED);
							 Chip_Clock_System_BusyWait_ms(500);
							 LED_Off(LED_RED);

							 Chip_PMU_Switch_OpenVDDBat();
							 while(1);
						}
						/* Set clock speed in kHz */
						else if((memcmp (pCommand, "clock", 5) == 0))
						{

							if(memcmp (pArgument, "8000", 4) == 0) 			Chip_Clock_System_SetClockFreq(8000000);
							else if(memcmp (pArgument, "4000", 4) == 0) 	Chip_Clock_System_SetClockFreq(4000000);
							else if(memcmp (pArgument, "2000", 4) == 0) 	Chip_Clock_System_SetClockFreq(2000000);
							else if(memcmp (pArgument, "1000", 4) == 0) 	Chip_Clock_System_SetClockFreq(1000000);
							else if(memcmp (pArgument, "500", 3) == 0) 		Chip_Clock_System_SetClockFreq(500000);
							else if(memcmp (pArgument, "250", 3) == 0) 		Chip_Clock_System_SetClockFreq(250000);
							else if(memcmp (pArgument, "125", 3) == 0) 		Chip_Clock_System_SetClockFreq(125000);
							else if(memcmp (pArgument, "62.5", 4) == 0) 	Chip_Clock_System_SetClockFreq(62500);
							else Chip_Clock_System_SetClockFreq(500000);

						}
						/* I2C testing */
						else if((memcmp (pCommand, "i2c", 3) == 0) || (memcmp (pCommand, "I2C", 3) == 0))
						{
							if(memcmp (pArgument, "on", 2) == 0)I2C_Init();
							else I2C_DeInit();
						}
						/* VEXT testing */
						else if((memcmp (pCommand, "VEXT", 4) == 0))
						{
							if(memcmp (pArgument, "on", 2) == 0) 		VEXT_On();
//							else if(memcmp (pArgument, "pre", 2) == 0)	VEXT_On(1);
							else VEXT_Off();
						}
						/* IMU */
						else if((memcmp (pCommand, "imu", 3) == 0) || (memcmp (pCommand, "IMU", 3) == 0))
						{
							if(memcmp (pArgument, "on", 2) == 0){
								int ret = IMU_Init();
								if (ret == 0) LED_On(LED_RED);
							}
							else if(memcmp (pArgument, "orient", 6) == 0){
								Chip_Clock_System_BusyWait_ms(1000);
								int val = IMU_read_orientation();
								itoa(val, sResponse, 10);
							}
							else{
								IMU_DeInit();
								LED_Off(LED_RED);
							}
						}
						/* IMU */
						else if((memcmp (pCommand, "mem", 3) == 0) || (memcmp (pCommand, "memory", 6) == 0))
						{
							if(memcmp (pArgument, "on", 2) == 0){
								Memory_Init();
							}
							else if(memcmp (pArgument, "write", 5) == 0){
								Chip_Clock_System_BusyWait_ms(1000);
								LED_On(LED_RED);
								Chip_Clock_System_BusyWait_ms(50);
								LED_Off(LED_RED);

								Memory_Write(1);

								LED_On(LED_RED);
								Chip_Clock_System_BusyWait_ms(50);
								LED_Off(LED_RED);
							}
							else if(memcmp (pArgument, "reset", 5) == 0){
								Memory_Reset();
							}
							else{
								Memory_DeInit();
							}
						}
                    }
                    break;
                default:
                    /* ignore */
                    break;
            }
        }
    }
}

/** Helper function to print a number (decimal). */
void Number2String(int remaining, char * end, int size)
{
    memset(end - size + 1, ' ', (size_t)size);
    *end = '0';
    while ((size > 0) && (remaining > 0)) {
        *end = (char)('0' + (remaining % 10));
        end--;
        remaining /= 10;
        size--;
    }
}

/**
 * Writes a human-readable version of a deci-value to the NFC buffer
 * @param value deci-value. For example, a value of 123 will result in "12.3"
 */
void DeciValue2String(int deciValue, char * end)
{
    bool positive = true;

    if (deciValue < 0) {
        positive = false;
        deciValue *= -1;
    }

    Number2String(deciValue % 10, end, 1);

    end--;
    *end = '.';

    end--;
    Number2String(deciValue / 10, end, 3);

    if (!positive) {
        end -= 2; /* If negative, the absolute number can have at most two digits. */
        *end = '-';
    }
}
