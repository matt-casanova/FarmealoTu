# Documentación de FarmealoTu

| Carpeta / archivo | Contenido |
|---|---|
| [`contratos.md`](contratos.md) | Contratos JSON de la API (endpoints NASA y motor de recomendación) |
| [`ejecucion_backend.md`](ejecucion_backend.md) | Cómo correr el backend y el túnel |
| [`uso_ia.md`](uso_ia.md) | Declaración de uso de IA (borrador) |
| [`envio/`](envio/) | Borrador del formulario del proyecto en inglés (`project_form_en.md`). La documentación completa, en inglés y español, está en el [README](../README.md) |
| [`ejemplos/`](ejemplos/) | Respuestas reales de la API (p. ej. `compare_punto_piloto.json`) |
| [`reto/`](reto/) | Enunciado oficial del reto «Field Shift: Adapting Farms with NASA Data» |
| [`planificacion/`](planificacion/) | Documentos de origen que el plan reconcilia en su sección 0: Plan Maestro Unificado, prompt del MVP y guía de simulación de hardware |
| [`UI_BACKEND_CONTRACT.md`](UI_BACKEND_CONTRACT.md) | Lo que la app Android espera de cada endpoint |
| [`ui_review/`](ui_review/) | Revisión de diseño de la app: capturas antes/después (claro, oscuro, letra 1.3) y resumen de cambios |
| [`prompts/`](prompts/) | Prompts usados con Claude Code (respaldo de `uso_ia.md`): backend, motor, UI, pulido con Impeccable e integración NASA |
| [`plan_fuente/`](plan_fuente/) | Fuente HTML y script del plan en PDF ([`../FarmealoTu_Plan_de_Proyecto.pdf`](../FarmealoTu_Plan_de_Proyecto.pdf)). `build.py` necesita Microsoft Edge y `pypdf`; `plan.html` lo genera el mismo script |

Los documentos de `planificacion/` y `prompts/` son **históricos**: si algo choca con el plan en PDF, con el
README o con `contratos.md`, mandan estos últimos.
