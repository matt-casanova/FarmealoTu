"""Capas NASA GIBS para el visor «Explorar mi zona» — SOLO identificadores verificados.

Verificación (se ejecuta antes, no durante la demo):
  1) la capa existe en el WMTSCapabilities oficial EPSG:3857 (Web Mercator, el del mapa de la app);
  2) se toman de ahí TileMatrixSet, formato, fecha por defecto y la plantilla REST;
  3) se descarga UNA tesela real sobre Cuatro Cañadas, se valida que sea PNG y se lee el píxel del punto piloto
     (¿hay dato ahí o está vacío por nubes / sin pasada?).
El resultado queda en backend/data/cache/gibs_layers.json (versionado). El endpoint solo lee ese archivo.

Uso (desde backend/):  python -m pipeline.gibs verify
"""
from __future__ import annotations

import io
import json
import math
import xml.etree.ElementTree as ET
from datetime import datetime, timedelta, timezone
from pathlib import Path

import requests

from pipeline import power as P

CAPS_URL = "https://gibs.earthdata.nasa.gov/wmts/epsg3857/best/1.0.0/WMTSCapabilities.xml"
GIBS_DOCS = "https://nasa-gibs.github.io/gibs-api-docs/access-basics/"
CACHE_FILE = P.CACHE_DIR / "gibs_layers.json"
NS = {"w": "http://www.opengis.net/wmts/1.0", "ows": "http://www.opengis.net/ows/1.1",
      "xlink": "http://www.w3.org/1999/xlink"}
ATTRIBUTION = "Imagery: NASA GIBS (Global Imagery Browse Services), NASA EOSDIS"

# Capa del visor → capa GIBS candidata (elegidas de GetCapabilities el 14-nov-2026).
LAYER_SPECS = [
    {"id": "rain", "name": "Lluvia", "gibs_id": "IMERG_Precipitation_Rate",
     "product": "GPM IMERG — tasa de precipitación diaria", "resolution": "~10 km (0.1°)",
     "interpretation": "Dónde llovió ese día en la región. Colores más intensos = más lluvia.",
     "explanation": "Estimación satelital (GPM IMERG). Muestra el patrón regional de la lluvia, no la lluvia exacta "
                    "de tu parcela."},
    {"id": "soil_moisture", "name": "Humedad del suelo", "gibs_id": "SMAP_L4_Analyzed_Root_Zone_Soil_Moisture",
     "product": "SMAP L4 — humedad de la zona radicular (0–100 cm), 12z", "resolution": "~9 km",
     "interpretation": "Cuánta agua hay en la zona de raíces de la región. Más color = suelo más húmedo.",
     "explanation": "Estimación de modelo con datos satelitales asimilados (SMAP L4), a ~9 km. No es un sensor "
                    "enterrado en tu campo."},
    {"id": "heat", "name": "Calor", "gibs_id": "VIIRS_NOAA20_Land_Surface_Temp_Day", "search_back_days": 10,
     "product": "VIIRS NOAA-20 — temperatura de la superficie (día)", "resolution": "~1 km",
     "interpretation": "Qué tan caliente estaba la superficie del suelo de día. Puede haber huecos por nubes.",
     "explanation": "Temperatura de la superficie vista por satélite (no la del aire). Un día nublado deja huecos "
                    "sin dato."},
    {"id": "vegetation", "name": "Vegetación", "gibs_id": "VIIRS_NOAA20_NDVI_8Day",
     "product": "VIIRS NOAA-20 — NDVI (8 días móviles)", "resolution": "~500 m",
     "interpretation": "Qué tan verde está la vegetación. Verde más intenso = más cobertura vegetal.",
     "explanation": "Índice de verdor (NDVI). Un NDVI bajo sugiere poca cobertura, pero por sí solo no demuestra "
                    "que un campo esté en barbecho."},
]


def _tile_xy(lat: float, lon: float, z: int) -> tuple[int, int]:
    n = 2 ** z
    x = int((lon + 180.0) / 360.0 * n)
    lat_r = math.radians(lat)
    y = int((1.0 - math.asinh(math.tan(lat_r)) / math.pi) / 2.0 * n)
    return x, y


def _pixel_in_tile(lat: float, lon: float, z: int) -> tuple[int, int]:
    n = 2 ** z
    fx = (lon + 180.0) / 360.0 * n
    fy = (1.0 - math.asinh(math.tan(math.radians(lat))) / math.pi) / 2.0 * n
    return int((fx % 1) * 256), int((fy % 1) * 256)


