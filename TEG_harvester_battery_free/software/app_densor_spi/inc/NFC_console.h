#ifndef NFC_CONSOLE_H_
#define NFC_CONSOLE_H_

#include <string.h>
#include <stdlib.h>

#define MAX_TEXT_PAYLOAD 64
#define LOCALE "en" /**< Language used when creating TEXT records. */
#define MIME "nhs31xx/example.ndef" /**< Mime type used when creating MIME records. */

void GenerateNdef_Text(void);
void ParseNdef(void);

void Number2String(int remaining, char * end, int size);
void DeciValue2String(int deciValue, char * end);


#endif /* NFC_CONSOLE_H_ */
