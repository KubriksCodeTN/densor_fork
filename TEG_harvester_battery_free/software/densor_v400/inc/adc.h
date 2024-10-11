#include <string.h>
#include <stdint.h>
#include <stdlib.h>


#ifndef ADC_H_
#define ADC_H_

#define VMEAS_ENABLE_PIN 	IOCON_PIO0_1
#define VMEAS_ADC_PIN 		IOCON_ANA0_0

#define VMEAS_R1			1500		/* in kOhm */
#define VMEAS_R2			470			/* in kOhm */

void VMEAS_On();
void VMEAS_Off();

int measure_VDD();

#endif /* ADC_H_ */
