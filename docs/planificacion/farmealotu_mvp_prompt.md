RotaSat — Implementación inmediata del MVP NASA Field Shift
1. Tu rol y misión
Actúa como arquitecto de software senior, ingeniero backend Python, desarrollador frontend y especialista en sistemas de apoyo a decisiones agrícolas.
Debes construir código ejecutable, no solamente describir una solución.
Estamos participando en el NASA Space Apps Challenge 2026, desafío Field Shift: Adapting Farms with NASA Data.
Disponemos de 12 horas para desarrollar un MVP completamente funcional llamado RotaSat.
Problema concreto: los agricultores de Cuatro Cañadas, Santa Cruz, Bolivia, necesitan decidir qué cultivos sembrar en sucesión para reducir su exposición a sequías, exceso de lluvias, deterioro del suelo y restricciones de producción.
Solución: una aplicación móvil-web que combina datos NASA históricos y recientes con información agrícola local y prioridades del agricultor para comparar estrategias de rotación y explicar riesgos, ventajas, limitaciones e incertidumbre.
La aplicación debe apoyar decisiones, nunca reemplazar al agricultor ni al agrónomo.
Restricciones obligatorias
- Exclusivamente Cuatro Cañadas, Bolivia.
- Máximo 12 horas de implementación.
- Una sola aplicación web responsive, utilizable desde un teléfono.
- Sin autenticación, pagos, chatbots ni funcionalidades irrelevantes.
- No utilizar LLM para calcular recomendaciones agronómicas.
- Algoritmos deterministas, explicables y comprobables.
- Incluir al menos tres estrategias de rotación, contando la actual del agricultor.
- Diferenciar siempre datos observados, estimaciones, supuestos y demostraciones.
- Priorizar funcionalidad sobre sofisticación visual.
- Código completo, ejecutable y organizado.
No agregues funcionalidades que no estén directamente relacionadas con la decisión agrícola.

2. Investigación agrícola: Cuatro Cañadas
Esta es la información investigada que debes incorporar como base de conocimiento.
2.1 Producción agrícola municipal
Fuente: Ministerio de Desarrollo Productivo y Economía Plural, Santa Cruz Industrial y Productiva 2024, fichas municipales, datos de producción 2023 del INE/MDRyT.
URL: https://www.siip.produccion.gob.bo/noticias/files/2025-c2369-SANTA-CRUZ-V3-FM.pdf
CultivoProducción 2023 (t)Superficie 2023 (ha)Soya811959318083Sorgo18759093955Maíz7319917936Girasol1858918025Trigo149787383Chía19732592Arroz367171
Interpretación obligatoria: estos números son la línea base municipal verificada para 2023, no datos de 2026 ni parámetros predictivos para una finca individual.
La soya es el cultivo predominante. Sorgo, maíz, girasol y trigo son alternativas locales documentadas. Chía puede figurar como cultivo conocido, pero no debe convertirse en recomendación prioritaria sin suficientes parámetros agronómicos.
2.2 Paradigma productivo
Fuentes:
ANAPO, julio 2024:
https://anapobolivia.org/anapo-reafirma-la-produccion-sostenible-con-la-siembra-directa-y-la-rotacion-de-cultivos/
Exposoya 2026:
https://larazon.bo/economia-y-empresa/2026/03/06/la-exposoya-2026-abre-sus-puertas-en-el-municipio-cruceno-de-cuatro-canadas/
Observaciones:
1. La región tiene una agricultura comercial de granos y oleaginosas.
2. Existen campañas agrícolas de verano e invierno.
3. La soya tiene un papel central en la producción de verano.
4. El sorgo, girasol, maíz y trigo participan en sistemas de rotación.
5. La siembra directa y la conservación del rastrojo forman parte de las prácticas promovidas por ANAPO.
6. Los cultivos de cobertura también se están promoviendo, pero su viabilidad depende del agua disponible y de su manejo.
7. No debe asumirse que todos los agricultores practican la misma rotación ni que todos tienen riego.
Importante: ANAPO reportó que el sorgo representó el 67% de la superficie de cultivos de rotación de invierno en sus datos regionales de 2023. Es un dato regional, no una distribución exacta de Cuatro Cañadas.
2.3 Evidencia de los riesgos
Inundación local, marzo 2025: se reportaron 1104 ha de soya perdidas por desbordamiento del río Grande en Cuatro Cañadas.
https://eldeber.com.bo/santa-cruz/cuatro-canadas-se-declara-en-zona-de-desastre-por-el-desborde-del-rio-grande_507510
Campaña 2025–2026: ANAPO reportó sequía entre diciembre de 2025 y enero de 2026, seguida por exceso de lluvias en febrero y marzo en zonas productivas que incluyen Cuatro Cañadas.
https://economy.com.bo/negocios/anapo-reporta-disminucion-produccion-soya-verano-2025-2026-debido-lluvias
Estos hechos justifican analizar dos riesgos distintos: falta de agua durante el desarrollo y exceso de precipitaciones durante etapas sensibles o cosecha.
No convertir estas noticias históricas en alertas meteorológicas actuales. Las advertencias activas deben calcularse con datos fechados.
2.4 Base de conocimiento estática
Crear tres archivos versionados:
- reference/crops.json: cultivos, familias botánicas, estaciones candidatas, demanda hídrica, características, restricciones y fuentes.
- reference/rotation_rules.json: reglas sobre sucesión de cultivos, cobertura, agua, familias botánicas y factibilidad.
- reference/agricultural_context.json: estadísticas municipales, prácticas documentadas, riesgos históricos y bibliografía.
Cada entrada debe tener como mínimo source_url, reference_year, verified_at, geographic_scope y evidence_status.
Estados posibles:
- documented: directamente respaldado por una fuente.
- derived: calculado a partir de datos.
- assumption: supuesto explícito.
- farmer_provided: proporcionado por el usuario.
- unavailable: dato faltante.
Los parámetros numéricos de los cultivos deben provenir de FAO-56, EcoCrop o investigaciones agrícolas identificables. Si no existe respaldo, usar null y explicar la limitación. No inventar coeficientes Kc, rendimientos, precios ni requerimientos hídricos exactos.
Decisión: para 12 horas, JSON versionado es suficiente. No instalar MariaDB. Usar SQLite únicamente si hace falta persistir caché o entradas del usuario.

