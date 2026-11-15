"""Reconocimiento de nombres de cultivos (plan 4.4).

Usa los sinónimos de reference/crops.json. Ignora mayúsculas y tildes. Un término que no está en el
diccionario devuelve «no reconocido»: nunca se adivina el cultivo más parecido.
"""
from __future__ import annotations

import re
import unicodedata
from dataclasses import dataclass
from functools import lru_cache

from engine.reference import load_reference

NOT_RECOGNIZED = "no reconocido"
CHOOSE_FROM_LIST = "No reconocimos ese cultivo. Elegilo de la lista."
# Palabras que, en una frase libre, separan el cultivo actual del anterior («… y antes tenía sorgo»).
PREVIOUS_CUES = ("antes", "anterior", "anteriormente", "el ano pasado", "la campana pasada")


def normalize(text: str) -> str:
    """Minúsculas, sin tildes y con espacios simples."""
    nfd = unicodedata.normalize("NFD", text or "")
    no_marks = "".join(ch for ch in nfd if unicodedata.category(ch) != "Mn")
    return re.sub(r"\s+", " ", no_marks.lower()).strip()


@lru_cache(maxsize=1)
def synonym_map() -> dict[str, str]:
    """{sinónimo normalizado: código}. Un sinónimo repetido entre cultivos es un error de datos."""
    out: dict[str, str] = {}
    for code, crop in load_reference()["crops"].items():
        for syn in crop.get("synonyms", []):
            key = normalize(syn)
            if key in out and out[key] != code:
                raise ValueError(f"Sinónimo ambiguo en crops.json: {syn!r} ({out[key]} y {code})")
            out[key] = code
    return out


@lru_cache(maxsize=1)
def _pattern() -> re.Pattern:
    alts = sorted(synonym_map(), key=len, reverse=True)  # «cultivo de cobertura» antes que «cobertura»
    return re.compile(r"\b(" + "|".join(re.escape(a) for a in alts) + r")\b")


@dataclass(frozen=True)
class CropMatch:
    term: str
    code: str | None
    status: str           # "reconocido" | "no reconocido"
    message: str | None = None


def parse_crop(term: str) -> CropMatch:
    """Un término suelto (chip o texto corto) → código, o «no reconocido»."""
    code = synonym_map().get(normalize(term))
    if code is None:
        return CropMatch(term=term, code=None, status=NOT_RECOGNIZED, message=CHOOSE_FROM_LIST)
    return CropMatch(term=term, code=code, status="reconocido")


def extract_crops(text: str) -> list[CropMatch]:
    """Cultivos mencionados en una frase, en orden de aparición (sin repetir el mismo código seguido)."""
    found: list[CropMatch] = []
    for m in _pattern().finditer(normalize(text)):
        code = synonym_map()[m.group(1)]
        if not found or found[-1].code != code:
            found.append(CropMatch(term=m.group(1), code=code, status="reconocido"))
    return found


def parse_rotation_sentence(text: str) -> dict:
    """Frase libre → {current_crop, previous_crop, recognized, message}.

    Solo asigna «anterior» cuando la frase lo dice con una palabra clave («antes», «anterior», «el año
    pasado»…). Si no hay forma de saberlo, lo devuelve sin asignar y pide confirmar: no se adivina.
    """
    norm = normalize(text)
    crops = extract_crops(text)
    result = {"current_crop": None, "previous_crop": None, "recognized": [c.code for c in crops], "message": None}
    if not crops:
        result["message"] = CHOOSE_FROM_LIST
        return result
    cue = next((m.start() for cue in PREVIOUS_CUES for m in [re.search(r"\b" + cue + r"\b", norm)] if m), None)
    if cue is None:
        if len(crops) == 1:
            result["current_crop"] = crops[0].code
        else:
            result["message"] = "Mencionaste varios cultivos: confirmá cuál sembrás ahora y cuál antes."
        return result
    before = [m for m in _pattern().finditer(norm) if m.start() < cue]
    after = [m for m in _pattern().finditer(norm) if m.start() > cue]
    if before:
        result["current_crop"] = synonym_map()[before[-1].group(1)]
    if after:
        result["previous_crop"] = synonym_map()[after[0].group(1)]
    if result["current_crop"] is None or result["previous_crop"] is None:
        result["message"] = "Confirmá cuál sembrás ahora y cuál sembraste antes."
    return result
