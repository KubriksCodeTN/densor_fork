#ifndef IMU_H_
#define IMU_H_

#include <string.h>
#include <stdlib.h>

//#define IMU_I2CHW
//#define IMU_I2CBB
#define IMU_SPI

#define I2C_BITRATE 100000			/* Bit-rate of the I2C bus*/

#define I2CBBM_SYSTEM_CLOCK_DIVIDER 16 /* SysClock @ 500 kHz */

/* Parameters for the IMU self-test */
#define SELF_TEST_SAMPLES  5

#define ST_MIN_POS      70.0f
#define ST_MAX_POS      1500.0f


void I2C_Init(void);
void I2C_DeInit(void);

void SPI_Init(void);
void SPI_DeInit(void);

int IMU_Init(void);
int IMU_DeInit(void);
int IMU_read_orientation(void);

#endif /* IMU_H_ */
