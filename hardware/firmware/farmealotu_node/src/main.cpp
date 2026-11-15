// FarmealoTu · nodo de campo (ESP32) → POST /api/field-node/readings
// Contrato: shared/sensor_reading.schema.json (v1). Datos faltantes = null + bandera, nunca un valor inventado.
//
// Sensores (lista de materiales del prototipo):
//   internos : 3 × humedad capacitiva v1.2 (A/B/C) + 3 × DS18B20 (bus 1-Wire)
//   externos : DHT11 (temperatura y humedad del aire) + LDR (luz relativa)
//   opcional : BME680 (I2C) → poner USE_BME680 en 1 solo si está conectado
//
// Pines (plan, sección 6): humedad A/B/C = GPIO 34/35/32 · LDR = GPIO 33 · DS18B20 = GPIO 4 (pull-up 4.7 kΩ)
//                          DHT11 = GPIO 27 · BME680 = I2C 21/22. Todos los analógicos son ADC1 (funcionan con Wi-Fi).

#include <Arduino.h>
#include <WiFi.h>
#include <WiFiClientSecure.h>
#include <HTTPClient.h>
#include <OneWire.h>
#include <DallasTemperature.h>
#include <DHT.h>
#include <ArduinoJson.h>
#include <time.h>

// ======================= CONFIGURACIÓN (cambiar esto) =======================
const char* WIFI_SSID     = "TU_HOTSPOT";          // red 2.4 GHz (el ESP32 no ve redes de 5 GHz)
const char* WIFI_PASSWORD = "TU_CLAVE";

// URL COMPLETA del endpoint. Opción 1 (recomendada): túnel HTTPS del backend de Alvin.
//   Si se reinicia el túnel, la URL cambia y hay que volver a cargar el firmware.
const char* API_URL = "https://investor-subscription-gains-speeches.trycloudflare.com/api/field-node/readings";
// Opción 2 (respaldo): PC y ESP32 en el mismo hotspot, sin túnel:
//   const char* API_URL = "http://<IP-de-la-PC>:8000/api/field-node/readings";

const char* NODE_ID   = "farmealotu-node-01";
const char* SOURCE    = "physical";                 // "wokwi" si corre en el simulador Wokwi
const float DEPTH_CM  = 7.0;                        // profundidad real de las sondas: MEDIRLA y corregir
const float LDR_R_FIXED_OHM = 10000.0;              // resistencia fija del divisor del LDR: poner el valor real
const unsigned long SEND_EVERY_MS = 10000;          // cada 10 s

// Qué hay conectado HOY. Lo que no está conectado se manda como null (nunca un pin "al aire", que da basura).
#define NUM_ZONES  1                                // cuántas macetas con humedad + DS18B20 (1 a 3)
#define USE_DHT11  1                                // 1 = DHT11 conectado
#define USE_LDR    1                                // 1 = LDR conectado
#define USE_BME680 0                                // 1 solo si hay un BME680 conectado

// ======================= PINES =======================
// Pongan aquí los pines que REALMENTE usaron. La humedad tiene que ir a un pin ADC1 (GPIO 32, 33, 34, 35, 36 o 39):
// los pines ADC2 (p. ej. 25, 26, 27, 14, 12, 13, 15, 2, 4) NO leen analógico mientras el Wi-Fi está encendido.
#define PIN_HUM_A   34
#define PIN_HUM_B   35
#define PIN_HUM_C   32
#define PIN_LDR     33
#define PIN_DS18B20 4
#define PIN_DHT11   27

#if USE_BME680
#include <Wire.h>
#include <Adafruit_Sensor.h>
#include <Adafruit_BME680.h>
Adafruit_BME680 bme;
bool bmeOk = false;
#endif

OneWire oneWire(PIN_DS18B20);
DallasTemperature ds(&oneWire);                    // también lee DS1820 / DS18S20
#if USE_DHT11
DHT dht(PIN_DHT11, DHT11);
#endif

// Orden de las sondas DS18B20 por zona. getTempCByIndex() ordena por dirección interna, NO por posición:
// al arrancar se imprimen las direcciones; calentá una sonda con la mano para saber cuál es cuál y ajustá
// este arreglo (índice de bus para la zona A, B, C).
const int DS_INDEX_FOR_ZONE[3] = {0, 1, 2};

