#include <Adafruit_GFX.h>
#include <RGBmatrixPanel.h>

#define CLK 8  // MUST be on PORTB!
#define LAT A3
#define OE  9
#define A   A0
#define B   A1
#define C   A2
RGBmatrixPanel matrix(A, B, C, CLK, LAT, OE, false);


void setup() {
  matrix.begin();
  Serial.begin(9600);
}

void loop() {
  while (Serial.available() >= 5) {
    uint8_t x = Serial.read();
    uint8_t y = Serial.read();
    uint8_t r = Serial.read();
    uint8_t g = Serial.read();
    uint8_t b = Serial.read();
    
    matrix.drawPixel(x, y, matrix.Color888(r, g, b, 1));

    byte coords[] = {byte(x), byte(y)};
    Serial.write(coords, 2);
  }
}
