# OpenMindy

Launcher para Android pensado para usarse con teclado, hecho en Kotlin y Jetpack Compose.

OpenMindy es un fork personal de [MinkLauncher OpenSource](https://github.com/katoaapps/openminilaunch) 3.2.2 (Apache 2.0). No está afiliado a Katoa Apps ni cuenta con su respaldo.

Versión actual: **1.0.0** · identificador de la app: `io.github.nellon86.openmindy`

## Diferencias con MinkLauncher OpenSource

- **Magic Box: primero las apps.** Si escribes sin ningún atajo, busca apps instaladas y sus accesos directos; Intro abre la primera coincidencia. Si no coincide ninguna app, Intro envía el texto al navegador.
- **`?` busca en la web.** `?` seguido de texto lo abre en el navegador predeterminado o en el que elijas; si el texto es una dirección web, la abre directamente. Sigue disponible el botón de IA.
- **Sin búsqueda de archivos** en la Magic Box (está desactivada con `MAGIC_FILE_SEARCH_ENABLED` en `features/magic/MagicBoxInput.kt`).
- **MindySpace con tres widgets rápidos.** Recuerdos (con Adrift) y la tarjeta de contacto virtual se han eliminado; en su lugar hay Widget rápido 1, 2 y 3, cada uno con su propio widget.
- **Sin MinkPad.** Se ha eliminado el modo de teclado, trackpad y mando Bluetooth, junto con sus permisos de Bluetooth y de servicio en primer plano.
- **Marca e identidad propias.** OpenMindy se instala como una app independiente y puede convivir con MinkLauncher. Busca actualizaciones en `nellon86/openmindy` y no incluye la beta de Google Play, F-Droid, el correo de soporte ni la comunidad del proyecto original.
- **Todo se llama Mindy.** MindySpace, Mindy's Day, Mindy Assistant y el resto de textos, clases y recursos usan el nuevo nombre. Solo las claves internas de almacenamiento y de copia de seguridad conservan `mink` para no perder datos ni compatibilidad.
- **Copias de seguridad compatibles.** Se pueden importar copias JSON de MinkLauncher OpenSource; sus ajustes de MinkPad simplemente se ignoran.

## Atajos de la Magic Box

| Tecla | Acción |
| --- | --- |
| (ninguna) | Buscar y abrir apps |
| `?` | Buscar en la web |
| `@` | Mensaje a un contacto o chat reciente |
| `#` | Llamar |
| `-` | Añadir una tarea |
| `/` | Nota |
| `+` | Evento de calendario (el parser local entiende español) |
| `*` | Enviar a la app de IA elegida |

En un móvil con teclado físico, pulsar cualquier tecla en Inicio abre la Magic Box con ese carácter ya escrito.

## Funciones que se mantienen

Inicio con MindySpace (tareas, calculadora, música y tres widgets rápidos), App Grid con carpetas, Quick Drawer, See All, Work Apps y Privacy Space, Conversaciones, Mindy's Day, página de widgets, temas e icon packs, Mindy Assistant, copias de seguridad y 15 idiomas. Los textos que explican `?` solo están actualizados en inglés y español.

## Compilar

```shell
python3 tools/validate_localizations.py
./gradlew testDebugUnitTest lintDebug assembleDebug
```

El APK se genera en `app/build/outputs/apk/debug/app-debug.apk`. Después de instalarlo, pulsa Inicio y elige **OpenMindy** como app de inicio.

## Privacidad

OpenMindy no tiene cuentas, anuncios, analíticas ni servidor propio. La única conexión a internet es la comprobación opcional de versiones en GitHub. Ver [PRIVACY.md](PRIVACY.md).

## Licencia

Código y arte bajo [Apache License 2.0](LICENSE). Ver [NOTICE](NOTICE) para la atribución al proyecto original y la descripción de los cambios. La licencia no concede el uso del nombre ni de la marca MinkLauncher.
