#include "board.h"
#include "memory.h"
#include "storage/storage.h"
#include "compress/compress.h"

int App_CompressCb(int eepromByteOffset, int bitCount, void * pOut)
{
	Chip_Clock_System_SetClockFreq(COMPRESSION_SYSTEMCLOCK);	/* Set the clock to the compression speed */
	ASSERT(bitCount == STORAGE_UNCOMPRESSED_BLOCK_SIZE_IN_BITS);
    (void)bitCount; /* suppress [-Wunused-parameter]: its value is known to be STORAGE_UNCOMPRESSED_BLOCK_SIZE_IN_BITS. */
    uint8_t data[STORAGE_UNCOMPRESSED_BLOCK_SIZE_IN_BYTES];
    Chip_EEPROM_Read(NSS_EEPROM, eepromByteOffset, data, STORAGE_UNCOMPRESSED_BLOCK_SIZE_IN_BYTES);
    int length = Compress_Encode(data, STORAGE_UNCOMPRESSED_BLOCK_SIZE_IN_BYTES, pOut, STORAGE_UNCOMPRESSED_BLOCK_SIZE_IN_BYTES);
    Chip_Clock_System_SetClockFreq(REG_SYSTEMCLOCK);	/* Set the clock back to the default */
    return length * 8;
}

/**
 * Connects the compress module with the storage module. Provides decompression of data.
 * @see STORAGE_DECOMPRESS_CB
 * @see pStorage_DecompressCb_t
 */
int App_DecompressCb(const uint8_t * pData, int bitCount, void * pOut)
{
	Chip_Clock_System_SetClockFreq(COMPRESSION_SYSTEMCLOCK);	/* Set the clock to the compression speed */
	int length = Compress_Decode(pData, STORAGE_IDIVUP(bitCount, 8), pOut, STORAGE_UNCOMPRESSED_BLOCK_SIZE_IN_BYTES);
    Chip_Clock_System_SetClockFreq(REG_SYSTEMCLOCK);	/* Set the clock back to the default */
    return (length == STORAGE_UNCOMPRESSED_BLOCK_SIZE_IN_BYTES) ? STORAGE_UNCOMPRESSED_BLOCK_SIZE_IN_BITS : 0;
}

void Memory_Reset(void)
{
    Storage_Reset(true);
}

void Memory_Init(void)
{
    Storage_Init();
}

void Memory_Write(uint16_t data)
{
   Storage_Write(&data, 1);
}

uint16_t Memory_Read(void)
{
	uint16_t data;
	Storage_Read(&data, Storage_GetCount());
	return data;
}
void Memory_DeInit(void)
{
    Storage_DeInit();
}
