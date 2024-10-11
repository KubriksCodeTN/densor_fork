#include <string.h>
#include <stdint.h>
#include <stdlib.h>


#ifndef FRAM_H_
#define FRAM_H_

/* OPCODEs for different function of the FRAM */
#define OPCODE_WRDI 	4
#define OPCODE_RDSR 	5
#define OPCODE_WREN 	6
#define OPCODE_RDID 	159
#define OPCODE_WRITE 	2
#define OPCODE_READ		3

/* Chip Select pin connected to the FRAM */
#define FRAM_CS_PIN IOCON_PIO0_3

void FRAM_Read(uint32_t addr, uint8_t *rdata, uint8_t rlen);
void FRAM_Write(uint32_t addr, uint8_t *wdata, uint8_t wlen);
void FRAM_Set_WREN();
uint8_t FRAM_Get_Status_Register();
void FRAM_Get_UID(uint8_t *UID);


#endif /* FRAM_H_ */
