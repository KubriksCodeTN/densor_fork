#include "IMU.h"

#include "board.h"
#include "i2cbbm/i2cbbm.h"
#include "lis2dw12_reg.h"

static uint8_t IMU_address;
stmdev_ctx_t dev_ctx;

int32_t platform_write(void *handle, uint8_t reg, const uint8_t *bufp, uint16_t len);
int32_t platform_read(void *handle, uint8_t reg, uint8_t *bufp, uint16_t len);

int lis2dw12_orientation(stmdev_ctx_t *dev_ctx);

void I2C_Init(void)
{
	#ifdef I2CHW /* If hardware I2C is used */
	Chip_Clock_System_SetClockFreq(I2C_SYSTEMCLOCK);	/* Set the clock frequency to 1 MHz (min. for I2C) */

    /* Remove the pull-down from the pre-charge pin*/
    Chip_IOCON_SetPinConfig(NSS_IOCON, IOCON_PIO0_3, IOCON_FUNC_0 | IOCON_RMODE_INACT);

    /* Configure the pins for I2C*/
    Chip_IOCON_SetPinConfig(NSS_IOCON, IOCON_PIO0_4, IOCON_FUNC_1 | IOCON_I2CMODE_STDFAST);
    Chip_IOCON_SetPinConfig(NSS_IOCON, IOCON_PIO0_5, IOCON_FUNC_1 | IOCON_I2CMODE_STDFAST);

    /* Reset the I2C Peripheral*/
    Chip_SysCon_Peripheral_AssertReset(SYSCON_PERIPHERAL_RESET_I2C0);
    Chip_SysCon_Peripheral_DeassertReset(SYSCON_PERIPHERAL_RESET_I2C0);

    Chip_I2C_Init(I2C0);
    Chip_I2C_SetClockRate(I2C0, I2C_BITRATE); /* Set I2C to 100 kHz. Lower speeds allow for lower pull-up*/

    /* Finish initialization for master I2C communication. */
    Chip_I2C_SetMasterEventHandler(I2C0, Chip_I2C_EventHandler);
    NVIC_EnableIRQ(I2C0_IRQn);

	#else /* If Bit-Bang I2C is used */
    Chip_IOCON_SetPinConfig(NSS_IOCON, IOCON_PIO0_3, IOCON_FUNC_0 | IOCON_RMODE_INACT);

    I2cbbm_Init();
    I2cbbm_SetAddress(24);
	#endif
}

void I2C_DeInit(void)
{
	#ifdef I2CHW /* If hardware I2C is used */
    Chip_Clock_System_SetClockFreq(REG_SYSTEMCLOCK);	/* Set the clock back to the default of 0.5 MHz */

    NVIC_DisableIRQ(I2C0_IRQn);				/* Disable the interrupt */
    Chip_I2C_DeInit(I2C0);					/* De-initilize the IMU */
    Chip_SysCon_Peripheral_AssertReset(SYSCON_PERIPHERAL_RESET_I2C0);	/* Reset the Peripheral*/

    /* Reset the pins to the initial state - Reduces power consumption */
    Chip_IOCON_SetPinConfig(NSS_IOCON, IOCON_PIO0_3, IOCON_FUNC_0 | BOARD_PIO3_PULL);
    Chip_IOCON_SetPinConfig(NSS_IOCON, IOCON_PIO0_4, IOCON_FUNC_0 | BOARD_PIO4_PULL);
    Chip_IOCON_SetPinConfig(NSS_IOCON, IOCON_PIO0_5, IOCON_FUNC_0 | BOARD_PIO5_PULL);
	#else /* If Bit-Bang I2C is used */
    I2cbbm_DeInit();

    Chip_IOCON_SetPinConfig(NSS_IOCON, IOCON_PIO0_3, IOCON_FUNC_0 | BOARD_PIO3_PULL);

	#endif
}

