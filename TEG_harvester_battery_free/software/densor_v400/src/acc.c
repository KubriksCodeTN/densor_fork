#include "acc.h"
#include "board.h"
#include "i2cbbm/i2cbbm.h"
#include "lis2dw12_reg.h"

stmdev_ctx_t dev_ctx;

int32_t platform_write(void *handle, uint8_t reg, const uint8_t *bufp, uint16_t len);
int32_t platform_read(void *handle, uint8_t reg, uint8_t *bufp, uint16_t len);
int lis2dw12_orientation(stmdev_ctx_t *dev_ctx);

/**
 * @brief Configure the IMU for use
 */
int acc_init(void)
{
	/* Set the Chip-Select pin as a output */
	Chip_GPIO_SetPinDIROutput(NSS_GPIO, 0, ACC_CS_PIN);

    /* Configure the device driver */
	dev_ctx.write_reg = platform_write;
	dev_ctx.read_reg = platform_read;
	dev_ctx.handle = NULL;

	/* Check device ID */
	uint8_t whoamI;
	lis2dw12_device_id_get(&dev_ctx, &whoamI);
	if (whoamI != LIS2DW12_ID) return -2;

	return 0;
}


int acc_deinit(void)
{
	return 0;
}

/**
 * @brief Read the 6 axis orientation of the IMU
 */
int acc_read_orientation(){
	int orientation = lis2dw12_orientation(&dev_ctx);
	return orientation;
}

/* Utility functions */
typedef union {
  int16_t i16bit[3];
} axis3bit16_t;

inline float ABSF(float _x)
{
  return (_x < 0.0f) ? -(_x) : _x;
}

int flush_samples(stmdev_ctx_t *dev_ctx)
{
  lis2dw12_reg_t reg;
  axis3bit16_t dummy;
  int samples = 0;
  lis2dw12_status_reg_get(dev_ctx, &reg.status);

  if (reg.status.drdy) {
    lis2dw12_acceleration_raw_get(dev_ctx, dummy.i16bit);
    samples++;
  }

  return samples;
}

/* IMU functions - Provided by STM */
int test_self_test_lis2dw12(stmdev_ctx_t *dev_ctx)
{
  uint8_t rst;
  axis3bit16_t data_raw_acceleration[SELF_TEST_SAMPLES];
  float acceleration_mg[SELF_TEST_SAMPLES][3];

  lis2dw12_reg_t reg;
  float media[3] = { 0.0f, 0.0f, 0.0f };
  float mediast[3] = { 0.0f, 0.0f, 0.0f };
  uint8_t j = 0;
  uint16_t i = 0;
  uint8_t k = 0;
  uint8_t axis;
  /* Restore default configuration */
  lis2dw12_reset_set(dev_ctx, PROPERTY_ENABLE);

  do {
    lis2dw12_reset_get(dev_ctx, &rst);
  } while (rst);

  lis2dw12_block_data_update_set(dev_ctx, PROPERTY_ENABLE);
  lis2dw12_full_scale_set(dev_ctx, LIS2DW12_4g);
  lis2dw12_power_mode_set(dev_ctx, LIS2DW12_HIGH_PERFORMANCE);
  lis2dw12_data_rate_set(dev_ctx, LIS2DW12_XL_ODR_50Hz);
  Chip_Clock_System_BusyWait_ms(100);
  /* Flush old samples */
  flush_samples(dev_ctx);

  do {
    lis2dw12_status_reg_get(dev_ctx, &reg.status);

    if (reg.status.drdy) {
      /* Read accelerometer data */
      memset(data_raw_acceleration[i].i16bit, 0x00, 3 * sizeof(int16_t));
      lis2dw12_acceleration_raw_get(dev_ctx,
                                    data_raw_acceleration[i].i16bit);

      for (axis = 0; axis < 3; axis++) {
        acceleration_mg[i][axis] =
          lis2dw12_from_fs4_to_mg(data_raw_acceleration[i].i16bit[axis]);
      }

      i++;
    }
  } while (i < SELF_TEST_SAMPLES);

  for (k = 0; k < 3; k++) {
    for (j = 0; j < SELF_TEST_SAMPLES; j++) {
      media[k] += acceleration_mg[j][k];
    }

    media[k] = (media[k] / j);
  }

  /* Enable self test mode */
  lis2dw12_self_test_set(dev_ctx, LIS2DW12_XL_ST_POSITIVE);
  Chip_Clock_System_BusyWait_ms(100);
  i = 0;
  /* Flush old samples */
  flush_samples(dev_ctx);

  do {
    lis2dw12_status_reg_get(dev_ctx, &reg.status);

    if (reg.status.drdy) {
      /* Read accelerometer data */
      memset(data_raw_acceleration[i].i16bit, 0x00, 3 * sizeof(int16_t));
      lis2dw12_acceleration_raw_get(dev_ctx, data_raw_acceleration[i].i16bit);

      for (axis = 0; axis < 3; axis++)
        acceleration_mg[i][axis] =
          lis2dw12_from_fs4_to_mg(data_raw_acceleration[i].i16bit[axis]);

      i++;
    }
  } while (i < SELF_TEST_SAMPLES);

  for (k = 0; k < 3; k++) {
    for (j = 0; j < SELF_TEST_SAMPLES; j++) {
      mediast[k] += acceleration_mg[j][k];
    }

    mediast[k] = (mediast[k] / j);
  }

  /* Check for all axis self test value range */
  for (k = 0; k < 3; k++) {
    if ((ABSF(mediast[k] - media[k]) >= ST_MIN_POS) &&
        (ABSF(mediast[k] - media[k]) <= ST_MAX_POS)) {
//      match[k] = 1;
    }else{
//    	pass = 0;
    }

  }

  /* Disable self test mode */
  lis2dw12_data_rate_set(dev_ctx, LIS2DW12_XL_ODR_OFF);
  lis2dw12_self_test_set(dev_ctx, LIS2DW12_XL_ST_DISABLE);
  return 0;
}

