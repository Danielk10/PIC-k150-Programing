# Notas de Lanzamiento - PIC-k150-Programing v2.8.5 (versionCode 52)

Fecha de lanzamiento / Release Date: 6 de Octubre de 2026 / October 6, 2026

---

## 🌐 Notas para Google Play Console (Bilingüe)

### Español (`es-419` / `es-ES`)
- Corrección de cierre inesperado en dispositivos Huawei y terminales sin Google Play Services completos.
- Actualización de seguridad y optimización de librerías del sistema.
- Mayor estabilidad y rendimiento general en la lectura y programación de microcontroladores.

### English (`en-US`)
- Fixed unexpected crash on Huawei devices and models without full Google Play Services.
- Security updates and system library optimizations.
- Improved overall stability and performance during microcontroller reading and programming.

---

## 🛠️ Detalle Técnico de Cambios

1. **Resolución de Crash en Crashlytics (`SignInHubActivity.onCreate`)**:
   - Se removió la dependencia innecesaria `firebase-auth` tanto de `app/build.gradle` como de `gradle/libs.versions.toml`.
   - Al retirar esta dependencia se eliminó del paquete final `com.google.android.gms:play-services-auth` (y su actividad interna `SignInHubActivity`), la cual provocaba un `NullPointerException` al inicializarse sin los extras esperados en dispositivos Huawei (`HRY-LX1T` / Honor 10 Lite) y equipos con entornos GMS incompletos.
   - Reducción del tamaño del binario y eliminación de código muerto.

2. **Actualización de Dependencias a Últimas Versiones Estables**:
   - `com.google.firebase:firebase-bom`: `34.14.0` ➔ `34.19.0`
   - `com.google.firebase:firebase-crashlytics`: `20.0.6` ➔ `20.1.1`
   - Plugin Crashlytics: `3.0.7` ➔ `3.0.8`
   - Plugin Google Services: `4.4.4` ➔ `4.5.0`
   - `androidx.appcompat:appcompat`: `1.7.1` ➔ `1.8.0`
   - `androidx.constraintlayout:constraintlayout`: `2.2.1` ➔ `2.2.2`
   - `org.mockito:mockito-core`: `5.23.0` ➔ `5.24.0`

3. **Optimización de Red en Entorno Gradle (`gradle.properties`)**:
   - Habilitación de `systemProp.java.net.preferIPv4Stack=true` y `-Djava.net.preferIPv4Stack=true` para garantizar resolución y descarga inmediata de artefactos desde los repositorios de Google Maven sin retrasos ni fallos por IPv6.

4. **Validación Integral de Integración**:
   - Ejecución exitosa de toda la suite de pruebas unitarias y de integración contra el emulador virtual K150 en C++ (`ProtocoloP18AIntegrationTest`), validando la lectura, borrado, grabación de ROM/EEPROM/Fuses/IDs y exportación HEX en familias PIC12, PIC16 y PIC18.
