# RotaSat · Field Shift: Adapting Farms with NASA Data
## Plan maestro unificado — NASA Space Apps Challenge 2026 (14–15 de noviembre)

**Equipo:** 4 Ing. en Sistemas · 1 Ing. Industrial · 1 Ing. Electromecánica
**Documento de trabajo:** 12 de noviembre de 2026
**Une tres documentos:** (1) *Plan integral del equipo* (PDF de 36 páginas), (2) *RotaSat · Resumen del proyecto* y (3) *Prototipo mini: humedad + temperatura + BME280*.

> **Aviso de vigencia.** Fechas, reglas y estado de los datos se verificaron el 12-nov-2026. Las guías 2026 de **envío** y de **jurado** se publican el **13 de noviembre**: léanlas primero; si algo cambia, ellas prevalecen sobre este documento.

---

## Índice

0. [Cómo usar este documento y reconciliación entre los tres documentos](#0-cómo-usar-este-documento-y-reconciliación-entre-los-tres-documentos)
1. [Resumen ejecutivo: el plan en una página](#1-resumen-ejecutivo-el-plan-en-una-página)
2. [El problema y el análisis del reto](#2-el-problema-y-el-análisis-del-reto)
3. [Conceptos científicos en lenguaje simple](#3-conceptos-científicos-en-lenguaje-simple)
4. [Visión de la solución: la app](#4-visión-de-la-solución-la-app)
5. [Datos NASA y complementarios](#5-datos-nasa-y-complementarios)
6. [Modelo analítico paso a paso](#6-modelo-analítico-paso-a-paso)
7. [Las estrategias de rotación y la tarjeta de estrategia](#7-las-estrategias-de-rotación-y-la-tarjeta-de-estrategia)
8. [Diseño centrado en el agricultor (UX)](#8-diseño-centrado-en-el-agricultor-ux)
9. [El prototipo físico: caja de tierra de 3 zonas](#9-el-prototipo-físico-caja-de-tierra-de-3-zonas)
10. [Organización del equipo y reparto de tareas](#10-organización-del-equipo-y-reparto-de-tareas)
11. [Cronograma hasta la entrega](#11-cronograma-hasta-la-entrega)
12. [Riesgos y planes de contingencia](#12-riesgos-y-planes-de-contingencia)
13. [Validación y control de calidad](#13-validación-y-control-de-calidad)
14. [Entrega, presentación y defensa ante el jurado](#14-entrega-presentación-y-defensa-ante-el-jurado)
15. [Después del evento: hoja de ruta](#15-después-del-evento-hoja-de-ruta)
- [Anexo A. Repositorio y pila](#anexo-a-repositorio-y-pila)
- [Anexo B. Contrato de la API](#anexo-b-contrato-de-la-api)
- [Anexo C. Base de cultivos (crops.csv)](#anexo-c-base-de-cultivos-cropscsv)
- [Anexo D. Código de referencia (Python)](#anexo-d-código-de-referencia-python)
- [Anexo E. Esquema de firmware del ESP32](#anexo-e-esquema-de-firmware-del-esp32)
- [Anexo F. Backlog priorizado](#anexo-f-backlog-priorizado)
- [Anexo G. Fuentes y enlaces](#anexo-g-fuentes-y-enlaces)

---

## 0. Cómo usar este documento y reconciliación entre los tres documentos

**Si solo tienen 10 minutos:** lean la sección 1, los roles de la sección 10 y el cronograma de la sección 11. El resto es referencia para consultar durante el evento.

Al juntar los tres documentos encontré diferencias. Estas son las decisiones que tomé para unificarlos; **revísenlas y corríjanlas si no coinciden con lo que acordaron como equipo**:

| # | Tema | Lo que decía cada documento | Decisión en este documento unificado |
|---|---|---|---|
| 1 | **Configuración del prototipo** | El *Resumen* sugería 2 profundidades por zona, ADS1115, panel solar + 18650 con *deep sleep* y pluviómetro. El *Prototipo mini* usa 1 profundidad (8–10 cm), 3 sensores de humedad directo a pines ADC1 del ESP32, 3 DS18B20, 1 BME280 y power bank. | **La base de construcción es el Prototipo mini** (más simple y realista en 27 horas). Las funciones del *Resumen* pasan a ser **mejoras opcionales** (sección 9.10). |
| 2 | **Formato JSON del nodo** | *Resumen*: `moisture_10cm` y `moisture_30cm`. *Mini*: `moisture` (una sola profundidad). | Un esquema único con `moisture` obligatorio y `moisture_30cm` opcional (Anexo B). |
| 3 | **Validación contra satélite** | El plan original hablaba de "validación satélite vs terreno con sesgo y RMSE". Ambos documentos del prototipo advierten que **no** debe compararse la caja cuantitativamente con SMAP. | **Corrijo el plan original:** la caja es una *prueba de concepto* con comparación **cualitativa**. La validación estadística real requiere un campo real o series abiertas (ISMN). Cambia la tarea de E1 y los criterios de "hecho" (secciones 10.6 y 13). |
| 4 | **Rol del hardware en el MVP** | El *Resumen* aclara que la caja **no alimenta el motor de recomendación** en el MVP. | Se mantiene: el hardware es **extensión**. La app debe funcionar completa sin él. |
| 5 | **Alimentación** | *Resumen*: solar + 18650. *Mini*: power bank. | Power bank para la demo (con la advertencia de apagado por bajo consumo); solar/18650 queda como mejora. |
| 6 | **Tiempos de curado de la silicona** | El *Mini* arma la caja el sábado por la mañana y manda "dejar secar bien antes de llenar". | **Riesgo nuevo que detecté:** muchas siliconas tardan del orden de un día en curar por completo. Ver sección 9.9: usar la **variante de 3 recipientes** (sin divisores sellados) o consultar al Local Lead si se pueden sellar piezas el viernes. |
| 7 | **Numeración de secciones** | El PDF original tenía 14 secciones. | Se añadió la sección 9 (prototipo) y las demás se corrieron un número a partir de ahí. |

---

## 1. Resumen ejecutivo: el plan en una página

**El reto en una frase.** Construir una herramienta de apoyo a la decisión que combine observaciones de la Tierra de la NASA con información local de suelo, características de cultivos y prioridades del agricultor, para comparar **al menos tres estrategias de rotación (una de ellas, la rotación actual)** y explicar, para cada una, por qué se sugiere, qué evidencia la respalda, qué beneficios ofrece, qué información falta y qué riesgos debe revisar un experto local. La herramienta **apoya, no reemplaza**, el criterio del agricultor.

**Lo que construimos (RotaSat).** Una aplicación web de cinco pasos (ubicar el campo → describir la rotación actual → priorizar → comparar estrategias → entender la evidencia) respaldada por un motor que calcula indicadores con datos NASA (POWER, GPM IMERG, SMAP, MODIS/VIIRS/HLS, SRTM, GRACE-FO y, como complemento, ECOSTRESS), detecta ventanas de suelo desnudo, puntúa estrategias con un método multicriterio explicable y muestra incertidumbre y vacíos de información. **Como extensión**, un prototipo físico (caja de tierra con 3 zonas y sensores) alimenta la pestaña "Datos de terreno" y demuestra que un campo no es uniforme.

> **Lo urgente: el evento es este fin de semana.** Sábado 14 y domingo 15 de noviembre de 2026 (hoy es jueves 12). Según la guía de envío 2025, el formulario abre el sábado a las 9:00 y cierra el domingo a las 23:59 (hora del evento local).

### Personas y roles

| Persona | Rol | Aporte principal |
|---|---|---|
| **S1** · Sistemas | Líder técnico + datos geoespaciales | Datos NASA fluyendo y armonizados; repositorio; integración y publicación |
| **S2** · Sistemas | Analítica e IA | Indicadores, fenología/suelo desnudo, incertidumbre, escenarios, explicaciones |
| **S3** · Sistemas | Motor de recomendación + API | Base de cultivos, estrategias, puntaje multicriterio, tarjetas, despliegue, endpoint del nodo |
| **S4** · Sistemas | Frontend, UX y mapas | Flujo de 5 pasos, mapa, gráficos, sliders, pestaña "Datos de terreno" |
| **I1** · Industrial | Producto, agroeconomía y gestión | Cultivos y reglas con fuentes, pesos, entrevistas, alcance, pitch y envío |
| **E1** · Electromecánica | Física de campo, sensores y prototipo | ET0/balance hídrico, **caja de 3 zonas**, calibración, firmware y envío de datos |

### Seis decisiones que deben quedar cerradas antes del sábado a las 9:00

1. **Región piloto y sistema de cultivos** (criterios en 4.6) y una persona local que pueda validar 15 minutos.
2. **Evento Local o Universal:** confirmar que los seis integrantes estén registrados y confirmados en el **mismo** evento (requisito para ser elegibles a premios).
3. **Ruta de datos:** principal (NASA POWER + Earthdata/AppEEARS) y respaldo (archivos descargados a mano); Google Earth Engine solo si ya tienen cuenta activa.
4. **Stack de interfaz:** React + Leaflet si alguien domina React; si no, Streamlit (decisión final el jueves por la noche).
5. **Formato de demo:** 7 diapositivas en inglés o video de 30 s (confirmar en la guía 2026).
6. **Hardware:** go/no-go el viernes a las 21:00 (¿hay piezas y alguien que lo arme sin frenar al resto?).

### Tres reglas de oro

1. **De punta a punta antes que perfecto:** a las 17:30 del sábado debe existir un flujo completo, aunque feo.
2. **Todo número en pantalla lleva fuente, resolución y límite.**
3. **Nada de promesas de rendimiento:** mostramos patrones, rangos y preguntas para el agrónomo.

**Mínimo funcional si el tiempo aprieta:** POWER + NDVI + SRTM + suelo + base de cultivos. Si IMERG o SMAP no llegan, se usan `GWETROOT` y `PRECTOTCORR` de POWER y se declara la limitación (checkpoint del sábado 17:30).

---

## 2. El problema y el análisis del reto

### 2.1 El problema que resuelve

Los agricultores deben decidir **qué sembrar después de cada cultivo** (la rotación). Hoy es más difícil por varias razones:

- **Clima menos predecible:** lluvias irregulares, sequías, heladas y calor extremo.
- **Suelo que se desgasta:** un campo en barbecho queda desnudo; la lluvia se lleva la tierra (erosión), se pierde materia orgánica y el agua se evapora. Repetir el mismo cultivo acumula plagas y agota nutrientes.
- **Dilemas locales:** un cultivo que ayuda al suelo en un lugar puede gastar demasiada agua, no tener mercado o no encajar en la temporada de otro lugar.
- **Los datos NASA existen, pero nadie los traduce** a una decisión de rotación en lenguaje claro.

### 2.2 Qué pide el reto (y qué no)

Convertir los datos de largo plazo de la NASA (lluvia, humedad del suelo, temperatura, vegetación, relieve) en información útil: *¿cuándo suele estar desnudo mi campo?, ¿en qué estaciones es más vulnerable mi cultivo actual?, ¿qué otras rotaciones podría probar?* Hay que comparar al menos 3 estrategias (incluida la rotación actual) y explicar, para cada una, por qué se sugiere, qué evidencia la respalda, qué beneficios ofrece, qué información falta y qué riesgos debe revisar un experto local.

> **Lo que el reto NO pide:** predecir rendimientos exactos, reemplazar al agrónomo, cubrir todos los cultivos del mundo ni entregar "el" cultivo ideal. Pide una herramienta de apoyo, honesta sobre la incertidumbre.

### 2.3 Lectura fina del enunciado

| Lo que dice el enunciado | Cómo lo interpretamos | Qué implica para el diseño |
|---|---|---|
| "app, model, interactive map, or other decision-support tool" | Formato libre; importa que apoye una decisión | Web app + motor de modelo + mapa |
| "NASA Earth observations along with local soil information, crop characteristics, and farmer priorities" | Cuatro insumos obligatorios | Pipeline NASA + base de cultivos + formulario de prioridades |
| "compare at least three rotation strategies, including the existing rotation" | Requisito duro | Mostramos cuatro: E0 actual, E1 suelo, E2 agua, E3 ingreso/forraje |
| "explain why it was suggested, evidence, benefits, information gaps, risks" | Cinco bloques obligatorios por estrategia | "Tarjeta de estrategia" con los cinco bloques + confianza |
| "highlight times when fields are typically bare, seasons when current crops appear more vulnerable" | Fenología (NDVI) y ventanas de riesgo | Detección de suelo desnudo + exposición por etapa |
| "does not replace farmer judgment or local agricultural expertise" | La humildad es requisito de diseño | Mensajes claros, semáforo de confianza, "preguntas para tu agrónomo" |
| "SI techniques" | El texto dice "SI"; lo interpretamos como técnicas de inteligencia artificial y análisis estadístico avanzado | Declarar en el envío qué hace la IA y qué hacen las reglas y la estadística |
| "harmonizing datasets with different spatial and temporal resolutions" | Mostrar cómo se llevan los datos a una escala común | Módulo de armonización documentado (5.5) |
| "clarify how satellite observations differ from ground-based measurements" | Explicar y, idealmente, demostrar | Glosario en la interfaz + **caja de 3 zonas** (sección 9) |
| "interactive maps… timelines… scenario sliders" (opcional) | No obligatorios, pero elevan Creatividad e Impacto | Mapa con capas NASA y sliders de ΔT/ΔP si el MVP está sólido |

### 2.4 Matriz de cumplimiento

| ID | Requisito | Prioridad | Cómo lo cumplimos | Responsable |
|---|---|---|---|---|
| R1 | Herramienta de apoyo a la decisión | MUST | Web app + API + mapa | S3 · S4 |
| R2 | Datos NASA + suelo local + cultivos + prioridades | MUST | Pipeline NASA, SoilGrids/local, base de cultivos, sliders | S1 · I1 |
| R3 | ≥ 3 estrategias, incluida la actual | MUST | E0 + E1 + E2 + E3 | S3 · I1 |
| R4 | Por qué · evidencia · beneficios · vacíos · riesgos | MUST | Tarjeta de estrategia con evidencia trazable | S2 · S3 · I1 |
| R5 | Apoya, no reemplaza, el juicio local | MUST | Confianza, vacíos, preguntas para el agrónomo | I1 · S4 |
| R6 | Suelo desnudo y vulnerabilidad estacional | SHOULD | Fenología NDVI + exposición por etapa | S2 |
| R7 | Armonizar resoluciones distintas | SHOULD | Rejilla/paso común, etiquetas de escala | S1 |
| R8 | Explicar incertidumbre y validación local | SHOULD | Monte Carlo + niveles de confianza | S2 · S3 |
| R9 | Explicar términos y satélite vs terreno | SHOULD | Glosario/tooltips + caja de 3 zonas | I1 · E1 · S4 |
| R10 | Prioridades y restricciones del usuario | SHOULD | Sliders y restricciones | S4 · I1 |
| R11 | Mapas, líneas de tiempo, capas, sliders de escenario | COULD | Capas GIBS + sliders ΔT/ΔP | S4 · S3 |
| R12 | Vulnerabilidades a escala de campo | COULD | Zonas con NDVI 30 m + relieve (+ clustering) y pestaña "Datos de terreno" | S2 · S1 · E1 |
| R13 | Explicar fijadores de N, coberturas, raíces profundas, tolerancia a sequía | COULD | Fichas educativas breves | I1 |
| R14 | Requisitos de envío Space Apps | MUST | Checklist de la sección 14 | I1 · S1 |

### 2.5 Cómo nos van a evaluar

El jurado de Space Apps usa cinco criterios. Esto es lo que significan para nosotros:

| Criterio | Qué buscan | Qué mostraremos |
|---|---|---|
| **Impacto** | Potencial de beneficiar a muchas personas | Un caso piloto con cifras calculadas (días de suelo desnudo, déficit hídrico, riesgo de erosión) y un camino para escalar |
| **Creatividad** | Una idea nueva o forma ingeniosa de usar los datos | Ventanas de suelo desnudo + "qué pasaría si" climático + incertidumbre explícita + **caja de campo que muestra la variabilidad que el satélite promedia** |
| **Validez** | Rigor científico y técnico | Métodos citados, pruebas, límites declarados, sin sobrepromesas |
| **Relevancia** | Que responda al reto | Matriz de cumplimiento: ≥ 3 estrategias, cinco bloques, apoyo (no reemplazo) |
| **Presentación** | Comunicar con claridad | Demo de 7 diapositivas o 30 s, historia del agricultor, README limpio, app pública |

### 2.6 Reglas del evento que condicionan el plan

Fuente: página oficial de preguntas frecuentes 2026 y guía de envío 2025 (la de 2026 sale el 13-nov; verifiquen cada punto).

- **Fechas:** sábado 14 y domingo 15 de noviembre de 2026; Evento Local o Evento Universal virtual. Equipos de hasta 6 personas (ustedes están en el máximo).
- **Elegibilidad:** todos los integrantes deben estar confirmados en el **mismo** Evento Local para ser elegibles a premios (guía 2025).
- **Envío:** abre el sábado 9:00 y cierra el domingo 23:59 hora del evento local (patrón 2025). Todo el contenido en **inglés**. Enlaces públicos y accesibles sin iniciar sesión.
- **Campos del formulario:** nombre, resumen, enlace a la demo, enlace al proyecto final, descripción detallada, datos NASA usados, otros datos y recursos, uso de IA.
- **Demo:** video de máximo 30 segundos **o** presentación de máximo 7 diapositivas; excederlos puede dejar al proyecto fuera del juzgamiento global.
- **Trabajo original:** se declara que el trabajo es del equipo y que todo recurso externo (código, texto, imágenes, aunque sea abierto) está listado en las referencias.
- **Uso de IA:** divulgar qué herramientas y para qué; identificar el contenido generado con IA; no usar la marca NASA en contenido generado con IA.
- **Datos NASA:** para premios globales el proyecto debe usar datos o recursos de la NASA.
- **Calendario posterior:** nominados y finalistas globales en diciembre de 2026; ganadores en enero de 2027.

> **Sobre el trabajo previo al evento.** No encontré una regla explícita sobre código escrito antes del sábado, pero sí la declaración de "trabajo original". Por prudencia, jueves y viernes se usan para **preparar el terreno, no el producto**: cuentas, entorno, lectura, decisiones, contactos, compra de piezas y pruebas de lectura de sensores. El código del producto, los modelos, la interfaz y los datos procesados se producen durante el evento. **Armar la caja puede caer en la categoría "producto"; si quieren adelantarla, confírmenlo con el Local Lead.**

### 2.7 Errores típicos que vamos a evitar

- Un tablero bonito de datos NASA **sin recomendación de rotación**: no responde la pregunta del reto.
- Entregar un único "mejor cultivo" sin comparar al menos tres estrategias ni explicar el porqué.
- Sobrepromesas ("sube el rendimiento 30 %") sin validación; ocultar incertidumbre o vacíos.
- Depender de APIs lentas en vivo durante la demo: se precalcula y se guarda en caché el sitio piloto.
- Hardware que no se integra con el software, o que consume al equipo la última noche.
- Comparar cuantitativamente una caja regada a mano contra un píxel de SMAP de 9 km: el resultado no tendría sentido.
- Alcance global ("todos los cultivos del mundo") en vez de un piloto profundo con arquitectura extensible.
- Dejar el formulario, el video y las referencias para los últimos 30 minutos.

---

## 3. Conceptos científicos en lenguaje simple

Guion para el glosario de la interfaz (tooltips de una línea) y para la presentación.

| Concepto | Qué es (versión corta) | Por qué importa en una rotación | Fuente de datos |
|---|---|---|---|
| **Evapotranspiración (ET)** | Agua que pasa del suelo y las plantas a la atmósfera. ET0 = demanda de referencia; ETc = Kc × ET0 es la de un cultivo | Define cuánta agua necesita cada cultivo y cuándo habrá déficit | POWER (ET0 calculable), MODIS MOD16, ECOSTRESS |
| **Humedad del suelo** | Agua disponible: superficial (0–5 cm) y de la zona radicular (hasta ~1 m) | Anticipa estrés hídrico antes de que se vea en la planta | SMAP L4, POWER (GWETROOT) |
| **Anomalía de humedad** | Cuánto se aleja el valor de hoy de lo normal: z = (valor − media)/desviación | Distingue "está seco" de "más seco de lo habitual" | Cálculo propio sobre SMAP/POWER |
| **NDVI / EVI** | Índices de "verdor" a partir de la luz reflejada | Revela cuándo el campo está desnudo, reverdece o sufre estrés | MODIS, VIIRS, HLS, Landsat |
| **Fenología y suelo desnudo** | Calendario de crecimiento; suelo desnudo = periodo con muy poca cobertura | El suelo desnudo se erosiona y pierde agua y materia orgánica; es donde caben coberturas | Series de NDVI |
| **Temperatura superficial (LST) y calor** | Temperatura de la superficie vista desde el satélite | Detecta estrés térmico y diferencias dentro del campo | ECOSTRESS, MODIS, POWER |
| **Riesgo de helada** | Probabilidad de ≤ 0 °C en etapas sensibles; período libre de heladas | Determina qué cultivos y ciclos caben | POWER (T mín), SRTM |
| **Precipitación extrema y días secos** | Percentil 95, racha máxima sin lluvia (CDD), índice SPI | Lluvias fuertes sobre suelo desnudo = erosión; rachas secas = pérdidas | GPM IMERG, POWER |
| **Balance hídrico** | Lluvia efectiva menos demanda del cultivo (P − ETc) | Compara estrategias por consumo de agua frente a oferta | Cálculo propio |
| **Almacenamiento de agua (GRACE-FO)** | Cambios de masa de agua a escala de cientos de km | Contexto de tendencias regionales; **no** sirve para un campo | GRACE / GRACE-FO |
| **Erosión (RUSLE)** | A = R × K × LS × C × P (lluvia, suelo, pendiente, cobertura, prácticas) | Compara estrategias por riesgo relativo de pérdida de suelo | IMERG/POWER, SoilGrids, SRTM, NDVI |

### 3.1 Cómo aportan al suelo las prácticas de rotación

| Práctica | Cómo ayuda a largo plazo | Matiz honesto |
|---|---|---|
| **Leguminosas fijadoras de N** (soya, haba, arveja, alfalfa, vicia, tréboles) | Bacterias en las raíces fijan nitrógeno del aire; reducen la fertilización nitrogenada siguiente | El aporte varía mucho; hay que validarlo localmente |
| **Cultivos de cobertura** (avena, centeno, vicia, rábano forrajero) | Protegen del impacto de la lluvia y el viento, aportan materia orgánica, reducen malezas y evaporación | Consumen agua: decidir fecha de siembra y de terminación |
| **Raíz profunda** (alfalfa, girasol, sorgo) | Acceden a agua más profunda y abren bioporos que mejoran la infiltración | Depende de la profundidad del suelo y del agua subterránea |
| **Tolerantes a sequía** (sorgo, mijo, garbanzo, lenteja, quinua según variedad) | Reducen el riesgo de pérdida y la demanda de riego | Pueden dar menor ingreso o tener mercado limitado |
| **Rotar familias botánicas** | Interrumpe ciclos de plagas y enfermedades específicas | La presión real es local: debe validarlo un agrónomo |

### 3.2 Satélite vs mediciones en tierra

| Aspecto | Satélite | Medición en tierra | Qué ganamos combinándolos |
|---|---|---|---|
| Cobertura | Global y continua, décadas de historia | Un punto; requiere instalación y solo tiene datos desde que se instala | El satélite da el contexto; el sensor da la verdad local |
| Escala | Píxeles de 30 m a decenas de km (SMAP 9 km; POWER ~50 km) | Centímetros alrededor del sensor | Si coinciden ganan confianza; si difieren, es información (sesgo, profundidad, suelo) |
| Profundidad | SMAP ve ~5 cm; la zona radicular proviene de un modelo asimilado | Se instala a la profundidad deseada | Posible calibración local de la recomendación |
| Limitaciones | Nubes, vegetación densa, pasos del satélite, versiones | Calibración, deriva, fallas, un solo punto | Dos fuentes con errores distintos |

---

## 4. Visión de la solución: la app

### 4.1 Propuesta de valor y usuarios

**RotaSat** (nombre de trabajo): *"En cinco pasos, el agricultor ve cómo las condiciones cambiantes afectan su rotación actual y compara alternativas que protegen el suelo, ahorran agua y encajan con sus prioridades, con evidencia NASA, incertidumbre explícita y preguntas para su agrónomo."*

> **La idea en una frase:** RotaSat ayuda a un agricultor a decidir qué sembrar después de cada cultivo, comparando al menos tres estrategias con datos NASA, y le dice con honestidad cuánto confiar en cada una y qué debe consultar con su agrónomo.

| Usuario | Necesidad | Qué le entrega RotaSat |
|---|---|---|
| Agricultor/a familiar o mediano/a | Decidir con clima incierto, poco tiempo y poco ancho de banda | Cuatro estrategias comparables, lenguaje claro, semáforo de confianza, hoja imprimible |
| Agrónomo/a o extensionista | Revisar rápido el contexto de varios predios | Evidencia trazable, vacíos explícitos y preguntas para la visita |
| Cooperativa / ONG / municipio | Priorizar zonas vulnerables | Mapa de vulnerabilidades y comparación entre sitios (extensión futura) |

### 4.2 Los cinco pasos del usuario (máximo 2–3 minutos, usable con un pulgar)

| Paso | Qué hace el usuario | Qué recibe |
|---|---|---|
| **1 · Ubica tu campo** | Marca un punto o dibuja el polígono en el mapa | Datos NASA del sitio |
| **2 · Tu rotación actual** | Elige sus cultivos y el orden (ej. maíz, luego barbecho) | La línea base E0 |
| **3 · ¿Qué te importa?** | Mueve sliders: agua, suelo, ingreso, forraje, bajo riesgo; indica restricciones (riego, maquinaria) | El ranking se recalcula |
| **4 · Compara estrategias** | Ve cuatro tarjetas con semáforo de confianza | E0 actual, E1 suelo, E2 agua, E3 ingreso y forraje |
| **5 · Entiende y decide** | Abre evidencia, vacíos y riesgos | Preguntas para su agrónomo y hoja imprimible |

### 4.3 Funciones clave

- **Detección de suelo desnudo:** cuántos días al año y en qué época el campo queda sin cobertura.
- **Cuatro estrategias comparadas**, cada una con una tarjeta de cinco bloques y nivel de confianza.
- **Semáforo de confianza** con rangos de incertidumbre (simulación Monte Carlo, ~500 corridas).
- **Preguntas para tu agrónomo** al final de cada tarjeta.
- **Opcionales:** sliders de escenario climático (cambio de temperatura y de lluvia) y la **pestaña "Datos de terreno"** (alimentada por la caja de sensores, sección 9).
- **Mensaje permanente:** *"Esta herramienta apoya tu decisión; no reemplaza a tu experiencia ni a tu asesor técnico."*

### 4.4 Alcance: MVP, extensiones y fuera de alcance

| MVP (debe existir el domingo) | Extensiones (si el MVP está sólido) | Fuera de alcance (lo decimos explícitamente) |
|---|---|---|
| Un sitio piloto completo (y uno de respaldo) | Sliders de escenario ΔT/ΔP | Predicción de rendimientos |
| Flujo de 5 pasos con prioridades y restricciones | Capas NASA comparativas en el mapa (GIBS) | Calendarios exactos de riego o fertilización |
| Indicadores climáticos, hídricos y de vegetación con datos NASA | Zonas de manejo dentro del campo (30 m) | Pronóstico de plagas |
| Detección de ventanas de suelo desnudo | **Caja de 3 zonas + pestaña "Datos de terreno"** | Cobertura global de todos los cultivos |
| Cuatro estrategias (E0–E3) con tarjeta de cinco bloques | Segundo sitio de clima contrastante | Optimización económica con precios reales |
| Confianza, vacíos y preguntas para el agrónomo | Informe imprimible o bot de mensajería | Reemplazar visitas técnicas o análisis de laboratorio |
| Glosario breve y avisos de apoyo, no reemplazo | Capa de explicación con LLM con límites estrictos | |
| App pública + repositorio + demo | | |

### 4.5 Arquitectura por capas

```
 1 · FUENTES              2 · DATOS E INDICADORES        3 · MOTOR DE ROTACIONES       4 · INTERFAZ
 ─────────────            ───────────────────────        ───────────────────────       ───────────────
 NASA POWER · IMERG   ┐   Clientes de datos + caché  S1  Base de cultivos + reglas I1  API REST (FastAPI) S3
 SMAP · GRACE-FO      │   Armonización (rejilla,     S1  Generador de candidatos   S3     │
 MODIS/VIIRS · HLS ·  ├──▶ pasos de 10 días, QA)         E0 actual + E1, E2, E3        ▼
 ECOSTRESS            │   Indicadores: calor,        S2  Puntaje multicriterio   S3/I1  Web app: mapa, tarjetas,
 SRTM                 │   helada, SPI, anomalía,         (MCDA, pesos del agricultor)    sliders, "Datos de terreno" S4
 Suelo (SoilGrids/    │   días secos                 S2  Monte Carlo → confianza S2/S3  Informe imprimible / bot S4
   análisis local)    │   Fenología NDVI → suelo     S2  Tarjeta de estrategia    S3    Demo, textos EN, envío I1
 Cultivos (FAO)       │   desnudo
 Agricultor (inputs)  │   Modelos físicos: ET0, ETc, E1
 Caja de campo        ┘   balance hídrico
 (opcional)  E1           Zonas de manejo (opcional)   S2
```
*(S1–S4, I1, E1 = responsable de cada bloque.)*

**Principio de diseño:** el motor es un **sistema explicable** (reglas agronómicas + puntaje multicriterio + estadística). La IA se usa donde aporta de verdad: fenología y detección de anomalías, zonificación y, de forma opcional y acotada, redacción en lenguaje claro de la evidencia ya calculada. Así cada recomendación se puede auditar.

> **Diseñen desde la demo, hacia atrás.** Escriban la historia de 3 minutos: *"Un agricultor con maíz y barbecho largo quiere ahorrar agua. RotaSat muestra X días de suelo desnudo en la época de lluvias intensas, compara cuatro estrategias, advierte que la confianza es media por falta de análisis de suelo y deja tres preguntas para su agrónomo. En la caja, regamos una zona y vemos que solo esa curva sube."* Todo lo que no aparezca en esa historia es candidato a recorte.

### 4.6 Cómo elegir la región piloto (decisión del jueves)

Puntúen de 1 a 5 cada criterio para 2 o 3 candidatos y elijan el de mayor puntaje (desempate: acceso a una persona local).

| Criterio | Por qué importa | Cómo evaluarlo |
|---|---|---|
| Acceso a validación local | Valida cultivos, reglas y riesgos (Validez) | ¿Hay un agrónomo, agricultor o docente que responda en 24 h? |
| Problema climático evidente | Hace visible el valor | Sequía, heladas, lluvias intensas, agotamiento de agua |
| Calidad de datos | Menos nubosidad persistente y campos más grandes que el píxel de 250–500 m | Ver series de NDVI en Worldview; latitud dentro de la cobertura de ECOSTRESS (~±52°) |
| Información de cultivos | Permite base de cultivos con fuentes | Rotaciones comunes documentadas por extensión o universidades |
| Familiaridad del equipo | Reduce errores de interpretación | ¿Conocen la región, los cultivos o el idioma de las fuentes? |

**Arquetipos útiles para pensar** (no son una recomendación de lugar): (A) secano de altura con riesgo de helada y sequía; (B) cinturón de granos con barbecho largo y lluvias intensas (erosión); (C) agricultura semiárida de pequeña escala con sequías recurrentes; (D) sistemas de riego con descenso de agua subterránea, donde GRACE-FO aporta contexto regional.

---

## 5. Datos NASA y complementarios

Resoluciones y estado verificados el 12-nov-2026; confirmen nombres exactos de colecciones y versiones en Earthdata Search antes de usarlos.

### 5.1 Catálogo

| Producto | Variable y uso en RotaSat | Resolución (espacio · tiempo) | Cobertura | Acceso | Prio. |
|---|---|---|---|---|---|
| **NASA POWER** (reanálisis + CERES) | T2M, T2M_MAX/MIN, PRECTOTCORR, RH2M, WS2M, radiación solar, humedad de suelo modelada (GWETROOT). Columna vertebral del clima y de ET0 | ≈0.5°×0.625° (50–60 km) · diario | 1981–hoy | API REST sin clave (power.larc.nasa.gov), comunidad AG | MUST |
| **GPM IMERG V07** | Precipitación: extremos (P95), días secos, SPI | 0.1° (~10 km) · 30 min/diario | 2000–hoy; Final con ~3.5 meses de latencia | GES DISC/Earthdata; GEE (`NASA/GPM_L3/IMERG_V07`) | MUST |
| **SMAP L4 (SPL4SMGP v8)** | Humedad superficial (0–5 cm) y zona radicular (0–100 cm); anomalías | 9 km · cada 3 h | 2015–hoy | NSIDC/Earthdata; GEE (`NASA/SMAP/SPL4SMGP/008`) | MUST |
| **MODIS NDVI/EVI (MOD13Q1) y VIIRS (familias VNP13/VJ113)** | Fenología, ventanas de suelo desnudo, estrés | 250–500 m · compuestos de 16 d | MODIS 2000–2026; VIIRS 2012–hoy | LP DAAC, AppEEARS; GEE | MUST |
| **HLS (HLSL30/HLSS30)** | NDVI a escala de parcela | 30 m · 2–3 días | 2013–hoy | LP DAAC, earthaccess, AppEEARS | SHOULD |
| **SRTM (30 m)** | Elevación, pendiente, aspecto: erosión y hondonadas frías | 30 m · estático | 2000 | GEE (`USGS/SRTMGL1_003`), LP DAAC, OpenTopography | MUST |
| **GRACE / GRACE-FO** | Anomalía de almacenamiento total de agua: tendencia regional | Cientos de km · mensual | 2002–hoy (hueco 2017–18) | PO.DAAC | SHOULD |
| **ECOSTRESS** (LSTE, ET, ESI) | Temperatura superficial, ET e índice de estrés a 70 m | 70 m · irregular (órbita ISS) | 2018–hoy (ET v3 desde oct-2025) | LP DAAC, Earthdata Search, AppEEARS | COULD |
| **MOD16A2GF / Landsat 8-9** | ET real de MODIS (complemento); Landsat como alternativa a HLS | 500 m · 8 d / 30 m · 16 d | 2000–hoy / 2013–hoy | LP DAAC; GEE | COULD |
| **SoilGrids 250 m** (ISRIC, *no NASA*) | Textura, carbono orgánico, N, pH: factor K y contexto de suelo | 250 m · estático | — | Rásters (files.isric.org) o GEE comunitario; API REST con límites | MUST |
| **FAO EcoCrop y FAO-56** (*no NASA*) | Rangos agroclimáticos y coeficientes Kc | Tablas | — | ecocrop.fao.org; boletín FAO-56 | MUST |
| **NASA GIBS / Worldview** | Capas de mapa listas (teselas WMTS) | Varía | Varía | gibs.earthdata.nasa.gov | SHOULD |
| **Datos locales** | Análisis de suelo del agricultor, estación meteorológica nacional, precios locales | — | — | Entrevista / archivos aportados | SHOULD |
| **Caja de campo** (propia) | Humedad y temperatura de suelo por zona; aire (BME280) | Puntual · cada 10–15 min | Desde su instalación | Endpoint `POST /field-node/readings` | COULD |
| **GEDI** (opcional) | Biomasa y estructura de la vegetación | 25 m–1 km · irregular | 2019–hoy | LP DAAC / ORNL DAAC | COULD |

### 5.2 Niveles de datos: qué debe estar listo y cuándo

| Nivel | Contenido | Meta | Si falla |
|---|---|---|---|
| **1 · Columna vertebral** | POWER diario (1981–2025) del sitio piloto: temperatura, lluvia, humedad, viento, radiación, GWETROOT. Salen heladas, calor, días secos, SPI, ET0 y balance hídrico | Sáb 12:30 | Sin respaldo: es una sola llamada; probarla el viernes |
| **2 · El "wow"** | NDVI (VIIRS/MODIS/HLS) para suelo desnudo + SRTM + suelo | Sáb 17:30 | NDVI de baja resolución o valores de suelo por defecto con aviso de incertidumbre alta |
| **3 · Refuerzo** | IMERG, SMAP L4, GRACE-FO y comparación con POWER | Sáb 21:00 | Mantener GWETROOT y PRECTOTCORR de POWER y declarar la limitación |
| **4 · Extensión** | HLS 30 m, ECOSTRESS, zonas de manejo, segundo sitio, caja de campo | Dom 12:00 | Se muestra como hoja de ruta |

### 5.3 Rutas de acceso (de menor a mayor fricción)

- **Ruta A, sin fricción:** NASA POWER por API REST (sin clave), teselas GIBS para el mapa y archivos descargados a mano desde Earthdata Search.
- **Ruta B, cuenta Earthdata gratuita:** biblioteca **earthaccess** (Python) para IMERG, SMAP, HLS y ECOSTRESS, y **AppEEARS** para extraer series de un punto o polígono de MODIS/VIIRS/SMAP/SRTM/ECOSTRESS. AppEEARS trabaja con tareas asíncronas (minutos u horas): se lanzan el **sábado a las 9:30**, no a las 17:00.
- **Ruta C, Google Earth Engine:** la más rápida para series de tiempo si ya hay cuenta y proyecto activos; si no, no la usen como dependencia crítica.

### 5.4 Cuidados y estado actual de los datos (verificado el 12-nov-2026)

| Tema | Qué encontramos | Qué hacemos |
|---|---|---|
| **MODIS (Terra y Aqua)** | NASA prepara la transición a VIIRS: la recolección de Terra terminaría hacia ene–feb de 2027 y la de Aqua hacia sep de 2027 | MODIS para el histórico 2000–2025; VIIRS/HLS para continuidad; documentar versión y fecha |
| **Suomi NPP (VIIRS)** | Dejó de entregar nuevos datos científicos el 2-nov-2026 | Para VIIRS reciente usar NOAA-20 o NOAA-21; no mezclar sin documentarlo |
| **IMERG Final** | Latencia de unos 3.5 meses (hasta 4) | Para meses recientes, Late/Early (menos exactos) o POWER, y marcarlo |
| **SMAP L4** | v8 disponible; la zona radicular es producto de modelo con asimilación, no medición directa | Presentarlo como "estimación" |
| **ECOSTRESS** | Cobertura irregular; ET v3 con datos desde 1-oct-2025 en su primera liberación | Demostración de estrés si hay escenas; no columna vertebral |
| **GRACE-FO** | Resolución efectiva de cientos de km | Solo tendencia regional; nunca "el agua subterránea de su finca" |
| **SoilGrids (API REST)** | Límites de uso y aviso de pausa temporal de ISRIC | No depender de la API en vivo: recortar el ráster, usar GEE o valores del agricultor; guardar en caché |

### 5.5 Armonización de resoluciones

Los conjuntos de datos van de 30 m a cientos de km y de 30 minutos a un mes. Hay que unificarlos para compararlos con honestidad:

| Dato | Escala | Tiempo entre datos |
|---|---|---|
| SRTM, SoilGrids | 30 m / 250 m | estático |
| HLS / Landsat 8-9 | 30 m | 2–3 días / 8 días |
| ECOSTRESS | 70 m | irregular |
| MODIS NDVI / VIIRS | 250 m / 375–500 m | compuesto de 16 d / diario |
| SMAP L4 | 9 km | 3 h |
| GPM IMERG | ~10 km | 30 min |
| NASA POWER | 50–60 km | diario |
| GRACE-FO | cientos de km | mensual |

- **Espacio.** Reproyectar todo a EPSG:4326. Mapa regional: cuadrícula de 0.01° (~1 km). Campo: cuadrícula de 30 m (HLS) dentro del polígono. Continuas: interpolación bilineal o promedio ponderado por área; categóricas: vecino más cercano. Los datos gruesos (POWER, SMAP, GRACE) se **muestrean** y se etiquetan "contexto regional", nunca "dato de su campo".
- **Tiempo.** Paso común de 10 días ("dekad") para agronomía: lluvia acumulada, temperatura media y extremos, humedad media, NDVI de valor máximo del periodo. Se conserva el diario para extremos.
- **Línea base climática.** POWER desde 1981 para climatología larga; SMAP (desde 2015) e IMERG (desde 2000) para periodos recientes y verificación. Se alinean por mapeo de cuantiles cuando se comparan.
- **Calidad.** Máscara de nubes y de calidad (QA de MODIS/VIIRS, Fmask de HLS), relleno de huecos por interpolación o suavizado de Whittaker, y una etiqueta por dato: escala (parcela/local/regional), cobertura (% válido) y versión.
- **Trazabilidad.** `data/catalog.yaml` con fuente, producto, versión, DOI, licencia, fecha de descarga y transformación aplicada.

---

## 6. Modelo analítico paso a paso

### 6.1 Biblioteca de indicadores

| Indicador | Definición y fórmula | Datos | Responde a |
|---|---|---|---|
| **Grados-día (GDD)** | GDD = Σ máx(0, (Tmáx + Tmín)/2 − Tbase) por etapa | POWER | ¿Cabe el ciclo del cultivo en la temporada? |
| **Heladas** | Fecha de última y primera helada; días con Tmín ≤ 0 °C en etapas sensibles; período libre de heladas | POWER, SRTM | Vulnerabilidad al frío |
| **Días de calor** | Días con Tmáx ≥ umbral del cultivo en floración y llenado | POWER | Vulnerabilidad al calor |
| **SPI y días secos** | SPI de 1, 3 y 6 meses (gamma → normal); CDD = racha máxima sin lluvia | IMERG, POWER | Riesgo de sequía |
| **Anomalía de humedad** | z = (SM − μ_mes) / σ_mes en zona radicular | SMAP L4, POWER | Cuánto más seco o húmedo de lo normal |
| **Aridez y balance hídrico** | AI = P / ET0; déficit = Σ máx(0, ETc − P_ef − ΔAW) | POWER, Kc FAO | Demanda frente a oferta de agua |
| **Lluvia extrema** | P95 diario, Rx5day | IMERG, POWER | Riesgo de erosión |
| **Días de suelo desnudo** | Días con NDVI < umbral (0.20–0.25, calibrar) por año; ventana continua ≥ 30 d | VIIRS/MODIS/HLS | Cuándo caben coberturas |
| **Erosión relativa (RUSLE)** | A = R × K × LS × C × P como índice relativo entre estrategias | IMERG/POWER, SoilGrids, SRTM, NDVI | Pérdida de suelo comparada |
| **Tendencia de agua regional** | Theil–Sen y Mann–Kendall sobre la anomalía de GRACE-FO | GRACE-FO | Contexto de largo plazo |

### 6.2 Detección de ventanas de suelo desnudo (algoritmo)

1. Obtener la serie de NDVI 2015–2025 del polígono (VIIRS/MODIS; HLS si hay tiempo) y aplicar la máscara de calidad.
2. Suavizar con Savitzky–Golay o Whittaker y rellenar huecos cortos.
3. Calcular la climatología por dekad (mediana) y la banda P10–P90 entre años.
4. Marcar "baja cobertura" cuando NDVI < umbral (empezar en 0.20–0.25; calibrar con imágenes de Worldview y con el agricultor).
5. Agrupar dekads consecutivos: ventanas de ≥ 30 días = **ventana de barbecho**. Estimar su recurrencia ("8 de 10 años").
6. Cruzar cada ventana con la probabilidad de lluvia intensa (P95) y la pendiente (SRTM): **riesgo de erosión en barbecho**.
7. Sugerir coberturas o intercultivos que quepan en la ventana según duración, temperatura y agua disponible.

> **Cuidado con los píxeles mezclados.** En MODIS/VIIRS un píxel de 250–500 m puede mezclar el campo con caminos, árboles o vecinos. Usen HLS (30 m) dentro del polígono cuando haya tiempo y declaren la escala en la interfaz.

### 6.3 Base de conocimiento de cultivos y reglas de rotación

El conocimiento agronómico se guarda en `crops.csv` con **una fuente por fila** (plantilla en el Anexo C). La tabla cualitativa muestra la forma de las fichas; los valores cuantitativos (Kc, Tbase, profundidad de raíz, rangos de N) deben tomarse de FAO-56, FAO EcoCrop y extensión local, y validarse con un experto.

| Cultivo | Familia | Rol típico | Demanda de agua | Tolerancia | Fija N | Nota de riesgo |
|---|---|---|---|---|---|---|
| Maíz | Poaceae | Cereal comercial; mucho residuo | Alta | Sequía: baja–media | No | Exigente en N y agua en floración |
| Trigo / cebada | Poaceae | Cereal de estación fría; cebada de ciclo corto | Media / media-baja | Helada: media | No | Enfermedades foliares si se repite |
| Soya | Fabaceae | Leguminosa comercial | Media-alta en floración | Calor: media | Sí | Poco residuo; cuidar cobertura |
| Haba / arveja / garbanzo / lenteja | Fabaceae | Leguminosas de grano de estación fría | Media-baja | Garbanzo y lenteja: sequía alta | Sí | Mercado y plagas locales |
| Alfalfa | Fabaceae | Forraje perenne de raíz profunda | Alta, pero explora capas profundas | Sequía: media-alta | Sí | Requiere agua al establecerse |
| Sorgo | Poaceae | Cereal/forraje tolerante a sequía | Media-baja | Sequía: alta | No | Mercado local variable |
| Girasol | Asteraceae | Oleaginosa de raíz profunda; rompe ciclos | Media | Sequía: media-alta | No | Enfermedades si se repite pronto |
| Papa | Solanaceae | Cultivo comercial de alto valor | Alta y sensible | Helada: baja | No | Alta presión sanitaria; suelo removido |
| Quinua | Amaranthaceae | Grano de valor; tolerante a sequía y frío según variedad | Baja–media | Sequía/helada: alta (variedad) | No | Fechas de siembra críticas |
| Canola / colza | Brassicaceae | Cultivo de "quiebre" frente a cereales | Media | Helada: media | No | Mercado y plagas |
| Avena + vicia | Poaceae + Fabaceae | Cobertura y forraje de invierno | Media-baja | Helada: media-alta | Sí | Fecha de terminación para no competir |
| Centeno / rábano forrajero | Poaceae / Brassicaceae | Coberturas: control de erosión y bioporos | Baja–media | Helada: alta | No | Rebrote o manejo del residuo |

| Regla de rotación | Fundamento | Cómo entra al puntaje | Límite |
|---|---|---|---|
| No repetir familia en estaciones consecutivas | Reduce acumulación de plagas y enfermedades | Penalización por retorno corto | La presión real es local |
| Leguminosa antes de un cultivo exigente en N | El N fijado reduce fertilización posterior | Crédito de N (rango) en suelo | El aporte varía mucho |
| Cobertura en ventanas de suelo desnudo | Menos erosión, más materia orgánica, menos evaporación directa | Reduce días desnudos y factor C | Consume agua |
| Raíz profunda después de raíz superficial | Explora otras capas y mejora infiltración | Bono de diversidad de raíces | Depende de la profundidad del suelo |
| Evitar alta demanda en la época de menor agua | Evita déficit y costo de riego | Déficit hídrico estacional (mm) | Requiere datos de riego reales |
| El cultivo debe caber en el período libre de heladas | Evita pérdidas por frío | Filtro de factibilidad (GDD y fecha de helada) | Microclimas del campo |

### 6.4 Generación de estrategias

1. **Factibilidad.** Filtrar cultivos cuyo ciclo (GDD a madurez) quepa en la ventana libre de heladas en ≥ 70 % de los años y que respeten las restricciones del agricultor.
2. **Candidatos.** Armar secuencias de 3–4 estaciones con reglas duras (sin repetir familia consecutiva salvo excepción).
3. **Puntaje.** Calcular los seis criterios y el puntaje ponderado (6.5).
4. **Selección diversa.** Quedarse con la línea base E0 y la mejor secuencia por perfil: **E1 Suelo primero**, **E2 Agua primero**, **E3 Ingreso y forraje**; opcional **E4 Equilibrada**. Exigir diferencias reales entre estrategias.

### 6.5 Puntaje multicriterio (MCDA)

> **Fórmula base:** `Puntaje(R) = Σ_k w_k · S_k(R)`, con `Σ w_k = 1` y cada `S_k` normalizado entre 0 y 100. Los `w_k` salen de los sliders del agricultor; los pesos por defecto se derivan con AHP simplificado (I1).

| Criterio | Qué mide y con qué | Peso inicial |
|---|---|---|
| **Suelo** | Cobertura continua (1 − días desnudos/365), crédito de N, aporte de residuo, diversidad de raíces, erosión relativa (RUSLE) | 25 % |
| **Agua** | Déficit hídrico estacional (ETc − P_ef − ΔAW), eficiencia y estrés de humedad de suelo | 25 % |
| **Riesgo climático** | Probabilidad histórica de calor, helada, sequía o lluvia extrema en etapas críticas; ajustada por escenarios | 20 % |
| **Plagas** | Diversidad de familias, intervalo mínimo de retorno, cultivos de quiebre | 10 % |
| **Economía y forraje** | Margen bruto aproximado o rango; valor forrajero; riesgo de mercado. Sin precios del agricultor se marca como vacío | 15 % |
| **Factibilidad** | Maquinaria, mano de obra y picos de trabajo (aporte de I1) | 5 % |

### 6.6 Incertidumbre y nivel de confianza

- **Monte Carlo (≈ 500 corridas):** remuestrear años climáticos (bootstrap por bloques), perturbar los pesos (Dirichlet alrededor de los del usuario) y muestrear los rangos de parámetros de cada cultivo.
- **Salidas:** mediana del puntaje, intervalo P10–P90 y probabilidad de que cada estrategia mantenga su posición.
- **Confianza (regla heurística, se declara como tal):** **Alta** si cobertura de datos ≥ 0.8, estabilidad del ranking ≥ 0.7 y P90 − P10 ≤ 15 puntos; **Baja** si cobertura < 0.5 o estabilidad < 0.4; **Media** en otro caso.
- **Vacíos automáticos:** cada insumo ausente genera una línea ("sin análisis de suelo local: se usó SoilGrids 250 m") y una pregunta para el experto.

### 6.7 Escenarios "qué pasaría si"

Método de cambio por delta: `T' = T + ΔT` (0 a +3 °C) y `P' = P × (1 + ΔP)` (−30 % a +20 %), aplicado a la serie histórica; se recalculan ET0, indicadores y puntajes. Es exploración, **no un pronóstico**: no cambia la distribución de intensidades de lluvia salvo que se escale aparte el extremo.

### 6.8 Qué hacen con los datos (cálculos propios, no algoritmos de la NASA)

- **Indicadores:** grados-día, heladas, días de calor, SPI y días secos, aridez, ET0/ETc y balance hídrico.
- **Suelo desnudo:** suavizar NDVI, marcar períodos bajo un umbral (0.20–0.25, a calibrar) y agrupar los de 30 días o más.
- **Erosión relativa (RUSLE)** para comparar estrategias.
- **MCDA** (suma ponderada de suelo, agua, riesgo, plagas, economía y factibilidad) con los pesos que mueve el agricultor.
- **Monte Carlo** para rangos y nivel de confianza.

### 6.9 Comparación de enfoques de IA y estadística

| Enfoque | Qué aporta | Costo y riesgo | Decisión |
|---|---|---|---|
| Reglas expertas + MCDA | Explicable, rápido, auditable | Subjetividad de pesos → sensibilidad y validación experta | **Núcleo del motor** |
| Estadística clásica (z, percentiles, SPI, Mann–Kendall) | Robusta e interpretable | Bajo | Base de indicadores |
| Series de tiempo y fenología (Savitzky–Golay, Whittaker) | Detecta ciclos, picos y ventanas desnudas | Sensible a nubes y píxeles mezclados | Se usa |
| ML no supervisado (k-means, GMM; Isolation Forest) | Zonas de manejo y anomalías | Interpretación y tiempo | Extensión |
| ML supervisado (bosques aleatorios, boosting) | Podría predecir NDVI pico o rendimiento | Exige datos de rendimiento que no tenemos; sobreajuste | Hoja de ruta |
| Modelos de cultivo (AquaCrop, DSSAT, APSIM) | Base física del crecimiento y agua | Calibración pesada para 48 h | Hoja de ruta |
| LLM como capa de explicación | Redacta en lenguaje claro la evidencia ya calculada | Alucinaciones → solo reescribe datos estructurados, muestra la evidencia original y se declara | Opcional |

---

## 7. Las estrategias de rotación y la tarjeta de estrategia

Cada estrategia se entrega como una **tarjeta** con los cinco bloques que exige el reto más un nivel de confianza. Las secuencias reales dependen del clima y de la base de cultivos de la región piloto; **todas las cifras de esta sección son ilustrativas** y en la app saldrán calculadas con los datos del sitio. En la demo no deben presentarse como resultado real hasta que se calculen.

### 7.1 Esquema visual (ilustrativo): ventanas de suelo desnudo

```
Año agrícola genérico (M1 = inicio de la siembra principal)   M1  M2  M3  M4  M5  M6  M7  M8  M9  M10 M11 M12
E0 · Línea base          ████ Cultivo de verano ███████████   ░░░░░ Suelo desnudo (barbecho) ░░░░░░░░░░░░░   ≈ 180 d desnudo
E1 · Suelo primero       ████ Cultivo de verano ███████████ ░ ▓▓▓ Cobertura (vicia + avena) ▓▓▓▓▓▓▓▓▓▓▓ ·    ≈ 10–20 d
E2 · Agua primero        ███ Sorgo / girasol ██████ ·rastrojo· ▓▓ Leguminosa de ciclo corto ▓▓▓▓▓▓▓▓ ·      ≈ 0–30 d
E3 · Ingreso y forraje   ███ Cultivo comercial ████ ░ ▓▓▓▓ Forraje (avena + vicia) ▓▓▓▓▓▓▓▓▓▓ · rastrojo   ≈ 30 d
```
*(░ = suelo desnudo; · = rastrojo/residuo. Esquema, no datos reales.)*

### 7.2 Molde de la tarjeta de estrategia

| Bloque | Qué debe responder | De dónde sale |
|---|---|---|
| **1 · Por qué se sugiere** | Señal concreta que motivó la estrategia ("[N] días de suelo desnudo en [época de lluvias intensas]") | Indicadores de 6.1 |
| **2 · Evidencia** | Indicadores y datasets con valor, escala y fecha; enlace a la serie | Catálogo de datos + gráficos |
| **3 · Beneficios posibles** | Efectos en suelo, agua, riesgo, plagas e ingreso, en rangos | Puntajes por criterio + Monte Carlo |
| **4 · Vacíos de información** | Lo que no sabemos o se asumió (suelo local, precios, riego, plagas) | Generado automáticamente |
| **5 · Riesgos para revisión local** | Lo que debe validar un agrónomo o el agricultor antes de decidir | Reglas + base de cultivos + preguntas sugeridas |
| **Confianza** | Alta / media / baja y por qué | Cobertura de datos + estabilidad del ranking |

### 7.3 Las cuatro estrategias (ejemplo ilustrativo)

| Estrategia | Secuencia de ejemplo | Días desnudo (ilustrativo) | Para quién |
|---|---|---|---|
| **E0 · Línea base** | Cultivo de verano, luego barbecho de invierno | ≈ 180 | Referencia: lo que hace hoy |
| **E1 · Suelo primero** | Cultivo de verano, luego cobertura (vicia + avena) | ≈ 10–20 | Quien prioriza el suelo y reducir fertilizante |
| **E2 · Agua primero** | Sorgo/girasol, luego leguminosa de ciclo corto | ≈ 0–30 | Secano con riesgo de sequía |
| **E3 · Ingreso y forraje** | Cultivo comercial, forraje de invierno | ≈ 30 | Quien tiene ganado o busca ingreso extra |

| Estrategia | Por qué y para quién | Beneficios posibles | Riesgos y vacíos a revisar |
|---|---|---|---|
| E0 | Referencia del agricultor; se evalúa su exposición: [N] días de suelo desnudo, [x] % de lluvia intensa en esa ventana | Bajo costo operativo conocido | Erosión y pérdida de materia orgánica en barbecho; sin información de plagas |
| E1 | Cubre la ventana de suelo desnudo y aporta N | Menos días desnudos, menos erosión relativa, crédito de N (rango) | Consumo de agua de la cobertura; fecha de terminación; costo de semilla |
| E2 | Reduce el déficit hídrico en la época de menor lluvia | Menor demanda de agua, raíces profundas, menor riesgo de calor | Mercado y precio; ajuste de fechas; menor ingreso potencial |
| E3 | Añade ingreso o alimento animal en la ventana libre | Ingreso adicional, cobertura parcial | Mercado y precios locales sin datos; carga de trabajo; confianza baja si faltan precios |

### 7.4 Ejemplo 1 · El mensaje para el agricultor

> "Tu campo está desnudo unos **180 días al año**, justo cuando caen las lluvias fuertes. Si siembras una cobertura de vicia y avena en ese periodo, bajarías esos días a unos **10–20**, protegerías el suelo y reducirías el fertilizante. **Confianza media**, porque no hay análisis de suelo de tu campo."

*(Cifras ilustrativas, tomadas del esquema de 7.1.)*

### 7.5 Ejemplo 2 · Una tarjeta completa (E1, redactada como ejemplo)

| Bloque | Contenido de ejemplo |
|---|---|
| 1 · Por qué se sugiere | [N] días de suelo desnudo en la época de lluvias intensas; una cobertura puede ocupar esa ventana |
| 2 · Evidencia | Días de suelo desnudo (NDVI VIIRS, 500 m), lluvia extrema P95 (POWER/IMERG), pendiente (SRTM 30 m). Cada valor con escala, fecha y versión |
| 3 · Beneficios posibles | Menos días desnudos, menor erosión relativa, crédito de nitrógeno (en rango) si incluye leguminosa |
| 4 · Vacíos de información | Sin análisis de suelo local: se usó SoilGrids 250 m. Sin precios locales |
| 5 · Riesgos para revisión local | La cobertura consume agua; fecha de terminación; costo de semilla; presión real de plagas |
| **Confianza** | **Media:** falta un análisis de suelo de su campo |

**Preguntas para tu agrónomo:** (1) ¿Cuándo conviene terminar la cobertura para que no compita con el cultivo siguiente? (2) ¿Hay plagas de la familia de este cultivo en mi zona que limiten repetirlo? (3) ¿Qué análisis de suelo me recomienda hacer?

---

## 8. Diseño centrado en el agricultor (UX)

```
┌────────────┐ ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌────────────┐
│1 Ubica tu  │ │2 Tu        │ │3 ¿Qué te   │ │4 Compara   │ │5 Entiende  │
│  campo     │▶│  rotación  │▶│  importa?  │▶│  estrategias│▶│  y decide  │
│ [  mapa  ] │ │ [Maíz][Soya]│ │ Agua  ▬▬●─ │ │ ● E0 Actual│ │[Por qué|Evi│
│    📍      │ │ [Trigo][Barb]│ │ Suelo ▬●── │ │ ● E1 Suelo │ │ dencia|...]│
│ Siguiente  │ │ Siguiente  │ │ Ver estrat.│ │ ● E2 Agua  │ │ Preguntas  │
└────────────┘ └────────────┘ └────────────┘ │ ● E3 Ingreso│ │ Imprimir   │
                                             └────────────┘ └────────────┘
```

### 8.1 Principios

- **Lenguaje llano:** nada de siglas sin explicación; tooltips de una línea con el glosario de la sección 3.
- **Semáforo de confianza** en cada estrategia, con el motivo en una frase.
- **Preguntas para tu agrónomo:** cada tarjeta termina con 2–3 preguntas concretas generadas desde vacíos y riesgos.
- **Prioridades del usuario al centro:** sliders (agua, suelo, ingreso, forraje, bajo riesgo) y restricciones (riego, maquinaria, mercado); el ranking se recalcula.
- **Siempre el mismo mensaje:** *"Esta herramienta apoya tu decisión; no reemplaza a tu experiencia ni a tu asesor técnico."*
- **Accesibilidad:** contraste alto, texto ≥ 16 px, iconos de cultivos, móvil y poca conexión (datos precalculados, imágenes ligeras), hoja imprimible.
- **Idiomas:** español e inglés desde el inicio (el envío es en inglés).

### 8.2 Microcopy de ejemplo

| Situación | Español | Inglés (para el jurado) |
|---|---|---|
| Aviso permanente | Esta es una guía para explorar opciones, no una receta. | This is a guide to explore options, not a prescription. |
| Confianza media | Confianza media: falta un análisis de suelo de su campo. | Medium confidence: no local soil analysis was provided. |
| Escala del dato | Dato regional (9 km): indica el contexto, no su campo. | Regional data (9 km): shows context, not your field. |
| Pregunta al experto | ¿Hay plagas de [familia] en su zona que limiten repetir este cultivo? | Are there [family] pests locally that limit repeating this crop? |
| Dato de aire regional | Dato regional (~50 km): contexto, no su campo. | Regional data (~50 km): context, not your field. |

### 8.3 Pestaña "Datos de terreno" (si la caja funciona)

- **Mapa de zonas:** rectángulo con 3 franjas (rojo = seca, amarillo = media, azul = húmeda) con la humedad y la temperatura de cada una.
- **Series de tiempo:** una línea de humedad por zona, con los riegos marcados; temperatura de suelo en un segundo gráfico.
- **Tarjeta de aire:** temperatura y humedad del BME280 junto al valor de POWER, con la etiqueta *"Dato regional (~50 km): contexto, no su campo"*.
- **Comparación con el satélite:** banda con el valor de SMAP/GWETROOT del sitio, **solo como referencia visual**.
- **Insignia de confianza:** por ejemplo *"Humedad calibrada con 3 puntos"* (o *"Calibrado con 3 puntos, 2 zonas con datos válidos"*).
- **Mensaje de ejemplo:** *"La zona A se seca el doble de rápido. Pregúntale a tu agrónomo si conviene una cobertura o un riego diferenciado ahí."*

---

## 9. El prototipo físico: caja de tierra de 3 zonas

> **Qué es.** Tres secciones de tierra (seca, media y húmeda). En cada una van enterrados un sensor de humedad y un sensor de temperatura; a un lado, un **BME280** mide el aire. Un **ESP32** envía todo por Wi-Fi a la pestaña "Datos de terreno" de la app. Muestra que **un campo no es uniforme** y que un satélite grueso lo promedia todo, y permite comparar el microclima local con lo que da NASA POWER.

```
 Zona A: seca         Zona B: media        Zona C: húmeda
 ┌───────────────┬───────────────────┬───────────────┐
 │   H1     T1   │     H2     T2     │   H3     T3   │   ← altura de tierra 12–15 cm
 │ (humedad+temp)│  (humedad+temp)   │ (humedad+temp)│     sensores a ~8–10 cm
 └───────────────┴───────────────────┴───────────────┘
            ~100 cm (láminas divisoras selladas)         [ESP32]  [BME280 al aire, a la sombra]
 H = humedad capacitiva · T = DS18B20 · láminas divisoras entre zonas
```

### 9.1 Qué mide cada sensor y con qué dato NASA se relaciona

| Sensor | Cant. | Qué mide | Referencia NASA (solo como contexto) |
|---|---|---|---|
| Humedad de suelo capacitivo v1.2 | 3 | Humedad de cada sección a ~8–10 cm | SMAP L4 y GWETROOT de POWER (escalas muy distintas) |
| DS18B20 impermeable | 3 | Temperatura del suelo de cada sección | Temperatura superficial (MODIS/ECOSTRESS), solo cualitativa |
| BME280 | 1 | Temperatura, humedad y presión del aire | T2M y RH2M de POWER; sirve para calcular ET0 |

> **Qué demuestra y qué no.** **Sí:** variabilidad entre zonas, medición y envío en vivo, calibración de un sensor y una comparación (cualitativa) del aire local contra POWER. **No:** una validación estadística contra SMAP. Preséntenlo como **prueba de concepto a escala de laboratorio**. La caja **no alimenta el motor de recomendación**; la app funciona completa sin ella.

### 9.2 Qué datos aporta que la NASA no da

| Dato de la caja | Qué muestra | Por qué la NASA no lo da |
|---|---|---|
| Humedad por zona | Cómo cambia en cada punto en minutos | SMAP da un valor por 9 km cada 3 h |
| Diferencia entre zonas | Variabilidad dentro de un campo | Ningún satélite grueso la ve |
| Velocidad de secado e infiltración | Cuánto tarda cada zona en absorber y perder agua tras un riego | Medición local de la física del suelo |
| Temperatura del suelo por zona | Suelo húmedo y seco se calientan distinto | Solo hay temperatura superficial |
| Temperatura y humedad del aire | Microclima para calcular ET0 | POWER lo da a ~50 km |

### 9.3 Lista de materiales

**Electrónica**

| ☐ | Cant. | Material | Nota |
|---|---|---|---|
| ☐ | 1 (ideal 2) | ESP32 DevKit (30 pines) | Un segundo, por si falla uno |
| ☐ | 3 (+1 repuesto) | Sensor de humedad de suelo **capacitivo v1.2** | No el resistivo de dos puntas: se corroe en horas |
| ☐ | 3 | DS18B20 impermeable (sonda con cable) | Los 3 comparten un solo pin |
| ☐ | 1 | Resistencia de 4.7 kΩ | Pull-up del bus de los DS18B20 |
| ☐ | 1 | BME280 (módulo 3.3 V, I2C) | Verificar que sea BME280 y no BMP280 (este no mide humedad) |
| ☐ | 1 | Protoboard + cables dupont | O placa perforada si quieren dejarlo fijo |
| ☐ | 1 | Power bank USB + cable | Ver nota de consumo bajo en 9.5, paso 8 |

**Caja y tierra**

| ☐ | Cant. | Material | Nota |
|---|---|---|---|
| ☐ | 1 | Recipiente rígido ~100 × 25 × 20 cm | Jardinera larga, bandeja o canaleta grande |
| ☐ | 2 | Láminas divisoras (plástico rígido o acrílico) | De la altura de la caja; se sellan con silicona |
| ☐ | 1 | Silicona sellante (+ pistola) | Dejar secar antes de llenar |
| ☐ | ~30–35 L | Tierra (la misma para las 3 zonas) | ~50 kg ya húmeda con 12–15 cm de espesor |
| ☐ | 1 | Pasacables o prensaestopas | Para sacar los cables |
| ☐ | 1 | Caja o tapa pequeña para ESP32 y BME280 | El BME280 al aire, a la sombra y lejos del ESP32 (se calienta) |

**Protección, calibración y herramientas**

| ☐ | Material | Para qué |
|---|---|---|
| ☐ | Termocontraíble o silicona/epoxi | Sellar la electrónica de cada sensor de humedad; solo las pistas van enterradas |
| ☐ | Bridas, cinta y etiquetas | Identificar cables y sensores (H1–H3, T1–T3) |
| ☐ | Balanza de cocina (0.1–1 g), recipientes pequeños, regador | Calibración gravimétrica de la humedad |
| ☐ | Microondas u horno | Secar las muestras de tierra |
| ☐ | Soldador y estaño, multímetro, taladro, cuchilla o sierra, cable USB y PC | Armado y programación |

> **Presupuesto:** no incluye precios porque varían según la tienda; el primer paso es comprobar qué piezas se consiguen en La Paz antes del viernes.

### 9.4 Conexiones del ESP32

| Componente | Pin del ESP32 | Observaciones |
|---|---|---|
| Humedad H1 (zona A) | **GPIO 34** (señal) · 3V3 · GND | Pin de solo entrada, ADC1 |
| Humedad H2 (zona B) | **GPIO 35** (señal) · 3V3 · GND | Pin de solo entrada, ADC1 |
| Humedad H3 (zona C) | **GPIO 32** (señal) · 3V3 · GND | ADC1 |
| 3 × DS18B20 (T1–T3) | **GPIO 4** (datos, los 3 juntos) · 3V3 · GND | Un solo 4.7 kΩ entre datos y 3V3. Cable: rojo 3V3, negro GND, amarillo datos (suele ser así; verifiquen el de su sonda) |
| BME280 | **SDA = GPIO 21 · SCL = GPIO 22** · 3V3 · GND | Dirección I2C 0x76 o 0x77 según el módulo |

- **No usen pines del ADC2** para la humedad: dejan de funcionar cuando el Wi-Fi está activo.
- En el código, **promedien 50–100 lecturas** de cada sensor de humedad para reducir el ruido del ADC del ESP32.
- Verifiquen el pinout de su placa antes de soldar; hay variantes de ESP32 DevKit.

### 9.5 Paso a paso

**Paso 1 · Probar cada sensor por separado (antes del sábado)**
- *Humedad:* lean cada sensor por el monitor serie. Anoten el valor al aire (seco) y con el sensor en agua hasta la marca (máximo). Descarten o marquen los que se salgan mucho de lo normal.
- *DS18B20:* conecten de uno en uno y anoten la dirección de cada sonda (código de 64 bits que muestra la biblioteca). Rotulen T1, T2, T3 según su dirección, porque después comparten el mismo cable de datos.
- *BME280:* comprueben que lee temperatura, humedad y presión; así confirman que no es un BMP280.
- Es la prueba de acceso que prevé el plan para el viernes. **Go/no-go del hardware a las 21:00**; si faltan piezas, usen datos abiertos de la International Soil Moisture Network (ISMN) y documenten el diseño.

**Paso 2 · Sellar los sensores de humedad**
- Cubran con termocontraíble o silicona/epoxi la electrónica del sensor (el borde superior con los componentes). Solo la zona con las pistas debe entrar en la tierra.
- Dejen curar el sellador el tiempo que indique el fabricante antes de enterrarlos.
- Etiqueten los cables: H1/A, H2/B, H3/C. Las sondas DS18B20 ya vienen selladas.

**Paso 3 · Armar la caja y las zonas**
- Marquen dos divisiones para 3 zonas de tamaño similar y coloquen las láminas verticales.
- Sellen los bordes con silicona para que el agua no pase entre zonas y dejen secar bien antes de llenar.
- Hagan los orificios del pasacables en un extremo, por encima del nivel de la tierra.

**Paso 4 · Llenar y enterrar los sensores**
- Llenen las 3 zonas con la misma tierra hasta unos 12–15 cm, compactando igual en todas.
- En cada zona entierren el sensor de humedad (vertical, ~8–10 cm, parte sellada sobre la tierra) y la sonda DS18B20 a la misma profundidad, a unos 5–8 cm de distancia entre ambos.
- Eviten que el cable de la sonda quede enrollado sobre el sensor de humedad.

**Paso 5 · Armar el circuito**
- Conecten según la tabla 9.4. Los 3 DS18B20 van en paralelo sobre el mismo pin de datos.
- Ubiquen el BME280 al aire, a la sombra, a un lado de la caja y lejos del ESP32, para no medir el calor de la placa.
- Hagan una primera prueba por serie antes de cerrar nada.

**Paso 6 · Preparar la diferencia entre zonas**
- A: tierra seca. B: humedad media. C: bien húmeda (sin encharcar).
- Para la demo en vivo, rieguen **una sola zona** frente al jurado: su curva de humedad sube y las otras casi no cambian. La temperatura de suelo también puede cambiar un poco entre zonas.

**Paso 7 · Calibrar la humedad, sensor por sensor (obligatorio)**
- Tomen la lectura de cada sensor en 3 estados: tierra seca, humedad media y tierra saturada.
- En cada estado saquen una muestra junto al sensor, pésenla, séquenla en el microondas u horno y vuelvan a pesar. **Contenido de agua = (peso húmedo − peso seco) / peso seco.**
- Con los 3 puntos ajusten la curva *lectura → % de humedad* de cada sensor. **No reutilicen la curva de uno en otro.**
- Comparen las 3 sondas DS18B20 con las tres sumergidas en el mismo vaso de agua: si difieren más de ~0.5 °C, anoten el desfase.
- **Sin la calibración de humedad, los números de la app no significan nada.**

**Paso 8 · Programar el ESP32 y enviar datos**
- Lean los 3 sensores de humedad (promediando), las 3 temperaturas y el BME280, apliquen la calibración y envíen cada 10–15 min (más seguido durante la demo). Esquema de firmware en el Anexo E.
- Envíen un JSON por Wi-Fi a la API (esquema en el Anexo B). Coordinen con **S3** el endpoint `POST /field-node/readings` y con **S4** la pestaña "Datos de terreno".
- *Power bank:* algunos se apagan solos cuando el consumo es bajo. Si pasa, usen uno con modo de bajo consumo o envíen datos con frecuencia.
- *Plan B:* si el Wi-Fi del evento falla, usen el punto de acceso del celular o guarden los datos y muestren el gráfico desde un archivo.

**Paso 9 · Probar de punta a punta**
- Rieguen la zona A y comprueben que su curva sube en la app y B y C casi no cambian.
- Dejen el nodo funcionando varias horas para tener una serie de tiempo presentable.
- Si hay tiempo, comparen temperatura y humedad del aire del BME280 con T2M y RH2M de POWER del mismo día: solo comparación **cualitativa** (POWER es un valor de ~50 km).
- Tomen fotos y un video corto para la demo.

### 9.6 Cómo se integra con el calendario del evento

| Momento | Tarea |
|---|---|
| Antes del sábado | Comprar piezas y probar la lectura de cada sensor por serie. **Go/no-go el viernes a las 21:00** |
| Sábado, mañana | Armar la caja, sellar las zonas y dejar secar la silicona |
| Sábado, tarde | Llenar, enterrar sensores, calibrar y enviar datos a la API. **Meta: nodo enviando hacia las 20:00** |
| Domingo, mañana | Probar de punta a punta, ver las curvas en la app, fotos y video. **Congelar el hardware antes del mediodía** |

> **Reglas del evento.** Antes del sábado solo se prepara el terreno (piezas, pruebas, lecturas); el producto se hace durante el evento. Armar la caja puede caer en esa categoría: si quieren adelantarla, confírmenlo con el Local Lead.

### 9.7 Variante aún más pequeña

Si no quieren cargar la caja de 1 m, usen **3 recipientes de ~25–30 cm en fila**, etiquetados A, B y C. Los sensores y los pasos son los mismos; se ahorran los divisores sellados y casi todo el peso.

### 9.8 Problemas típicos y cómo resolverlos

| Problema | Causa probable | Qué hacer |
|---|---|---|
| Lecturas de humedad ruidosas | ADC del ESP32 | Promediar 50–100 lecturas; cables cortos; alimentación estable |
| Un sensor de humedad "se vuelve loco" | Humedad en la electrónica o unidad defectuosa | Sellar mejor o cambiar por el repuesto |
| La humedad deja de leer al activar el Wi-Fi | Pin del ADC2 | Mover la señal a GPIO 32–39 (ADC1) |
| Los DS18B20 dan −127 °C o 85 °C | Mala conexión o falta del 4.7 kΩ | Revisar resistencia, cableado de datos y alimentación |
| BME280 no se detecta | Dirección I2C o módulo BMP280 | Probar 0x76 y 0x77; confirmar que el chip es BME280 |
| La temperatura del aire sale demasiado alta | BME280 cerca del ESP32 o al sol | Alejarlo y ponerlo a la sombra |
| El agua pasa de una zona a otra | Sellado deficiente | Resellar con silicona y dejar curar |
| No hay Wi-Fi en el evento | Red saturada | Punto de acceso del celular o datos guardados |

### 9.9 Riesgo añadido al unificar: tiempo de curado de la silicona

El *Prototipo mini* arma y sella la caja el sábado por la mañana y manda "dejar secar bien antes de llenar", pero muchas siliconas tardan del orden de **un día** en curar del todo (depende del producto; revisen el envase). Eso puede empujar el llenado y la calibración más allá de las 20:00 del sábado. Opciones:

1. **Usar la variante de 3 recipientes (9.7):** elimina el sellado de divisores y el riesgo de que el agua pase entre zonas.
2. **Pedir permiso al Local Lead** para sellar divisores y sensores el viernes (es armado de piezas, no código ni modelos).
3. Si se mantiene la caja larga, **sellar primero** (sábado 9:30) y **calibrar los sensores en recipientes aparte mientras cura**, para enterrarlos ya calibrados.

### 9.10 Mejoras opcionales (del *Resumen del proyecto*; solo si el nodo básico ya funciona)

| Mejora | Qué aporta | Nota |
|---|---|---|
| **Dos profundidades por zona** (~10 y ~30 cm) con **ADS1115** | Compara con la zona radicular de SMAP/GWETROOT; el ADC externo es menos ruidoso (6 sensores = 3 zonas × 2 profundidades) | Añade complejidad de calibración (×2 sensores) |
| **Panel solar + módulo de carga + celda 18650 + *deep sleep*** | Autonomía: lee, envía cada 10–15 min y duerme | Alternativa al power bank |
| **Pluviómetro de balancín** | Lluvia puntual para comparar con IMERG y PRECTOTCORR | Útil con lluvia real, poco en interior |
| **Mini bomba con relé** | Riego automático ante déficit hídrico, conectado a la fórmula de potencia de bombeo `P = ρ·g·Q·H/η` | Solo si el nodo ya funciona; diferenciador para E1 |

### 9.11 ¿Complementa o reemplaza a la NASA?

**Complementa.** El satélite da cobertura continua y décadas de historia en cualquier lugar, pero a escala de 9–50 km. El sensor da la verdad local en un punto, pero solo tiene datos desde que se instala. Juntos se cubren sus debilidades: si coinciden, ganan confianza; si difieren, es información útil (sesgo del satélite, diferencia de profundidad o particularidad del suelo).

**Matiz clave:** en el MVP la caja no alimenta el motor de recomendación; las estrategias se calculan con datos NASA, suelo y cultivos. La app funciona completa aunque el hardware falle. La caja alimenta la pestaña "Datos de terreno" y puede subir la confianza de las tarjetas. Por eso el nodo sensor es una **extensión, no parte del MVP**.

### 9.12 Advertencias honestas sobre la caja

- **No comparar la caja con SMAP de forma cuantitativa:** una caja regada a mano no representa 9 km de terreno y el RMSE saldría sin sentido.
- Presentarla como **prototipo a escala de laboratorio**; cualquier dato simulado debe etiquetarse como **simulado**.
- En ~27 horas habrá solo horas o un par de días de datos: es prueba de concepto del método (calibración, ingesta, visualización, variabilidad entre zonas).
- **Calibrar cada sensor por separado:** los capacitivos baratos varían bastante entre unidades.

---

## 10. Organización del equipo y reparto de tareas

Seis personas, ~27 horas efectivas (dos jornadas de ~13 h con descanso) y un producto de punta a punta. El reparto sigue el flujo de datos (datos → analítica → motor → interfaz) y asigna a Industrial y Electromecánica los frentes donde su formación aporta lo que Sistemas no cubre: **conocimiento agroeconómico y validación** (I1) y **física del agua y mediciones en terreno** (E1).

```
 S1 Datos ──features.parquet + catálogo──▶ S2 Analítica ──indicators.py + riesgos──▶ S3 Motor/API
    ▲                                          ▲                                        │ OpenAPI v1 (se congela
    │ validación (cualitativa)                 │ ET0 + balance                          ▼ en las primeras 2 h)
 E1 Física y campo ───────────────────────────┘      I1 Producto/agro ──crops.csv·pesos·reglas──▶ S3
    │  + caja de 3 zonas ──POST /field-node/readings──▶ API ──▶ S4 UI ("Datos de terreno")
    └──────────────┬──────────────────────────────────────┴────────────┬──────────────┘
                   ▼  Entregable final: app pública + repo abierto + demo (≤7 diapositivas o ≤30 s) + formulario en inglés
```

> **Cómo adaptar el reparto.** S1–S4 son roles, no personas. Asignen los cuatro roles de Sistemas según la experiencia real (quién domina Python geoespacial, quién React, quién FastAPI, quién estadística). Cada rol tiene un **respaldo** (10.8) para que nadie sea punto único de falla.

### 10.1 S1 · Líder técnico e ingeniería de datos geoespaciales
*Misión: que los datos NASA fluyan limpios y a tiempo hasta el motor, y que el repositorio sea la fuente de verdad.*

- **Funciones**
  - Repositorio, ramas, revisión de PR y despliegue final; resolver bloqueos técnicos; integración extremo a extremo.
  - Clientes de datos (POWER, IMERG, SMAP, NDVI, SRTM, suelo, GRACE-FO) con caché en Parquet/NetCDF y comando reproducible `make data`.
  - Armonización a pasos de 10 días y rejilla común; control de calidad; `catalog.yaml`.
  - Lanzar a las 9:30 las tareas asíncronas de AppEEARS (NDVI/SRTM/SMAP) del sitio piloto.
- **Entregables**
  - *Jue–Vie:* cuentas (Earthdata; GEE opcional), entorno y prueba "hello-data" (1 llamada POWER + 1 búsqueda earthaccess).
  - *Sáb:* 10:00 repo y contrato de datos · 12:30 POWER 1981–2025 en caché · 15:00 IMERG + SMAP (o GWETROOT) · 17:30 NDVI limpio + SRTM + suelo · 20:00 GRACE-FO y "sitio piloto v1".
  - *Dom:* segundo sitio (si hay tiempo), documentación de datos, release final y enlaces públicos.
- **Hecho cuando…** `make data` regenera todo; cada dato tiene metadatos; las pruebas pasan; el README de datos explica escala y limitaciones.
- **Herramientas:** Python 3.11, earthaccess, xarray, rioxarray, rasterio, pandas, requests, DuckDB/Parquet, Git/GitHub, AppEEARS.
- **Plan B:** si earthaccess o AppEEARS fallan: POWER + GIBS + descargas manuales desde Earthdata Search. Si GEE no está disponible: no se usa.

### 10.2 S2 · Analítica e IA
*Misión: convertir series de datos en indicadores y evidencia confiable, con incertidumbre explícita.*

- **Funciones**
  - Biblioteca de indicadores (GDD, heladas, calor, SPI, anomalía de humedad, CDD, P95, aridez, días de suelo desnudo) con pruebas unitarias y docstrings con unidades y fuente.
  - Fenología NDVI y ventanas de suelo desnudo; riesgo por etapa del cultivo (con S3).
  - Motor de incertidumbre Monte Carlo y reglas de confianza; sliders de escenario ΔT/ΔP.
  - Zonas de manejo (k-means/GMM) como extensión; capa de explicación (plantillas y, opcional, LLM con límites); **declaración de uso de IA**; model card.
- **Entregables**
  - *Jue–Vie:* repaso de FAO-56, SPI y fenología; cuaderno de pruebas (exploración, no producto).
  - *Sáb:* 12:30 indicadores climáticos v1 · 16:00 humedad de suelo + NDVI y suelo desnudo v1 · 19:00 riesgo por etapa (librería importable) · 22:00 Monte Carlo v1 y confianza.
  - *Dom 11:00:* escenarios, explicaciones en lenguaje claro, model card y declaración de IA.
- **Hecho cuando…** cada indicador tiene prueba, unidades y fuente; los cuadernos reproducen los números del demo; se documentan umbrales y supuestos.
- **Herramientas:** Python, NumPy, pandas, SciPy, statsmodels, scikit-learn, pymannkendall, matplotlib/Plotly; API de un LLM (opcional).
- **Plan B:** si zonas o ML no alcanzan, se omiten; estadística + reglas ya cubren el reto.

### 10.3 S3 · Backend y motor de recomendación
*Misión: construir el cerebro (cultivos → estrategias → puntajes → explicación) y exponerlo con una API estable.*

- **Funciones**
  - Contrato OpenAPI y **mocks desde la primera hora** para desbloquear a S4.
  - Carga y validación de la base de cultivos (CSV de I1) con pydantic; reglas de rotación.
  - Generador de candidatos, filtro de factibilidad, plantillas E0–E3, puntaje MCDA y Monte Carlo (con S2).
  - Tarjetas de estrategia en JSON (cinco bloques + confianza); caché, logging y despliegue público.
  - **Endpoint `POST /field-node/readings`** (y `GET` para la serie) para la caja de sensores, con validación del esquema y marca de tiempo del servidor si el ESP32 no envía hora.
- **Entregables**
  - *Sáb:* 10:00 OpenAPI v0 + mocks (congelado; **incluye `/field-node/readings`**) · 13:00 `/site/summary` e `/indicators` · 17:00 `/rotations/compare` con 3 estrategias · 21:00 tarjetas completas + confianza.
  - *Dom 10:00:* `/scenario`, exportación (JSON/HTML imprimible) y despliegue público estable.
- **Hecho cuando…** API pública con pruebas de contrato; respuestas < 3 s con datos precalculados; errores controlados.
- **Herramientas:** FastAPI, pydantic, pandas/NumPy, SQLite o Turso, pytest, Docker (opcional), Render/Railway/Fly/Hugging Face Spaces.
- **Plan B:** si el despliegue falla: ejecución local reproducible + video + repositorio con instrucciones; la demo no depende de internet.

### 10.4 S4 · Frontend, UX y visualización geoespacial
*Misión: que un agricultor sin formación técnica entienda en dos minutos qué se propone y por qué.*

- **Funciones**
  - Diseño visual (paleta, tipografía legible, iconos de cultivos) y flujo de 5 pasos.
  - Mapa con selección de punto/polígono y capas NASA (teselas GIBS); gráficos (series, climatología con banda P10–P90, calendario de cobertura, criterios).
  - Tarjetas con semáforo de confianza, "ver evidencia", vacíos y preguntas al agrónomo; sliders de prioridades y de escenario.
  - **Pestaña "Datos de terreno"** (8.3): mapa de 3 franjas, series con riegos marcados, tarjeta de aire, insignia de calibración.
  - Accesibilidad, español/inglés, móvil, impresión; prueba con 2–3 personas no técnicas.
- **Entregables**
  - *Jue–Vie:* decisión React vs Streamlit y bocetos en papel (sin código de producto).
  - *Sáb:* 10:30 estructura y mocks · 14:00 mapa y pasos 1–3 · 18:00 tarjetas conectadas a la API · 22:00 gráficos clave y sliders.
  - *Dom:* 10:00 pulido, accesibilidad, inglés y **pestaña de terreno con datos reales de la caja** · 12:00 UI congelada.
- **Hecho cuando…** flujo completo en < 2 minutos, sin errores en consola, contraste correcto, usable en teléfono.
- **Herramientas:** React + Vite + Leaflet o MapLibre + Recharts/Plotly + Tailwind; alternativa Streamlit + folium/pydeck.
- **Plan B:** si React se complica antes del sábado a las 14:00 (¿ya hay mapa y paso 1 funcionando?), pasar a Streamlit. Si no llegan datos de la caja, la pestaña muestra datos *etiquetados como simulados* o se omite.

### 10.5 I1 · Ingeniería Industrial: producto, agroeconomía y gestión
*Misión: asegurar que lo que construimos responde al agricultor real y al reto, que el conocimiento agronómico es correcto y que se entrega a tiempo.*

- **Funciones**
  - **Gestión:** tablero Kanban, bloques de tiempo, checkpoints, registro de decisiones y riesgos; guardián del alcance.
  - **Base de cultivos:** 15–20 cultivos de la región con fuente por fila (FAO EcoCrop, FAO-56, extensión) y reglas de rotación.
  - **Usuario:** personas, 2–3 entrevistas de 15 minutos (agricultor, extensionista, agrónomo) y lista de prioridades y restricciones reales.
  - **MCDA y economía:** criterios y pesos por defecto (AHP simplificado), sensibilidad, margen bruto aproximado, riesgo de mercado y factibilidad operativa (calendario de labores, maquinaria, mano de obra).
  - **Impacto y pitch:** métricas de impacto, ruta de adopción, historia de la demo y **dueño del envío** (textos en inglés).
  - **Apoyo logístico al prototipo:** lista de compras, control del presupuesto y del calendario de la caja (con E1).
- **Entregables**
  - *Jue–Vie:* región piloto con criterios, contactos expertos, lectura de las guías 2026, tablero y plantilla de cultivos.
  - *Sáb:* 11:00 CSV de cultivos v0 (10) · 15:00 v1 (15–20) + pesos · 18:00 entrevista 1 y ajustes.
  - *Dom:* 09:00 validación experta de las cuatro estrategias · 12:00 guion y diapositivas · 14:00 textos en inglés · 17:00 borrador del formulario · 19:00–20:00 envío final.
- **Hecho cuando…** cada cultivo tiene fuente; cada estrategia lista riesgos "para revisar localmente"; el formulario está completo con enlaces públicos verificados.
- **Herramientas:** hojas de cálculo, GitHub Projects/Trello/Notion, Google Slides/Canva, Miro.
- **Plan B:** si no hay entrevistas, usar literatura de extensión, declarar el vacío y marcarlo en la UI como "sin validación local".

### 10.6 E1 · Ingeniería Electromecánica: física de campo, sensores y prototipo
*Misión: aterrizar los datos de satélite al suelo: modelos físicos correctos y un puente tangible con mediciones en terreno.*

- **Funciones**
  - **Modelos físicos:** ET0 de FAO-56 Penman–Monteith con datos POWER (Hargreaves–Samani como respaldo), ETc = Kc·ET0, balance hídrico tipo "balde" y exposición a helada/estrés térmico.
  - **Energía–agua:** potencia de bombeo `P = ρ·g·Q·H/η` y costo energético por mm de riego, como indicador de "costo del agua" (diferenciador).
  - **Caja de 3 zonas (sección 9):** compra y pruebas, armado, sellado, calibración gravimétrica por sensor, firmware del ESP32, envío a la API y cuidado del plan B.
  - **Comparación aire local vs POWER** (T2M/RH2M): **cualitativa**, con la etiqueta de escala regional. La validación estadística real contra satélite queda para un campo real o series abiertas (ISMN).
  - Documentar "satélite vs terreno" para la interfaz (3.2) y producir fotos/video del prototipo.
- **Entregables**
  - *Jue–Vie:* inventario y compra de piezas; prueba de lectura por serie de cada sensor (paso 1 de 9.5); repaso de FAO-56. **Go/no-go del hardware: viernes 21:00.**
  - *Sáb:* 9:30–12:30 armar y sellar (o variante de 3 recipientes) + ET0/ETc v1 · 14:00–17:00 llenar, enterrar, **calibrar** y balance hídrico v1 · **20:00 nodo enviando a la API** (o plan B).
  - *Dom:* 09:00 prueba de punta a punta (riego de la zona A, curva en la app) y comparación cualitativa del aire con POWER · **antes de las 12:00 congelar hardware** · 13:00 fotos y video.
- **Hecho cuando…** ET0 reproduce los ejemplos del capítulo 4 de FAO-56; cada sensor de humedad está calibrado con 3 puntos; la demo de riego de una sola zona funciona; las limitaciones están documentadas.
- **Herramientas:** Python (NumPy/pandas), Arduino IDE/PlatformIO o MicroPython, ESP32, sensor capacitivo de humedad, BME280, DS18B20, power bank, balanza, microondas/horno, multímetro.
- **Plan B:** sin hardware, **datos abiertos de ISMN** y diseño del nodo documentado. Cualquier dato simulado se etiqueta como **simulado**.

### 10.7 Matriz de responsabilidades (R = ejecuta, A = dueño, C = consultado, I = informado)

| Tarea                                                            | S1      | S2      | S3            | S4           | I1          | E1      |
| ---------------------------------------------------------------- | ------- | ------- | ------------- | ------------ | ----------- | ------- |
| Selección de región piloto                                       | C       | C       | C             | C            | **A/R**     | C       |
| Ingesta y armonización de datos NASA                             | **A/R** | C       | I             | I            | I           | C       |
| Indicadores y fenología                                          | C       | **A/R** | C             | I            | C           | C       |
| Base de cultivos y reglas de rotación                            | I       | C       | C             | I            | **A/R**     | C       |
| Motor de puntaje y API                                           | C       | C       | **A/R**       | C            | C           | I       |
| Interfaz, mapa y gráficos                                        | I       | C       | C             | **A/R**      | C           | I       |
| Modelos físicos (ET0, balance hídrico)                           | I       | C       | C             | I            | I           | **A/R** |
| **Caja de 3 zonas: compra, armado, calibración, firmware**       | C       | I       | C             | I            | R (compras) | **A/R** |
| **Endpoint `/field-node/readings` y pestaña "Datos de terreno"** | I       | I       | **A/R** (API) | **A/R** (UI) | I           | C       |
| Incertidumbre y confianza                                        | I       | **A/R** | R             | C            | C           | I       |
| Validación con expertos                                          | I       | C       | I             | I            | **A/R**     | C       |
| Pitch, demo y video (incluye riego en vivo)                      | C       | C       | C             | R            | **A/R**     | R       |
| Formulario de envío (en inglés)                                  | R       | C       | C             | C            | **A/R**     | C       |
| Declaración de uso de IA                                         | C       | **A/R** | C             | C            | C           | C       |
| README y reproducibilidad                                        | **A/R** | R       | R             | R            | C           | R       |

### 10.8 Respaldos y reglas de trabajo

| Rol | Respaldo | Rol | Respaldo |
|---|---|---|---|
| S1 datos | S2 (conoce las series) | S4 interfaz | I1 (contenido) y S3 (API) |
| S2 analítica | E1 (física y modelos) | I1 producto | S4 (presentación) y S1 (envío) |
| S3 motor/API | S1 (despliegue) | E1 campo | S2 (modelos) y **S3 (endpoint y firmware de envío)** |

- **Un solo canal** de mensajería, un tablero y un repositorio. Las decisiones importantes se anotan en un registro de decisiones.
- **Contrato primero:** el esquema de datos y el OpenAPI se congelan en las primeras 2 horas; cambios solo por acuerdo explícito.
- **Git:** trunk-based, PR pequeños (< 200 líneas), integrar al menos cada 2 horas, `main` siempre desplegable.
- **Bloques de tiempo:** si una tarea supera 1.5 veces su estimación, se pide ayuda o se recorta.
- **Definition of Done:** funciona, tiene prueba mínima, está documentado en el README y lo vio otra persona.
- **Cuidado del equipo:** dormir al menos 6 horas, comer a horas fijas, pausas de 5 minutos por hora y turnos de guardia para procesos largos.

---

## 11. Cronograma hasta la entrega

### 11.1 Preparación (jueves 12 y viernes 13): terreno, no producto

| Cuándo | Quién | Tarea |
|---|---|---|
| Jueves, hoy | Todos | Leer este documento y el enunciado completo (30 min). Confirmar que los 6 estén registrados en el mismo evento y conocer la hora de inicio local |
| Jueves, hoy | I1 | Reunión de 45 min para cerrar las seis decisiones; crear tablero; elegir región piloto (4.6); escribir a 2–3 contactos (agrónomo, agricultor, docente) pidiendo 15 minutos el sábado o domingo |
| Jueves, hoy | S1 · S2 · E1 | Crear cuenta gratuita de Earthdata Login (inmediata). S1 crea el repositorio con README y licencia abierta, sin código de producto |
| Jueves, hoy | S3 · S4 | Elegir hosting y cuentas de despliegue; instalar herramientas; decidir React o Streamlit |
| Jueves, hoy | **E1 · I1** | **Armar la lista de compras de la caja y comprobar qué piezas se consiguen localmente antes del viernes** |
| Viernes, mañana | I1 | Leer las guías 2026 de envío y de jurado (se publican el 13). Ajustar demo, campos y plazos |
| Viernes, mañana | S1 · S2 | Pruebas de acceso: una llamada a NASA POWER, una búsqueda con earthaccess y, si se desea, una solicitud mínima en AppEEARS (un punto, un mes). Solo prueba |
| Viernes, tarde | **E1** | **Paso 1 de 9.5:** probar cada sensor por serie (humedad al aire/en agua, direcciones de los 3 DS18B20, BME280 vs BMP280). **21:00 go/no-go**: si faltan piezas, plan B con ISMN |
| Viernes, tarde | Todos | Ensayo de 60 minutos: roles, canales, decisiones, calendario, comidas, descanso; cada quien confirma su checklist (herramientas, cargadores, red alternativa) y duerme temprano |

### 11.2 El evento hora por hora (reloj de referencia: sábado 9:00; ajustar al inicio real)

**Sábado 14**

| Hora | Bloque | Objetivo y criterio de salida |
|---|---|---|
| 09:00–09:30 | Kickoff | Confirmar alcance MVP, región piloto, contrato de datos y de API; registro de decisiones v1 |
| 09:30–10:30 | Arranque | Repo, entorno, API con mocks (incluye `/field-node/readings`), esqueleto de UI y tablero. S1 lanza las tareas de AppEEARS. **E1 arma y sella la caja (o los 3 recipientes) y calibra sensores en recipientes aparte** |
| 10:30–13:00 | Sprint 1 · esqueleto andante | POWER → tres indicadores → un gráfico en pantalla; CSV de cultivos v0; ET0 v1 |
| 13:00–14:00 | Almuerzo y sincronía | 15 minutos de revisión de bloqueos |
| 14:00–17:30 | Sprint 2 · datos e indicadores | NDVI, humedad de suelo, SRTM y suelo; puntaje v1; mapa y pasos 1–3. **E1: llenar, enterrar sensores y calibrar** |
| 17:30–18:00 | **Checkpoint 1** | Demo interna del flujo (aunque feo). **Go/no-go:** si NDVI o SMAP no están, usar respaldo de POWER y recortar alcance |
| 18:00–21:30 | Sprint 3 · estrategias | E0–E3, tarjetas con evidencia y vacíos; **nodo enviando a la API (meta 20:00)**; entrevista 1 |
| 21:30–22:15 | **Checkpoint 2** | Demo extremo a extremo (v0.9). Lista de errores. Se congela el alcance del MVP |
| 22:15–23:15 | Cierre del día | Documentación, commits, procesos largos para la noche (con guardia). **Dejar el nodo registrando datos toda la noche** para tener una serie de tiempo presentable |
| 23:15–07:30 | Descanso | Mínimo 6 horas. Nadie programa de madrugada |

**Domingo 15**

| Hora | Bloque | Objetivo y criterio de salida |
|---|---|---|
| 07:30–12:00 | Sprint 4 · confianza y pulido | Incertidumbre, escenarios, UI pulida, inglés, accesibilidad. **E1: prueba de punta a punta (riego de zona A), comparación cualitativa del aire con POWER, congelar hardware antes de las 12:00** |
| 13:00 | **Congelamiento de funciones** | A partir de aquí solo se corrigen errores |
| 13:00–16:00 | Endurecimiento y materiales | Capturas, **fotos y video de la caja**, diapositivas, README, borrador del formulario en inglés, despliegue estable |
| 16:00–17:00 | QA con ojos nuevos | Alguien que no construyó la función recorre el flujo; arreglos críticos |
| 17:00–19:00 | Pitch y demo | Ensayo (con riego en vivo), video o diapositivas finales; verificar enlaces en ventana privada |
| 19:00–20:00 | **Envío (meta interna 20:00)** | Revisar cada casilla y confirmar que el estado sea "enviado" |
| 20:00–23:59 | Colchón | Solo emergencias. No se agregan funciones |

> **Criterios Go / No-Go.** **Checkpoint 1 (sáb 17:30):** ¿un usuario puede elegir un sitio y ver al menos tres indicadores reales y una comparación de estrategias, aunque simple? Si no: congelar extensiones (incluida la caja) y concentrar a todos en el flujo mínimo. **Checkpoint 2 (sáb 22:00):** ¿existen las cuatro tarjetas con los cinco bloques? Si no: reducir a tres estrategias (E0, E1, E2) y completar bien esas.

---

## 12. Riesgos y planes de contingencia

| # | Riesgo | Prob. | Imp. | Mitigación y plan B | Dueño |
|---|---|---|---|---|---|
| 1 | Acceso a datos NASA lento, caído o con cuentas sin activar | M | A | Probar el viernes; caché del piloto; POWER como columna vertebral; descargas manuales | S1 |
| 2 | Resolución insuficiente (SMAP 9 km, GRACE cientos de km) lleva a conclusiones erróneas a escala de campo | A | M | Etiquetar "contexto regional"; usar HLS/SRTM/suelo para campo; declarar el límite | S2 |
| 3 | Alcance excesivo (scope creep) | A | A | MVP definido; I1 guardián; congelamiento el domingo 13:00; **la caja es extensión** | I1 |
| 4 | Integración tardía entre módulos | M | A | Contrato y mocks en la primera hora; integrar cada 2 h; checkpoints | S1 |
| 5 | Conocimiento agronómico incorrecto | M | A | Fuentes FAO; validación con experto; marcar "sin validar" | I1 |
| 6 | Sobrepromesas o falta de validez científica | M | A | Lenguaje condicional, rangos, limitaciones, model card, pruebas con valores conocidos | S2 |
| 7 | Hardware no llega o no funciona | M | M | Opcional; go/no-go del viernes; plan B con ISMN y diseño documentado; la app funciona sin la caja | E1 |
| 8 | **Silicona sin curar a tiempo; agua pasa entre zonas** | M | M | Variante de 3 recipientes; sellar temprano o consultar al Local Lead por el viernes (9.9) | E1 |
| 9 | **Sensor de humedad defectuoso, ruidoso o sin calibrar** | A | M | 1 repuesto; promedios de 50–100 lecturas; calibración gravimétrica por sensor; ADC1 | E1 |
| 10 | **Comparación cuantitativa indebida con SMAP (RMSE sin sentido)** | M | A | Solo comparación cualitativa y etiqueta de escala; el jurado debe oír "prueba de concepto" | E1 · I1 |
| 11 | Falla de despliegue o de internet en el evento | M | A | Despliegue el sábado por la tarde; ejecución local; video de respaldo; punto de acceso del celular | S3 |
| 12 | Fatiga, enfermedad o desbalance de carga | A | M | Descanso obligatorio; respaldos por rol; recorte de alcance antes que sobreesfuerzo | I1 |
| 13 | Errores en el envío (idioma, enlaces privados, límites de demo) | M | A | Checklist; probar en ventana privada; enviar a las 20:00, no a las 23:59 | I1 |
| 14 | Uso de IA no declarado o material con derechos de autor | B | A | Registro de uso de IA; lista de recursos externos (incluye librerías de Arduino); sin marca NASA en contenido generado por IA | S2 |
| 15 | Cambios o retiros de productos (MODIS, Suomi NPP, API de SoilGrids) | M | M | Usar VIIRS/HLS; documentar versión y fecha; no depender de APIs en vivo | S1 |
| 16 | Las reglas 2026 difieren de las de 2025 (incluida la regla de trabajo previo) | M | M | Leer las guías el 13-nov; consultar al Local Lead por el armado de la caja el viernes | I1 |
| 17 | Datos personales o ubicación exacta de agricultores | B | M | Sitios genéricos o consentimiento; no publicar datos personales | I1 |

*Prob.: B = baja, M = media, A = alta. Imp. (impacto): M = medio, A = alto.*

---

## 13. Validación y control de calidad

| Nivel | Qué se comprueba | Cómo | Dueño |
|---|---|---|---|
| **Técnico** | Los indicadores calculan bien | Pruebas unitarias con series sintéticas y valores conocidos (GDD, SPI ≈ normal, ET0 contra ejemplos de FAO-56); pruebas de contrato de la API; rangos físicos y unidades | S2 · S3 · E1 |
| **Datos** | No se mezclan escalas ni versiones | Catálogo con versión y fecha; % de datos válidos por indicador; revisión visual de NDVI con Worldview/HLS | S1 |
| **Científico** | Las señales son coherentes | Ventanas de suelo desnudo contrastadas con calendarios agrícolas y con el agricultor; sensibilidad del ranking a los pesos (Monte Carlo). **Caja:** calibración con 3 puntos (error documentado) y comparación *cualitativa* del aire con POWER. **Validación estadística satélite vs terreno: solo con campo real o ISMN** | E1 · S2 |
| **Agronómico** | Las estrategias son plausibles | Revisión por un agrónomo o extensionista: cultivos, fechas, plagas, mercado | I1 |
| **Usuario** | Se entiende sin explicación | Prueba con 2–3 personas no técnicas: ¿pueden decir qué estrategia se propone y por qué en 2 minutos? | S4 · I1 |

### 13.1 Métricas que mostraremos en el piloto

- Días de suelo desnudo por año (línea base vs cada estrategia) y su recurrencia entre años.
- Índice de erosión relativo (RUSLE) y su rango de incertidumbre.
- Déficit hídrico estacional (mm) y probabilidad de estrés por calor, helada o sequía en etapas críticas.
- Crédito de nitrógeno estimado (rango) y diversidad de familias de la rotación.
- Estabilidad del ranking ante cambios de pesos y de clima.
- **Caja:** diferencia de humedad entre zonas, tiempo de respuesta tras el riego y velocidad de secado de cada zona.

### 13.2 Limitaciones que declararemos (en la app, el README y el formulario)

- Los datos de satélite son estimaciones con escalas distintas a la del campo; los de 9 km o más son contexto, no medición local.
- El puntaje multicriterio depende de reglas y pesos; se muestran sensibilidad e incertidumbre, no se presentan como verdad.
- No se predicen rendimientos ni se sustituyen análisis de suelo ni visitas técnicas.
- Los escenarios ΔT/ΔP son exploración, no pronóstico.
- La base de cultivos cubre un conjunto limitado y debe validarse localmente.
- **La caja es un prototipo a escala de laboratorio, con horas o pocos días de datos; no valida estadísticamente al satélite.**

---

## 14. Entrega, presentación y defensa ante el jurado

### 14.1 Checklist del envío (confirmar con la guía 2026)

- [ ] **Todos confirmados en el mismo evento:** los 6 integrantes figuran como participantes confirmados del mismo Evento Local (o del Universal).
- [ ] **Nombre y resumen:** en inglés; qué hace, para quién y con qué datos NASA.
- [ ] **Demo:** 7 diapositivas o 30 s (lo que permita la guía 2026); enlace público; primera diapositiva con nombre del equipo, integrantes y reto.
- [ ] **Proyecto final:** URL de la app pública y del repositorio abierto (README con instrucciones, licencia y créditos). Probar ambos en ventana privada.
- [ ] **Descripción detallada:** qué hace, cómo funciona, beneficios, herramientas, validación, limitaciones y próximos pasos.
- [ ] **Datos NASA y otros datos:** cada conjunto (POWER, IMERG, SMAP, MODIS/VIIRS/HLS, SRTM, GRACE-FO…) y cómo se usó; otros datos (SoilGrids, FAO, ISMN si se usa) y bibliotecas.
- [ ] **Uso de IA:** herramientas y para qué; código y datos generados con IA reconocidos; contenido generado con IA identificado; sin marca NASA.
- [ ] **Referencias:** todo recurso externo (código, texto, imágenes, datos, librerías de firmware) listado.
- [ ] **Originalidad:** casilla de confirmación de que el trabajo es original del equipo.
- [ ] **Estado "enviado"** verificado, no borrador.

### 14.2 Borrador de textos en inglés (con huecos para completar)

**Project name:** `RotaSat: a crop-rotation advisor powered by NASA Earth observations` *(working title; confirm before submitting)*

**High-level summary (draft):**
> RotaSat is a farmer-centered decision-support tool that helps explore crop-rotation strategies under changing weather and water conditions. Given a field location, the current rotation and the farmer's priorities (saving water, soil health, income, forage), it combines NASA data (POWER, GPM IMERG, SMAP, MODIS/VIIRS/HLS vegetation indices, SRTM and GRACE-FO) with soil and crop information to compare at least three strategies, including the existing rotation. For each strategy it explains why it was suggested, the supporting evidence, expected benefits, information gaps and risks that need local review. It highlights typically bare-field periods and vulnerable seasons, reports uncertainty, and is designed to support, not replace, farmer judgment and local agricultural expertise. [Optional: A low-cost field node (ESP32 with soil-moisture, soil-temperature and air sensors) shows how local measurements can complement coarse satellite data.]

**Detailed description (outline):**
1. *Problem.* Climate variability and soil degradation make rotation choices hard.
2. *What it does.* Five-step flow; compares [N] strategies; five-block explanation cards; confidence levels.
3. *How it works.* Data harmonization to a common grid and 10-day steps; indicators; bare-window detection; multi-criteria scoring; Monte Carlo uncertainty.
4. *Pilot result.* [Region/crop system]: [X] bare days per year in the baseline; [Y] after the proposed strategy; water deficit [Z] mm.
5. *Field prototype.* [Three-zone soil box: moisture and temperature per zone, BME280 for air]; calibrated with [3 points]; shows within-field variability that coarse satellite pixels average out. Proof of concept at laboratory scale; not a statistical validation of satellite products.
6. *Validation and limits.* [Calibration error]; [expert review]; limitations listed openly.
7. *Next steps.* More regions and crops, local weather stations, offline channel, pilots with extension services.

**Use of AI (template):**
> We used [tools] for [purposes, e.g., code assistance, text editing]. AI-assisted code was reviewed and tested by team members. [If applicable] An LLM rewrites precomputed evidence into plain language; it does not generate numbers. AI-generated media is labeled and does not use NASA branding.

### 14.3 Plan de diapositivas (7) o guion de video (30 s)

| # | Diapositiva | Mensaje |
|---|---|---|
| 1 | Título | Nombre del proyecto, reto "Field Shift", nombre del equipo e integrantes |
| 2 | El dilema del agricultor | Clima cambiante + suelo degradado: ¿qué sembrar después? |
| 3 | La solución | Captura de la app y la promesa en una línea |
| 4 | Cómo funciona | Datos NASA → indicadores → estrategias → explicación |
| 5 | Resultados del piloto | Comparación de estrategias con cifras reales y calendario de suelo desnudo |
| 6 | Confianza y verdad local | Incertidumbre, vacíos, **foto de la caja y su curva por zona**; apoyo, no reemplazo |
| 7 | Impacto y próximos pasos | A quién ayuda, cómo escala, contacto y enlaces |

**Guion de 30 segundos (si optan por video):** 0–5 s: *"Farmers face shifting weather and tired soils."* · 5–12 s: mapa, se marca el campo, sliders de prioridades · 12–22 s: cuatro tarjetas con semáforo de confianza y ventana de suelo desnudo · 22–27 s: *"Built on NASA data, with uncertainty and local review built in."* · 27–30 s: nombre del proyecto y del equipo.

### 14.4 Demo en vivo de 3 minutos

- **0–0.5 min:** el problema en una frase y el agricultor del caso piloto.
- **0.5–1.5 min:** ubicar el campo, elegir la rotación actual y mover prioridades.
- **1.5–2.3 min:** comparar estrategias; abrir una tarjeta y mostrar evidencia, vacíos y preguntas para el agrónomo.
- **2.3–3 min:** pestaña "Datos de terreno": **regar una sola zona frente al jurado** y mostrar cómo sube su curva mientras las otras casi no cambian; cierre con la frase de pitch.

> **Frase de pitch para el prototipo:** *"El satélite nos da el contexto y décadas de historia; el sensor nos da la verdad local. Nuestra caja es una prueba de concepto de cómo un agricultor podría calibrar la recomendación con su propio suelo."*

### 14.5 Preguntas difíciles y respuestas cortas

| Pregunta probable | Respuesta corta |
|---|---|
| ¿Por qué confiar en datos de 9 km para un campo? | No lo hacemos: los datos gruesos se etiquetan como contexto regional. Para el campo usamos 30 m, relieve, suelo y lo que dice el agricultor |
| ¿Qué pasa si la recomendación se equivoca? | Por eso mostramos rangos, vacíos y riesgos, y cada tarjeta termina en preguntas para un experto. No predice rendimientos ni reemplaza visitas técnicas |
| ¿Su caja valida el satélite? | Es una prueba de concepto a escala de laboratorio, no una validación estadística. Con un campo real se calcularía el sesgo frente a SMAP |
| ¿Dónde está la IA y qué hace? | En la fenología (NDVI), la detección de anomalías y, opcionalmente, la redacción de la evidencia ya calculada. La decisión se basa en reglas explicables |
| ¿Cómo evitan recomendaciones equivocadas? | Varias estrategias con rangos, confianza, vacíos y riesgos; cada tarjeta termina en preguntas para un experto |
| ¿Cómo lo validaron? | Pruebas con valores conocidos, calibración gravimétrica de los sensores, comparación cualitativa con POWER y revisión de un agrónomo; limitaciones declaradas |
| ¿Cómo llega a quien tiene poco internet? | Datos precalculados, interfaz ligera y hoja imprimible. Siguiente paso: mensajería y modo sin conexión |
| ¿Escala a otras regiones? | Sí: el pipeline es el mismo; se amplía la base de cultivos y la validación local |
| ¿La caja alimenta las recomendaciones? | En el MVP no: las estrategias salen de datos NASA, suelo y cultivos. La caja alimenta la pestaña de terreno y puede subir la confianza de las tarjetas |

### 14.6 Tu mayor fortaleza

**Es honesta.** Muchos proyectos prometen "aumentar el rendimiento 30 %"; RotaSat muestra patrones, rangos y dudas. Eso encaja con el criterio de **Validez** y con la idea del reto de que la herramienta no reemplaza el criterio local.

---

## 15. Después del evento: hoja de ruta

- **Corto plazo (1–2 meses):** pilotos con extensionistas, ampliar la base de cultivos con fuentes locales, integrar estaciones meteorológicas y análisis de suelo reales; instalar el nodo en un **campo real** para calcular el sesgo frente a SMAP/GWETROOT con datos suficientes.
- **Mediano plazo:** modelos de cultivo calibrados (AquaCrop/DSSAT), datos de rendimiento para ML supervisado, canal sin conexión y por mensajería, idiomas locales; nodo con dos profundidades, ADS1115 y energía solar; riego automático con la fórmula de potencia de bombeo.
- **Sostenibilidad:** código abierto, alianzas con universidades, cooperativas y ONG; datos y decisiones bajo control de los agricultores.

---

## Anexo A. Repositorio y pila

```text
rotasat/
  README.md                 # qué es, cómo ejecutar, datos, IA usada, créditos y licencias
  LICENSE
  data/
    catalog.yaml            # fuente, producto, versión, DOI, licencia, fecha de descarga
    raw/  processed/        # no subir archivos pesados; documentar cómo regenerarlos
  pipeline/                 # S1: clientes de datos y armonización
    power.py  imerg.py  smap.py  ndvi.py  srtm.py  soil.py  grace.py  harmonize.py
  analytics/                # S2 (+E1): indicadores y modelos
    indicators.py  phenology.py  uncertainty.py  scenarios.py  physics.py
  engine/                   # S3 (+I1): estrategias y puntaje
    crops.csv  rules.yaml  candidates.py  scoring.py  cards.py
  api/                      # S3: FastAPI + esquemas pydantic
    main.py  schemas.py     # incluye /rotations/compare y /field-node/readings
  web/                      # S4: interfaz (React + Vite)  o  app.py (Streamlit)
  hardware/                 # E1: firmware ESP32, esquema de conexiones, calibración
    firmware/  calibration/  photos/
  tests/                    # pruebas de indicadores, contrato de API y datos
  docs/                     # decisiones, model card, data card, uso de IA
  Makefile                  # make data | make test | make run
```

**Pila recomendada:** Python 3.11, FastAPI, pandas/NumPy/SciPy, xarray/rioxarray, DuckDB/Parquet, scikit-learn; interfaz con React + Vite + Leaflet/MapLibre (o Streamlit como alternativa); despliegue en Render, Railway, Fly.io o Hugging Face Spaces; firmware en Arduino IDE/PlatformIO. Aprovechen lo que ya conocen: la novedad debe estar en el uso de los datos, no en el tooling.

---

## Anexo B. Contrato de la API

### B.1 Comparación de estrategias

```json
POST /rotations/compare
{
  "site": {"lat": 0.0, "lon": 0.0, "polygon": null},
  "current_rotation": ["maize", "fallow"],
  "priorities": {"water": 0.8, "soil": 0.65, "income": 0.4, "forage": 0.25, "low_risk": 0.55},
  "constraints": {"irrigation": "rainfed", "equipment": ["tractor"], "excluded_crops": []},
  "scenario": {"delta_t_c": 0.0, "delta_p_pct": 0}
}
-> 200
{
  "strategies": [
    {"id": "E1", "name": "Suelo primero", "sequence": ["maize", "cover:vetch+oat", "soybean"],
     "score": {"median": 71, "p10": 63, "p90": 78},
     "criteria": {"soil": 80, "water": 62, "risk": 70, "pests": 75, "economy": 60, "feasibility": 85},
     "confidence": {"level": "medium", "reasons": ["no local soil analysis", "NDVI at 250 m"]},
     "card": {"why": "...",
              "evidence": [{"indicator": "bare_days", "value": 0, "unit": "d", "source": "VIIRS", "scale": "local"}],
              "benefits": ["..."], "gaps": ["..."], "local_review": ["..."],
              "questions_for_agronomist": ["..."]}}
  ],
  "data_quality": {"coverage": 0.82, "versions": {"POWER": "...", "SMAP_L4": "v8"}}
}
```

### B.2 Lecturas de la caja de sensores (esquema unificado)

Unifica el formato del *Resumen* (dos profundidades) y el del *Prototipo mini* (una profundidad). `moisture` es obligatorio; `moisture_30cm` es opcional (solo si se añade la segunda profundidad). `ts` es opcional: si falta, el servidor pone la hora de recepción.

```json
POST /field-node/readings
{
  "node_id": "caja1",
  "ts": "2026-11-14T15:00:00Z",
  "zones": [
    {"id": "A", "moisture": 0.12, "soil_temp_c": 19.2, "raw": 3010, "moisture_30cm": null},
    {"id": "B", "moisture": 0.24, "soil_temp_c": 18.1, "raw": 2540},
    {"id": "C", "moisture": 0.38, "soil_temp_c": 17.0, "raw": 2010}
  ],
  "air": {"t_c": 22.4, "rh": 41, "p_hpa": 745.0},
  "calibration": {"points": 3, "method": "gravimetric", "date": "2026-11-14"}
}
-> 201 {"stored": true}
```
*(`moisture` = contenido volumétrico o gravimétrico en fracción 0–1: definan cuál usan al calibrar y escríbanlo en la UI. Los valores del ejemplo son ilustrativos. `p_hpa` depende de la altitud del sitio.)*

---

## Anexo C. Base de cultivos (crops.csv)

| Columna | Ejemplo | Fuente |
|---|---|---|
| `crop_id`, `name_es`, `name_en` | soybean, Soya, Soybean | — |
| `family`, `type` | Fabaceae, legume | Botánica / extensión |
| `cycle_days`, `t_base`, `t_opt`, `t_max` | valores por cultivo | FAO EcoCrop, literatura local |
| `frost_tolerance_c`, `drought_tolerance` | −2 °C; media | FAO EcoCrop, extensión |
| `kc_ini`, `kc_mid`, `kc_end` | valores FAO-56 | FAO-56 (tablas de Kc) |
| `root_depth_m`, `water_need_mm` | rango | FAO-56 / extensión |
| `n_fix_kg_ha_min`, `n_fix_kg_ha_max` | rango (cero si no fija) | Literatura local |
| `residue_t_ha`, `cover_c_factor` | rango; factor C | Literatura, RUSLE |
| `pest_family_group`, `min_return_years` | Fabaceae; 3 | Extensión |
| `forage_value`, `market_risk`, `labor_peaks` | alto/medio/bajo; meses | Entrevistas, mercado local |
| `planting_window`, `harvest_window` | meses por zona | Calendario agrícola local |
| `source_ids` | enlaces o citas | **Obligatorio en cada fila** |

---

## Anexo D. Código de referencia (Python)

Son esquemas para orientar; **no están probados contra las API reales** (no tuve acceso a esas API desde mi entorno). Sí probé con datos sintéticos la lógica de los fragmentos 3 y 4 y la lectura del JSON de POWER. Confirmen parámetros y nombres de productos en la documentación oficial. Si usan código generado o adaptado, regístrenlo en la declaración de uso de IA y en las referencias.

```python
# 1) NASA POWER: serie diaria de un punto (sin clave). Máximo 20 parámetros por solicitud.
import requests, pandas as pd
def fetch_power(lat, lon, start="19810101", end="20251231"):
    params = {"parameters": "T2M,T2M_MAX,T2M_MIN,PRECTOTCORR,RH2M,WS2M,ALLSKY_SFC_SW_DWN,GWETROOT",
              "community": "AG", "longitude": lon, "latitude": lat,
              "start": start, "end": end, "format": "JSON"}
    r = requests.get("https://power.larc.nasa.gov/api/temporal/daily/point", params=params, timeout=120)
    r.raise_for_status()
    d = r.json()["properties"]["parameter"]
    df = pd.DataFrame(d); df.index = pd.to_datetime(df.index, format="%Y%m%d")
    return df.replace(-999, float("nan"))          # -999 = dato faltante en POWER

# 2) Búsqueda con earthaccess (requiere cuenta gratuita de Earthdata)
import earthaccess
earthaccess.login()
res = earthaccess.search_data(short_name="SPL4SMGP", bounding_box=(lon-0.2, lat-0.2, lon+0.2, lat+0.2),
                              temporal=("2023-01-01", "2023-01-31"))
files = earthaccess.download(res[:2], "./data/raw/smap")   # confirmar nombre y versión en Earthdata Search
```

```python
# 3) Indicadores básicos (pandas/NumPy)
import numpy as np
def gdd(tmax, tmin, tbase): return np.maximum(0, (tmax + tmin) / 2 - tbase)
def frost_days(tmin, thr=0.0): return (tmin <= thr).astype(int)
def cdd(precip, wet=1.0):                       # racha máxima de días secos
    run = best = 0
    for p in precip:
        run = run + 1 if p < wet else 0
        best = max(best, run)
    return best
def zscore_by_month(s):                          # anomalía respecto de la climatología mensual
    g = s.groupby(s.index.month)
    return (s - g.transform("mean")) / g.transform("std")
def bare_windows(ndvi_dekad, thr=0.22, min_dekads=3):   # ventanas continuas de baja cobertura
    low = (ndvi_dekad < thr).to_numpy(); out = []; start = None
    for i, v in enumerate(low):
        if v and start is None: start = i
        if (not v or i == len(low) - 1) and start is not None:
            end = i if v else i - 1
            if end - start + 1 >= min_dekads: out.append((start, end))
            start = None
    return out

# 4) Puntaje multicriterio + Monte Carlo (esqueleto)
rng = np.random.default_rng(42)
def score(criteria, w):                          # criteria y w: diccionarios {criterio: valor}
    return sum(w[k] * criteria[k] for k in w)
def monte_carlo(criteria_fn, weights, n=500, conc=60):
    keys = list(weights); base = np.array([weights[k] for k in keys])
    out = []
    for _ in range(n):
        w = dict(zip(keys, rng.dirichlet(base * conc)))    # pesos perturbados alrededor de los del usuario
        out.append(score(criteria_fn(rng), w))             # criteria_fn remuestrea años y parámetros
    out = np.array(out)
    return {"median": np.median(out), "p10": np.percentile(out, 10), "p90": np.percentile(out, 90)}
```

---

## Anexo E. Esquema de firmware del ESP32

> **No probado en hardware.** Es un esqueleto para ahorrar tiempo: revisen bibliotecas, pines y la variante de su placa antes de usarlo. Bibliotecas de Arduino: *Adafruit BME280* (con *Adafruit Unified Sensor*), *OneWire* y *DallasTemperature*. Regístrenlas en las referencias del envío.

```cpp
// RotaSat · nodo de campo (3 zonas) — esquema de referencia
#include <WiFi.h>
#include <HTTPClient.h>
#include <OneWire.h>
#include <DallasTemperature.h>
#include <Wire.h>
#include <Adafruit_BME280.h>

const char* WIFI_SSID = "...";
const char* WIFI_PASS = "...";
const char* API_URL   = "http://TU-API/field-node/readings";   // si es HTTPS: WiFiClientSecure (setInsecure solo para pruebas)
const unsigned long INTERVAL_MS = 15UL * 60UL * 1000UL;        // 10–15 min; bajar a 10–30 s durante la demo

const int  PIN_H[3]  = {34, 35, 32};     // humedad zonas A, B, C -> ADC1 (nunca ADC2 con Wi-Fi)
const int  PIN_OW    = 4;                // bus DS18B20 (con 4.7 kΩ a 3V3)
const char* ZONE[3]  = {"A", "B", "C"};

// Calibración POR SENSOR (paso 7): lectura cruda [seca, media, saturada] y humedad medida (fracción)
// Valores de EJEMPLO: reemplazar con los medidos. El sensor capacitivo da menos lectura cuando hay más agua.
float RAW[3][3] = {{3200, 2600, 1900}, {3200, 2600, 1900}, {3200, 2600, 1900}};
float VWC[3][3] = {{0.05, 0.20, 0.40}, {0.05, 0.20, 0.40}, {0.05, 0.20, 0.40}};

// Direcciones de los DS18B20 anotadas en el paso 1 (T1, T2, T3)
DeviceAddress T_ADDR[3] = {{0,0,0,0,0,0,0,0}, {0,0,0,0,0,0,0,0}, {0,0,0,0,0,0,0,0}};

Adafruit_BME280 bme;
OneWire ow(PIN_OW);
DallasTemperature ds(&ow);

float readRaw(int pin, int n = 64) {            // promedio de 50–100 lecturas
  long s = 0;
  for (int i = 0; i < n; i++) { s += analogRead(pin); delay(5); }
  return s / (float)n;
}

float rawToVwc(float raw, const float r[3], const float v[3]) {   // interpolación lineal por tramos
  if (raw >= r[0]) return v[0];
  if (raw <= r[2]) return v[2];
  if (raw >= r[1]) return v[0] + (v[1] - v[0]) * (r[0] - raw) / (r[0] - r[1]);
  return v[1] + (v[2] - v[1]) * (r[1] - raw) / (r[1] - r[2]);
}

void setup() {
  Serial.begin(115200);
  analogReadResolution(12);
  analogSetAttenuation(ADC_11db);                                // rango ~0–3.3 V
  ds.begin();
  if (!bme.begin(0x76) && !bme.begin(0x77)) Serial.println("BME280 no detectado (¿es un BMP280?)");
  WiFi.begin(WIFI_SSID, WIFI_PASS);
  while (WiFi.status() != WL_CONNECTED) { delay(500); Serial.print("."); }
}

void loop() {
  ds.requestTemperatures();
  String json = "{\"node_id\":\"caja1\",\"zones\":[";
  for (int i = 0; i < 3; i++) {
    float raw = readRaw(PIN_H[i]);
    float vwc = rawToVwc(raw, RAW[i], VWC[i]);
    float t   = ds.getTempC(T_ADDR[i]);                          // -127 = error de conexión
    if (i) json += ",";
    json += "{\"id\":\"" + String(ZONE[i]) + "\",\"moisture\":" + String(vwc, 3) +
            ",\"soil_temp_c\":" + String(t, 1) + ",\"raw\":" + String(raw, 0) + "}";
  }
  json += "],\"air\":{\"t_c\":" + String(bme.readTemperature(), 1) +
          ",\"rh\":" + String(bme.readHumidity(), 0) +
          ",\"p_hpa\":" + String(bme.readPressure() / 100.0F, 1) + "}}";
  Serial.println(json);

  if (WiFi.status() == WL_CONNECTED) {
    HTTPClient http;
    http.begin(API_URL);
    http.addHeader("Content-Type", "application/json");
    int code = http.POST(json);
    Serial.printf("POST -> %d\n", code);
    http.end();
  } else {
    WiFi.reconnect();                                            // plan B: guardar en memoria/archivo y reenviar
  }
  delay(INTERVAL_MS);
}
```

---

## Anexo F. Backlog priorizado

| # | Historia de usuario / tarea | Prioridad | Dueño | Estimación |
|---|---|---|---|---|
| 1 | Contrato de datos y OpenAPI v0 con mocks (incluye `/field-node/readings`) | P0 | S1 · S3 | 1 h |
| 2 | Cliente POWER con caché y catálogo | P0 | S1 | 2 h |
| 3 | Indicadores climáticos: heladas, calor, CDD, SPI, GDD | P0 | S2 | 2.5 h |
| 4 | ET0/ETc y balance hídrico | P0 | E1 | 3 h |
| 5 | CSV de cultivos v0 con fuentes (10 cultivos) | P0 | I1 | 2 h |
| 6 | Pasos 1–3 del flujo con mapa y prioridades | P0 | S4 | 4 h |
| 7 | NDVI limpio y ventanas de suelo desnudo | P0 | S1 · S2 | 4 h |
| 8 | Puntaje multicriterio y tres estrategias | P0 | S3 | 4 h |
| 9 | Tarjetas de estrategia con cinco bloques | P0 | S3 · S4 · I1 | 4 h |
| 10 | Formulario en inglés, diapositivas y ensayo | P0 | I1 · S4 | 4 h |
| 11 | SRTM + suelo (SoilGrids o dato local) → erosión relativa | P1 | S1 · S2 | 3 h |
| 12 | Monte Carlo, confianza y vacíos automáticos | P1 | S2 · S3 | 3 h |
| 13 | Entrevista a agrónomo y ajuste de reglas | P1 | I1 | 2 h |
| 14 | IMERG, SMAP y GRACE-FO en el sitio piloto | P1 | S1 | 3 h |
| 15 | **Compra de piezas y prueba de lectura por serie de cada sensor (viernes)** | P1 | E1 · I1 | 3 h |
| 16 | **Armado y sellado de la caja (o 3 recipientes)** | P2 | E1 | 3 h |
| 17 | **Llenado, enterrado y calibración gravimétrica de 3 sensores de humedad** | P2 | E1 | 3 h |
| 18 | **Firmware del ESP32 y envío a `/field-node/readings`** | P2 | E1 · S3 | 3 h |
| 19 | **Pestaña "Datos de terreno" (zonas, series, tarjeta de aire, insignia)** | P2 | S4 | 3 h |
| 20 | **Prueba de punta a punta (riego de zona A) y fotos/video** | P2 | E1 · S4 | 2 h |
| 21 | Sliders de escenario ΔT/ΔP | P2 | S2 · S4 | 2 h |
| 22 | Segundo sitio contrastante | P2 | S1 · S2 | 2 h |
| 23 | Hoja imprimible y/o bot | P3 | S4 | 2 h |
| 24 | Mejoras del nodo (2 profundidades + ADS1115, solar, pluviómetro, bomba) | P3 | E1 | — |

*P0 = imprescindible; P1 = importante; P2 = diferenciador; P3 = si sobra tiempo.*

---

## Anexo G. Fuentes y enlaces

**Documentos del equipo fusionados aquí:** *Plan integral del equipo* (PDF), *RotaSat · Resumen del proyecto* (PDF), *Prototipo mini: humedad + temperatura + BME280* (PDF).

- Space Apps · preguntas frecuentes de participantes: <https://www.spaceappschallenge.org/resources/participant-faqs/>
- Space Apps · guía de envío de proyectos (versión 2025; la 2026 se publica el 13-nov): <https://www.spaceappschallenge.org/resources/project-submission-guide>
- NASA POWER: <https://power.larc.nasa.gov/>
- NASA Earthdata: <https://www.earthdata.nasa.gov/>
- earthaccess (Python): <https://github.com/nsidc/earthaccess>
- NASA Worldview / GIBS: <https://worldview.earthdata.nasa.gov/>
- SMAP L4 (SPL4SMGP): <https://nsidc.org/data/spl4smgp>
- Transición de MODIS a VIIRS: <https://ladsweb.modaps.eosdis.nasa.gov/learn/modis-to-viirs-transition/>
- ECOSTRESS v3: <https://www.earthdata.nasa.gov/data/alerts-outages/ecostress-version-3-higher-level-data-products-released>
- GRACE-FO en PO.DAAC: <https://podaac.jpl.nasa.gov/>
- SoilGrids (ISRIC): <https://docs.isric.org/globaldata/soilgrids/>
- FAO EcoCrop: <https://ecocrop.fao.org/>
- International Soil Moisture Network (mediciones en terreno): <https://ismn.earth/>

**Literatura base:** Allen, Pereira, Raes y Smith (1998), *Crop evapotranspiration*, FAO Irrigation and Drainage Paper 56. Renard et al. (1997), *Predicting soil erosion by water: RUSLE*, USDA Handbook 703. McKee, Doesken y Kleist (1993), índice SPI. Saaty (1980), *The Analytic Hierarchy Process*. Claverie et al. (2018), HLS, *Remote Sensing of Environment* 219. Poggio et al. (2021), SoilGrids 2.0, *SOIL* 7. Landerer y Swenson (2012), GRACE, *Water Resources Research* 48. Fisher et al. (2020), ECOSTRESS, *Water Resources Research* 56.

> **Verifiquen antes de citar.** Los enlaces y los estados de los productos reflejan lo consultado el 12-nov-2026. Las guías 2026 del evento (envío y jurado) prevalecen sobre cualquier dato de este documento.
