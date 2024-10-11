/*
 * Copyright 2016-2019 NXP
 * This software is owned or controlled by NXP and may only be used strictly
 * in accordance with the applicable license terms.  By expressly accepting
 * such terms or by downloading, installing, activating and/or otherwise using
 * the software, you are agreeing that you have read, and that you agree to
 * comply with and are bound by, such license terms.  If you do not agree to
 * be bound by the applicable license terms, then you may not retain, install,
 * activate or otherwise use the software.
 */

#ifndef __UCODE_H_
#define __UCODE_H_

/** @defgroup MODS_NSS_UCODE ucode : module to communicate with UCODE-I2C SL3S4011 or SL3S4021
 * @ingroup MODS_NSS
 * This module abstracts away the communication with the UCode-I2C over an I2C bus.
 * Both the HW I2C block - using PIO4 and PIO5 for SCL and SDA respectively - and the I2C bit-banging module
 * @ref MODS_NSS_I2CBBM are supported.
 *
 * The module supports adjusting the 12-byte EPC code - using #Ucode_SetNfcUid and #Ucode_SetMark - and writing to the
 * User Memory bank of the UCODE-I2C - using #Ucode_WriteData. The module assumes the EPC code is a concatenation of:
 * - the 8-byte NFC ID - 7 bytes plus a @c 0 byte where in NFC memory a checksum is placed, see #NSS_NFC_UID
 * - 3 bytes indicating the status of the IC and its current logging and monitoring process - the 'mark'
 * - a single byte indicating the version of the EPC code format - see #UCODE_VERSION.
 * There are no restrictions or assumptions on the User Memory bank imposed by this module.
 *
 * @anchor ucode_anchor_verification
 * @note Each write action is verified by reading back the contents after being written first. This catches
 *  - possible I2C bus problems (unlikely),
 *  - possible UCode-I2C memory problems (unlikely),
 *  - UHF / NFC arbitration problems in the UCode-I2C (possible)
 *  Multiple attempts are made to circumvent the arbitration problems: when an UHF field is present, it is
 *  necessary to wait until the field is gone before the UCODE-I2C can be written to via I2C. Each time a comparison
 *  fails, a few milliseconds are waited before a new attempt is initiated. Only if several
 *  attempts fail, the write is aborted.
 *
 * @warning The ICs have not been designed with mutual compatibility in mind.
 *  As a result, it is highly cumbersome to achieve functionalities as
 *  - Start trigger via UHF
 *  - Full data readout via UHF
 *  - Bidirectional communication via UHF
 *  Even if the above could be achieved - by adding extra components, by restricting the use case, and/or by adding
 *  assumptions on real-life usage - they come with severe limitations. @n
 *  Assuming immediate consumption is not required, typical information that can be conveyed:
 *  - from tag to host: label information, status reports, specific statistical information, the first @em n excursions, ...
 *  - from host to tag: handover data including timestamp, owner IDs, geolocation data, ...
 *
 * @par Diversity
 *  This module supports a number of diversity flags. Check the
 *  @ref MODS_NSS_UCODE_DFT "Message handler module diversity settings" for a full list and their implications.
 *
 * @{
 */

#include <stdbool.h>
#include "board.h"
#include "ucode_dft.h"

/* ------------------------------------------------------------------------- */

/**
 * Defines the size of the User Memory bank, where data can be read from or written to both via the I2C and the UHF
 * interface.
 */
#define UCODE_USER_MEMORY_SIZE ((3328U /* bits */) / 8U)

/**
 * A single char indicating the version of the EPC code format.
 * - the first 8 bytes of the EPC code form the NFC ID;
 * - the last 4 bytes of the EPC code are as follows: state LSB, state 2nd byte, state 3rd byte, 'T'
 * - The user memory is not used.
 */
#define UCODE_VERSION 'T'

/* ------------------------------------------------------------------------- */

/**
 * This function must be the first function to call in this module after going to deep power down or power-off power
 * save mode. Not calling this function will result in a hang.
 * @pre The I2C driver is not in use: #Chip_I2C_Init has not yet been called, or #Chip_I2C_DeInit was the last call to
 *  this driver.
 * @post The I2C driver is in exclusive use by the Ucode-I2C module
 */
void Ucode_Init(void);

/**
 * Resets the memory contents of the Ucode-I2C. This includes:
 * - Writing the NFC ID - calling #Ucode_SetNfcUid
 * - Setting the mark to its initial value - calling #Ucode_SetMark with #UCODE_EVENT_PRISTINE as argument.
 * @warning Blocking call. Can potentially last multiple seconds - see @ref ucode_anchor_verification "retries".
 * @return @c True when the memory contents were successfully updated in the UCode-I2C memory.
 */
bool Ucode_Reset(void);

/**
 * De-initializes the Ucode-I2C and the I2C driver.
 * No memory contents on the Ucode-I2C will be changed during this call.
 * @post The I2C driver is not claimed anymore.
 * @post All pins listed in #UCODE_PULLUPS as reconfigured as output, outputting low.
 */
void Ucode_DeInit(void);

/* ------------------------------------------------------------------------- */

/**
 * Writes the NFC UID as an EPC code in the Ucode-I2C.
 * @warning Blocking call. Can potentially last multiple seconds - see @ref ucode_anchor_verification "retries".
 * @return @c True when the memory contents were successfully updated in the UCode-I2C memory.
 */
bool Ucode_SetNfcUid(void);

/**
 * Changes the EPC code to allow quick and easy identification of marked labels/products.
 * @param events A bitmask. Each bit represents an application-specific event.
 * @warning Blocking call. Can potentially last multiple seconds - see @ref ucode_anchor_verification "retries".
 * @return @c True when the memory contents were successfully updated in the UCode-I2C memory.
 */
bool Ucode_SetMark(unsigned int events);

/* ------------------------------------------------------------------------- */

/**
 * Write data in the User Memory bank of the UCODE-I2C. This data is treated as raw bytes; meaning is to be given and
 * interpretation is to be done by the NHS3100 and UHF reader applications.
 * @param offset Defines the starting location to write in the User Memory bank.
 * @param data Pointer to the array of bytes to write.
 * @param len The number of bytes to write.
 * @pre offset + @c len must not exceed the User Memory region bank (which is 3328 bits or 416 bytes in size).
 * @warning Blocking call. Can potentially last multiple seconds - see @ref ucode_anchor_verification "retries".
 * @return @c True when the memory contents were successfully updated in the UCode-I2C memory.
 */
bool Ucode_WriteData(unsigned int offset, const uint8_t * data, unsigned int len);

/**
 * Read data from the User Memory bank of the UCODE-I2C. This data is treated as raw bytes; meaning is to be given and
 * interpretation is to be done by the NHS3100 and UHF reader applications.
 * @param offset Defines the starting location to read from the User Memory bank.
 * @param data Pointer to an array of bytes where the read data can be copied to.
 * @param len The number of bytes to read; the array pointed to by @c data must have at least this size.
 * @pre @c offset + @c len must not exceed the User Memory region bank (which is 3328 bits or 416 bytes in size).
 * @warning Blocking call. Can potentially last multiple seconds - see @ref ucode_anchor_verification "retries".
 * @return @c True when the memory contents were successfully retrieved from the UCode-I2C memory.
 */
bool Ucode_ReadData(unsigned int offset, uint8_t * data, unsigned int len);

#endif /** @} */