3. Arquitectura técnica definitiva
Implementa:
Frontend
React + Vite + TypeScript + Tailwind CSS, responsive y mobile-first.
Backend
Python 3.11+ con FastAPI, Pydantic, Requests y NumPy. Pandas solo cuando simplifique el procesamiento de series temporales.
Motor
Reglas agronómicas + evaluación multicriterio (MCDA) + remuestreo estadístico de años climáticos.
Datos
NASA POWER obligatoria, NASA GIBS para visualización cuando haya capas válidas, SMAP e IMERG mediante adaptadores opcionales con datos reales descargados.
Persistencia
Archivos JSON para catálogos y datos precalculados. SQLite opcional.
Flujo:
NASA POWER / SMAP / IMERG / NDVI
↓
Clientes Python de ingestión
↓
Normalización temporal, calidad y caché
↓
Indicadores climáticos
↓
Información agrícola de Cuatro Cañadas + datos del agricultor
↓
Generador y evaluador de rotaciones
↓
FastAPI REST
↓
RotaSat — interfaz web móvil
El navegador nunca consulta directamente NASA. Las credenciales NASA Earthdata, cuando correspondan, permanecen en el backend.
Separar la ingestión histórica, que se ejecuta anticipadamente, de la consulta interactiva: comparar rotaciones debe funcionar sobre datos ya procesados, sin esperar una descarga satelital.

