#include <ESP8266WiFi.h>

// Constants
static const int PING_TIME = 1000;
static const String PING_MSG = "SOCKET_PING";
static const String CONNECTED_MSG = "SOCKET_CONNECTED";
static const int PORT = 80;

WiFiServer server(PORT);

void setup() {
  Serial.begin(115200);
  Serial.println();
  WiFi.begin("CiscoB6848", "");   //Connect to our router
  Serial.print("Connecting");
  while (WiFi.status() != WL_CONNECTED)
  {
    delay(500);
    Serial.print(".");
  }
  Serial.println();
  Serial.print("Connected, IP address (connect to this address on the phone!): ");
  Serial.println(WiFi.localIP());
  server.begin();
}
void got_message(String msg) {
  Serial.println("[RX] " + msg);
}
void send_message(WiFiClient client, String msg) {
  client.println(msg);
}
void loop() {
  WiFiClient client = server.available();
  if (client) {
    String rx_buffer = "";
    String tx_buffer = "";
    int senttime = millis();
    Serial.println("Connected to client.");
    client.println(CONNECTED_MSG);
    int i = 0;
    while (client.connected()) {
      
      if (Serial.available() > 0) {
          // read the incoming byte:
          //incomingByte = Serial.readString();
          send_message(client, Serial.readString());
      }
      delay(1000);
      
      /*
      if(client.available()) {
        while(client.available()) {
          char c = client.read();
          if(c == '\n') {
            got_message(rx_buffer);
            send_message(client, rx_buffer);
            rx_buffer = "";
          } else {
            rx_buffer += c;
          }
        }
      }
      if(Serial.available()) {
        while(Serial.available()) {
          char c = Serial.read();
          if(c == '\n') {
            send_message(client, tx_buffer);
            tx_buffer = "";
          } else tx_buffer += c;
        }
      }
      */
      // Send regular ping messages to keep the connection open
      int now = millis();
      if (now - senttime > PING_TIME) {
        client.println(PING_MSG);
        senttime = now;
      }
    }
    
    // Give the client time to receive data before closing
    Serial.println("Disconnecting from client. Cy@!");
    delay(100);
    client.flush();
    client.stop();
  }
}
