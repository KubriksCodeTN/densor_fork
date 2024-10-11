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

#include <string.h>
#include "mac.h"
#include "chaskey/chaskey.h"

__attribute__ ((section(".noinit")))
static uint32_t sKey[4];
__attribute__ ((section(".noinit")))
static uint32_t sSubKey1[4];
__attribute__ ((section(".noinit")))
static uint32_t sSubKey2[4];

void Mac_Init(const uint32_t key[4])
{
    memcpy(sKey, key, 16);
    subkeys(sSubKey1, sSubKey2, sKey);
}

void Mac_Sign(const uint8_t * message, unsigned int messageLength, uint8_t tag[16])
{
    chaskey(tag, 16, message, messageLength, sKey, sSubKey1, sSubKey2);
}