4. Integración real con NASA
4.1 NASA POWER — integración obligatoria
Documentación:
https://power.larc.nasa.gov/docs/services/api/temporal/daily/
NASA POWER ofrece una API REST para datos meteorológicos y agroclimáticos. No necesita clave para las solicitudes públicas habituales.
Para el piloto usar como punto representativo de la localidad:
- Latitud: -17.2374
- Longitud: -62.2427
Son coordenadas de referencia de Cuatro Cañadas, no las de una finca real ni el centroide oficial del municipio.
El agricultor debe poder indicar sus propias coordenadas dentro del área piloto.
URL base:
https://power.larc.nasa.gov/api/temporal/daily/point
Parámetros iniciales:
T2M,T2M_MAX,T2M_MIN,PRECTOTCORR,RH2M,WS2M,ALLSKY_SFC_SW_DWN
Solicitar community=AG, format=JSON, coordenadas, fecha de inicio y fecha final. Probar GWETROOT separadamente si está disponible en el catálogo vigente; no asumir que siempre se devuelve.
Períodos:
- Histórico: 2015–2025, o mayor si resulta fácil.
- Reciente: desde enero de 2026 hasta la última fecha efectivamente disponible.
- Fecha de referencia del proyecto: 14 de noviembre de 2026.
La API no garantiza datos completos hasta el día actual. Detectar automáticamente la última observación válida.
Implementar pipeline/power.py con una función equivalente a:
fetch_power_daily(lat, lon, start, end)
Debe:
1. Ejecutar HTTP GET con requests y timeout.
2. Validar respuesta HTTP y estructura del JSON.
3. Leer properties.parameter.
4. Transformar las fechas YYYYMMDD en fechas ISO.
5. Alinear todas las variables por fecha.
6. Reemplazar códigos de dato faltante, como -999, por null.
7. Conservar unidades y metadatos.
8. Guardar resultados en caché.
9. Reintentar errores temporales de manera limitada.
10. Si NASA no responde, reutilizar la última descarga válida mostrando su fecha.
No llamar a NASA cada vez que un agricultor cambia sus prioridades.
4.2 Variables y decisiones
VariableIndicador derivadoUtilidadPRECTOTCORRLluvia acumulada, anomalías, rachas secasIdentificar déficit o exceso de lluviaT2M_MAXDías de calor intensoRiesgo térmicoT2M_MINDías fríos y umbrales térmicosCompatibilidad estacionalRH2MHumedad relativaContexto de demanda atmosféricaWS2MVientoEstimación de evapotranspiraciónALLSKY_SFC_SW_DWNRadiación solarEstimación de demanda hídricaGWETROOT, si existeHumedad relativa modelada en zona radicularContexto hídrico regional
No calcular ET0 con una fórmula incompleta ni presentar un índice simplificado como evapotranspiración física.
Si hay variables suficientes, implementar FAO Penman-Monteith siguiendo FAO-56; en caso contrario usar únicamente indicadores hídricos válidos y marcar la ET0 como no disponible.
Referencia:
https://www.fao.org/4/X0490E/x0490e0a.htm
4.3 SMAP — humedad del suelo
Producto: SMAP L4 SPL4SMGP, versión 8.
https://nsidc.org/data/spl4smgp/versions/8
Es una estimación regional que combina observaciones satelitales y un modelo de superficie terrestre, aproximadamente a 9 km de resolución y 3 horas de intervalo.
No describirla como un sensor enterrado en la finca.
Crear pipeline/smap.py, adaptador independiente.
Usar NASA Earthdata Login y la biblioteca earthaccess si el acceso está configurado.
Priorizar archivos o subconjuntos previamente descargados. No convertir una descarga extensa de HDF5 en requisito para arrancar la app.
La integración debe registrar la fecha, versión, cobertura geográfica y procedencia de cada observación.
4.4 GPM IMERG — precipitaciones
Referencia:
https://gpm.nasa.gov/data/imerg
Proporciona estimaciones de precipitación con mayor resolución espacial que POWER.
Crear pipeline/imerg.py.
Distinguir:
- Final Run: preferible para análisis histórico, con latencia.
- Early/Late Run: utilidad para observaciones recientes, con sus limitaciones.
No mezclar productos ni unidades sin normalizarlos. Las tasas de lluvia en mm/h deben convertirse adecuadamente a acumulados en mm.
MVP: si la descarga y extracción de IMERG retrasan la integración, calcular los indicadores de lluvia con NASA POWER y declarar la resolución regional.
4.5 Vegetación, relieve y temperatura superficial
Preparar adaptadores opcionales para:
- MODIS/VIIRS/HLS: NDVI y periodos de baja cobertura vegetal.
- SRTM: pendiente y relieve.
- ECOSTRESS: temperatura superficial y estrés térmico, solo si hay escenas válidas.
- NASA GIBS: teselas geoespaciales para el visor.
Documentación GIBS:
https://nasa-gibs.github.io/gibs-api-docs/access-basics/
NDVI bajo puede sugerir baja cobertura, pero por sí solo no demuestra que una finca haya estado en barbecho. Confirmar con calendario agrícola, resolución y calidad de observación.
4.6 Prioridades de integración
P0: POWER histórico real + actualización reciente + caché + indicadores.
P1: GIBS y una serie de vegetación real previamente descargada, si está disponible.
P2: IMERG y SMAP reales mediante archivos precalculados.
Fuera del camino crítico: ECOSTRESS, GRACE-FO, análisis espacial avanzado.
Usar SoilGrids únicamente con datos previamente obtenidos o un método de acceso funcional verificado. Su API REST ha presentado interrupciones; no hacer depender el MVP de ella.
Para cualquier fuente ausente, mostrar Sin datos disponibles. Nunca crear datos satelitales ficticios para completar visualizaciones.