int lis2dw12_orientation(stmdev_ctx_t *dev_ctx)
{
  lis2dw12_reg_t int_route;
  uint8_t rst, whoamI;
  /* Check device ID */
  lis2dw12_device_id_get(dev_ctx, &whoamI);

  if (whoamI != LIS2DW12_ID) return 0;

  /* Restore default configuration */
  lis2dw12_reset_set(dev_ctx, PROPERTY_ENABLE);

  do {
    lis2dw12_reset_get(dev_ctx, &rst);
  } while (rst);

  /* Set full scale */
  lis2dw12_full_scale_set(dev_ctx, LIS2DW12_2g);
  /* Configure power mode */
  lis2dw12_power_mode_set(dev_ctx, LIS2DW12_CONT_LOW_PWR_LOW_NOISE_12bit);
  /* Set threshold to 60 degrees */
  lis2dw12_6d_threshold_set(dev_ctx, 0x02);
  /* LPF2 on 6D function selection. */
  lis2dw12_6d_feed_data_set(dev_ctx, LIS2DW12_LPF2_FEED);
  /* Enable interrupt generation on 6D INT1 pin. */
  lis2dw12_pin_int1_route_get(dev_ctx, &int_route.ctrl4_int1_pad_ctrl);
  int_route.ctrl4_int1_pad_ctrl.int1_6d = PROPERTY_ENABLE;
  lis2dw12_pin_int1_route_set(dev_ctx, &int_route.ctrl4_int1_pad_ctrl);
  /* Set Output Data Rate */
  lis2dw12_data_rate_set(dev_ctx, LIS2DW12_XL_ODR_200Hz);


  /* Wait Events. */
  while (1) {
    lis2dw12_all_sources_t all_source;
    lis2dw12_all_sources_get(dev_ctx, &all_source);

  if (all_source.sixd_src.xh) 		return 1;
  else if (all_source.sixd_src.xl) 	return 2;
  else if (all_source.sixd_src.yh) 	return 3;
  else if (all_source.sixd_src.yl) 	return 4;
  else if (all_source.sixd_src.zh) 	return 5;
  else if (all_source.sixd_src.zl) 	return 6;

  }
}


/* Define the read and write function used by the IMU driver */
int32_t platform_write(void *handle, uint8_t reg, const uint8_t *bufp, uint16_t len)
{
	(void) handle;
	Chip_GPIO_SetPinOutLow(NSS_GPIO, 0, ACC_CS_PIN);

	Chip_SSP_WriteFrames_Blocking(NSS_SSP0, &reg, 1);
	Chip_SSP_WriteFrames_Blocking(NSS_SSP0, bufp, len);

	Chip_GPIO_SetPinOutHigh(NSS_GPIO, 0, ACC_CS_PIN);
	return 0;
}

int32_t platform_read(void *handle, uint8_t reg, uint8_t *bufp, uint16_t len)
{
	(void) handle;
	Chip_GPIO_SetPinOutLow(NSS_GPIO, 0, ACC_CS_PIN);

	int imu_register = reg | (1 << 7);
	Chip_SSP_WriteFrames_Blocking(NSS_SSP0, &imu_register , 1);
	int num_bytes = Chip_SSP_ReadFrames_Blocking(NSS_SSP0, bufp, len);

	Chip_GPIO_SetPinOutHigh(NSS_GPIO, 0, ACC_CS_PIN);
	return num_bytes != len;
}
