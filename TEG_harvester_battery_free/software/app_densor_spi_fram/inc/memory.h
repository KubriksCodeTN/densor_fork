#ifndef MEMORY_H_
#define MEMORY_H_

#include <string.h>
#include <stdlib.h>

#include "compress/compress.h"

void Memory_Init(void);
void Memory_DeInit(void);
void Memory_Reset(void);

void Memory_Write(uint16_t data);
uint16_t Memory_Read();

#endif /* MEMORY_H_ */