5. Motor de recomendaciones
Implementar engine/recommendation.py.
No usar machine learning supervisado ni IA generativa.
En este MVP no disponemos de suficientes datos locales de rendimiento, costos y rotaciones históricas etiquetadas para entrenar y validar un predictor agrícola confiable.
El motor será interpretable y auditable.
5.1 Entradas del agricultor
- Ubicación de su parcela o punto de referencia.
- Cultivo actual y cultivo anterior.
- Rotación actual, incluyendo barbecho si corresponde.
- Cultivos que puede o no puede sembrar.
- Disponibilidad de riego.
- Problemas observados del suelo, si los conoce.
- Prioridades: cuidar agua, proteger suelo, reducir riesgos.
- Opcional: costos, precios o condiciones comerciales aportadas por el agricultor.
Permitir respuestas abiertas en lenguaje natural, por ejemplo:
"Estoy sembrando soya y antes tenía sorgo. Este año tengo poca agua y quiero evitar pérdidas."
El frontend ofrecerá sugerencias de cultivos y reconocimiento de nombres frecuentes como soya/soja, maíz/maiz.
Utilizar un parser de términos sencillo y verificable. Si una entrada no se reconoce, solicitar selección o corrección. No inferir información agronómica que el agricultor no proporcionó.
5.2 Generación de rotaciones
Crear cuatro estrategias, cuando las restricciones lo permitan:
- E0 Actual: la rotación proporcionada por el agricultor.
- E1 Cuidar suelo: prioriza cobertura, residuos y diversidad agronómica.
- E2 Cuidar agua: prioriza compatibilidad entre demanda hídrica y oferta estacional.
- E3 Equilibrada: busca compromiso entre agua, suelo y riesgo.
Trabajar con un horizonte comparable de 12 meses que contemple verano e invierno. Extender a 24 meses solo si resulta sencillo y hay datos suficientes.
Utilizar como candidatos principales soya, sorgo, maíz, girasol y trigo, más barbecho o cobertura solo si hay reglas y parámetros válidos.
Cada propuesta debe superar filtros de factibilidad: estación, agua, duración, restricciones del agricultor y parámetros conocidos.
No inventar alternativas para cumplir artificialmente la cuota de tres. Si hay menos de tres opciones defendibles, explicar por qué.
5.3 Indicadores matemáticos
Implementar, como mínimo:
Racha seca máxima
Número máximo de días consecutivos donde la precipitación diaria es menor a un umbral configurable, inicialmente 1 mm/día.
Anomalía de lluvia
anomalia_pct = 100 * (P_actual - P_media_historica) / P_media_historica
Usar períodos estacionales equivalentes. Manejar explícitamente el caso de media igual a cero.
Exposición a calor
dias_calor = sum(Tmax_dia > umbral_cultivo)
El umbral debe provenir de una fuente para el cultivo y la etapa de desarrollo. Si falta, usar un indicador meteorológico general claramente etiquetado, sin afirmar daño específico al cultivo.
Balance hídrico simplificado
balance_mm = lluvia_efectiva_mm - demanda_cultivo_mm
Solo calcularlo cuando existan datos suficientes para estimar la lluvia efectiva y la demanda. No confundir precipitación total con precipitación efectiva.
Exposición a lluvias intensas
Contar días o períodos donde la lluvia supera un percentil histórico definido y calculado para una serie comparable.
Es un indicador de exposición meteorológica, no una predicción directa de inundación fluvial.
5.4 Puntuación multicriterio
Construir tres puntuaciones normalizadas:
- S_agua: 0–100
- S_suelo: 0–100
- S_riesgo: 0–100
La fórmula general es:
Score(R) = w_agua*S_agua + w_suelo*S_suelo + w_riesgo*S_riesgo
Con:
w_agua + w_suelo + w_riesgo = 1
Pesos iniciales de demostración:
- Agua: 0.40
- Suelo: 0.35
- Riesgo: 0.25
Son preferencias iniciales de producto, no pesos científicamente validados.
El agricultor podrá modificarlos en la interfaz.
Documentar la fórmula exacta utilizada para obtener cada subpuntuación, sus unidades, normalización, supuestos y límites.
Si un criterio esencial carece de datos, no completar silenciosamente el valor. Aplicar una política explícita de exclusión o resultado insuficiente y recalcular los pesos válidos cuando proceda.
La economía se muestra como criterio cualitativo hasta contar con datos aportados por el agricultor. No estimar ganancias ni precios sin fuentes.
5.5 Reglas agronómicas
Entre las reglas candidatas:
1. Evitar sucesiones incompatibles con la estación o disponibilidad de agua.
2. Penalizar exposición elevada a déficit hídrico durante etapas críticas.
3. Valorar cobertura y residuos cuando las condiciones permitan mantenerlos.
4. Considerar diversidad de familias y cultivos.
5. No asumir que sembrar una cobertura siempre mejora el resultado: puede consumir agua.
6. No conceder automáticamente un crédito fijo de nitrógeno después de soya.
7. No asumir que cambiar la rotación elimina el riesgo de inundación.
Cada regla debe conservar una explicación y su fundamento.
Ejemplo de salida:
"Esta estrategia recibe una mejor evaluación en conservación del suelo porque incorpora mayor permanencia de residuos. Sin embargo, su demanda de agua necesita verificación para el invierno."
La explicación debe generarse a partir de indicadores y reglas reales, no de texto aleatorio.
5.6 Incertidumbre
Implementar una versión sencilla y reproducible de bootstrap por años climáticos.
- Utilizar los años históricos disponibles.
- Remuestrear años completos, no días independientes.
- Realizar 200 iteraciones inicialmente.
- Recalcular las puntuaciones por estrategia.
- Obtener mediana y percentiles P10/P90.
- Registrar estabilidad del ordenamiento de estrategias.
- Utilizar semilla fija para reproducibilidad.
Presentar P10–P90 como rango de variabilidad del modelo bajo remuestreo histórico, no como garantía de rendimiento ni pronóstico meteorológico.
Mostrar incertidumbre adicional cuando faltan análisis de suelo o parámetros locales.

