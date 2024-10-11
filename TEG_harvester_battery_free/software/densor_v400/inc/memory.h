#ifndef MEMORY_H_
#define MEMORY_H_

#include <string.h>
#include <stdlib.h>

/* --- EEPROM MEMORY MAP ---
 * BYTE 0	| BYTE 1 	| BYTE 2 	| BYTE 3 	| BYTE 4 	| BYTE 5 	| BYTE 6 	| BYTE 7
 * ENABLE 	  COMMAND	 COUNTER     PDOWN time   Min Voltage Meas EN    BOARD ID
 * TEMP 10	 TEMP 1	     TEMP 0.1    Orient
 */

#define EEPROM_ENABLE_ADDR 		0
#define EEPROM_CMD_ADDR 		1
#define EEPROM_COUNTER_ADDR 	2
#define EEPROM_PDOWN_ADDR 		3
#define EEPROM_VOLT_ADDR		4
#define EEPROM_MEAS_EN_ADDR		5
#define EEPROM_BOARD_ID_ADDR	6


#define TEMP_EN_BIT (1 << 0)
#define ACC_EN_BIT  (1 << 1)

#define EEPROM_MEAS_ADDR		8
#define EEPROM_MEAS_SIZE		4
#define EEPROM_MAX_MEAS			32

void clear_memory(void);
void write_test_data_to_memory(void);

uint8_t number_of_measurements();
void add_measurement(int, int);

#endif /* MEMORY_H_ */
