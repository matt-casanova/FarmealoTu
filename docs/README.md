# Documentación de FarmealoTu

| Carpeta / archivo | Contenido |
|---|---|
| [`contratos.md`](contratos.md) | Contratos JSON de la API (endpoints NASA y motor de recomendación) |
| [`ejecucion_backend.md`](ejecucion_backend.md) | Cómo correr el backend y el túnel |
| [`uso_ia.md`](uso_ia.md) | Declaración de uso de IA (borrador) |
| [`envio/`](envio/) | **Borradores para el envío, en inglés:** README final (`README_en.md`) y formulario del proyecto (`project_form_en.md`). Hay que confirmar los campos con la guía oficial 2026 |
| [`ejemplos/`](ejemplos/) | Respuestas reales de la API (p. ej. `compare_punto_piloto.json`) |
| [`reto/`](reto/) | Enunciado oficial del reto «Field Shift: Adapting Farms with NASA Data» |
| [`planificacion/`](planificacion/) | Documentos de origen que el plan reconcilia en su sección 0: Plan Maestro Unificado, prompt del MVP y guía de simulación de hardware |
| [`prompts/`](prompts/) | Prompts usados con Claude Code (respaldo de `uso_ia.md`) |
| [`plan_fuente/`](plan_fuente/) | Fuente HTML y script del plan en PDF. **Versión anterior al cambio de nombre** (dice «RotaSat»): el PDF oficial es [`../FarmealoTu_Plan_de_Proyecto.pdf`](../FarmealoTu_Plan_de_Proyecto.pdf). `build.py` necesita Microsoft Edge y `pypdf`; `plan.html` lo genera el mismo script |

Los documentos de `planificacion/` y `prompts/` son **históricos**: si algo choca con el plan en PDF, con el
README o con `contratos.md`, mandan estos últimos.
