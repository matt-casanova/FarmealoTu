# FarmealoTu

> **DRAFT** (14-Nov-2026). Meant to replace the Spanish `README.md` before submission, once the Android app is merged
> into `master`. Lines marked **[UPDATE]** depend on work still in progress.

**NASA Space Apps Challenge 2026 · Challenge: _Field Shift: Adapting Farms with NASA Data_**
Team FarmealoTu: Alvin, Matías, Kendra (app) · Vanesa, Christian, Mateo (prototype and simulation).

FarmealoTu is an **Android decision-support app** for farmers in **Cuatro Cañadas (Santa Cruz, Bolivia)**. It compares
crop-rotation strategies, including the farmer's current one, using NASA Earth observations, local crop information
and the farmer's own priorities (water, soil, risk). For every strategy it shows why it was suggested, what evidence
supports it, its benefits and trade-offs, where information is missing, and which questions need a local agronomist.
**It supports, and does not replace, the judgment of farmers and agronomists.**

The full project plan (Spanish) is in [`FarmealoTu_Plan_de_Proyecto.pdf`](FarmealoTu_Plan_de_Proyecto.pdf).

---

## Why Cuatro Cañadas

Cuatro Cañadas is one of Bolivia's main soybean areas: soybean in the summer campaign, sorghum, maize, sunflower or
wheat in the dry winter campaign. Recent seasons show how variable the conditions are. From NASA POWER, cached in this
repo, for the reference point −17.2374, −62.2427:
- Rainfall from 1 January to 11 November 2026 was **+24 %** above the 2015–2025 mean for the same dates.
- The driest window of Dec-2025/Jan-2026 had **−32 %** rainfall.
- The wettest window of Feb–Mar-2026 had **+135 %** rainfall, consistent with what the producers' association ANAPO
  reported.

## How it works

```
NASA POWER (daily/hourly) ─┐
NASA GIBS (WMTS) ──────────┼─▶ pipeline/ (download, validation, cache) ─▶ analytics/ (agro-climatic indicators)
Local crop base (FAO-56) ──┘                                                  │
                                                                              ▼
Android app (Compose) ◀── HTTPS ── FastAPI /api/* ◀── engine/ (rules + MCDA) + analytics/uncertainty.py (bootstrap)
        ▲                                 ▲
        └── "Field data" tab ◀────────────┴── POST /api/field-node/readings ◀── digital twin · Wokwi · ESP32 node
```

1. **NASA data, cached.**
   - NASA POWER daily data 2015 → Nov-2026 for the pilot point: temperature, corrected precipitation, humidity, wind,
     solar radiation and root-zone soil wetness.
   - Four NASA GIBS layers for the map: IMERG precipitation, SMAP L4 root-zone soil moisture, VIIRS land-surface
     temperature, and VIIRS NDVI.
   - Neither the app nor the API calls NASA during use.
2. **Indicators.** Maximum dry spell, rainfall anomaly against 2015–2025, heat days (above the monthly P90), heavy-rain
   days (above P95) and effective rainfall (FAO method). They are computed per month, per campaign and per agricultural
   year (Nov → Oct).
3. **Rotation engine. Deterministic and explainable: no ML, no LLM.**
   - Feasibility rules: season, cycle length, crops the farmer excluded, and missing parameters. Each exclusion keeps
     its reason.
   - Three sub-scores from 0 to 100:
     - **Water**: share of crop water demand (Kc × ET0) covered by effective rainfall.
     - **Soil**: months covered, residue class, crop-family diversity, and a repetition penalty.
     - **Risk**: historical exposure to dry spells and heat at flowering, and to heavy rain at harvest.
   - The sub-scores are combined with the **farmer's own weights** (MCDA). If a criterion is missing, its weight is
     redistributed and the card says "partial score".
   - Four profiles:
     - **E0**: current rotation.
     - **E1**: protect soil.
     - **E2**: save water.
     - **E3**: balanced, chosen by the maximin rule.
4. **Uncertainty.** A bootstrap over 200 resamples of whole agricultural years (11 years, seed 42) gives P10/P50/P90 per
   strategy and ranking stability. With regional data only, the confidence level is capped at "medium".
