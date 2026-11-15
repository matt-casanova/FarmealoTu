# Uso de inteligencia artificial en FarmealoTu

> Borrador del 14-nov-2026. Se traduce al inglés para el formulario de envío. **Confirmar con la guía oficial de
> Space Apps 2026** qué formato pide la declaración de uso de IA.

## 1. Resumen

- **La IA se usó para construir la herramienta**, no dentro de ella. FarmealoTu **no usa IA, ML ni LLM** para calcular,
  ordenar ni explicar recomendaciones.
- El motor es **determinista y explicable**: reglas agronómicas, puntuación multicriterio (MCDA) con pesos que elige el
  agricultor, y bootstrap de años climáticos con semilla fija. La misma entrada da siempre la misma salida, y cada
  número se puede rastrear hasta su fórmula y su fuente.
- Los textos que ve el agricultor (razones, ventajas, riesgos) salen de **plantillas** que se llenan con los números
  reales del motor. No los genera un modelo de lenguaje en tiempo real.

## 2. Herramientas

| Herramienta | Para qué |
|---|---|
| **Claude Code** (Anthropic), asistente de programación en la terminal | Escribir y revisar código, pruebas y documentación |
| Skill **Impeccable** (pbakaus/impeccable, Apache 2.0) dentro de Claude Code | Guía de revisión de diseño UI/UX (Material 3, accesibilidad) para la app Android |

## 3. Qué se hizo con ayuda de IA, por parte

| Parte | Responsable | Uso de IA | Cómo lo verificó el equipo |
|---|---|---|---|
| Plan de proyecto (PDF) | Alvin | Redacción y maquetación a partir de los documentos del reto y de las decisiones del equipo | Revisión del equipo; las cifras de contexto citan su fuente |
| Pipeline NASA POWER, indicadores, capas GIBS, API base, nodo de campo | Alvin | Código y pruebas | Pruebas automáticas; respuestas reales de NASA POWER; capas GIBS verificadas con `GetCapabilities` y una tesela real |
| Motor de recomendación, referencia agrícola, incertidumbre | Matías | Código, pruebas y transcripción inicial de parámetros FAO-56 | 117 pruebas en verde; Kc, etapas y raíz cotejados contra el texto de FAO-56 (se corrigió un dato del trigo); falta el cotejo de una persona (`verified_at`) |
| Integración de la app con los endpoints NASA | Matías | Código y pruebas de contrato | Prueba de contrato con respuestas reales del backend |
| App Android (UI/UX) | Kendra | Código Compose y pulido de diseño con Impeccable | Capturas antes/después en el emulador (`docs/ui_review/`) |
| Gemelo digital, firmware, prototipo | Vanesa, Christian, Mateo | *A completar por el subequipo* | *A completar* |

## 4. Límites que se respetaron

- **No se inventaron datos.** Cada valor lleva fuente, fecha, unidad, escala y `evidence_status`
  (`documented | derived | assumption | farmer_provided | unavailable`). Lo que no tiene fuente es `null`, con el motivo.
- **Los parámetros transcritos por la IA se marcan.** `crops.json` dice cuáles fueron transcritos por el asistente y
  si ya se cotejaron.
- **Ni la app ni la API llaman a NASA durante el uso.** Los datos salen de una caché descargada antes de la demo.
- **Las decisiones de producto las tomó el equipo:** región, estrategias, pesos iniciales y política de confianza.
  Los supuestos se rotulan `assumption` y la app los muestra.

## 5. Datos y créditos

Datos: NASA POWER (Daily y Hourly API) y NASA GIBS (WMTS). Fuentes agronómicas: FAO-56 (Allen et al., 1998), FAO
(lluvia efectiva), ANAPO y MDPyEP. El catálogo completo está en `GET /api/sources`. FarmealoTu no está respaldado
por la NASA.
