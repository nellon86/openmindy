# Publicar un APK de OpenMindy

Los APK se publican como assets de una release de GitHub, no se guardan en el repositorio.

## Configuración inicial de la firma

1. Crea una clave de firma propia y guárdala en `.signing/` (Git la ignora):

   ```shell
   mkdir -p .signing
   keytool -genkeypair -v -keystore .signing/openmindy-release.jks \
     -alias openmindy -keyalg RSA -keysize 4096 -validity 10000
   ```

2. Crea `.signing/release.properties`:

   ```properties
   storeFile=.signing/openmindy-release.jks
   storePassword=...
   keyAlias=openmindy
   keyPassword=...
   ```

3. Haz una copia de seguridad de los dos archivos en un gestor de contraseñas. Sin esta clave no podrás actualizar las instalaciones existentes.

4. Inicia sesión en GitHub CLI como `nellon86` y sube los secrets del workflow:

   ```shell
   tools/configure_github_release_secrets.sh
   ```

   El script crea `ANDROID_SIGNING_KEY_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS` y `ANDROID_KEY_PASSWORD` en `nellon86/openmindy`.

## Publicar

1. Actualiza `versionCode` y `versionName` en `app/build.gradle.kts`, y también `RELEASE_NOTES.md`.
2. Crea y sube el tag de la versión, por ejemplo `git tag v1.0.0 && git push origin v1.0.0`.
3. El workflow `Publish OpenMindy APK` ejecuta los tests, compila la release firmada y publica `OpenMindy.apk` junto con su suma SHA-256.

El comprobador de actualizaciones de la app lee `https://api.github.com/repos/nellon86/openmindy/releases/latest`, así que el tag debe seguir el formato `vX.Y.Z`.

## Compilación local firmada

Con `.signing/release.properties` en su sitio, basta con ejecutar:

```shell
./gradlew assembleRelease
```
