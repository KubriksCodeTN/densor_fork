/*
 * Copyright 2020 NXP
 * This software is owned or controlled by NXP and may only be used strictly
 * in accordance with the applicable license terms.  By expressly accepting
 * such terms or by downloading, installing, activating and/or otherwise using
 * the software, you are agreeing that you have read, and that you agree to
 * comply with and are bound by, such license terms.  If you do not agree to
 * be bound by the applicable license terms, then you may not retain, install,
 * activate or otherwise use the software.
 */

/**
 * @defgroup MODS_NSS_MAC mac: Chaskey Signing module
 * @ingroup MODS_NSS
 * The MAC module provides a way to sign messages for message integrity verification. It is suitable for applications
 * requiring a lightweight cryptographic mechanism.
 *
 * This module can be used as
 * - a data integrity mechanism to verify that data has not been altered in an unauthorized manner.
 * - a message authentication mechanism to provide assurance that a message has been originated by an entity in
 *  possession of the secret key.
 *
 * This module uses the Chaskey algorithm to generate a signature for a given 128-bit key and arbitrarily long messages.
 * - More information can be found here: https://eprint.iacr.org/2014/386.pdf
 * - Chaskey-12 is standardized in https://www.iso.org/standard/71116.html; here the original 8-round variant is used.
 *
 * @par Example 1
 *  @snippet mac_mod_example_1.c mac_mod_example_1
 *
 * @{
 */

#ifndef __MAC_H__
#define __MAC_H__

#include <stdint.h>
#include "mac_dft.h"

/**
 * Initialization of the keys used for signing.
 * @pre This is the module's first function to be called after startup.
 * @param key : Encryption key
 */
void Mac_Init(const uint32_t key[4]);

/**
 * Signs a message with the given keys.
 * @param message : A pointer to the message to be sent.
 * @param messageLength : The length of the message in bytes. Padding is automatically added when needed, i.e. only
 *  when the message length is not a multiple of 16. @n
 *  For example, when @c messageLength equals 12, these padding bytes are used during signing:
 *  @c 04h @c 00h @c 00h @c 00h
 * @param [out] tag : A 16-byte array where the message hash will be stored.
 */
void Mac_Sign(const uint8_t * message, const unsigned int messageLength, uint8_t tag[16]);

#endif /** @} */
