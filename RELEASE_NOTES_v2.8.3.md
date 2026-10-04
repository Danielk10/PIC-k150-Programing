# Notas de Lanzamiento - PIC-k150-Programing v2.8.3 (versionCode 50)

## 🌐 Notas para Google Play Console (Bilingüe)

### Español (`es-419` / `es-ES`)
- Optimización de exportación binaria con FileChannel y ByteBuffer directos.
- Transferencia Zero-Copy a nivel de kernel para volcados sin sobrecarga de memoria.
- Protección WakeLock durante la programación ininterrumpida de ROM, EEPROM y fuses.
- Recepción USB optimizada con búferes reutilizables sin pausas de Garbage Collector.
- Compatibilidad completa con Android API 37 y largeHeap habilitado.

### English (`en-US`)
- Optimized binary dump export via FileChannel and direct ByteBuffer.
- Kernel-level Zero-Copy transfer support for low-overhead memory dumps.
- WakeLock protection during uninterrupted ROM, EEPROM, and fuse operations.
- Optimized USB reception with reusable buffers avoiding GC pauses.
- Full Android API 37 support and largeHeap enabled.

---

## 🛠️ Detalle Técnico de Cambios

1. **Exportación Binaria Zero-Copy (`FileManager.java` y `HexExportManager.java`)**:
   - Uso de `FileChannel.transferTo()` y `ByteBuffer` directo para transferir volcados sin duplicar arrays en el Heap de la JVM.
2. **Protección de Energía USB y CPU (`PicProgrammingManager.java`)**:
   - `WakeLock` (`PARTIAL_WAKE_LOCK`) con timeout de seguridad cubriendo lectura, escritura, borrado y verificación.
3. **Recepción USB sin Pausas de GC (`Protocolo.java`)**:
   - Reutilización de búferes fijos en los bucles de recepción, eliminando la creación de objetos temporales.
4. **Configuración de Manifiesto y SDK (`app/build.gradle` y `AndroidManifest.xml`)**:
   - `compileSdk 37`, `targetSdk 37`, `versionCode 50`, `versionName "2.8.3"`.
   - `android:largeHeap="true"` y `tools:targetApi="37"`.