void SPI_Init(){
	/* Set PIO2 as a input without pull-up, PIO5 is used as CS instead */
    Chip_IOCON_SetPinConfig(NSS_IOCON, IOCON_PIO0_2, IOCON_FUNC_0 | IOCON_RMODE_INACT);

    /* Unground the pins used for SCLK */
    Chip_IOCON_UngroundAnabus(NSS_IOCON, (1 << (IOCON_ANA0_2 - IOCON_ANA0_0)));
    Chip_IOCON_UngroundAnabus(NSS_IOCON, (1 << (IOCON_ANA0_3 - IOCON_ANA0_0)));

    /* Init the MOSI, MISO and SCLK pins*/
    Chip_IOCON_SetPinConfig(NSS_IOCON, IOCON_PIO0_6, IOCON_FUNC_1);
    Chip_IOCON_SetPinConfig(NSS_IOCON, IOCON_PIO0_8, IOCON_FUNC_1);
    Chip_IOCON_SetPinConfig(NSS_IOCON, IOCON_PIO0_9, IOCON_FUNC_1);

    /* Configure the CS pin */
    Chip_GPIO_SetPinDIROutput(NSS_GPIO, 0, IOCON_PIO0_5);
    Chip_GPIO_SetPinOutHigh(NSS_GPIO, 0, IOCON_PIO0_5);

    /* Raise the system clock. Keep the software defined limits in mind*/
    Chip_Clock_System_SetClockFreq(200000);

    /* Start the SPI Module */
    Chip_SSP_Init(NSS_SSP0);
    Chip_SSP_SetMaster(NSS_SSP0, true);
    /* Mode 3 is supported by both the IMU and Memory used in V200 and V300 Densors */
    Chip_SSP_SetFormat(NSS_SSP0, SSP_BITS_8, SSP_FRAME_FORMAT_SPI, SSP_CLOCK_MODE3);
    Chip_SSP_SetBitRate(NSS_SSP0, 100000);
    Chip_SSP_Enable(NSS_SSP0);

}

void SPI_DeInit(){
	Chip_SSP_Disable(NSS_SSP0);
	Chip_SSP_DeInit(NSS_SSP0);

    Chip_Clock_System_SetClockFreq(REG_SYSTEMCLOCK);

//	Chip_IOCON_SetPinConfig(NSS_IOCON, IOCON_PIO0_2, IOCON_FUNC_0);
    Chip_IOCON_SetPinConfig(NSS_IOCON, IOCON_PIO0_2, IOCON_FUNC_0 | BOARD_PIO2_PULL);

    Chip_IOCON_SetPinConfig(NSS_IOCON, IOCON_PIO0_6, IOCON_FUNC_0);
    Chip_IOCON_SetPinConfig(NSS_IOCON, IOCON_PIO0_8, IOCON_FUNC_0);
    Chip_IOCON_SetPinConfig(NSS_IOCON, IOCON_PIO0_9, IOCON_FUNC_0);
}


void VEXT_On()
{
    /* Un-ground the Analog pad connected to VEXT */
    Chip_IOCON_UngroundAnabus(NSS_IOCON, (1 << (IOCON_ANA0_5 - IOCON_ANA0_0)));

    /* Set VEXT as an output */
//    Chip_IOCON_SetPinConfig(NSS_IOCON, IOCON_PIO0_7, IOCON_FUNC_0 | IOCON_RMODE_INACT | IOCON_DDRIVE_ULTRAHIGH);
    Chip_GPIO_SetPinDIROutput(NSS_GPIO, 0, IOCON_PIO0_7);
    Chip_GPIO_SetPinOutHigh(NSS_GPIO, 0, IOCON_PIO0_7);

    /* Set VMEM */
	Chip_GPIO_SetPortDIRInput(NSS_GPIO, 0, IOCON_PIO0_4);
    Chip_IOCON_SetPinConfig(NSS_IOCON, IOCON_PIO0_4, IOCON_FUNC_0 | IOCON_RMODE_INACT);

    Chip_GPIO_SetPinDIROutput(NSS_GPIO, 0, IOCON_PIO0_3);
    Chip_GPIO_SetPinOutHigh(NSS_GPIO, 0, IOCON_PIO0_3);
}

