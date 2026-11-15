package com.example.farmealotu.ui.components

/** Textos de «¿Qué significa?» (glosario del plan, Anexo B, y límites de la sección 11.3). */
data class GlossaryEntry(
    val title: String,
    val body: String,
    val limitation: String? = null,
)

object Glossary {
    val relativeScore = GlossaryEntry(
        title = "Puntaje relativo",
        body = "Es una nota de 0 a 100 que sirve para comparar las opciones entre sí. La calcula FarmealoTu con reglas y con las prioridades que elegiste.",
        limitation = "No es una probabilidad de éxito, ni un rendimiento esperado, ni una ganancia.",
    )
    val water = GlossaryEntry(
        title = "Agua",
        body = "Qué parte del agua que piden los cultivos de esa opción alcanza a cubrir la lluvia efectiva del período.",
        limitation = "Usa lluvia regional (~50 km) y valores generales de cada cultivo. No mide tu campo.",
    )
    val soil = GlossaryEntry(
        title = "Suelo",
        body = "Cuánto protege el suelo esa opción: meses con el suelo cubierto, rastrojo que deja y variedad de cultivos.",
        limitation = "No reemplaza un análisis de suelo. Parte de los valores son supuestos del modelo.",
    )
    val risk = GlossaryEntry(
        title = "Riesgo",
        body = "Un valor alto indica que, en los años pasados, esa opción estuvo menos expuesta a rachas secas, calor y lluvias fuertes en sus etapas sensibles.",
        limitation = "Mira el pasado: no pronostica el clima ni las inundaciones.",
    )
    val range = GlossaryEntry(
        title = "Rango P10–P90",
        body = "Se repite el cálculo muchas veces combinando años pasados. El rango muestra entre qué valores cae la evaluación la mayoría de las veces; el punto es el valor del medio.",
        limitation = "Estos rangos muestran cómo varía la evaluación al reutilizar distintos años del historial; no predicen el rendimiento.",
    )
    val rankStability = GlossaryEntry(
        title = "Estabilidad del orden",
        body = "De todas las repeticiones del cálculo, en qué porcentaje esa opción quedó primera.",
        limitation = "Un porcentaje bajo quiere decir que el orden cambia fácil según el año que toque.",
    )
    val confidence = GlossaryEntry(
        title = "Confianza",
        body = "Qué tan firme es la comparación con los datos que hay. Con datos regionales y valores generales nunca se rotula como alta.",
        limitation = "Baja: falta información de suelo, falta un criterio o el orden es inestable.",
    )
    val drySpell = GlossaryEntry(
        title = "Racha seca",
        body = "Días seguidos casi sin lluvia (menos de 1 mm).",
        limitation = "Dato regional (~50 km): es el contexto de la zona, no una medición de tu campo.",
    )
    val heavyRain = GlossaryEntry(
        title = "Lluvia intensa",
        body = "Un día de lluvia «muy fuerte»: solo el 5 % de los días lluviosos lo superan.",
        limitation = "Estar expuesto a lluvia intensa no equivale a predecir un desborde del río.",
    )
    val heat = GlossaryEntry(
        title = "Días de calor",
        body = "Días en que la temperatura máxima pasó un umbral. Si no hay umbral propio del cultivo se usa uno general del mes.",
        limitation = "Con umbral general no se afirma daño al cultivo.",
    )
    val rainAnomaly = GlossaryEntry(
        title = "Anomalía de lluvia",
        body = "Cuánto llovió más o menos que lo habitual en el mismo período, en porcentaje.",
    )
    val ndvi = GlossaryEntry(
        title = "Vegetación (NDVI)",
        body = "Índice de «verdor» visto desde el satélite. Un valor bajo puede indicar poca cobertura.",
        limitation = "NDVI bajo no prueba que el campo esté en barbecho.",
    )
    val soilMoistureSmap = GlossaryEntry(
        title = "Humedad del suelo (SMAP)",
        body = "Estimación del agua en el suelo que combina satélite y modelo, para áreas de unos 9 km.",
        limitation = "No es un sensor enterrado en tu finca.",
    )
    val waterBalance = GlossaryEntry(
        title = "Balance hídrico",
        body = "Lluvia efectiva menos lo que pide el cultivo: si es negativo, falta agua.",
        limitation = "Es un cálculo. No es un mapa de inundación observada.",
    )
    val regional = GlossaryEntry(
        title = "Dato regional (NASA POWER)",
        body = "Combinación de observaciones y modelos que da datos diarios continuos para áreas de unos 50 km.",
        limitation = "Es el contexto de la zona; no se puede atribuir a una parcela exacta.",
    )
    val vwc = GlossaryEntry(
        title = "Humedad volumétrica",
        body = "Qué parte del volumen del suelo es agua. El nodo la manda en m³/m³ y acá se muestra en % (se multiplica por 100).",
        limitation = "Solo vale si el sensor fue calibrado; cada sensor barato se calibra por separado.",
    )
    val moistureIndex = GlossaryEntry(
        title = "Índice de humedad del sensor",
        body = "El índice compara la lectura con el sensor seco (0) y en agua (100). Sirve para ver si el suelo se moja o se seca; no es el porcentaje real de agua del suelo.",
        limitation = "Un suelo saturado tiene alrededor de 40–50 % de agua, no 100 %. Para el % real hace falta calibrar el sensor con muestras de suelo.",
    )
    val lightIndex = GlossaryEntry(
        title = "Luz (índice relativo)",
        body = "Valor de 0 a 100 que el nodo calcula con su sensor de luz (LDR). Sirve para comparar momentos: si hay más o menos luz que antes.",
        limitation = "No son lux ni radiación solar, y no se puede comparar con otro sensor.",
    )
    val satelliteVsGround = GlossaryEntry(
        title = "Satélite y sensor en tierra",
        body = "El satélite da el contexto de una zona grande y muchos años de historia. El sensor da la medición de un punto. Un campo no es uniforme, y un píxel de 9 a 50 km lo promedia todo.",
    )