// Calibración de 2 puntos de cada sensor de humedad (lecturas crudas de Mateo): seco al aire y sumergido en agua.
// Da un ÍNDICE RELATIVO 0–100 (soil_moisture_index_pct), NO humedad volumétrica: soil_moisture_vwc sigue en null
// hasta la calibración gravimétrica. Poner 0 en ambos para no calcular el índice de esa zona.
const int HUM_RAW_AIRE[3] = {3450, 0, 0};          // sensor SECO al aire  → 0
const int HUM_RAW_AGUA[3] = {1300, 0, 0};          // sensor en AGUA       → 100

// LDR: en este divisor, valor crudo ALTO = MÁS luz. light_pct = crudo / 4095 × 100 (índice relativo, no lux).
#define LDR_HIGH_IS_BRIGHT 1
const char* ZONE_IDS[3] = {"A", "B", "C"};
const int HUM_PINS[3] = {PIN_HUM_A, PIN_HUM_B, PIN_HUM_C};

int readAdcAvg(int pin, int n = 64) {
  long acc = 0;
  for (int i = 0; i < n; i++) { acc += analogRead(pin); delay(2); }
  return (int)(acc / n);
}

void connectWifi() {
  if (WiFi.status() == WL_CONNECTED) return;
  WiFi.mode(WIFI_STA);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  Serial.print("Conectando a Wi-Fi");
  unsigned long t0 = millis();
  while (WiFi.status() != WL_CONNECTED && millis() - t0 < 20000) { delay(500); Serial.print("."); }
  if (WiFi.status() == WL_CONNECTED) {
    Serial.printf("\nWi-Fi OK · IP %s\n", WiFi.localIP().toString().c_str());
    configTime(0, 0, "pool.ntp.org", "time.google.com");   // hora UTC para "ts"
  } else {
    Serial.println("\nSin Wi-Fi: se reintenta en el próximo ciclo");
  }
}

// "ts" en UTC ISO-8601. Si no hay hora NTP, devuelve false y el campo no se manda (el servidor usa received_at).
bool utcNow(char* out, size_t len) {
  struct tm t;
  if (!getLocalTime(&t, 2000)) return false;
  strftime(out, len, "%Y-%m-%dT%H:%M:%SZ", &t);
  return true;
}

void printDsAddresses() {
  int n = ds.getDeviceCount();
  Serial.printf("DS18B20 encontrados: %d\n", n);
  for (int i = 0; i < n; i++) {
    DeviceAddress a;
    if (ds.getAddress(a, i)) {
      Serial.printf("  índice %d: ", i);
      for (int b = 0; b < 8; b++) Serial.printf("%02X", a[b]);
      Serial.println();
    }
  }
}

void setup() {
  Serial.begin(115200);
  delay(500);
  analogReadResolution(12);
  for (int i = 0; i < NUM_ZONES; i++) analogSetPinAttenuation(HUM_PINS[i], ADC_11db);
#if USE_LDR
  analogSetPinAttenuation(PIN_LDR, ADC_11db);
#endif

  ds.begin();
  printDsAddresses();
#if USE_DHT11
  dht.begin();
#endif
#if USE_BME680
  bmeOk = bme.begin(0x76) || bme.begin(0x77);
  if (bmeOk) {
    bme.setTemperatureOversampling(BME680_OS_8X);
    bme.setHumidityOversampling(BME680_OS_2X);
    bme.setPressureOversampling(BME680_OS_4X);
    bme.setIIRFilterSize(BME680_FILTER_SIZE_3);
    bme.setGasHeater(320, 150);
  } else {
    Serial.println("BME680 no encontrado (se manda null + bandera)");
  }
#endif
  connectWifi();
}

