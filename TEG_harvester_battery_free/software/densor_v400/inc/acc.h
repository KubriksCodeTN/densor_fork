#ifndef IMU_H_
#define IMU_H_

#include <string.h>
#include <stdlib.h>

#define ACC_CS_PIN IOCON_PIO0_4

/* Parameters for the IMU self-test */
#define SELF_TEST_SAMPLES  5

#define ST_MIN_POS      70.0f
#define ST_MAX_POS      1500.0f

int acc_init(void);
int acc_deinit(void);
int acc_read_orientation(void);

#endif /* ACC_H_ */