5. **Field node (prototype).** A small ESP32 node and its digital twin share one JSON contract. The node has
   capacitive soil moisture and DS18B20 sensors in three pots, plus BME680, DHT11 and LDR. Readings are shown with a
   SIMULATED / WOKWI / PHYSICAL badge and **do not feed the engine**. **[UPDATE: prototype status]**

Every value carries a source, date, unit, scale and an evidence status: `documented | derived | assumption |
farmer_provided | unavailable`. Missing data is `null` with a reason, never a made-up number.

## Current status

| Part | Status |
|---|---|
| NASA POWER pipeline, indicators, GIBS layers, `/api/indicators`, `/api/nasa/status`, `/api/map/layers` | ✅ |
| Crop base with per-value provenance (FAO-56 Tables 11, 12 and 22) and crop-name parser | ✅ |
| Engine: feasibility, soil and risk scores, MCDA, E0/E1/E3, bootstrap uncertainty | ✅ |
| Water score, E2 and the global ranking | ⏳ Need daily ET0 (FAO-56 Penman–Monteith). Until then the app says so explicitly. **[UPDATE]** |
| Android app: polished UI, real data from the API, demo mode clearly labelled "DEMO" | ✅ **[UPDATE after the merge into `master`]** |
| Field node: digital twin, Wokwi, physical ESP32 | ⏳ **[UPDATE]** |
| Automated tests (backend) | ✅ 117 passing; none of them calls NASA |

## Run it (Windows PowerShell)

```powershell
cd backend
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8000     # API docs: http://localhost:8000/docs
python -m pytest -q
```

Android app:
1. Open the repository root in Android Studio.
2. In `local.properties`, set `farmealotu.apiBaseUrl` (the emulator uses `http://10.0.2.2:8000/`) and
   `farmealotu.demoMode=false`.
3. Build with `.\gradlew.bat assembleDebug`.

API contracts are in [`docs/contratos.md`](docs/contratos.md), and a real `compare` response is in
[`docs/ejemplos/compare_punto_piloto.json`](docs/ejemplos/compare_punto_piloto.json).

## NASA data

| Product | Use | Resolution |
|---|---|---|
| NASA POWER Daily API (AG community): T2M, T2M_MAX, T2M_MIN, PRECTOTCORR, RH2M, WS2M, ALLSKY_SFC_SW_DWN, GWETROOT | Indicators, engine, regional context | ~0.5° × 0.625° (~50 km) |
| NASA POWER Hourly API | Forcing for the digital twin (scenarios: baseline, Dec-25/Jan-26 drought, Feb–Mar-26 excess rain) | ~50 km, hourly |
| NASA GIBS WMTS: IMERG precipitation rate, SMAP L4 root-zone soil moisture, VIIRS NOAA-20 LST (day), VIIRS NOAA-20 NDVI 8-day | "Explore my area" map | ~10 km · ~9 km · ~1 km · ~500 m |

## Limitations

- NASA POWER is **regional context (~50 km cell)**, not a measurement of the farm. SMAP L4 is a model product with data
  assimilation, not a buried sensor.
- Crop parameters come from generic FAO-56 tables, without local calibration. The sowing date is assumed at the start
  of each campaign.
- Scores depend on rules and on weights. The initial weights (0.40/0.35/0.25) are product choices, not validated
  values. Scores are relative between options, not probabilities of success.
- P10–P90 is the model's variability under historical resampling, not a guarantee or a forecast.
- Heavy rain is shown as exposure. Changing the rotation does not remove flood risk.
- No yield or price prediction; economics is mentioned only qualitatively. The tool does not replace a soil test or a
  visit from an agronomist.
- The field node is a lab prototype. It does not statistically validate satellite data and does not feed the engine.

## Use of AI

Claude Code (Anthropic) was used as a coding and documentation assistant. **No AI, ML or LLM is used to compute or
explain recommendations.** Details are in [`docs/uso_ia.md`](docs/uso_ia.md) (Spanish).
**[UPDATE: English version]**

## Sources

- NASA POWER, NASA GIBS, and FAO-56 (Allen et al., 1998).
- FAO effective rainfall method (Brouwer & Heibloem, 1986).
- ANAPO, and MDPyEP, *Santa Cruz Industrial y Productiva 2024*.

The full catalogue with links is available at `GET /api/sources`. FarmealoTu is not endorsed by NASA.
