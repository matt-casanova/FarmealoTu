# Project submission form — DRAFT (English)

> **Verify before pasting.** The official guide (spaceappschallenge.org → Resources → Project Submission Guide) could
> not be opened from the team PC on 14-Nov-2026 because of a TLS certificate error. The fields below follow the
> structure of earlier editions. **Confirm the 2026 field names, required fields, character limits and demo format in
> the official guide**, and adjust.
> **[UPDATE]** marks fields the team fills in at submission time (demo link, public repository, references list).
> The complete, up-to-date documentation is the repository [README](../../README.md).

---

## Project name
FarmealoTu

## Challenge
Field Shift: Adapting Farms with NASA Data

## High-level project summary
FarmealoTu is an Android decision-support app that helps farmers in Cuatro Cañadas (Santa Cruz, Bolivia) compare
crop-rotation strategies, including their current rotation. It does this with NASA Earth observations, local crop
parameters and the farmer's own priorities: water, soil and risk.
- **Inputs:** NASA POWER daily data from 2015 to today, turned into agro-climatic indicators (dry spells, rainfall
  anomaly, heat days, heavy rain, effective rainfall), and NASA GIBS satellite layers for context.
- **Engine:** deterministic and explainable (rules, multi-criteria scoring with farmer-set weights, and a bootstrap
  over historical years). No machine learning or LLM is involved.
- **What each strategy shows:** why it was suggested, the evidence behind it, benefits and trade-offs, an uncertainty
  range, missing information, and questions for a local agronomist.
- **Honesty about data:** every number carries its source and scale (regional ~50 km, not the farm), and missing data
  is shown as missing.

The tool supports, and does not replace, farmer and agronomist judgment.

## Link to project demo
**[UPDATE: public link to the video or slides. Confirm the required format and length in the 2026 guide.]**

## Link to final project
https://github.com/matt-casanova/FarmealoTu (branch `master`)
**[UPDATE: confirm the repository is public, and the deployed backend URL if any]**

## Detailed project description

### What it does
A farmer answers five simple questions:
1. What do you grow now?
2. What did you grow before?
3. What worries you most?
4. Do you have irrigation?
5. Which crops can't you plant?

FarmealoTu then compares the current rotation (E0) with alternative 12-month rotations (summer + winter campaign) and
shows four modules:
- **Compare:** scores 0–100 per strategy, with partial-score badges when a criterion is missing.
- **Trade-offs:** advantage, disadvantage and main risk of each strategy, generated from real numbers.
- **Risks:** a months × years heat map of NASA POWER indicators.
- **How sure is this?:** P10–P90 ranges, ranking stability, what comes from NASA, what the farmer provided, what is
  assumed and what is missing.

A map view shows NASA GIBS layers for the area (IMERG precipitation, SMAP L4 soil moisture, VIIRS land-surface
temperature, VIIRS NDVI). A "Field data" tab shows readings from a small sensor node, always labelled SIMULATED, WOKWI
or PHYSICAL.

### How it works
- **Data pipeline (Python):**
  - Downloads, validates and caches NASA POWER daily and hourly data for the pilot point.
  - Verifies GIBS layers against their GetCapabilities and a real tile.
  - Nothing calls NASA during use.
- **Indicators:** computed per month, per campaign (summer Nov–Apr, winter May–Oct) and per agricultural year, with
  data-quality counts (valid days, % missing).
- **Engine (FastAPI backend):**
  - Feasibility rules: season, cycle length and excluded crops; each rule records its reason.
  - Water sub-score: crop water demand coverage, Kc × ET0 against effective rain.
  - Soil sub-score: cover months, residue, family diversity, repetition penalty.
  - Risk sub-score: historical dry-spell and heat exposure at flowering, heavy rain at harvest.
  - Weighted MCDA with the farmer's weights, with a declared missing-data policy.
  - Profile selection: E1 protect soil, E2 save water, E3 balanced (maximin).
- **Uncertainty:** 200 bootstrap resamples of whole agricultural years (seed 42) give P10/P50/P90 and ranking
  stability. Confidence is capped at "medium" because the data is regional.
