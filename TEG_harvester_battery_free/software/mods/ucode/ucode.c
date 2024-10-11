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

#include <string.h>
#include <ucode/ucode_i2c.h>
#include "board.h"
#include "ucode.h"

/** The Ucode-I2C is using 16-bit wide words. */
#define UCODE_WORD_SIZE 2

/**
 * The length of the EPC code is defined by the value of the @c PC content at address 2002h.
 */
#define UCODE_ADDRESS_EPC_LENGTH 0x2002

/**
 * See Application note AN10940, rev. 1.3 - May 13, 2014;
 * Question 17: How can the EPC Backscatter length changed?
 * @warning The application note contains errors.
 * @note The value @c 0x3000 corresponds to a byte length of @c 12
 */
#define UCODE_DATA_EPC_LENGTH 0x3000

/**
 * The Ucode-I2C has an 160-bit EPC memory on bank 01b and is accessible starting at address 2004h.
 */
#define UCODE_ADDRESS_EPC 0x2004

/**
 * The number of bytes appended to the EPC code to indicate the monitoring status.
 */
#define UCODE_SETMARKER_LENGTH 4

/**
 * Special features of the UCODE-I2C are managed using a Configuration Word located at the end of the EPC memory bank.
 */
#define UCODE_ADDRESS_CONFIG 0x2040 /* Address via I2C */

/**
 * See Product data sheet SL3S4011_4021, rev. 3.4 - May 24, 2017;
 * Table 9/10: Configuration Word accessible located at address 200h via UHF of the EPC bank and I2C address 2040h
 *  (1/2 RF front end version SL3S4011/SL3S4021)
 *
 *  | bit | access | feature                                |
 *  |-:   |:-:     |:-                                      |
 *  | 0   | RO     | Download indicator 0                   |
 *  | 1   | RO     | Externally supplied flag               |
 *  | 2   | RO     | RF active flag                         |
 *  | 3   | RO     | Upload indicator                       |
 *  | 4   | RO     | I2C address bit 3                      |
 *  | 5   | RO     | I2C address bit 2                      |
 *  | 6   | RO     | I2C address bit 1                      |
 *  | 7   | RO     | I2C port on/off                        |
 *  | 8   | RW     | UHF antenna port1 on/off               |
 *  | 9   | RW     | rfu / UHF antenna port2 on/off         |
 *  | 10  | RO     | rfu                                    |
 *  | 11  | RO     | SCL INT enable                         |
 *  | 12  | RW     | bit for read protect user memory       |
 *  | 13  | RW     | bit for read protect EPC               |
 *  | 14  | RW     | bit for read protect TID SNR (48 bits) |
 *  | 15  | RO     | PSF alarm flag                         |
 *
 * For example, to enable the two antenna's and leave read protection disabled, use @c 0b0000001100000000
 */
#define UCODE_DATA_CONFIG 0x0100 /* Setting bit 8 */

/** The Ucode-I2c has 3328 bits of user memory on bank 11b. */
#define UCODE_ADDRESS_USER_MEMORY 0x6000

/**
 * Reading and writing the UCODE-I2C is only possible when no UHF field is present. Whenever reading or writing
 * fails, the module will back off and wait some time, allowing the UHF field to finish communication. After that
 * time, the operation is attempted again. The number of attempts is defined here.
 * @warning Be careful not to try too many times; calls are blocking, preventing your main context to do anything
 *  else.
 */
#define UCODE_I2C_ATTEMPTS 3 /* Just try one to a few times. Any reasonable number will do. */

/**
 * Reading and writing the UCODE-I2C is only possible when no UHF field is present. Whenever reading or writing
 * fails, the module will back off and wait some time, allowing the UHF field to finish communication. After that
 * time, the operation is attempted again. The time between two attempts is defined here, in milliseconds.
 * @warning Be careful not to try too many times; calls are blocking, preventing your main context to do anything
 *  else.
 */
#define UCODE_I2C_BACKOFF_WAIT_PERIOD 135 /* Just wait some time. Any reasonable number will do. */

/* ------------------------------------------------------------------------- */

static void Backoff(void);
static bool Read(uint16_t address, uint16_t * pWords, unsigned int len);
static bool Write(uint16_t address, const uint16_t * pWords, unsigned int len);

#if UCODE_PULLUP_COUNT
static uint32_t sPullups[UCODE_PULLUP_COUNT] = UCODE_PULLUPS;
#endif

/**
 * Checks the contents of the EPC code in the Ucode-I2C.
 * @return @c true when the EPC code written in the Ucode-I2C matches with the NFC UID; @c false otherwise.
 */
