# Densor - Software

## About the project 

This repository contains the design of Densor: an intraoral, actively powered, battery-free platform featuring multi-modal sensors and an extended lifespan. The repository consists of all components that make Densor:
- [Hardware](https://github.com/TUDSSL/densor/tree/master/NFC_harvester_battery_free/hardware)
- [Densor Firmware](https://github.com/TUDSSL/densor/tree/master/NFC_harvester_battery_free/software/DentalSensor_StorageProject)
- [Android app](https://github.com/TUDSSL/densor/tree/master/NFC_harvester_battery_free/software/source_ST25NFCApplication_V3_9)
- [Experiment setup and data collected](https://github.com/TUDSSL/densor/tree/master/NFC_harvester_battery_free/data/experiments)

This folder contains all the code related to the project i.e the firmware for the MCU , and the android app for the smartphone. 

## Folder layout

- **[STM32 CubeIDE Project - DentalSensor_StorageProject](https://github.com/TUDSSL/densor/tree/master/NFC_harvester_battery_free/software/DentalSensor_StorageProject):** contains the CubeIDE project for the STM32L021 MCU.
- **[Android App - source_ST25NFCApplication_V3_9](https://github.com/TUDSSL/densor/tree/master/NFC_harvester_battery_free/software/source_ST25NFCApplication_V3_9):** contains the Android Studio project to build the app.

## Running the software

1. **STM32 software**

Download and install [STM32CubeIDE](https://www.st.com/en/development-tools/stm32cubeide.html) from the official website. Then open the `DentalSensor_StorageProject` folder as a new project. Build and run the program with a debugger of your choice. In our case, the program is uploaded using a [J-Link Debug probe](https://www.segger.com/products/debug-probes/j-link/) and 10-pin header. The SWD programming pins can be used with other debugger/programmers (such as [ST-LINK](https://www.st.com/en/development-tools/st-link-v2.html) for example) with the required connections.

2. **Android app**

Follow the instructions [here](https://github.com/TUDSSL/densor/tree/master/NFC_harvester_battery_free/software/source_ST25NFCApplication_V3_9) to build and run the android app using Android studio. 

## Copyright

<a href="https://www.tudelft.nl"><img src="https://github.com/TUDSSL/DIPS/blob/master/images/tudelft_logo.png" width="300px"></a><a href="https://www.radboudumc.nl/en/about-radboudumc"><img src="https://github.com/TUDSSL/densor/blob/master/NFC_harvester_battery_free/images/radboudumc-logo-en-us.svg" width="250px"></a> 

Copyright (C) 2024 TU Delft Embedded Systems Group/Sustainable Systems Laboratory.

MIT Licence or otherwise specified. See [license](https://github.com/TUDSSL/ENGAGE/blob/master/LICENSE) file for details.