    fun forCriterion(key: com.example.farmealotu.domain.CriterionKey): GlossaryEntry = when (key) {
        com.example.farmealotu.domain.CriterionKey.WATER -> water
        com.example.farmealotu.domain.CriterionKey.SOIL -> soil
        com.example.farmealotu.domain.CriterionKey.RISK -> risk
    }

    /** Explicación general para un indicador de la API, según su id. */
    fun forIndicatorId(id: String): GlossaryEntry? {
        val key = id.lowercase()
        return when {
            "ndvi" in key || "cover" in key || "cobertura" in key -> ndvi
            "dry" in key || "cdd" in key || "seca" in key -> drySpell
            "anomal" in key -> rainAnomaly
            "heavy" in key || "p95" in key || "intens" in key || "extreme" in key -> heavyRain
            "hot" in key || "heat" in key || "calor" in key -> heat
            "balance" in key -> waterBalance
            else -> null
        }
    }
}

/** Limitaciones que la app declara siempre (plan 11.3). */
val FIXED_LIMITATIONS = listOf(
    "NASA POWER es una celda de unos 50 km: es contexto regional, no una medición de tu finca.",
    "Los puntajes dependen de reglas y pesos. Son relativos entre opciones, no probabilidades.",
    "P10–P90 es la variabilidad al reutilizar años pasados; no es un pronóstico ni una garantía.",
    "No se predicen rendimientos ni precios, y no se reemplaza el análisis de suelo ni la visita técnica.",
    "Cambiar la rotación no elimina el riesgo de inundación.",
    "Los datos del nodo de campo son de un prototipo y no cambian la recomendación en esta versión.",
)

const val NASA_ROLE = "NASA aporta datos; FarmealoTu los interpreta. No es un producto oficial de NASA."