6. Diseño de la aplicación: agricultores primero
Nombre: RotaSat
Lema principal:
"Decidí qué sembrar después, con información sobre el clima y tu tierra."
El público objetivo incluye agricultores acostumbrados a trabajar con experiencia de campo, no necesariamente con aplicaciones analíticas.
El diseño debe ser mobile-first, con botones grandes, texto legible, contraste alto, español sencillo y navegación con una mano.
6.1 Pantalla principal
Esta pantalla no debe parecer un panel de ingeniería.
Debe responder inmediatamente:
¿Qué conviene revisar antes de mi próxima siembra?
Orden de elementos:
1. Encabezado "Mi campo — Cuatro Cañadas".
2. Tarjeta principal de recomendación con acción concreta.
3. Advertencias relevantes, con fecha de los datos.
4. Botón grande "Comparar opciones de siembra".
5. Accesos a los cuatro módulos.
6. Acceso secundario "Ver datos de mi zona".
Ejemplos de mensajes de interfaz, sujetos a resultados reales:
- "Conviene revisar una alternativa con menor demanda de agua."
- "Las lluvias de este período están por debajo de lo habitual."
- "Falta información sobre tu suelo para recomendar con mayor seguridad."
- "Revisá con tu técnico si esta opción se adapta a tu próxima campaña."
Nunca mostrar como alerta actual un ejemplo no calculado.
Usar semáforos de tres niveles, pero acompañarlos siempre con texto. No depender solo del color.
Mostrar "Datos actualizados al: [fecha real]" y distinguir histórico de reciente.
6.2 Entrada de datos
Diseñar un pequeño flujo conversacional:
Pregunta 1: "¿Qué sembrás actualmente?"
Campo abierto y chips de sugerencias: Soya, Maíz, Sorgo, Girasol, Trigo, Otro.
Pregunta 2: "¿Qué sembraste antes?"
Campo abierto, con posibilidad de indicar barbecho.
Pregunta 3: "¿Qué te preocupa más?"
Opciones grandes: Falta de agua, Cuidar suelo, Evitar pérdidas, Quiero equilibrar.
Pregunta 4: "¿Tenés riego?"
Sí / No / A veces / No sé.
Pregunta 5: "¿Querés contar algo de tu terreno?"
Área de texto libre, opcional.
No exigir laboratorio, GPS exacto, documentos ni conocimientos técnicos. La ubicación predeterminada será el punto piloto de Cuatro Cañadas y se podrá cambiar.
Las respuestas pueden modificarse antes y después de generar resultados.
6.3 Los cuatro módulos obligatorios
Los cuatro módulos deben existir y ser funcionales.
Module 01 — Compare Rotations
Título visible: Comparar formas de sembrar
Mostrar la rotación actual y hasta tres estrategias candidatas.
Cada tarjeta incluye:
- Secuencia de cultivos.
- Evaluación relativa en barras horizontales.
- Ventaja más importante.
- Desventaja más importante.
- Riesgo principal.
- Botón "¿Por qué esta opción?"
Una barra debe explicar claramente qué significa su puntuación. Evitar que el agricultor interprete 80/100 como 80% de probabilidad de éxito.
Module 02 — Reveal Tradeoffs
Título visible: Ventajas y desventajas
Al entrar, mostrar primero frases sencillas y comparaciones comprensibles.
Por ejemplo:
"Protege más el suelo, pero puede necesitar más agua."
En una sección expandible "Ver comparación técnica", incluir un gráfico radar con agua, suelo y riesgo, además de criterios adicionales solo cuando existan datos suficientes.
No poner el gráfico como primer elemento de la pantalla.
Module 03 — Understand Risk
Título visible: Conocer los riesgos
Mostrar riesgos por temporada o período:
- Sequía.
- Calor.
- Exceso de lluvia.
- Baja cobertura del suelo, cuando exista evidencia.
Incluir una cuadrícula tipo heatmap de meses por años históricos o por períodos, coloreada según un indicador calculado.
No fabricar mapas espaciales de alta resolución a partir de una sola celda regional NASA POWER.
Permitir tocar cada celda para explicar período, valor, unidad y fuente.
Module 04 — Make Uncertainty Visible
Título visible: ¿Qué tan segura es esta comparación?
Mostrar:
- Rango P10–P90 de cada estrategia.
- Qué información proviene de NASA.
- Qué información proporcionó el agricultor.
- Qué datos faltan.
- Cuáles son los supuestos más importantes.
- Qué debería confirmar un agrónomo.
Usar una barra sencilla de rango o intervalo, acompañada de lenguaje cotidiano.
No llamar "confianza alta" a una recomendación basada solamente en datos regionales y parámetros genéricos sin aclarar sus limitaciones.