def _parse_caps(xml_bytes: bytes) -> dict:
    root = ET.fromstring(xml_bytes)
    out = {}
    for layer in root.iter(f"{{{NS['w']}}}Layer"):
        ident = layer.findtext("ows:Identifier", namespaces=NS)
        if not ident:
            continue
        dim = layer.find("w:Dimension", NS)
        legend = layer.find("w:Style/w:LegendURL", NS)
        colormap = None
        for md in layer.findall("ows:Metadata", NS):
            href = md.get(f"{{{NS['xlink']}}}href") or ""
            if "/colormaps/v1.3/" in href:
                colormap = href
        out[ident] = {
            "colormap_url": colormap,
            "title": layer.findtext("ows:Title", namespaces=NS),
            "formats": [f.text for f in layer.findall("w:Format", NS)],
            "tile_matrix_sets": [t.findtext("w:TileMatrixSet", namespaces=NS)
                                 for t in layer.findall("w:TileMatrixSetLink", NS)],
            "default_time": dim.findtext("w:Default", namespaces=NS) if dim is not None else None,
            "time_ranges": [v.text for v in dim.findall("w:Value", NS)] if dim is not None else [],
            "template": next((r.get("template") for r in layer.findall("w:ResourceURL", NS)), None),
            "legend_url": legend.get(f"{{{NS['xlink']}}}href") if legend is not None else None,
        }
    return out


def _legend_from_colormap(url: str | None, session=None, max_entries: int = 8) -> dict:
    """Leyenda oficial de GIBS (colormap v1.3): entradas con etiqueta → [{label, color}] + unidades."""
    if not url:
        return {"legend": [], "units": None, "legend_source": None}
    http = session or requests
    try:
        r = http.get(url, timeout=30)
        r.raise_for_status()
        root = ET.fromstring(r.content)
    except (requests.RequestException, ET.ParseError) as exc:
        return {"legend": [], "units": None, "legend_source": url, "legend_error": str(exc)}
    # Hay varios ColorMap (No Data, Fill, clasificaciones…): la escala de datos es la de más entradas.
    maps = [cm for cm in root.iter("ColorMap") if cm.find("Legend") is not None]
    maps.sort(key=lambda cm: len(cm.find("Legend").findall("LegendEntry")), reverse=True)
    for cm in maps[:1]:
        leg = cm.find("Legend")
        entries = [e for e in leg.findall("LegendEntry")]
        labeled = [e for e in entries if e.get("showLabel") == "true"] or entries
        if len(labeled) > max_entries:  # se reparten de forma pareja para no saturar la pantalla
            step = (len(labeled) - 1) / (max_entries - 1)
            labeled = [labeled[round(i * step)] for i in range(max_entries)]
        legend = []
        for e in labeled:
            rgb = [int(v) for v in (e.get("rgb") or "0,0,0").split(",")]
            legend.append({"label": e.get("label") or e.get("tooltip"), "color": "#%02X%02X%02X" % tuple(rgb[:3])})
        return {"legend": legend, "units": cm.get("units"), "legend_title": cm.get("title"), "legend_source": url}
    return {"legend": [], "units": None, "legend_source": url}


def _probe_tile(url: str, lat: float, lon: float, z: int, session=None) -> dict:
    http = session or requests
    r = http.get(url, timeout=30)
    res = {"http_status": r.status_code, "content_type": r.headers.get("Content-Type"), "bytes": len(r.content)}
    if r.status_code != 200 or not r.content.startswith(b"\x89PNG"):
        res["ok"] = False
        return res
    res["ok"] = True
    try:
        from PIL import Image  # pillow
        img = Image.open(io.BytesIO(r.content)).convert("RGBA")
        px, py = _pixel_in_tile(lat, lon, z)
        rgba = img.getpixel((px, py))
        alpha = img.getchannel("A")
        hist = alpha.histogram()
        res["pixel_at_point"] = {"x": px, "y": py, "rgba": list(rgba), "has_data": rgba[3] > 0}
        res["tile_coverage_pct"] = round(100 * (1 - hist[0] / sum(hist)), 1)
    except Exception as exc:  # noqa: BLE001
        res["pixel_at_point"] = {"error": str(exc)}
    return res