static bool CompareNfcUid(void);

/**
 * Checks the contents of the EPC code in the Ucode-I2C.
 * @param events Bitmask. Each bit represents an application-specific event. The events to compare against.
 * @return @c true when the EPC code written in the Ucode-I2C matches with the given events; @c false otherwise.
 */
static bool CompareMark(unsigned int events);

/* ------------------------------------------------------------------------- */

/**
 * - Attempts to power-off the UCODE-I2C - #Ucode_DeInit
 * - Waits - #UCODE_I2C_BACKOFF_WAIT_PERIOD
 * - Re-initializes the UCODE-I2C - #Ucode_Init
 */
static void Backoff(void)
{
    Ucode_DeInit();
    Chip_Clock_System_BusyWait_ms(UCODE_I2C_BACKOFF_WAIT_PERIOD);
    Ucode_Init();
}

static bool Read(uint16_t address, uint16_t * pWords, unsigned int len)
{
    bool success = true;
    for (unsigned int i = 0; i < len; i++) {
        success &= Ucode_I2cRead(address, &pWords[i]);
        address = (uint16_t)(address + 2);
    }
    return success;
}

static bool Write(uint16_t address, const uint16_t * pWords, unsigned int len)
{
    bool success = true;
    for (unsigned int i = 0; i < len; i++) {
        success &= Ucode_I2cWrite(address, pWords[i]);
        address = (uint16_t)(address + 2);
    }
    return success;
}

static bool CompareNfcUid(void)
{
    uint8_t epc[sizeof(NSS_NFC_UID->bytes)];
    ASSERT(sizeof(NSS_NFC_UID->bytes) == 8);
    bool success = Read(UCODE_ADDRESS_EPC, (uint16_t *)epc, sizeof(epc) / sizeof(uint16_t));
    /* Indexing rationale: see comment inside Ucode_SetNfcUid */
    return success && (epc[0] == NSS_NFC_UID->bytes[1])
            && (epc[1] == NSS_NFC_UID->bytes[0])
            && (epc[2] == NSS_NFC_UID->bytes[4])
            && (epc[3] == NSS_NFC_UID->bytes[2])
            && (epc[4] == NSS_NFC_UID->bytes[6])
            && (epc[5] == NSS_NFC_UID->bytes[5])
            && (epc[6] == 0x00)
            && (epc[7] == NSS_NFC_UID->bytes[7]);
}

static bool CompareMark(unsigned int events)
{
    uint8_t mark[UCODE_SETMARKER_LENGTH];
    bool success = Read(UCODE_ADDRESS_EPC + sizeof(NSS_NFC_UID->bytes), (uint16_t *)mark, sizeof(mark) / 2);
    return success && (mark[0] == (uint8_t)(events & 0xFF))
            && (mark[1] == (uint8_t)((events >> 8) & 0xFF))
            && (mark[2] == (uint8_t)((events >> 16) & 0xFF))
            && (mark[3] == UCODE_VERSION);
}

/* ------------------------------------------------------------------------- */

void Ucode_Init(void)
{
    Ucode_I2cInit();

#if UCODE_PULLUP_COUNT
    /* Configure pull-ups. */
    uint32_t pinMask = 0;
    for (int i = 0; i < UCODE_PULLUP_COUNT; i++) {
        Chip_IOCON_SetPinConfig(NSS_IOCON, sPullups[i], IOCON_FUNC_0 | IOCON_RMODE_PULLUP);
        pinMask |= (uint32_t)NSS_GPIOn_PINMASK(sPullups[i]);
    }
    if (pinMask) {
        Chip_GPIO_SetPortDIRInput(NSS_GPIO, 0, pinMask);
    }
#endif

    /* Power up Ucode-I2C. Use the pull-up to slowly charge the external capacitor. */
    Chip_IOCON_SetPinConfig(NSS_IOCON, UCODE_I2C_POWER_PIN, IOCON_FUNC_0 | IOCON_RMODE_PULLUP);
    Chip_Clock_System_BusyWait_ms(5);
    Chip_GPIO_SetPinDIROutput(NSS_GPIO, 0, UCODE_I2C_POWER_PIN);
    Chip_GPIO_SetPinState(NSS_GPIO, 0, UCODE_I2C_POWER_PIN, true);
}

