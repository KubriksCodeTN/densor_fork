/*
 * Copyright 2014-2018 NXP
 * This software is owned or controlled by NXP and may only be used strictly
 * in accordance with the applicable license terms.  By expressly accepting
 * such terms or by downloading, installing, activating and/or otherwise using
 * the software, you are agreeing that you have read, and that you agree to
 * comply with and are bound by, such license terms.  If you do not agree to
 * be bound by the applicable license terms, then you may not retain, install,
 * activate or otherwise use the software.
 */

#ifndef __APP_SEL_H_
#define __APP_SEL_H_

#define NDEFT2T_FIELD_STATUS_CB App_FieldStatusCb
#define NDEFT2T_MSG_AVAILABLE_CB App_MsgAvailableCb
#define NDEFT2T_MSG_READ_CB App_MsgReadCb

#define STORAGE_COMPRESS_CB App_CompressCb
#define STORAGE_DECOMPRESS_CB App_DecompressCb

#define REG_SYSTEMCLOCK 500000				/* System clock for regular operation */
#define I2C_SYSTEMCLOCK 1000000				/* System clock speed for I2C communication */
#define COMPRESSION_SYSTEMCLOCK 2000000		/* System clock during compression */


/* Settings for the storage module */
#define STORAGE_TYPE uint16_t
#define STORAGE_BITSIZE 16
#define STORAGE_SIGNED 0

/* Don't use the ALON registers */
#define STORAGE_SAMPLE_ALON_CACHE_COUNT 0

#define STORAGE_BLOCK_SIZE_IN_SAMPLES 10

#endif
