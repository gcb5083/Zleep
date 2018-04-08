#include <math.h>
#include <Wire.h>
//#include "CurieIMU.h"
#include "MMA7660.h"
MMA7660 acc;

const int B=4275;                 // B value of the thermistor
const int R0 = 100000;            // R0 = 100k
const int pinTempSensor = A0;     // Grove - Temperature Sensor connect to A5
const int pinSoundSensor = A1;    // sound yo

void setup()
{
    acc.init();
    pinMode(3, OUTPUT);
    Serial.begin(115200);
}

void loop()
{
    static long cnt     = 0;
    static long cntout  = 0;
    float ax,ay,az;
    int8_t x, y, z;

    acc.getXYZ(&x,&y,&z);
    acc.getAcceleration(&ax,&ay,&az);

    // Temperature
    int a = analogRead(pinTempSensor );
    float R = 1023.0/((float)a)-1.0;
    R = 100000.0*R;
    float temperature = 1.0/(log(R/100000.0)/B+1/298.15)-273.15;
   
    //Sound level
    int sound = analogRead(pinSoundSensor);

    String output = String(temperature) + "_" + String(sound) + "_" + String(ax) + "_" + String(ay) + "_" + String(az) + "_";

    for (int i = 0; i < output.length(); i++){
      Serial.write(output[i]);
    }
    //Serial.print(output);

    delay(1000);   
}
