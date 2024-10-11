# Earpiece
The earpiece is a tiny PCB containing a STMicroelectronics LIS2DW12 accelerometer. This PCB can be inserted in the ear using a commodity earplug to measure acceleration inside the ear canal. This PCB is used for the comparison between ear and mouth experiments.

## Fabrication
### Soldering
First, some solderpase is applied to the pads on the boards. Next, this solder paste is melted and equally distributed over the different pads using a soldering iron. Then, a hot air soldering station is used to re-melt the solder and apply the LIS2DW12 and its 100 nF decoupling capacitor. Finally, four wires are soldered to the connection pads which are used for power and data transfer.

### Sealing
To ensure the PCB is completely sealed, minimizing the risk of shorts and shocks, it is suspended in the air and covered in a thin layer of food grade epoxy. This is the same epoxy used for Densor. The particular epoxy used has a curing time of 3 days, after which the PCB can be used. The wires attached to the PCB are also partially coated in epoxy to strengthen them.

### Inserting
To make sure the PCB can sit comfortably in the ear, it is inserted inside a commodity earplug, normally used to block sound. The sound filter is removed from the plug by pulling it out and replaced with the PCB.

## Connection
The earpiece PCB is connected to an [Arduino Leonardo](https://docs.arduino.cc/hardware/leonardo/) which sets the accelerometer up correctly and then requests and reads a readings at a frequency of 25 Hz. The readings are then written on the serial bus of the Arduino. The firmware running on the system can be found at *lis2dw12_test_arduino.ino*

The Arduino is connected to a computer running the *serial_reader.py* script. This script takes the reading from the earpiece and writes it to a provided CSV file. (Note: the CSV file is overwritten. Please provide a name for a file that does not exist yet or can be overwritten.) The script is operated as follows:

`python serial_reader.py <path-to-serial-arduino> <path-to-csv-file>`

### Dependencies
The dependencies of the serial reader are:

- pyserial
- matplotlib

Both libraries are included in the conda environment for the Densor programs. This environment can be found at *../environment.yml*.

