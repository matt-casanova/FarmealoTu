"""Contexto agrícola de Cuatro Cañadas y catálogo de fuentes (GET /api/context/cuatro-canadas, GET /api/sources).

ESQUELETO de Alvin con información DOCUMENTADA del plan (sección 2.2 y anexo D). Matías es el dueño de la base
agrícola (reference/agricultural_context.json): cuando exista, este router debe leer de ahí.
"""
from __future__ import annotations

from fastapi import APIRouter

router = APIRouter(prefix="/api", tags=["contexto y fuentes"])

MDPYEP = "https://www.siip.produccion.gob.bo/noticias/files/2025-c2369-SANTA-CRUZ-V3-FM.pdf"

PRODUCTION_2023 = [  # (cultivo, código, t, ha) — re-tabulado del plan; VERIFICAR contra el PDF (Matías)
    ("Soya", "soy", 811959, 318083), ("Sorgo", "sorghum", 187590, 93955), ("Maíz", "maize", 73199, 17936),
    ("Girasol", "sunflower", 18589, 18025), ("Trigo", "wheat", 14978, 7383), ("Chía", "chia", 1973, 2592),
    ("Arroz", "rice", 367, 171),
]

SOURCES = [
    {"id": "nasa_power", "title": "NASA POWER — Daily/Hourly API (community AG)",
     "url": "https://power.larc.nasa.gov/docs/services/api/temporal/daily/",
     "description": "Temperatura, lluvia, humedad, viento y radiación diarios/horarios; base de los indicadores.",
     "reference_year": 2026, "geographic_scope": "punto (celda ~50 km)", "used_for": "indicadores, forzante del gemelo"},
    {"id": "nasa_gibs", "title": "NASA GIBS (WMTS)", "url": "https://nasa-gibs.github.io/gibs-api-docs/access-basics/",
     "description": "Teselas para el visor geoespacial (solo capas verificadas).", "reference_year": 2026,
     "geographic_scope": "global", "used_for": "visor"},
    {"id": "smap_l4", "title": "SMAP L4 SPL4SMGP v8", "url": "https://nsidc.org/data/spl4smgp/versions/8",
     "description": "Humedad del suelo (modelo + satélite, ~9 km). Opcional (P2).", "reference_year": 2026,
     "geographic_scope": "global", "used_for": "opcional"},
    {"id": "gpm_imerg", "title": "GPM IMERG", "url": "https://gpm.nasa.gov/data/imerg",
     "description": "Precipitación ~10 km. Opcional (P2).", "reference_year": 2026, "geographic_scope": "global",
     "used_for": "opcional"},
    {"id": "fao56", "title": "FAO-56: Crop evapotranspiration (Allen et al., 1998)",
     "url": "https://www.fao.org/4/X0490E/x0490e00.htm",
     "description": "ET0 Penman–Monteith y coeficientes Kc.", "reference_year": 1998, "geographic_scope": "global",
     "used_for": "ET0, Kc"},
    {"id": "fao_effective_rain", "title": "Brouwer y Heibloem (1986), Irrigation Water Needs, FAO",
     "url": "https://www.fao.org/4/s2022e/s2022e00.htm",
     "description": "Lluvia efectiva mensual empírica (supuesto declarado).", "reference_year": 1986,
     "geographic_scope": "global", "used_for": "lluvia efectiva"},
    {"id": "mdpyep_2024", "title": "MDPyEP — Santa Cruz Industrial y Productiva 2024 (datos 2023 INE/MDRyT)",
     "url": MDPYEP, "description": "Producción y superficie municipal 2023.", "reference_year": 2023,
     "geographic_scope": "municipio de Cuatro Cañadas", "used_for": "contexto"},
    {"id": "anapo_2024", "title": "ANAPO — siembra directa y rotación de cultivos (jul-2024)",
     "url": "https://anapobolivia.org/anapo-reafirma-la-produccion-sostenible-con-la-siembra-directa-y-la-rotacion-de-cultivos/",
     "description": "Prácticas promovidas; sorgo 67 % de la rotación de invierno (dato regional).",
     "reference_year": 2024, "geographic_scope": "regional (Santa Cruz)", "used_for": "contexto"},
    {"id": "eldeber_2025", "title": "El Deber — Cuatro Cañadas en zona de desastre (mar-2025)",
     "url": "https://eldeber.com.bo/santa-cruz/cuatro-canadas-se-declara-en-zona-de-desastre-por-el-desborde-del-rio-grande_507510",
     "description": "1104 ha de soya perdidas por desborde del río Grande.", "reference_year": 2025,
     "geographic_scope": "municipio", "used_for": "riesgos históricos"},
    {"id": "anapo_2026", "title": "ANAPO vía economy.com.bo — campaña de verano 2025-2026",
     "url": "https://economy.com.bo/negocios/anapo-reporta-disminucion-produccion-soya-verano-2025-2026-debido-lluvias",
     "description": "Sequía dic-2025/ene-2026 y exceso de lluvia feb–mar-2026.", "reference_year": 2026,
     "geographic_scope": "zonas productivas de Santa Cruz", "used_for": "riesgos históricos"},
]


@router.get("/context/cuatro-canadas", summary="Contexto agrícola documentado (histórico, no alertas actuales)")
def get_context():
    total_ha = sum(r[3] for r in PRODUCTION_2023)
    return {
        "municipio": "Cuatro Cañadas, Santa Cruz, Bolivia",
        "punto_de_referencia": {"lat": -17.2374, "lon": -62.2427,
                                "nota": "Coordenadas de referencia; no es una finca real ni el centroide oficial"},
        "produccion_2023": {
            "evidence_status": "documented", "source_url": MDPYEP, "reference_year": 2023,
            "nota": "Línea base municipal 2023; no son datos de 2026 ni predicciones para una finca. "
                    "Re-tabulado del plan: verificar contra el PDF.",
            "cultivos": [{"cultivo": n, "codigo": c, "produccion_t": t, "superficie_ha": ha,
                          "porcentaje_superficie": round(100 * ha / total_ha, 1)} for n, c, t, ha in PRODUCTION_2023],
        },
        "practicas": {
            "evidence_status": "documented", "source_url": SOURCES[7]["url"], "reference_year": 2024,
            "items": ["Campañas de verano (soya central) e invierno (sorgo, maíz, girasol, trigo)",
                      "Siembra directa y conservación del rastrojo promovidas por ANAPO",
                      "Cultivos de cobertura promovidos; su viabilidad depende del agua y del manejo",
                      "Sorgo ≈ 67 % de la superficie de rotación de invierno 2023 (dato regional ANAPO)"],
        },
        "riesgos_historicos": {
            "evidence_status": "documented",
            "nota": "Hechos históricos: no son alertas actuales. Las alertas de la app se calculan con datos fechados.",
            "items": [
                {"fecha": "2025-03", "evento": "1104 ha de soya perdidas por desborde del río Grande",
                 "source_url": SOURCES[8]["url"]},
                {"fecha": "2025-12/2026-01", "evento": "Sequía durante la campaña de verano",
                 "source_url": SOURCES[9]["url"]},
                {"fecha": "2026-02/2026-03", "evento": "Exceso de lluvias en etapas sensibles y cosecha",
                 "source_url": SOURCES[9]["url"]},
            ],
        },
    }


@router.get("/sources", summary="Catálogo de fuentes y bibliografía")
def get_sources():
    return {"sources": SOURCES}