bool Ucode_Reset(void)
{
    bool ok = false;
    if (Ucode_I2cWrite(UCODE_ADDRESS_EPC_LENGTH, UCODE_DATA_EPC_LENGTH)) {
        if (Ucode_I2cWrite(UCODE_ADDRESS_CONFIG, UCODE_DATA_CONFIG)) {
            if (Ucode_SetNfcUid()) {
                if (Ucode_SetMark(UCODE_EVENT_PRISTINE)) {
                    ok = true;
                }
            }
        }
    }
    return ok;
}

void Ucode_DeInit(void)
{
    Ucode_I2cDeInit();

#if UCODE_PULLUP_COUNT
    /* Re-configure pull-ups. */
    uint32_t pinMask = 0;
    for (int i = 0; i < UCODE_PULLUP_COUNT; i++) {
        Chip_IOCON_SetPinConfig(NSS_IOCON, sPullups[i], IOCON_FUNC_0 | IOCON_RMODE_INACT);
        pinMask |= (uint32_t)NSS_GPIOn_PINMASK(sPullups[i]);
    }
    Chip_GPIO_SetPortDIROutput(NSS_GPIO, 0, pinMask);
    Chip_GPIO_SetPortOutLow(NSS_GPIO, 0, pinMask);
#endif

    Chip_GPIO_SetPinDIRInput(NSS_GPIO, 0, UCODE_I2C_POWER_PIN);
    Chip_IOCON_SetPinConfig(NSS_IOCON, UCODE_I2C_POWER_PIN, IOCON_FUNC_0 | IOCON_RMODE_PULLDOWN);
}

bool Ucode_SetNfcUid(void)
{
    ASSERT(sizeof(NSS_NFC_UID->bytes) == 8);
    uint8_t epc[sizeof(NSS_NFC_UID->bytes)];
    /* - Do not copy NSS_NFC_UID->bytes[3]: it is a checksum byte.
     * - The UCODE-I2C memory is 16-bit wide. Swap the byte order for each two successive NFC ID bytes.
     * - Append with a zero byte to fill the last 16-bit word.
     * Thus: 00112233CC556677 -> 0011 2244 5566 7700 -> 11 00, 44 22, 66 55, 00 77
     */
    epc[0] = NSS_NFC_UID->bytes[1];
    epc[1] = NSS_NFC_UID->bytes[0];
    epc[2] = NSS_NFC_UID->bytes[4];
    epc[3] = NSS_NFC_UID->bytes[2];
    epc[4] = NSS_NFC_UID->bytes[6];
    epc[5] = NSS_NFC_UID->bytes[5];
    epc[6] = 0x00;
    epc[7] = NSS_NFC_UID->bytes[7];

    bool ok = false;
    int tries = UCODE_I2C_ATTEMPTS;
    while ((!ok) && tries) {
        tries--;
        ok = Write(UCODE_ADDRESS_EPC, (uint16_t *)epc, sizeof(epc) / sizeof(uint16_t));
        if (ok) {
            ok = CompareNfcUid();
        }
        if (!ok) {
            Backoff();
        }
    }
    return ok;
}

bool Ucode_SetMark(unsigned int events)
{
    uint8_t mark[UCODE_SETMARKER_LENGTH];
    mark[0] = (uint8_t)(events & 0xFF);
    mark[1] = (uint8_t)((events >> 8) & 0xFF);
    mark[2] = (uint8_t)((events >> 16) & 0xFF);
    mark[3] = 'T';

    bool ok = false;
    int tries = UCODE_I2C_ATTEMPTS;
    while ((!ok) && tries) {
        tries--;
        ok = Write(UCODE_ADDRESS_EPC + sizeof(NSS_NFC_UID->bytes), (uint16_t *)mark, sizeof(mark) / 2);
        if (ok) {
            ok = CompareMark(events);
        }
        if (!ok) {
            Backoff();
        }
    }
    return ok;
}

bool Ucode_WriteData(unsigned int offset, const uint8_t * data, unsigned int len)
{
    uint16_t address = (uint16_t)(UCODE_ADDRESS_USER_MEMORY + offset);

    bool ok = false;
    int tries = UCODE_I2C_ATTEMPTS;
    while ((!ok) && tries) {
        tries--;
        ok = Write(address, (uint16_t *)data, len);
        if (!ok) {
            Backoff();
        }
    }
    return ok;
}

bool Ucode_ReadData(unsigned int offset, uint8_t * data, unsigned int len)
{
    uint16_t address = (uint16_t)(UCODE_ADDRESS_USER_MEMORY + offset);

    bool ok = false;
    int tries = UCODE_I2C_ATTEMPTS;
    while ((!ok) && tries) {
        tries--;
        ok = Read(address, (uint16_t *)data, len);
        if (!ok) {
            Backoff();
        }
    }
    return ok;
}
