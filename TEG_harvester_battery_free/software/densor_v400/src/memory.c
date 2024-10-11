#include "board.h"
#include "memory.h"

/**
 * @brief Fill the EEPROM memory with spaces (' ')
 */
void clear_memory(void){
	uint8_t empty_buffer[EEPROM_MEAS_SIZE];
	memset(empty_buffer, ' ', EEPROM_MEAS_SIZE);
	/* Clear the measurement counter */
	uint8_t num = 0;
	Chip_EEPROM_Write(NSS_EEPROM, EEPROM_COUNTER_ADDR, &num, sizeof(uint8_t));

	/* Clear the measurements */
	for(int i=EEPROM_MEAS_ADDR; i<((EEPROM_MAX_MEAS*EEPROM_MEAS_SIZE)+EEPROM_MEAS_ADDR); i+=EEPROM_MEAS_SIZE){
		Chip_EEPROM_Write(NSS_EEPROM, i, &empty_buffer, sizeof(uint8_t)*EEPROM_MEAS_SIZE);
	}
	/* Commit all the changes */
	Chip_EEPROM_Flush(NSS_EEPROM, true);
}

/**
 * @brief Write test data to the EEPROM Memory
 */
void write_test_data_to_memory(void){
	uint8_t test_buffer[EEPROM_MEAS_SIZE] = {'2', '1', '8', '4'};
	for(int i=EEPROM_MEAS_ADDR; i<((EEPROM_MAX_MEAS*EEPROM_MEAS_SIZE)+EEPROM_MEAS_ADDR); i+=EEPROM_MEAS_SIZE){
		Chip_EEPROM_Write(NSS_EEPROM, i, &test_buffer, sizeof(uint8_t)*EEPROM_MEAS_SIZE);
	}
	Chip_EEPROM_Flush(NSS_EEPROM, true);
}

/**
 * @brief Retrieve the number of measurements stored in EEPROM
 *
 * @return number of measurements
 */
uint8_t number_of_measurements(void){
	uint8_t number = 0;
	Chip_EEPROM_Read(NSS_EEPROM, EEPROM_COUNTER_ADDR, &number, sizeof(uint8_t));
	return number;
}

/**
 * @brief Write a measurement to the EEPROM
 *
 * @note Stops once EEPROM_MAX_MEAS is reached
 */
void add_measurement(int temperature, int orientation){
	/* Retrieve the number of stored measurements */
	uint8_t num_measurements = number_of_measurements();

	/* Check if their is room left */
	if(num_measurements >= EEPROM_MAX_MEAS) return;
	/* Check the data */
	if (temperature < 0 || temperature > 999) return;
	if (orientation < 0 || orientation > 9) return;

	/* Create a empty buffer with spaces (\0 causes string termination when reading with the NXP-app)*/
	uint8_t measurement_buffer[EEPROM_MEAS_SIZE+1] = {' ', ' ', ' ', ' ', ' '};
	/* Convert the integers to strings */
	itoa(temperature, measurement_buffer, 10);
	itoa(orientation, measurement_buffer+3, 10);

	/* Store the measurement in EEPROM */
	Chip_EEPROM_Write(NSS_EEPROM, (num_measurements*EEPROM_MEAS_SIZE) + EEPROM_MEAS_ADDR,
			&measurement_buffer, sizeof(uint8_t)*EEPROM_MEAS_SIZE);

	/* Increase the measurement counter */
	num_measurements += 1;
	Chip_EEPROM_Write(NSS_EEPROM, EEPROM_COUNTER_ADDR, &num_measurements, sizeof(uint8_t));

	/* Flush to the EEPROM */
	Chip_EEPROM_Flush(NSS_EEPROM, true);
}