- **App:** Kotlin and Jetpack Compose (Material 3), Spanish, designed for mid-range phones used outdoors.
- **Field node:** ESP32 with capacitive soil moisture, DS18B20, DHT11 and LDR sensors (BME680 optional), sending a
  reading every 3 s, plus a digital twin forced with NASA POWER hourly data. Both share one JSON contract and were
  verified live end to end (node → API → app).

### Benefits
- Makes long-term NASA climate records usable by farmers with no geospatial background.
- Shows when current crops are most exposed, for example sorghum flowering in June–August overlapping long dry spells.
- Shows how much the soil stays covered across the year.
- Explicit about uncertainty and data gaps, so it points to where local expertise is needed instead of hiding it.

### Limitations (declared in the app)
- NASA POWER is regional context, not a farm measurement.
- Crop parameters are generic FAO-56 values, and sowing dates are assumed.
- Scores are relative, not probabilities.
- No yield or price prediction.
- Daily ET0 (FAO-56 Penman–Monteith) comes from regional NASA POWER data; it reproduces the FAO-56 worked examples
  and matches the independent `pyet` implementation.

### Tools and languages
Python (FastAPI, pandas, NumPy, pytest), Kotlin (Jetpack Compose, Material 3, Retrofit, osmdroid), ESP32/Arduino,
Wokwi, Cloudflare Tunnel, Git/GitHub.

## Use of Artificial Intelligence
We used **Claude Code (Anthropic)** as a coding and documentation assistant for:
- the data pipeline, the API, the recommendation engine and its tests;
- the Android UI (with the open-source "Impeccable" design-review skill);
- the project plan.

The team reviewed and tested the work:
- 125 automated backend tests and 72 Android tests;
- checks against real NASA responses;
- GIBS layers verified tile by tile;
- crop parameters checked against the FAO-56 tables, which led to correcting one value the assistant had transcribed.

**FarmealoTu itself does not use AI, ML or LLMs to calculate, rank or explain recommendations.** The engine is
deterministic: the same input always gives the same output. The explanatory texts come from templates filled with the
engine's real numbers.

## NASA data
- **NASA POWER Daily API** (AG community), point −17.2374, −62.2427, 2015-01-01 to Nov-2026. Variables: T2M, T2M_MAX,
  T2M_MIN, PRECTOTCORR, RH2M, WS2M, ALLSKY_SFC_SW_DWN, GWETROOT.
  - Use: indicators, engine and regional context.
  - Docs: https://power.larc.nasa.gov/docs/services/api/temporal/daily/
- **NASA POWER Hourly API**: forcing for the digital twin (baseline, Dec-2025/Jan-2026 drought, Feb–Mar-2026 excess
  rain).
- **NASA GIBS WMTS** (EPSG:3857), in the map view:
  - IMERG_Precipitation_Rate;
  - SMAP_L4_Analyzed_Root_Zone_Soil_Moisture;
  - VIIRS_NOAA20_Land_Surface_Temp_Day;
  - VIIRS_NOAA20_NDVI_8Day.

## Space agency partner and other data
- **Crop parameters:** FAO Irrigation and Drainage Paper 56 (Allen et al., 1998), Tables 11, 12 and 22.
- **Effective rainfall method:** FAO (Brouwer & Heibloem, 1986).
- **Local context and campaigns:** ANAPO (Bolivian oilseed and wheat producers' association), and MDPyEP *Santa Cruz
  Industrial y Productiva 2024* for 2023 production.
- **Basemap:** © OpenStreetMap contributors.

## References
**[UPDATE: copy the full list with links from `GET /api/sources` and `backend/reference/crops.json`.]**
- Allen, R. G., Pereira, L. S., Raes, D., Smith, M. (1998). *Crop evapotranspiration*. FAO Irrigation and Drainage Paper
  56. https://www.fao.org/4/x0490e/x0490e00.htm
- NASA POWER Project. https://power.larc.nasa.gov/
- NASA GIBS. https://nasa-gibs.github.io/gibs-api-docs/
- Brouwer, C., Heibloem, M. (1986). *Irrigation Water Management: Irrigation Water Needs*. FAO.

## Tags
agriculture · crop rotation · decision support · NASA POWER · GIBS · soil health · climate risk · Bolivia
