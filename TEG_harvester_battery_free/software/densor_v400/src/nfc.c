#include "board.h"
#include "ndeft2t/ndeft2t.h"
#include "nfc.h"
#include "vext.h"
#include "spi.h"
#include "acc.h"
#include "fram.h"
#include "memory.h"
#include "tmeas/tmeas.h"

uint8_t sName[MAX_TEXT_PAYLOAD] = "Dental Sensor";
uint8_t sResponse[MAX_TEXT_PAYLOAD] = "No Response";
uint8_t sTime[MAX_TEXT_PAYLOAD] = "-";

const char * tempResponseString = "Current temperature: ___._C";

/**
 * @brief Function for generating a default NDEF
 */
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

/**
 * @brief Function for generating a NDEF with the EEPROM contents
 */
void generate_ndef_eeprom(void)
{
    uint8_t instance[NDEFT2T_INSTANCE_SIZE];
    uint8_t buffer[NFC_SHARED_MEM_BYTE_SIZE ];
    NDEFT2T_CREATE_RECORD_INFO_T measurementRecordInfo = {.pString = (uint8_t *)"en" /* language code */,
													   .shortRecord = true,
													   .uriCode = 0 /* don't care */};
    NDEFT2T_CREATE_RECORD_INFO_T settingRecordInfo = {.pString = (uint8_t *)"en" /* language code */,
                                                   .shortRecord = true,
                                                   .uriCode = 0 /* don't care */};
    NDEFT2T_CREATE_RECORD_INFO_T eepromRecordInfo = {.pString = (uint8_t *)"en" /* language code */,
                                                   .shortRecord = true,
                                                   .uriCode = 0 /* don't care */};
    uint8_t num_measurements = number_of_measurements();

    /* Display the number of measurements */
    uint8_t sInformation[32] = "V1.0; ID:XX M: ";
    
    /* Add the board ID to the information string*/
    uint8_t board_id = 0;
    Chip_EEPROM_Read(NSS_EEPROM, EEPROM_BOARD_ID_ADDR, &board_id, sizeof(uint8_t));
    sInformation[9]  = '0' + board_id/10;
    sInformation[10] = '0' + board_id%10;

    itoa(num_measurements, sInformation + strlen(sInformation), 10);
    uint8_t string_len = strlen(sInformation);
    sInformation[string_len++] = '/';
    itoa(EEPROM_MAX_MEAS, sInformation + string_len, 10);
    sInformation[string_len+2] = '\0';

    NDEFT2T_CreateMessage(instance, buffer, NFC_SHARED_MEM_BYTE_SIZE, true);
    if (NDEFT2T_CreateTextRecord(instance, &measurementRecordInfo)) {
        if (NDEFT2T_WriteRecordPayload(instance, sInformation, strlen(sInformation))) {
            NDEFT2T_CommitRecord(instance);
        }
    }

    /* Format a string to display the setting in EEPROM
     * (E)nable Bit: can be 0 or 1
     * (P)ower down: Shows power-down time in seconds
     * (V)oltage: Shows the turn on voltage in mV
     * (T)emperature enable: shows if temperature measurements is enabled
     * (O)rientation enable: shows if the orientation measurements is enabled.  */
    uint8_t sSettings[32] = "E:0 P:000 V:0000 T:0 O:0";
    uint8_t eeprom_reading = 0;

    /* Read the enable status byte */
    Chip_EEPROM_Read(NSS_EEPROM, EEPROM_ENABLE_ADDR, &eeprom_reading, sizeof(uint8_t));
    if(eeprom_reading) sSettings[2] = '1';

    /* Convert the power done time byte to a char array */
    Chip_EEPROM_Read(NSS_EEPROM, EEPROM_PDOWN_ADDR, &eeprom_reading, sizeof(uint8_t));
    itoa(eeprom_reading, sSettings + 6, 10);

    /* Convert the turn-on voltage to a char array */
    int vdd_threshold = 0;
    Chip_EEPROM_Read(NSS_EEPROM, EEPROM_VOLT_ADDR, &vdd_threshold, sizeof(uint8_t));
    vdd_threshold = vdd_threshold << 3;
    vdd_threshold += 1800;
    itoa(vdd_threshold, sSettings + 12, 10);

	/* Read the temperature and orientation bits */
	Chip_EEPROM_Read(NSS_EEPROM, EEPROM_MEAS_EN_ADDR, &eeprom_reading, sizeof(uint8_t));
	if((eeprom_reading >> 0) & 1) sSettings[19] = '1';
	if((eeprom_reading >> 1) & 1) sSettings[23] = '1';

	/* Remove the string terminators left by atoi in the settings string */
	for(int i = 0; i < 24; i++) if(sSettings[i] == '\0') sSettings[i] = ' ';

    if (NDEFT2T_CreateTextRecord(instance, &settingRecordInfo)) {
        if (NDEFT2T_WriteRecordPayload(instance, sSettings, 32)) {
            NDEFT2T_CommitRecord(instance);
        }
    }

    /* Format readings stored in EEPROM  */
    uint8_t eeprom_buffer[EEPROM_MAX_MEAS*7];
    for(int i = 0; i < num_measurements; i++)
    {
    	Chip_EEPROM_Read(NSS_EEPROM, EEPROM_MEAS_ADDR+(EEPROM_MEAS_SIZE*i), eeprom_buffer+(7*i), sizeof(uint8_t)*2);
    	eeprom_buffer[(7*i)+2] = '.';
    	Chip_EEPROM_Read(NSS_EEPROM, EEPROM_MEAS_ADDR+(EEPROM_MEAS_SIZE*i)+2, eeprom_buffer+(7*i)+3, sizeof(uint8_t));
    	eeprom_buffer[(7*i)+4] = '|';
    	Chip_EEPROM_Read(NSS_EEPROM, EEPROM_MEAS_ADDR+(EEPROM_MEAS_SIZE*i)+3, eeprom_buffer+(7*i)+5, sizeof(uint8_t));
    	eeprom_buffer[(7*i)+6] = '\n';
    }

    if (NDEFT2T_CreateTextRecord(instance, &eepromRecordInfo)) {
        if (NDEFT2T_WriteRecordPayload(instance, eeprom_buffer, num_measurements*7)) {
            NDEFT2T_CommitRecord(instance);
        }
    }

    NDEFT2T_CommitMessage(instance); /* Copies the generated message to NFC shared memory. */
}