void VEXT_Off(void)
{
	/* Turn off the output */
	Chip_GPIO_SetPinOutLow(NSS_GPIO, 0, IOCON_PIO0_7);
	/* Set as input */
	Chip_GPIO_SetPortDIRInput(NSS_GPIO, 0, IOCON_PIO0_7);
	/* Re-ground the Analog pad*/
	Chip_IOCON_GroundAnabus(NSS_IOCON, (1 << (IOCON_ANA0_5 - IOCON_ANA0_0)));

    /* Reset the pins to the initial state - Reduces power consumption */
    Chip_IOCON_SetPinConfig(NSS_IOCON, IOCON_PIO0_3, IOCON_FUNC_0 | BOARD_PIO3_PULL);
    Chip_IOCON_SetPinConfig(NSS_IOCON, IOCON_PIO0_4, IOCON_FUNC_0 | BOARD_PIO4_PULL);
    Chip_IOCON_SetPinConfig(NSS_IOCON, IOCON_PIO0_5, IOCON_FUNC_0 | BOARD_PIO5_PULL);
	Chip_IOCON_SetPinConfig(NSS_IOCON, IOCON_PIO0_7, IOCON_FUNC_0 | BOARD_PIO7_PULL);

}

int IMU_Init(void)
{
	VEXT_On();	/* Turn on VEXT and pre-charge the caps */

	#if defined(IMU_I2CHW) || defined(IMU_I2CBB)
		I2C_Init();	/* Initialize the I2C bus*/
		/* Determine the I2C address of the IMU; This can be either 24 or 25 (DEC) */
	    uint8_t val;
	    uint8_t reg = 15;


	    /* Test for an ACK on register 15 on both addresses. If one response, set as the address*/
	    if (Chip_I2C_MasterCmdRead(I2C0, 24, &reg, &val, 1) == 1){
	    	IMU_address = 24;
	    }else if (Chip_I2C_MasterCmdRead(I2C0, 25, &reg, &val, 1) == 1){
	    	IMU_address = 25;
	    }else{
	    	/* If their was no response, de-init the IMU */
	    	IMU_DeInit();
	    	return -1;
	    }
	#else
		SPI_Init();
	#endif

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

int IMU_DeInit(void)
{
	VEXT_Off();
	#if defined(IMU_I2CHW) || defined(IMU_I2CBB)
		I2C_DeInit();	/* Initialize the I2C bus*/
	#else
		SPI_DeInit();
	#endif
	return 0;
}

int IMU_read_orientation(){
	IMU_Init();
	int orientation = lis2dw12_orientation(&dev_ctx);
	IMU_DeInit();
	return orientation;
}

void PIO0_IRQHandler(void)
{
    uint32_t states = Chip_GPIO_GetRawInts(NSS_GPIO, 0);
    if (states & NSS_GPIOn_PINMASK(8)) {

    }
    Chip_GPIO_ClearInts(NSS_GPIO, 0, states);
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

/* IMU functions */
int test_self_test_lis2dw12(stmdev_ctx_t *dev_ctx)
{
  uint8_t rst;
  axis3bit16_t data_raw_acceleration[SELF_TEST_SAMPLES];
  float acceleration_mg[SELF_TEST_SAMPLES][3];

  lis2dw12_reg_t reg;
  float media[3] = { 0.0f, 0.0f, 0.0f };
  float mediast[3] = { 0.0f, 0.0f, 0.0f };
  uint8_t match[3] = { 0, 0, 0 };
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
      match[k] = 1;
    }else{
//    	pass = 0;
    }

  }

  /* Disable self test mode */
  lis2dw12_data_rate_set(dev_ctx, LIS2DW12_XL_ODR_OFF);
  lis2dw12_self_test_set(dev_ctx, LIS2DW12_XL_ST_DISABLE);
}