7. Visor geoespacial multicapa
Crear una segunda vista accesible mediante "Explorar mi zona".
Diseño: Mission Terminal geoespacial, con cinco capas visuales apiladas en perspectiva isométrica.
Capas exactas:
1. Lluvia
2. Humedad del suelo
3. Calor
4. Vegetación
5. Agua
Las capas deben poder seleccionarse individualmente y ofrecer información comprensible.
Utilizar Leaflet para el mapa 2D principal y CSS transforms para la representación isométrica apilada. No implementar un motor 3D.
Origen esperado:
- Lluvia: POWER y, opcionalmente, IMERG.
- Humedad: SMAP, si se dispone de datos válidos.
- Calor: POWER como contexto regional o ECOSTRESS cuando existan escenas.
- Vegetación: MODIS/VIIRS/HLS.
- Agua: balance hídrico derivado; no presentarlo como un mapa de inundación observada.
Restricción: si una capa no tiene un raster georreferenciado válido, mostrar estado "Información regional" o "Datos no disponibles". No generar texturas o cuadrículas numéricas que simulen mediciones satelitales inexistentes.
Cada capa incluirá:
- Fuente real.
- Fecha.
- Escala espacial.
- Leyenda.
- Interpretación agrícola sencilla.
- Botón "¿Qué significa?"
La representación isométrica será principalmente un elemento de navegación y comprensión. No debe comprometer la exactitud de los datos.

