#ifndef NFC_CONSOLE_H_
#define NFC_CONSOLE_H_

#include <string.h>
#include <stdlib.h>
#include <stdint.h>

#define MAX_TEXT_PAYLOAD 64
#define LOCALE "en" /**< Language used when creating TEXT records. */
#define MIME "nhs31xx/example.ndef" /**< Mime type used when creating MIME records. */

extern uint8_t sResponse[MAX_TEXT_PAYLOAD];
extern const char * tempResponseString;


void GenerateNdef_Text(void);

void generate_ndef_eeprom(void);
int ParseCommandFromNDEF(int powered_by_nfc);
void ExecuteCommand(int command_number);

void Number2String(int remaining, char * end, int size);
void DeciValue2String(int deciValue, char * end);


#endif /* NFC_H_ */
