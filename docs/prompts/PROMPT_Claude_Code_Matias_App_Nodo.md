# PROMPT — Claude Code · App Android: «Datos de terreno» con el nodo físico y el gemelo digital

> **Para Matías:** en la raíz del repo de Android (rama `master` actualizada) ejecutá `claude` y escribí:
> **«Lee `docs/prompts/PROMPT_Claude_Code_Matias_App_Nodo.md` y sigue sus instrucciones. Empieza por la Fase 0.»**

## 0. Contexto (15-nov-2026, verificado por Alvin)

La API (backend de Alvin, rama `master`) ya recibe y guarda lecturas de **dos fuentes reales**, que funcionan:

| Fuente | `node_id` | `source` | Qué manda |
|---|---|---|---|
| Prototipo físico (ESP32 de Mateo, cada 10 s) | `farmealotu-node-01` | `physical` | Zona A: `soil_moisture_raw`, **`soil_moisture_index_pct`** (nuevo), `soil_temp_c`; `dht11` (t_c, rh_pct); `ldr` (raw, mv, `light_pct`); `bme680` = null; `soil_moisture_vwc` = **null** (sin calibración gravimétrica) |
| Gemelo digital (simulación de Christian) | `farmealotu-gemelo-01` | `simulated` | Zona A: `soil_moisture_vwc` (fracción 0–1), `soil_temp_c`; `dht11`; `ldr.light_pct`; `scenario` (S1…S5); campo extra **`sim_time`** (hora dentro de la simulación; `ts` lo pone el servidor) |

- API en la red de la demo (hotspot «kjj»): **`http://10.244.148.243:8000/`** · monitor web: `http://10.244.148.243:8000/monitor`.
- Contrato: `shared/sensor_reading.schema.json` y `backend/app/schemas.py` (clase `Zone`).
- **Campo nuevo:** `internal.zones[].soil_moisture_index_pct` (0–100). Es un **índice RELATIVO del sensor** (calibración de 2 puntos de Mateo: seco al aire = 0, sumergido en agua = 100). **NO es humedad volumétrica (VWC)**: un suelo saturado tiene ~40–50 % de VWC, no 100 %.

**Problema actual:** «Datos de terreno» muestra la humedad solo desde `soil_moisture_vwc` (`moisturePct`). Para el nodo físico aparece **«s/d»**, aunque llegan el índice y el valor crudo.

## 1. Reglas

1. Rama nueva desde `origin/master`: `git fetch; git switch -c matias/app-nodo origin/master`. Commits pequeños. Podés hacer push de **esa** rama; **no** merge ni push a `master` (Alvin integra).
2. **No toques** `backend/`, `hardware/`, `shared/` ni el firmware.
3. **No inventes datos:** el índice NUNCA se muestra como «humedad del suelo» ni se convierte a VWC. Si un valor es null → «sin dato», nunca 0.
4. Mantené el diseño actual (Material 3, tema claro/oscuro, insignias SIMULADO / WOKWI / FÍSICO). Cambios mínimos y legibles al sol.
5. Textos en español sencillo para el agricultor.

## 2. Tareas

### Fase 0 — Inspección (sin editar)
Revisa: `data/model/ApiDtos.kt` (DTO de la zona: `soilMoistureRaw`, `ldr.lightPct`), `data/model/DtoMappers.kt` (`toFieldReadings`, `toReading`), `domain/ResultModels.kt` (`NodeReading` y la zona: `moistureVwc`, `moisturePct`, `moistureRaw`), `ui/screens/field/FieldDataScreen.kt` (líneas con `moisturePct`: texto, gráfico, lectura seleccionada y tabla), `viewmodel/SecondaryViewModels.kt` (`FieldViewModel.refresh`). Con el backend corriendo, mira una respuesta real:
`http://10.244.148.243:8000/api/field-node/readings?limit=3&source=physical` y `...&source=simulated`.