8. Diseño de la API FastAPI
Implementar al menos:
GET /api/health
Verifica el servidor.
GET /api/context/cuatro-canadas
Devuelve los cultivos documentados, riesgos históricos, prácticas agronómicas, referencias y sus años de origen.
GET /api/nasa/status
Devuelve disponibilidad por fuente, última descarga exitosa, última fecha con observaciones, versión de productos y uso de caché.
GET /api/indicators
Recibe latitud, longitud y período cuando corresponda. Devuelve indicadores calculados, unidades, procedencia y calidad.
POST /api/rotations/compare
Recibe información de la parcela y prioridades. Calcula estrategias, puntuaciones, incertidumbre y explicaciones.
Ejemplo conceptual de request:
{"site":{"lat":-17.2374,"lon":-62.2427},"current_rotation":["soy","sorghum"],"priorities":{"water":0.4,"soil":0.35,"risk":0.25},"constraints":{"irrigation":"rainfed","excluded_crops":[]},"notes":"Mi terreno suele secarse en invierno"}
La respuesta debe incluir:
- site
- data_status
- alerts
- strategies
- recommended_strategy_id
- limitations
Cada estrategia contendrá:
- id
- name
- sequence
- score
- criteria
- uncertainty
- tradeoffs
- reasons
- evidence
- missing_information
- agronomist_questions
GET /api/map/layers
Devuelve las capas disponibles y sus metadatos. No ofrecer una URL inventada de teselas NASA.
GET /api/sources
Devuelve bibliografía y catálogo de fuentes.
Utilizar esquemas Pydantic, CORS configurado para el frontend, respuestas JSON consistentes y OpenAPI disponible en /docs.

9. Estructura de archivos
Organizar el repositorio así:
rotasat/
- backend/app/main.py
- backend/app/schemas.py
- backend/pipeline/power.py
- backend/pipeline/smap.py
- backend/pipeline/imerg.py
- backend/analytics/indicators.py
- backend/analytics/uncertainty.py
- backend/engine/recommendation.py
- backend/engine/rules.py
- backend/reference/crops.json
- backend/reference/rotation_rules.json
- backend/reference/agricultural_context.json
- backend/data/cache/
- backend/tests/
- backend/requirements.txt
- frontend/src/pages/Home.tsx
- frontend/src/pages/Compare.tsx
- frontend/src/pages/Tradeoffs.tsx
- frontend/src/pages/Risks.tsx
- frontend/src/pages/Uncertainty.tsx
- frontend/src/pages/GeoViewer.tsx
- frontend/src/components/
- frontend/src/services/api.ts
- frontend/package.json
- README.md
- .env.example
No sobrearquitectar componentes o carpetas si no son necesarios.

