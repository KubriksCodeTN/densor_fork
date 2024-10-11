// #include <LIS2DW12Sensor.h>
#include <Wire.h>

// LIS2DW12Sensor *Accel;

#define ACCEL_ADDR          0x19
#define ACCEL_CTRL1   			0x20
#define ACCEL_CTRL3         0x22
#define ACCEL_OUT_X_L				0x28
#define ACCEL_START_POL     0x03

#define BUFFER_SIZE         256
#define LEDB                24

#define INTERVAL            40 // maximum is 15 ms, currently set to 25 Hz

void setup() {
  // put your setup code here, to run once:
  Serial.begin(115200);

  // TwoWire dev_i2c(A4, A5);
  // dev_i2c.begin();
  // LIS2DW12Sensor Accelero(&dev_i2c);
  // Accel = &Accelero;
  // Accel->begin();
  // Accel->Enable_X();

  pinMode(LEDB, OUTPUT);

  Wire.begin();

  uint8_t enableAccel = 0b00010000; //low power mode, lowest frequency, 12 bit
  enableAccel = 0x3B; // 0b0011 10 11
  Wire.beginTransmission(ACCEL_ADDR);
  Wire.write(ACCEL_CTRL1);
  Wire.write(enableAccel);
  Wire.endTransmission();

}

void loop() {
  // put your main code here, to run repeatedly:
  long start = millis();
  // Start conversion
  Wire.beginTransmission(ACCEL_ADDR);
  Wire.write(ACCEL_CTRL3);
  Wire.write(ACCEL_START_POL);
  Wire.endTransmission();

  delay(10); // Moved from 100

  Wire.beginTransmission(ACCEL_ADDR);
  Wire.write(ACCEL_OUT_X_L);
  Wire.endTransmission();
  Wire.requestFrom(ACCEL_ADDR, 6);

  uint8_t out[6] = {};
  int idx = 0;
  while (Wire.available()) {
    out[idx] = Wire.read();
    idx++;
  }

  int16_t x = (((int16_t) out[1]) << 8) | out[0];
  int16_t y = (((int16_t) out[3]) << 8) | out[2];
  int16_t z = (((int16_t) out[5]) << 8) | out[4];

  // Serial.print("X: ");

  // Serial.print(((float) x) / 16384);
  // Serial.print(" Y: ");
  // Serial.print(",");
  // Serial.print(((float) y) / 16384);
  // Serial.print(" Z: ");
  // Serial.print(",");
  // Serial.println(((float) z) / 16384);

  Serial.write(((byte*) &x), 2); 
  Serial.write(((byte*) &y), 2); 
  Serial.write(((byte*) &z), 2); 
  Serial.println("");

  long end = millis();
  long sleepTime = max(INTERVAL - (end - start), 0);
  // Serial.print("Sleeping: ");
  // Serial.println(sleepTime);

  delay(sleepTime);

  // Serial.println("Scanning...");
  // int32_t out[3] = {};
  // Accel->Get_X_Axes(out);
  // delay(5000);

  // Serial.print("X: ");
  // Serial.println(out[0]);


  // byte error, address;
  // int nDevices;
  // nDevices = 0;
  // for(address = 1; address < 127; address++ )
  // {
  //   // The i2c_scanner uses the return value of
  //   // the Write.endTransmisstion to see if
  //   // a device did acknowledge to the address.
  //   Wire.beginTransmission(address);
  //   error = Wire.endTransmission();

  //   if (error == 0)
  //   {
  //     Serial.print("I2C device found at address 0x");
  //     if (address<16)
  //       Serial.print("0");
  //     Serial.print(address,HEX);
  //     Serial.println("  !");

  //     nDevices++;
  //   }
  //   else if (error==4)
  //   {
  //     Serial.print("Unknown error at address 0x");
  //     if (address<16)
  //       Serial.print("0");
  //     Serial.println(address,HEX);
  //   }
  // }
  // if (nDevices == 0)
  //   Serial.println("No I2C devices found\n");
  // else
  //   Serial.println("done\n");

  // delay(5000);           // wait 5 seconds for next scan
}
