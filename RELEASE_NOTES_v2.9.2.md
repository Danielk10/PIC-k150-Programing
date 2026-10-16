# Notas de Lanzamiento - PIC-k150-Programing v2.9.2 (versionCode 55)

Fecha de lanzamiento / Release Date: 16 de Octubre de 2026 / October 16, 2026

---

## 🌐 Notas para Google Play Console (Bilingüe)

### Español (`es-419` / `es-ES`)
- 🔘 **Selector de Protocolos con Alto Contraste**: Rediseño visual del diálogo de protocolos con RadioButtons de alta visibilidad (`#00E676` seleccionado / `#B0BEC5` no seleccionado) en tarjetas oscuras con borde dinámico, eliminando problemas de contraste en temas oscuros y claros.
- ⚡ **Diagnóstico de Hardware y Botón 13V/5V**: Actualización precisa del control de voltajes a `13V/5V` en todos los idiomas, reflejando la energización simultánea de VPP (13V) y VDD (5V) para pruebas seguras de microcontroladores.
- 🧮 **Editor Hexadecimal en 3 Tarjetas**: Interfaz renovada con métricas claras (Dirección, Valor Actual, Límite Máximo), desglose técnico Little-Endian en badges visuales y campo de entrada centrado monospace de 20sp.
- 🛡️ **Blindaje No Bloqueante en Zócalo ZIF**: Detección segura en socket que evita bloqueos de comunicación cuando el zócalo está vacío o el chip no hace contacto eléctrico.
- 📋 **Copiado de Log al Portapapeles (Estilo C-PIC)**: Sustitución de toques accidentales por pulsación prolongada con soporte de selección manual de texto.
- 🧼 **Limpieza Visual y Erradicación de Paréntesis**: Supresión sistemática de paréntesis informales en toda la app para un acabado técnico profesional.
- 🌍 **Paridad Total en 53 Idiomas (474 Claves)**: Sincronización completa de los 53 archivos de recursos mediante subagentes paralelos en lotes de 5, erradicando cadenas hardcodeadas en código.
- 📱 **Ecosistema de Aplicaciones**: Enlaces directos a las aplicaciones Curso Microcontroladores PIC, Tabla Periódica PTC y Mini Calculadora.

### English (`en-US`)
- 🔘 **High-Contrast Protocol Selector**: Redesigned protocol selection dialog featuring high-contrast RadioButtons (`#00E676` checked / `#B0BEC5` unchecked) within dark interactive cards, guaranteeing optimal contrast across dark and light themes.
- ⚡ **Hardware Diagnostics & 13V/5V Button**: Voltage control button precisely labeled `13V/5V` across all languages to accurately reflect simultaneous VPP (13V) and VDD (5V) rail activation.
- 🧮 **3-Card Live Hex Editor**: Restructured layout with 3 clean cards for memory overview metrics, visual Little-Endian byte breakdown badges, and centered 20sp monospace input.
- 🛡️ **Non-Blocking ZIF Socket Probe**: Safe socket polling preventing programmer firmware freezes when the ZIF socket is empty or pins have poor contact.
- 📋 **Log Clipboard Support (C-PIC Compiler Style)**: Accidental taps replaced with long-click copying, scroll focus protection, and manual text selection.
- 🧼 **Clean Technical UI**: Systematic eradication of informal parentheses across all dialogs and screens for a professional engineering experience.
- 🌍 **Full 53-Language Parity (474 Keys)**: Complete 100% resource synchronization across 53 languages via parallel subagents in batches of 5, with zero hardcoded Java strings.
- 📱 **Developer Ecosystem**: Direct links to PIC Microcontrollers Course, Periodic Table PTC, and Mini Calculator on Google Play.

---

## 🛠️ Detalle Técnico de Cambios

1. **Selector de Protocolos y Estilos de Controles**:
   - `MainActivity.java`: Sustitución del diálogo simple por contenedor con `ScrollView`, `RadioGroup`, tarjetas con `GradientDrawable` y `ColorStateList` personalizado.
   - `themes.xml`: Inclusión de `colorControlNormal` (`#B0BEC5`) y `colorControlActivated` (`#00E676`).
   - 4 nuevas claves localizadas en 53 idiomas: `proto_desc_p18a`, `proto_desc_p018`, `proto_desc_p016` y `proto_desc_p014`.

2. **Diagnóstico de Hardware y Botón de Voltajes**:
   - Actualización de la clave `diag_btn_vpp_13v` a `13V/5V` en las 53 variantes de recursos lingüísticos.
   - Cerrojo atómico (`AtomicBoolean voltajesActivos`) y apagado fail-safe de voltajes al cerrar o descartar el diálogo.

3. **Editor Hexadecimal Modernizado**:
   - Estructuración en 3 tarjetas independientes en [`HexEditorManager.java`](file:///home/danielpdiamon/PIC-k150-Programing/app/src/main/java/com/diamon/managers/HexEditorManager.java) con colores de estado temáticos y fondo `#181824`.

4. **Saneamiento de Recursos y Cero Hardcodeo**:
   - 100% de paridad en los 53 archivos de idioma (`values-*/strings.xml`), alcanzando exactamente 474 claves en cada uno.
   - Eliminación de cadenas literales en Java unificando referencias con `getString(R.string.*)`.

5. **Pruebas y Certificación**:
   - 109/109 pruebas unitarias y de integración Java aprobadas contra el emulador virtual K150 en C++.
   - Verificación de compilación Debug y Release (`assembleRelease` y `bundleRelease`).