10. Pruebas y criterios de aceptación
Debes incluir pruebas que demuestren funcionamiento real.
Prueba A: conexión NASA
Al ejecutar la ingestión, la aplicación obtiene valores meteorológicos reales del punto piloto, conserva fecha/unidades/fuente y los almacena en caché.
Debe ser posible inspeccionar el JSON recibido y los indicadores generados.
Prueba B: recomendación
Un agricultor introduce su rotación actual y prioridades.
El motor evalúa la opción actual y al menos dos alternativas factibles, muestra puntuaciones y explica sus diferencias.
Prueba C: prioridades
Al incrementar la prioridad de conservar agua, se recalculan los pesos y los puntajes de forma reproducible.
Demostrar que puede cambiar el orden de estrategias si los datos y el modelo justifican ese cambio, sin forzar artificialmente un ganador distinto.
Prueba D: información incompleta
Cuando falten datos de suelo, NDVI o SMAP, la app continúa funcionando con los indicadores disponibles, muestra los vacíos y reduce el alcance de sus afirmaciones.
Prueba E: incertidumbre
La aplicación muestra rangos de puntuación producidos por el bootstrap y permite comprender la diferencia entre dos estrategias.
Prueba F: interfaz móvil
Un usuario puede introducir datos y llegar a la recomendación en menos de tres minutos usando un teléfono, sin conocimientos de ingeniería.
Prueba G: trazabilidad
Todo resultado relevante debe tener una fuente, una fecha, una unidad y una explicación verificable.
No afirmar que NASA recomienda directamente un cultivo: NASA proporciona datos, RotaSat interpreta esos datos mediante su propio motor.

11. Plan de implementación de 12 horas
Horas 0–2: infraestructura y conexión real
- Crear proyecto backend y frontend.
- Implementar API FastAPI básica.
- Conectar NASA POWER.
- Descargar datos históricos.
- Crear caché.
- Confirmar una respuesta JSON real.
Horas 2–4: conocimiento agrícola y cálculo
- Crear tablas agrícolas locales.
- Implementar indicadores meteorológicos.
- Crear reglas agronómicas.
- Crear primeras pruebas.
Horas 4–6: motor de recomendación
- Generar estrategias.
- Calcular puntuaciones.
- Incorporar prioridades del agricultor.
- Devolver evidencia y limitaciones.
- Conectar POST /api/rotations/compare.
Horas 6–8: interfaz principal
- Entrada sencilla de información.
- Pantalla de recomendación.
- Módulo de comparación.
- Módulo de tradeoffs.
- Módulo de riesgos.
Horas 8–10: incertidumbre y geovisor
- Bootstrap.
- Cuarto módulo.
- Visor multicapa con datos disponibles.
- Integrar una segunda fuente NASA solo si está lista y es verificable.
Horas 10–12: validación y entrega
- Pruebas de extremo a extremo.
- Corregir errores.
- Optimizar interfaz móvil.
- Preparar demo reproducible.
- README con fuentes, limitaciones e instrucciones de ejecución.

12. Cómo debes trabajar conmigo
No respondas con una descripción abstracta de arquitectura.
Empieza a implementar inmediatamente.
En tu primera respuesta debes entregar:
1. Árbol definitivo de archivos del MVP.
2. Comandos exactos para crear y ejecutar backend/frontend en Windows PowerShell.
3. Código completo de main.py, schemas.py y pipeline/power.py.
4. Archivos de referencia agrícola con los datos documentados y sus fuentes.
5. Una prueba real de conexión y lectura de NASA POWER, o instrucciones ejecutables para realizarla si no tienes acceso a internet.
6. Primer endpoint funcional de indicadores.
7. El siguiente conjunto de archivos que debes implementar para conectar el motor y el frontend.
Después continúa con el motor y la interfaz sin rediseñar la arquitectura.
Cuando entregues código, indica siempre ruta exacta del archivo y contenido completo, listo para copiar. No omitas implementaciones con TODO, ... o pseudocódigo en funcionalidades P0.
Si tienes acceso a edición de archivos y terminal, crea los archivos y ejecútalos directamente.
Si una dependencia externa requiere credenciales o no está disponible, proporciona el procedimiento preciso y conserva un camino funcional con datos NASA reales previamente almacenados.
Prioridad máxima: conseguir un recorrido extremo a extremo real: NASA → Python → indicadores → recomendación → aplicación.
El proyecto no se considera terminado por tener una interfaz bonita. Se considera terminado cuando un agricultor cambia sus condiciones y prioridades, recibe una comparación recalculada, puede entender por qué se produjo y puede comprobar de dónde salieron los datos.