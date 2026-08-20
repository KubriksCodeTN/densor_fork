/* USER CODE BEGIN Header */
// small doc mainpage
/**
 * @mainpage
 *
 * Densor is an intraoral sensor platform, attached to regular dental retainers or aligners, that can take measurements inside the mouth.
 *
 * Fatures:
 * 		- Contactless transmission: Densor is completely sealed and interacts with a smartphone via NFC protocol.
 * 		- Energy harvesting: To stay powered Densor harvests energy, either from the NFC communication itself or from the temperature gradient between the body and cold water.
 * 		- Free of external hardware: Operating Densor does not require any specialized tools, other than a NFC-compatible smartphone.
 */

/**
  ******************************************************************************
  * @file           : main.h
  * @brief          : Header for main.c file.
  *                   This file contains the common defines of the application.
  ******************************************************************************
  * @attention
  *
  * Copyright (c) 2023 STMicroelectronics.
  * All rights reserved.
  *
  * This software is licensed under terms that can be found in the LICENSE file
  * in the root directory of this software component.
  * If no LICENSE file comes with this software, it is provided AS-IS.
  *
  ******************************************************************************
  */
/* USER CODE END Header */

/* Define to prevent recursive inclusion -------------------------------------*/
#ifndef __MAIN_H
#define __MAIN_H

#ifdef __cplusplus
extern "C" {
#endif

/* Includes ------------------------------------------------------------------*/
#include "stm32l0xx_hal.h"

/* Private includes ----------------------------------------------------------*/
/* USER CODE BEGIN Includes */

/* USER CODE END Includes */

/* Exported types ------------------------------------------------------------*/
/* USER CODE BEGIN ET */

/* USER CODE END ET */

/* Exported constants --------------------------------------------------------*/
/* USER CODE BEGIN EC */

/* USER CODE END EC */

/* Exported macro ------------------------------------------------------------*/
/* USER CODE BEGIN EM */

/* USER CODE END EM */

/* Exported functions prototypes ---------------------------------------------*/
void Error_Handler(void);

/* USER CODE BEGIN EFP */

/* USER CODE END EFP */

/* Private defines -----------------------------------------------------------*/
#define NFC_SWITCH_Pin GPIO_PIN_14
#define NFC_SWITCH_GPIO_Port GPIOC
#define pd_Pin GPIO_PIN_0
#define pd_GPIO_Port GPIOA
#define moisture_Pin GPIO_PIN_1
#define moisture_GPIO_Port GPIOA

/* USER CODE BEGIN Private defines */

/* USER CODE END Private defines */

#ifdef __cplusplus
}
#endif

#endif /* __MAIN_H */
