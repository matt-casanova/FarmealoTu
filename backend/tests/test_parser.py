"""Parser de cultivos (plan 4.4): sinónimos, tildes, términos desconocidos y la frase del plan."""
import pytest

from engine.parser import NOT_RECOGNIZED, extract_crops, normalize, parse_crop, parse_rotation_sentence


@pytest.mark.parametrize("term,code", [
    ("soya", "soy"), ("Soja", "soy"), ("SOY", "soy"),
    ("maíz", "maize"), ("maiz", "maize"), ("Maíz", "maize"),
    ("sorgo", "sorghum"), ("girasol", "sunflower"), ("trigo", "wheat"),
    ("barbecho", "fallow"), ("Descanso", "fallow"), ("  chía ", "chia"),
])
def test_synonyms_accents_and_case(term, code):
    m = parse_crop(term)
    assert m.code == code and m.status == "reconocido"


@pytest.mark.parametrize("term", ["quinua", "arroz", "soy beans", "maizal", "", "sorgo forrajero x"])
def test_unknown_term_is_not_guessed(term):
    m = parse_crop(term)
    assert m.code is None and m.status == NOT_RECOGNIZED and m.message


def test_normalize():
    assert normalize("  Maíz   y  CHÍA ") == "maiz y chia"


def test_plan_sentence():
    s = "Estoy sembrando soya y antes tenía sorgo. Este año tengo poca agua y quiero evitar pérdidas."
    assert [c.code for c in extract_crops(s)] == ["soy", "sorghum"]
    r = parse_rotation_sentence(s)
    assert r["current_crop"] == "soy" and r["previous_crop"] == "sorghum" and r["message"] is None


def test_sentence_without_cue_does_not_guess_order():
    r = parse_rotation_sentence("Tengo soya y sorgo")
    assert r["recognized"] == ["soy", "sorghum"]
    assert r["current_crop"] is None and r["previous_crop"] is None and r["message"]


def test_sentence_with_unknown_crop_only():
    r = parse_rotation_sentence("Siembro quinua")
    assert r["recognized"] == [] and r["current_crop"] is None and r["message"]


def test_whole_words_only():
    assert extract_crops("maizal y trigal") == []   # «maizal» no es «maíz»: no se adivina