### Fase 1 — Humedad del suelo según lo que haya (P0)
1. Agrega `@SerialName("soil_moisture_index_pct") val soilMoistureIndexPct: Double? = null` al DTO de la zona, el campo en el modelo de dominio y en el mapper.
2. En **texto, tabla y lectura seleccionada**, mostrar en este orden de prioridad:
   - si hay `soil_moisture_vwc` → «Humedad del suelo: 21.0 %» (como hoy);
   - si no, pero hay índice → «Índice de humedad del sensor: 40 % (relativo, sin calibrar)» y, en chico, «lectura cruda 2600»;
   - si no, pero hay crudo → «Lectura cruda del sensor: 2600 (sin calibrar)»;
   - si no hay nada → «sin dato».
   Agrega una ayuda breve (glosario o texto secundario): «El índice compara la lectura con el sensor seco (0) y en agua (100). Sirve para ver si el suelo se moja o se seca; no es el porcentaje real de agua del suelo.»
3. **Gráfico:** no mezclar unidades. La serie usa VWC (%) si la fuente la tiene; si no, el índice, con el eje/leyenda rotulado «Índice del sensor (0–100)». Para FÍSICO hoy será el índice; para SIMULADO, la VWC.

### Fase 2 — Detalles (P1)
1. **Luz:** que `ldr.light_pct` se muestre como «Luz (índice relativo): 51 %» (no es lux ni radiación).
2. **Simulación:** junto a la insignia SIMULADO y el escenario, mostrar `sim_time` si viene («Hora simulada: 14-nov 10:20»). Es un campo extra: léelo con un `@SerialName("sim_time")` opcional o desde el JSON crudo.
3. **Carga por fuente:** hoy `refresh()` pide todas las lecturas (el backend devuelve hasta 2000) y la pantalla filtra por fuente. Pide por fuente (`source=physical` / `simulated`) o con `limit` razonable (p. ej. 300) para que el nodo físico (1 lectura cada 10 s) no tape a la simulación ni haga lenta la pantalla. Conserva el selector de fuente actual.
4. **Refresco:** si la pantalla ya refresca sola, déjalo; si no, refresco cada 10–15 s **solo mientras «Datos de terreno» está visible** (lifecycle), sin bucles ni tráfico en segundo plano.

### Fase 3 — Pruebas y verificación
1. Pruebas unitarias del mapper con extractos REALES (pídelos a la API o usa estos):
   - físico: `{"node_id":"farmealotu-node-01","source":"physical","internal":{"depth_cm":7,"zones":[{"id":"A","flags":["moisture_uncalibrated"],"soil_moisture_raw":2600,"soil_moisture_vwc":null,"soil_moisture_index_pct":39.5,"soil_temp_c":23.7}]},"external":{"dht11":{"t_c":23.5,"rh_pct":48.6},"ldr":{"raw":2100,"mv":1700,"r_fixed_ohm":10000,"light_pct":51.3},"bme680":null}}`
   - simulado: `{"node_id":"farmealotu-gemelo-01","source":"simulated","scenario":"S1","sim_time":"2026-11-14T10:20:00Z","internal":{"depth_cm":7,"zones":[{"id":"A","soil_moisture_vwc":0.292,"soil_temp_c":24.7,"flags":[]}]},"external":{"dht11":{"t_c":23.0,"rh_pct":55.0},"ldr":{"light_pct":79.0},"bme680":null}}`
   Verificar: índice ≠ VWC, null no se vuelve 0, `sim_time` se lee.
2. `.\gradlew.bat assembleDebug testDebugUnitTest` en verde (no borres tests).
3. En el celular/emulador conectado al hotspot «kjj»: pantalla **Conexión** → `http://10.244.148.243:8000/` → Guardar, **Modo demo apagado** → **Mi Zona → Datos de terreno**. Comprobar con datos en vivo: FÍSICO muestra el índice, la temperatura, el aire y la luz y cambia al regar la maceta; SIMULADO muestra la VWC, el escenario y la hora simulada. Capturas en claro y oscuro.
4. El modo demo (`FakeFarmealoTuRepository`) puede seguir igual; si lo tocas, que siga rotulado DEMO.

## 3. Entrega
- Lista de commits en `matias/app-nodo` (pusheada) y `git status`.
- Resultados reales de compilación y pruebas.
- Capturas de «Datos de terreno» con FÍSICO y con SIMULADO.
- Avisar a Alvin para que revise e integre en `master`.
