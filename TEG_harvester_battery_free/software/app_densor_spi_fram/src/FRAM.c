#include "FRAM.h"
#include "board.h"
#include "SPI.h"


void FRAM_Get_UID(uint8_t *UID)
{
    /* The UID Should be uint8_t FRAM_UID[9] = {0xA1,0x2D,0xC2,0x7F,0x7F,0x7F,0x7F,0x7F,0x7F};
     * IMU can cause interference. Only first four bits match */

	uint8_t reg = OPCODE_RDID;
    SPI_Write_Read(&reg, 1, UID, 9);
}

void FRAM_Set_WREN()
{
	uint8_t reg = OPCODE_WREN;
    SPI_Write(&reg, 1);
}

uint8_t FRAM_Get_Status_Register()
{
	uint8_t reg = OPCODE_RDSR;
	uint8_t status_register = 0;
    SPI_Write_Read(&reg, 1, &status_register, 1);
    return status_register;
}

void FRAM_Write(uint32_t addr, uint8_t *wdata, uint8_t wlen)
{
	uint8_t write_buffer[255];
	write_buffer[0] = OPCODE_WRITE;

	write_buffer[1] = (addr & 0x00ff0000) >> 16;
	write_buffer[2] = (addr & 0x0000ff00) >> 8;
	write_buffer[3] = (addr & 0x000000ff);

	for(int i = 0; i < wlen; i++){
		write_buffer[i+4] = wdata[i];
	}

	FRAM_Set_WREN();
	SPI_Write(write_buffer, 14);
}

void FRAM_Read(uint32_t addr, uint8_t *rdata, uint8_t rlen)
{
	uint8_t read_buffer[4] = {OPCODE_READ, (addr & 0x00ff0000) >> 16, (addr & 0x0000ff00) >> 8, (addr & 0x000000ff)};
    SPI_Write_Read(&read_buffer, 4, rdata, rlen);
}
