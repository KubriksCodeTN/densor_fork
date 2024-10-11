
# Densor - Hardware

## About the project 

This repository contains the design of Densor: an intraoral, actively powered, battery-free platform featuring multi-modal sensors and an extended lifespan. The repository consists of all components that make Densor:
- [Hardware](https://github.com/TUDSSL/densor/tree/master/NFC_harvester_battery_free/hardware)
- [Densor Firmware](https://github.com/TUDSSL/densor/tree/master/NFC_harvester_battery_free/software/DentalSensor_StorageProject)
- [Android app](https://github.com/TUDSSL/densor/tree/master/NFC_harvester_battery_free/software/source_ST25NFCApplication_V3_9)
- [Experiment setup and data collected](https://github.com/TUDSSL/densor/tree/master/NFC_harvester_battery_free/data/experiments)

This folder contains all the harware design files.

## Components used with Links

|Reference          |Value               |Footprint                                                 |Qty|DNP| Link to purchase |
|-------------------|--------------------|----------------------------------------------------------|---|---|------------------|
|C1,C2              |C_Polarized         |antennas:CPX3225A                                         |2  |   |https://www.digikey.com/en/products/detail/seiko-instruments/CPH3225A/8692444 |
|C4,C5,C6,C7,C12,C13|100n                |Capacitor_SMD:C_0603_1608Metric                           |6  |   |https://nl.farnell.com/en-NL/kemet/c0603x104m4ractu/cap-mlcc-0-1uf-16v-0603/dp/2904941?st=0603%200.1uf |
|D1                 |D_Zener_3v3         |Diode_SMD:D_SOD-323F                                      |1  |   |https://nl.farnell.com/on-semiconductor/mm3z3v6c/zener-diode-0-2w-3-6v-sod-323f/dp/2824925?CMP=i-ddd7-00001003 |
|D2                 |BAS316              |Diode_SMD:D_SOD-323                                       |1  |   | https://nl.farnell.com/nexperia/1ps76sb40-115/diode-schottky-sod323/dp/1349646?CMP=i-ddd7-00001003   |
|D3                 |D_Photo             |antennas:pd_vemd1060x01                                   |1  |   |https://nl.mouser.com/ProductDetail/Vishay-Semiconductors/VEMD1060X01?qs=pdQl6rENvLQkGyXPnNHd%2FQ%3D%3D&gclid=Cj0KCQjwmvSoBhDOARIsAK6aV7gSam9QYNxRe1G3dFcvfZGu4WvhXWOQdtZbTP3WNCzxGxyePn8TLa0aAiUbEALw_wcB |
|J1                 |Conn_ARM_JTAG_SWD_10|Connector_PinHeader_1.27mm:PinHeader_2x05_P1.27mm_Vertical|1  |   |https://nl.farnell.com/samtec/ftsh-105-01-l-d-k/connector-header-10pos-2row-1/dp/2856435?CMP=i-ddd7-00001003 |
|J2                 |Conn_01x08_Pin      |antennas:fpc_503480-0800                                  |1  |   | |
|J3                 |Conn_01x04_Pin      |Connector_PinHeader_2.54mm:PinHeader_1x04_P2.54mm_Vertical|1  |   | |
|R2,R3,R4           |100k                |Resistor_SMD:R_0603_1608Metric                            |3  |   |https://nl.farnell.com/vishay/crcw0603100kfkea/res-100k-1-0-1w-0603-thick-film/dp/1469649?st=0603%20100k |
|R7                 |R                   |Resistor_SMD:R_0603_1608Metric                            |1  |   | |
|U1                 |ST25DV04K-JFR6D3    |ST25DV04K-JFR6D3:PSON50P300X300X60-13N                    |1  |   |https://www.digikey.nl/en/products/detail/stmicroelectronics/ST25DV64K-JFR6D3/7312974  |
|U2                 |AM1805              |Package_DFN_QFN:QFN-16-1EP_3x3mm_P0.5mm_EP1.7x1.7mm       |1  |   |https://nl.farnell.com/en-NL/abracon/ab1805-t3/real-time-clock-i2c-qfn-16/dp/2467703?st=ab1805 |
|U4                 |STM32L021F3Ux       |Package_DFN_QFN:ST_UFQFPN-20_3x3mm_P0.5mm                 |1  |   |https://www.digikey.nl/en/products/detail/stmicroelectronics/STM32L021F4U6TR/6166967  |
|U5                 |LIS2DW12            |Package_LGA:Kionix_LGA-12_2x2mm_P0.5mm_LayoutBorder2x4y   |1  |   |https://nl.farnell.com/en-NL/stmicroelectronics/lis2dw12tr/lga-12-i-mems-digital-o-p-motion/dp/2849615?st=LIS2DW12TR  |
|U6                 |TS3A44159           |antennas:RSV16                                            |1  |   |https://www.digikey.nl/en/products/detail/texas-instruments/TS3A44159RSVR/1909585 |
|Y1                 |Crystal             |antennas:ABS05W_Crystal                                   |1  |   |https://nl.farnell.com/en-NL/abracon/abs05w-32-768khz-j-2-t/crystal-32-768khz-4pf-smd-1-6/dp/2850126?st=abs05w |

HREC safe solder paste: https://www.digikey.nl/en/products/detail/chip-quik-inc/SMD291SNL50T3/5130160


## Copyright

<a href="https://www.tudelft.nl"><img src="https://github.com/TUDSSL/DIPS/blob/master/images/tudelft_logo.png" width="300px"></a><a href="https://www.radboudumc.nl/en/about-radboudumc"><img src="https://github.com/TUDSSL/densor/blob/master/NFC_harvester_battery_free/images/radboudumc-logo-en-us.svg" width="250px"></a> 

Copyright (C) 2024 TU Delft Embedded Systems Group/Sustainable Systems Laboratory.

MIT Licence or otherwise specified. See [license](https://github.com/TUDSSL/ENGAGE/blob/master/LICENSE) file for details.