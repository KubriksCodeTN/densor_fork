# Densor - Smartphone App (Android)

## About the project 

This repository contains the design of Densor: an intraoral, actively powered, battery-free platform featuring multi-modal sensors and an extended lifespan. The repository consists of all components that make Densor:
- [Hardware](https://github.com/TUDSSL/densor/tree/master/NFC_harvester_battery_free/hardware)
- [Densor Firmware](https://github.com/TUDSSL/densor/tree/master/NFC_harvester_battery_free/software/DentalSensor_StorageProject)
- [Android app](https://github.com/TUDSSL/densor/tree/master/NFC_harvester_battery_free/software/source_ST25NFCApplication_V3_9)
- [Experiment setup and data collected](https://github.com/TUDSSL/densor/tree/master/NFC_harvester_battery_free/data/experiments)

## Smartphone App 

This project contains the android app for Densor. The app is based on the ST25 NFC Tap app from STMicroelectronics (version 3.9).

## Setup
To open and compile the project, please download [Android Studio](https://developer.android.com/studio). The project can be opened in Android Studio, by pressing open and selecting this folder. The app is build using gradle 7.4.1. The app can then be compiled to a phone by [enabling debugging on the phone](https://developer.android.com/studio/debug/dev-options), connecting the phone using USB to the computer running Android Studio, selecting the phone from the drop-down menu on the top of the screen (next to the triangular play button) and pressing the play button to compile and run the app on the phone.
