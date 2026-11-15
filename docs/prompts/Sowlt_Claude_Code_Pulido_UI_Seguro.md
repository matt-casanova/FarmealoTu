# Sowlt — Pulido visual seguro de la UI

## Contexto

Actúa como diseñador UI/UX senior especializado en Android, Kotlin y Jetpack Compose.

**Sowlt ya está completamente funcional y conectado al backend.** El proyecto está terminado a nivel funcional; solo queremos hacer **ajustes visuales pequeños y puntuales**, siguiendo exclusivamente las indicaciones de Kendra.

La rama de trabajo para estos cambios es `ui_pulido_final`, creada a partir de `master`. **No trabajes directamente en `master`.** Antes de hacer cambios, verifica la rama con `git branch --show-current` y revisa `git status`. Si estás en otra rama, no modifiques nada y avisa.

## Reglas absolutas

1. **NO modificar funcionalidades ni comportamientos existentes.**
2. **NO tocar** backend, endpoints, red, Retrofit, Repository, ViewModel, DTO, modelos de datos, estado, persistencia, navegación, lógica de negocio ni flujo de información.
3. **NO** añadir dependencias, cambiar la arquitectura ni refactorizar código que funciona.
4. **NO** modificar textos, botones, pantallas o interacciones que Kendra no haya indicado expresamente.
5. Modificar **solo propiedades de presentación** dentro de los componentes UI existentes: colores, espaciados, tamaños, tipografía, alineación y detalles decorativos que Kendra solicite.
6. Hacer cambios **mínimos y localizados**. Conservar las firmas y callbacks de los composables, la estructura de navegación y todos los estados de la app.
7. No modificar archivos fuera de `app/src/main/java/com/example/farmealotu/ui/`, salvo autorización explícita. Dentro de `ui/`, no alterar lógica ni comportamiento.
8. **NO realizar merge, commit, push ni cambiar de rama automáticamente.**
9. Si un ajuste solicitado requiere tocar funcionalidad o archivos protegidos, **no implementarlo**; explica la limitación y propón una alternativa puramente visual.
10. Si no hay indicaciones concretas de Kendra, **no cambies ningún archivo todavía**.

## Procedimiento

1. Leer las indicaciones exactas de Kendra antes de editar.
2. Identificar los archivos y las líneas de UI pertinentes.
3. Implementar exclusivamente los cambios visuales solicitados, uno por uno.
4. Revisar el diff para detectar cualquier cambio ajeno a la presentación.
5. Ejecutar, si el entorno lo permite:

   ```powershell
   .\gradlew.bat assembleDebug
   .\gradlew.bat testDebugUnitTest
   ```

6. Comparar los archivos cambiados respecto de la base:

   ```powershell
   git diff --name-status master
   git diff master -- app/src/main/java/com/example/farmealotu/ui/
   ```

7. Informar los archivos modificados, los cambios visuales precisos y los resultados reales de las pruebas. Si no pudiste ejecutar alguna verificación, indícalo; **no asegures que todo funciona sin haberlo comprobado**.

## Criterio de éxito

La aplicación mantiene **exactamente las mismas funcionalidades y comportamiento**, pero incorpora únicamente los retoques visuales autorizados por Kendra.

**Prioridad absoluta: CERO regresiones funcionales, CERO cambios no solicitados y CERO alteraciones del backend.**

**Primera acción:** verificar la rama y esperar las indicaciones visuales concretas de Kendra si aún no las has recibido.