def verify_layers(lat: float = P.PILOT_LAT, lon: float = P.PILOT_LON, session=None) -> dict:
    http = session or requests
    caps_resp = http.get(CAPS_URL, timeout=120)
    caps_resp.raise_for_status()
    caps = _parse_caps(caps_resp.content)
    results = []
    for spec in LAYER_SPECS:
        entry = {**spec, "verified": False}
        c = caps.get(spec["gibs_id"])
        if not c:
            entry["reason"] = "identificador no encontrado en GetCapabilities EPSG:3857"
            results.append(entry)
            continue
        tms = c["tile_matrix_sets"][0]
        max_zoom = int(tms.rsplit("Level", 1)[1]) if "Level" in tms else 6
        fmt_ext = "png" if "image/png" in c["formats"] else "jpg"
        z = min(6, max_zoom)
        x, y = _tile_xy(lat, lon, z)
        # Fechas candidatas: la por defecto y, si la capa lo pide, días anteriores DENTRO del último rango publicado.
        candidates = [c["default_time"]]
        back = spec.get("search_back_days", 0)
        if back and c["default_time"] and c["time_ranges"]:
            start_last = c["time_ranges"][-1].split("/")[0]
            d0 = datetime.fromisoformat(c["default_time"][:10])
            candidates += [(d0 - timedelta(days=i)).date().isoformat() for i in range(1, back + 1)
                           if (d0 - timedelta(days=i)).date().isoformat() >= start_last[:10]]
        tried, first, found = [], None, False
        for date in candidates:
            tpl = (c["template"] or "").replace("{Time}", date or "default").replace("{TileMatrixSet}", tms)
            test_url = tpl.replace("{TileMatrix}", str(z)).replace("{TileRow}", str(y)).replace("{TileCol}", str(x))
            probe = _probe_tile(test_url, lat, lon, z, session)
            first = first or (date, tpl, test_url, probe)
            tried.append({"date": date, "http": probe.get("http_status"),
                          "has_data_at_point": (probe.get("pixel_at_point") or {}).get("has_data")})
            if not back:
                found = True
                break
            if probe.get("ok") and (probe.get("pixel_at_point") or {}).get("has_data"):
                found = True
                break
        if back:
            entry["dates_tried"] = tried
            if not found:  # ninguna fecha con dato en el punto: se publica la más reciente y se avisa
                date, tpl, test_url, probe = first
                entry["note"] = f"Sin dato en el punto en los últimos {back} días (nubes o sin pasada)"
        entry.update({
            "gibs_title": c["title"], "tile_matrix_set": tms, "format": fmt_ext, "date": date,
            "available_time_ranges_tail": c["time_ranges"][-2:], "max_zoom": max_zoom,
            "tile_url": tpl if probe.get("ok") else None, "legend_url": c["legend_url"],
            **_legend_from_colormap(c["colormap_url"], session),
            "test_tile": {"z": z, "x": x, "y": y, "url": test_url, **probe},
            "verified": bool(probe.get("ok")),
        })
        if not probe.get("ok"):
            entry["reason"] = f"tesela de prueba falló (HTTP {probe.get('http_status')})"
        results.append(entry)
    out = {"verified_at": datetime.now(timezone.utc).replace(microsecond=0).isoformat(),
           "capabilities_url": CAPS_URL, "point": {"lat": lat, "lon": lon}, "layers": results}
    CACHE_FILE.write_text(json.dumps(out, ensure_ascii=False, indent=2), encoding="utf-8")
    return out


def load_verified() -> dict | None:
    if not CACHE_FILE.exists():
        return None
    return json.loads(CACHE_FILE.read_text(encoding="utf-8"))


WATER_LAYER = {
    "id": "water", "name": "Agua (balance hídrico)", "status": "unavailable", "evidence_status": "unavailable",
    "source": "Derivado: lluvia efectiva (NASA POWER) − demanda del cultivo (ET0 FAO-56 × Kc)",
    "reason": "Requiere la ET0 FAO-56 (physics.py), todavía no integrada.",
    "interpretation": "Cuando esté disponible: valores negativos = la lluvia no alcanza para lo que pide el cultivo.",
    "explanation": "Es un cálculo regional (~50 km), NO un mapa de inundación observada.",
    "scale": P.SCALE_LABEL, "tile_url": None,
}