int lis2dw12_orientation(stmdev_ctx_t *dev_ctx)
{
  lis2dw12_reg_t int_route;
  uint8_t rst, whoamI;
  /* Check device ID */
  lis2dw12_device_id_get(dev_ctx, &whoamI);

  if (whoamI != LIS2DW12_ID)
    while (1) {
      /* manage here device not found */
    }

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

//  /* Sleep till the interrupt */
//  NVIC_EnableIRQ(PIO0_IRQn);
//  Chip_GPIO_SetupPinInt(NSS_GPIO, 0, 8, GPIO_INT_RISING_EDGE);
//  Chip_GPIO_EnableInt(NSS_GPIO, 0, NSS_GPIOn_PINMASK(8));
//  Chip_PMU_PowerMode_EnterSleep();

  /* Wait Events. */
  while (1) {
    lis2dw12_all_sources_t all_source;
    lis2dw12_all_sources_get(dev_ctx, &all_source);

  if (all_source.sixd_src.xh) 		return 0;
  else if (all_source.sixd_src.xl) 	return 1;
  else if (all_source.sixd_src.yh) 	return 2;
  else if (all_source.sixd_src.yl) 	return 3;
  else if (all_source.sixd_src.zh) 	return 4;
  else if (all_source.sixd_src.zl) 	return 5;

  }
}


/* Define the read and write function used by the IMU driver */
int32_t platform_write(void *handle, uint8_t reg, const uint8_t *bufp, uint16_t len)
{
	(void) handle;
	#if defined(IMU_SPI) /* If SPI is defined as the communication method */
		Chip_GPIO_SetPinDIROutput(NSS_GPIO, 0, IOCON_PIO0_5);
		Chip_GPIO_SetPinOutLow(NSS_GPIO, 0, IOCON_PIO0_5);

		Chip_SSP_WriteFrames_Blocking(NSS_SSP0, &reg, 1);
		Chip_SSP_WriteFrames_Blocking(NSS_SSP0, bufp, len);

		Chip_GPIO_SetPinDIROutput(NSS_GPIO, 0, IOCON_PIO0_5);
		Chip_GPIO_SetPinOutHigh(NSS_GPIO, 0, IOCON_PIO0_5);
		return 0;
	#elif defined(IMU_I2CHW) /* If I2C is defined as the communication method */
		ASSERT(len == 1);	/* Only support writes with a length of 1 */
		uint8_t buf[2] = {reg, bufp[0]};
		Chip_I2C_MasterSend((I2C_ID_T) handle, IMU_address, buf, 2);
		return 0;
	#elif defined(IMU_I2CBB) /* If bit-bang I2C is defined as the communication method */
		ASSERT(len == 1);	/* Only support writes with a length of 1 */
		uint8_t buf[2] = {reg, bufp[0]};
		I2cbbm_Write(buf, 2);
		return 0;
	#else
		return -1
	#endif
}

int32_t platform_read(void *handle, uint8_t reg, uint8_t *bufp, uint16_t len)
{
	(void) handle;
	#if defined(IMU_SPI) /* If SPI is defined as the communication method */
		Chip_GPIO_SetPinDIROutput(NSS_GPIO, 0, IOCON_PIO0_5);
		Chip_GPIO_SetPinOutLow(NSS_GPIO, 0, IOCON_PIO0_5);

		int imu_register = reg | (1 << 7);
		Chip_SSP_WriteFrames_Blocking(NSS_SSP0, &imu_register , 1);
		int num_bytes = Chip_SSP_ReadFrames_Blocking(NSS_SSP0, bufp, len);

		Chip_GPIO_SetPinDIROutput(NSS_GPIO, 0, IOCON_PIO0_5);
		Chip_GPIO_SetPinOutHigh(NSS_GPIO, 0, IOCON_PIO0_5);
		return num_bytes != len;
	#elif defined(IMU_I2CHW) /* If I2C is defined as the communication method */
		int num_bytes = Chip_I2C_MasterCmdRead((I2C_ID_T) handle, IMU_address, &reg, bufp, len);
		return num_bytes != len;
	#elif defined(IMU_I2CBB) /* If bit-bang I2C is defined as the communication method */
		int num_bytes = I2cbbm_WriteRead(&reg, 1, bufp, len);
		return num_bytes != len;
	#else
		return -1
	#endif
}