/**
 * Brief: Retrieves commands from a NDEF message
 */
int ParseCommandFromNDEF(int powered_by_nfc)
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

						/* Set the Enable bit in the EEPROM */
						if((memcmp (pCommand, "enable", 6) == 0))
						{
						    uint8_t enable_bit = 1;
							Chip_EEPROM_Write(NSS_EEPROM, EEPROM_ENABLE_ADDR, &enable_bit, sizeof(uint8_t));
						    Chip_EEPROM_Flush(NSS_EEPROM, true);
						    return 1;
						}
						/* Clear the Enable bit in the EEPROM  */
						else if((memcmp (pCommand, "disable", 7) == 0))
						{
						    uint8_t enable_bit = 0;
							Chip_EEPROM_Write(NSS_EEPROM, EEPROM_ENABLE_ADDR, &enable_bit, sizeof(uint8_t));
						    Chip_EEPROM_Flush(NSS_EEPROM, true);
						    return 1;
						}
						/* Clear the EEPROM Memory */
						else if((memcmp (pCommand, "clear", 5) == 0))
						{
							clear_memory();
						}
						/* Add test data to the EEPROM */
						else if((memcmp (pCommand, "testdata", 8) == 0))
						{
							write_test_data_to_memory();
						}
						/* Do a temperature measurement and store the result in memory */
						else if((memcmp (pCommand, "temp", 4) == 0))
						{
							if(memcmp (pArgument, "on", 2) == 0)
							{
							    uint8_t measure_enable_bit = 0;
								Chip_EEPROM_Read(NSS_EEPROM, EEPROM_MEAS_EN_ADDR, &measure_enable_bit, sizeof(uint8_t));
								measure_enable_bit |= TEMP_EN_BIT;
								Chip_EEPROM_Write(NSS_EEPROM, EEPROM_MEAS_EN_ADDR, &measure_enable_bit, sizeof(uint8_t));
							    Chip_EEPROM_Flush(NSS_EEPROM, true);
							}
							else if(memcmp (pArgument, "measure", 7) == 0)
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
								add_measurement(value, 0);
							}
							else
							{
							    uint8_t measure_enable_bit = 0;
								Chip_EEPROM_Read(NSS_EEPROM, EEPROM_MEAS_EN_ADDR, &measure_enable_bit, sizeof(uint8_t));
								measure_enable_bit &= ~TEMP_EN_BIT;
								Chip_EEPROM_Write(NSS_EEPROM, EEPROM_MEAS_EN_ADDR, &measure_enable_bit, sizeof(uint8_t));
							    Chip_EEPROM_Flush(NSS_EEPROM, true);
							}
						}
						/* Enable or disable acc measurements */
						else if((memcmp (pCommand, "acc", 3) == 0))
						{
							if(memcmp (pArgument, "on", 2) == 0)
							{
							    uint8_t measure_enable_bit = 0;
								Chip_EEPROM_Read(NSS_EEPROM, EEPROM_MEAS_EN_ADDR, &measure_enable_bit, sizeof(uint8_t));
								measure_enable_bit |= ACC_EN_BIT;
								Chip_EEPROM_Write(NSS_EEPROM, EEPROM_MEAS_EN_ADDR, &measure_enable_bit, sizeof(uint8_t));
							    Chip_EEPROM_Flush(NSS_EEPROM, true);
							}
							else
							{
							    uint8_t measure_enable_bit = 0;
								Chip_EEPROM_Read(NSS_EEPROM, EEPROM_MEAS_EN_ADDR, &measure_enable_bit, sizeof(uint8_t));
								measure_enable_bit &= ~ACC_EN_BIT;
								Chip_EEPROM_Write(NSS_EEPROM, EEPROM_MEAS_EN_ADDR, &measure_enable_bit, sizeof(uint8_t));
							    Chip_EEPROM_Flush(NSS_EEPROM, true);
							}
						}
						/* Resume the deep-sleep with the remaining time */
						else if((memcmp (pCommand, "resume", 6) == 0))
						{
							Chip_Clock_System_BusyWait_ms(1000);

							Chip_RTC_Wakeup_SetReload(NSS_RTC, Chip_RTC_Wakeup_GetRemaining(NSS_RTC));
							Chip_PMU_PowerMode_EnterDeepPowerDown(false);
							while(1);
						}
						/* Command to set the powerdown time after a measurement */
						else if(memcmp (pCommand, "pdown", 5) == 0)
						{
							uint8_t pdown_time = 10;
							 if (pArgument != NULL)
							 {
								 pdown_time = (uint8_t) atoi(pArgument);

								 Chip_EEPROM_Write(NSS_EEPROM, EEPROM_PDOWN_ADDR, &pdown_time, sizeof(uint8_t));
								 Chip_EEPROM_Flush(NSS_EEPROM, true);
							 }
						}
						/* Set the Turn-On voltage threshold*/
						else if(memcmp (pCommand, "volt", 4) == 0)
						{
							int on_voltage = 1800;
							 if (pArgument != NULL)
							 {
								 on_voltage = atoi(pArgument);
								 if (on_voltage > 3300) on_voltage = 3300;
								 if (on_voltage < 1800) on_voltage = 1800;

								 /* Scale the voltage to fit in the single byte */
								 on_voltage = (on_voltage - 1800) >> 3;

								 Chip_EEPROM_Write(NSS_EEPROM, EEPROM_VOLT_ADDR, &on_voltage, sizeof(uint8_t));
								 Chip_EEPROM_Flush(NSS_EEPROM, true);
							 }
						}
						/* Turn off the MCU  */
						else if(memcmp (pCommand, "off", 3) == 0)
						{
							Chip_Clock_System_BusyWait_ms(1000);
							Chip_PMU_Switch_OpenVDDBat();
							while(1);
						}
						/* Command to set the board ID */
						else if(memcmp (pCommand, "ID", 2) == 0)
						{
							uint8_t board_id = 10;
							 if (pArgument != NULL)
							 {
								 board_id = (uint8_t) atoi(pArgument);

								 Chip_EEPROM_Write(NSS_EEPROM, EEPROM_BOARD_ID_ADDR, &board_id, sizeof(uint8_t));
								 Chip_EEPROM_Flush(NSS_EEPROM, true);
							 }
						}
						else{
							/* Some commands are executed after power up */
							uint8_t command_byte = 0;

							/* Assign a command number to the text commands */
							if((memcmp (pCommand, "position", 8) == 0))  	command_byte = 2;
							else if((memcmp (pCommand, "memory", 6) == 0))  command_byte = 3;

							/* If the chip is powered by NFC, write the command to EEPROM and execute it next time */
							if(powered_by_nfc)
							{
								Chip_EEPROM_Write(NSS_EEPROM, 1, &command_byte, sizeof(uint8_t));
							    Chip_EEPROM_Flush(NSS_EEPROM, true);
							}
							else /* Otherwise execute the command directly */
							{
								ExecuteCommand(command_byte);
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
    return 0;
}

void ExecuteCommand(int command_number){
    if(command_number == 1)
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

		/* Format the response in a human readable format */
		strcpy(sResponse, tempResponseString);
		DeciValue2String(value, &sResponse[25]);

    }
    else if(command_number == 2)
    {
		VEXT_On();
		SPI_Init();
		acc_init();
    	int val = acc_read_orientation();
		itoa(val, sResponse, 10);
		SPI_DeInit();
		VEXT_Off();
    }
    else if(command_number == 3)
    {
		VEXT_On();
		SPI_Init();

		FRAM_Read(0, sResponse, 10);
		sResponse[10] = '\0';

		SPI_DeInit();
		VEXT_Off();
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