def _point_note(layer: dict) -> str | None:
    pix = ((layer.get("test_tile") or {}).get("pixel_at_point") or {})
    if pix.get("has_data") is True:
        return None
    if layer["id"] == "rain":
        return "Ese día no se detectó lluvia sobre el punto de referencia (sin color = sin lluvia o < 0.2 mm/h)."
    return layer.get("note") or "Sin dato sobre el punto de referencia ese día (nubes o sin pasada del satélite)."


def _legend_for_app(layer: dict) -> tuple[list[dict], str | None]:
    legend, units = layer.get("legend") or [], layer.get("units")
    if units == "K":  # temperatura: se agrega °C (conversión exacta) para que el agricultor la entienda
        out = []
        for e in legend:
            try:
                k = float(e["label"])
                out.append({"label": f"{k - 273.15:.0f} °C ({k:.0f} K)", "color": e["color"]})
            except (TypeError, ValueError):
                out.append(e)
        return out, "°C (convertido de K)"
    return legend, units


def build_map_layers() -> dict:
    """Respuesta de GET /api/map/layers (forma de docs/UI_BACKEND_CONTRACT.md §5). Solo lee la caché verificada."""
    v = load_verified()
    if not v:
        layers = [{"id": s["id"], "name": s["name"], "status": "unavailable", "evidence_status": "unavailable",
                   "reason": "Capas GIBS no verificadas todavía (python -m pipeline.gibs verify)", "tile_url": None}
                  for s in LAYER_SPECS] + [WATER_LAYER]
        return {"verified_at": None, "layers": layers}
    out = []
    for L in v["layers"]:
        ok = L.get("verified") and L.get("tile_url")
        legend, units = _legend_for_app(L)
        item = {
            "id": L["id"], "name": L["name"], "status": "available" if ok else "unavailable",
            "evidence_status": "documented" if ok else "unavailable",
            "source": f"NASA GIBS · {L['product']}", "gibs_layer": L["gibs_id"], "date": L.get("date"),
            "resolution": L["resolution"], "scale": f"regional ({L['resolution']})",
            "legend": legend, "legend_title": L.get("legend_title"), "unit": units, "legend_url": L.get("legend_url"),
            "interpretation": L["interpretation"], "explanation": L["explanation"],
            "tile_url": L["tile_url"] if ok else None,
            "tile_url_zxy": (L["tile_url"].replace("{TileMatrix}", "{z}").replace("{TileRow}", "{y}")
                             .replace("{TileCol}", "{x}") if ok else None),
            "tile_matrix_set": L.get("tile_matrix_set"), "projection": "EPSG:3857 (GoogleMapsCompatible)",
            "min_zoom": 0, "max_zoom": L.get("max_zoom"), "attribution": ATTRIBUTION,
            "at_point": {"has_data": ((L.get("test_tile") or {}).get("pixel_at_point") or {}).get("has_data"),
                         "note": _point_note(L)},
            "verified_at": v["verified_at"],
        }
        if not ok:
            item["reason"] = L.get("reason", "no verificada")
        out.append(item)
    out.append(WATER_LAYER)
    return {"verified_at": v["verified_at"], "capabilities_url": v["capabilities_url"],
            "point": v["point"], "attribution": ATTRIBUTION, "docs": GIBS_DOCS, "layers": out,
            "notes": ["Solo se publican capas cuyo identificador está en GetCapabilities y cuya tesela de prueba "
                      "sobre Cuatro Cañadas respondió con una imagen válida.",
                      "Las capas satelitales muestran el contexto regional; no son mediciones de una parcela."]}


if __name__ == "__main__":
    import sys
    if len(sys.argv) >= 2 and sys.argv[1] == "verify":
        res = verify_layers()
        for L in res["layers"]:
            t = L.get("test_tile", {})
            pix = t.get("pixel_at_point", {})
            print(f"- {L['id']:<14} {L['gibs_id']:<42} verificada={L['verified']} fecha={L.get('date')} "
                  f"zmax={L.get('max_zoom')} HTTP={t.get('http_status')} {t.get('bytes')}B "
                  f"cobertura_tesela={t.get('tile_coverage_pct')}% dato_en_punto={pix.get('has_data')} "
                  f"{L.get('reason', '')}")
        print("guardado:", CACHE_FILE)
    else:
        print(__doc__)
