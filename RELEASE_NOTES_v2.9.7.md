# 🚀 Notas de Lanzamiento / Release Notes - v2.9.7 (VersionCode 61)

Fecha de lanzamiento / Release Date: 4 de Noviembre de 2026 / November 4, 2026

---

## 🇪🇸 Español

### 📌 Resumen General
Esta versión v2.9.7 resuelve de forma crítica el fallo `IllegalArgumentException: Read buffer too small` en dispositivos y programadores con convertidores USB-Serial FTDI (o clones FTDI), blindando la capa de lectura física de paquetes USB y garantizando la integridad de datos seriales en todas las plataformas.

### 🛠️ Correcciones Principales

#### 1. Blindaje de Búfer USB contra Drivers FTDI (Crashlytics `98da621ca29a1c7804af7c57e98e2d81`)
• **Causa raíz:** A diferencia de chips Prolific o CH340, los convertidores FTDI (`FtdiSerialDriver`) anteponen obligatoriamente 2 bytes de cabecera de estado del módem/línea (`READ_HEADER_LENGTH = 2`) en cada paquete USB. Si la aplicación intentaba leer 1 o 2 bytes pasando un búfer de tamaño menor o igual a 2 (como ocurría en `leerVectorDeDepuracionDelPic` o `resetearComandos`), el driver lanzaba `IllegalArgumentException: Read buffer too small` provocando el crash de la app.
• **Corrección:**
  - Se fijó `MIN_READ_BUFFER_SIZE = 3` en [`Protocolo.java`](app/src/main/java/com/diamon/nucleo/Protocolo.java), garantizando que jamás se pase al driver USB un búfer inferior a 3 bytes.
  - Se implementó una cola FIFO `bytesPendientes` (`Deque<Byte>`) que retiene en memoria los bytes leídos en exceso al solicitar tamaños mínimos, entregándolos en la siguiente lectura sin pérdida de datos ni desincronización de tramas.
  - Se creó el método auxiliar `readDisponible()` en `Protocolo.java` y se migraron todas las lecturas cortas directas en [`ProtocoloP18A.java`](app/src/main/java/com/diamon/protocolo/ProtocoloP18A.java).
  - Se envolvieron excepciones no comprobadas de drivers USB de terceros en `UsbCommunicationException`.

#### 2. Soporte de Comandos ICD en Emulador K150 (C++ y Python)
• Se incorporaron los comandos `22` (`0x16`, programar vector de depuración ICD) y `23` (`0x17`, leer vector de depuración ICD) en [`emulador_k150.cpp`](emulador_pic_k150/emulador_k150.cpp) y [`emulador_k150.py`](emulador_pic_k150/emulador_k150.py).
• Se actualizó [`guia_emulacion_k150.md`](emulador_pic_k150/guia_emulacion_k150.md).

### ✅ Validación
• **Tests Unitarios de Regresión:** Creado [`ProtocoloFtdiReadBufferTest.java`](app/src/test/java/com/diamon/protocolo/ProtocoloFtdiReadBufferTest.java), simulando el comportamiento estricto del driver FTDI.
• **Tests de Integración con Emulador K150:** Superadas al 100% las pruebas de emulación física para PIC12, PIC16, PIC18 y Vector ICD (`ProtocoloP18AIntegrationTest.java`).
• **Compilación Release:** Binarios AAB y APK firmados con la clave de producción en modo release.

---

## 🇺🇸 English

### 📌 Overview
This v2.9.7 release resolves the critical `IllegalArgumentException: Read buffer too small` crash observed on devices and programmers using FTDI (or FTDI clone) USB-to-UART bridges, hardening the low-level USB packet reading layer and ensuring serial data integrity across all supported hardware.

### 🛠️ Key Fixes

#### 1. Hardened USB Read Buffer for FTDI Drivers (Crashlytics `98da621ca29a1c7804af7c57e98e2d81`)
• **Root cause:** Unlike Prolific or CH340 chips, FTDI USB bridges (`FtdiSerialDriver`) prepend 2 modem/line status bytes (`READ_HEADER_LENGTH = 2`) to every incoming bulk USB packet. Passing destination buffers of 1 or 2 bytes caused the driver to immediately throw `IllegalArgumentException: Read buffer too small`.
• **Fix:**
  - Enforced `MIN_READ_BUFFER_SIZE = 3` in [`Protocolo.java`](app/src/main/java/com/diamon/nucleo/Protocolo.java).
  - Implemented a FIFO pending buffer queue (`bytesPendientes`) to preserve any excess bytes and deliver them on subsequent reads without data loss or protocol desynchronization.
  - Introduced `readDisponible()` in `Protocolo.java` and migrated short direct driver reads in [`ProtocoloP18A.java`](app/src/main/java/com/diamon/protocolo/ProtocoloP18A.java).
  - Wrapped third-party USB driver runtime exceptions in `UsbCommunicationException`.

#### 2. ICD Vector Command Support in K150 Hardware Emulator (C++ and Python)
• Added support for commands `22` (`0x16`, program ICD debug vector) and `23` (`0x17`, read ICD debug vector) in [`emulador_k150.cpp`](emulador_pic_k150/emulador_k150.cpp) and [`emulador_k150.py`](emulador_pic_k150/emulador_k150.py).
• Updated [`guia_emulacion_k150.md`](emulador_pic_k150/guia_emulacion_k150.md).

### ✅ Validation
• **Regression Unit Tests:** Created [`ProtocoloFtdiReadBufferTest.java`](app/src/test/java/com/diamon/protocolo/ProtocoloFtdiReadBufferTest.java).
• **K150 Emulator Integration Tests:** 100% passed for PIC12, PIC16, PIC18 and ICD Debug Vector in [`ProtocoloP18AIntegrationTest.java`](app/src/test/java/com/diamon/protocolo/ProtocoloP18AIntegrationTest.java).
• **Production Release Build:** AAB and APK signed with production release keystore.

### 📦 Artifacts
• **APK Release:** `PIC-k150-Programing-v2.9.7-release.apk`  
  **SHA-256:** `5ebb851088e8b4cef1b695772b14926c668479e67a1d2297f1160665cf81abbe`
• **AAB Release (Google Play Bundle):** `PIC-k150-Programing-v2.9.7-release.aab`  
  **SHA-256:** `dca2015afe313589456062700d7b05bcd7d039d08c2dfd926bad9d11798b0a00`

---

## 📋 Notas Cortas para Google Play Console / Short Play Store Release Notes

### 🇪🇸 Español (`es-419` / `es-ES`)
```text
• Corrección crítica de crash en programadores K150 con chip USB-Serial FTDI (IllegalArgumentException: Read buffer too small).
• Ahora compatible con chips Prolific, FTDI, CH340 y CP2102 sin ningún fallo.
• Soporte de comandos ICD (Vector de Depuración 22/23) en el emulador K150.
• Tests de integración al 100% en PIC12, PIC16 y PIC18.
```

### 🇺🇸 English (`en-US`)
```text
• Critical crash fix for K150 programmers with FTDI USB-Serial adapters (IllegalArgumentException: Read buffer too small).
• Now fully compatible with Prolific, FTDI, CH340, and CP2102 chips without any failure.
• ICD Debug Vector commands 22/23 added to the K150 hardware emulator.
• 100% integration tests passing for PIC12, PIC16, and PIC18.
```

---

## 🔗 Enlaces / Links
• **Repositorio / Repository:** https://github.com/Danielk10/PIC-k150-Programing
• **Lanzamiento / Release:** https://github.com/Danielk10/PIC-k150-Programing/releases/tag/v2.9.7