void loop() {
  connectWifi();

  JsonDocument doc;
  doc["schema_version"] = "1.0";
  doc["node_id"] = NODE_ID;
  doc["source"] = SOURCE;
  char ts[25];
  if (utcNow(ts, sizeof(ts))) doc["ts"] = ts;
  JsonArray qflags = doc["quality_flags"].to<JsonArray>();

  // ---------- internos: zonas A/B/C ----------
  JsonObject internal = doc["internal"].to<JsonObject>();
  internal["depth_cm"] = DEPTH_CM;
  JsonArray zones = internal["zones"].to<JsonArray>();
  ds.requestTemperatures();
  for (int z = 0; z < NUM_ZONES; z++) {
    JsonObject zo = zones.add<JsonObject>();
    zo["id"] = ZONE_IDS[z];
    JsonArray zf = zo["flags"].to<JsonArray>();

    int raw = readAdcAvg(HUM_PINS[z]);
    zo["soil_moisture_raw"] = raw;
    zo["soil_moisture_vwc"] = nullptr;          // sin calibración gravimétrica todavía: NO se inventa la VWC
    zf.add("moisture_uncalibrated");
    if (HUM_RAW_AIRE[z] != HUM_RAW_AGUA[z]) {    // índice relativo de 2 puntos (aire = 0, agua = 100)
      float idx = 100.0f * (HUM_RAW_AIRE[z] - raw) / (float)(HUM_RAW_AIRE[z] - HUM_RAW_AGUA[z]);
      zo["soil_moisture_index_pct"] = round(constrain(idx, 0.0f, 100.0f) * 10) / 10.0;
    }
    if (raw <= 10 || raw >= 4085) zf.add("moisture_adc_out_of_range");   // cable suelto o saturado

    float tc = ds.getTempCByIndex(DS_INDEX_FOR_ZONE[z]);
    if (tc == DEVICE_DISCONNECTED_C || tc < -40 || tc > 85) {   // -127 = sonda desconectada
      zo["soil_temp_c"] = nullptr;
      char f[32]; snprintf(f, sizeof(f), "ds18b20_%s_disconnected", ZONE_IDS[z]);
      zf.add(f);
    } else if (tc >= 84.9f) {                    // 85.0 °C = valor de encendido del DS18B20 (no midió): NO es real
      zo["soil_temp_c"] = nullptr;
      char f[32]; snprintf(f, sizeof(f), "ds18b20_%s_reset_85", ZONE_IDS[z]);
      zf.add(f);
    } else {
      zo["soil_temp_c"] = tc;
    }
  }

  // ---------- externos ----------
  JsonObject external = doc["external"].to<JsonObject>();

#if USE_DHT11
  float dt = dht.readTemperature();
  float dh = dht.readHumidity();
  if (isnan(dt) || isnan(dh)) {
    external["dht11"] = nullptr;
    qflags.add("dht11_read_fail");
  } else {
    JsonObject d = external["dht11"].to<JsonObject>();
    d["t_c"] = dt;
    d["rh_pct"] = dh;
  }
#else
  external["dht11"] = nullptr;                 // todavía no conectado
#endif

#if USE_LDR
  int ldrRaw = readAdcAvg(PIN_LDR);
  JsonObject l = external["ldr"].to<JsonObject>();
  l["raw"] = ldrRaw;
  l["mv"] = analogReadMilliVolts(PIN_LDR);
  l["r_fixed_ohm"] = LDR_R_FIXED_OHM;
  float lp = 100.0f * ldrRaw / 4095.0f;        // índice relativo 0–100 (no lux ni radiación)
  l["light_pct"] = round((LDR_HIGH_IS_BRIGHT ? lp : 100.0f - lp) * 10) / 10.0;
  if (ldrRaw >= 4085) qflags.add("ldr_adc_saturated");
#else
  external["ldr"] = nullptr;                   // todavía no conectado
#endif

#if USE_BME680
  if (bmeOk && bme.performReading()) {
    JsonObject b = external["bme680"].to<JsonObject>();
    b["t_c"] = bme.temperature;
    b["rh_pct"] = bme.humidity;
    b["p_hpa"] = bme.pressure / 100.0;
    b["gas_kohm"] = bme.gas_resistance / 1000.0;
  } else {
    external["bme680"] = nullptr;
    qflags.add("bme680_read_fail");
  }
#else
  external["bme680"] = nullptr;                // no instalado en este prototipo
#endif

  JsonObject cal = doc["calibration"].to<JsonObject>();
  cal["moisture_method"] = "two_point_air_water_index";   // "gravimetric_3pt" cuando se calibre la VWC
  cal["date"] = nullptr;

  String body;
  serializeJson(doc, body);
  Serial.println(body);

  // ---------- envío ----------
  if (WiFi.status() == WL_CONNECTED) {
    HTTPClient http;
    WiFiClientSecure tls;
    WiFiClient plain;
    bool https = strncmp(API_URL, "https://", 8) == 0;
    if (https) {
      tls.setInsecure();                       // prototipo: no se valida el certificado (datos no sensibles)
      http.begin(tls, API_URL);
    } else {
      http.begin(plain, API_URL);
    }
    http.setTimeout(15000);
    http.addHeader("Content-Type", "application/json");
    int code = http.POST(body);
    Serial.printf("POST → HTTP %d\n", code);    // 201 = guardada · 422 = el JSON no cumple el contrato
    if (code > 0) Serial.println(http.getString());
    http.end();
  }

  delay(SEND_EVERY_MS);
}
