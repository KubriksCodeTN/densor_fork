#ifndef SPI_H_
#define SPI_H_

#include <string.h>
#include <stdint.h>
#include <stdlib.h>


void SPI_Init();
void SPI_DeInit();
void SPI_Write(uint8_t cs_pin, uint8_t *wdata, uint8_t wlen);
void SPI_Write_Read(uint8_t cs_pin, uint8_t *wdata, uint8_t wlen, uint8_t *rdata, uint8_t rlen);


#endif /* SPI_H_ */
