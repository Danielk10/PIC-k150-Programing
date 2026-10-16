# 🚀 Notas de Lanzamiento - v2.9.4 (VersionCode 58)

## 📌 Resumen General
Esta versión introduce el blindaje definitivo de compras in-app con **Google Play Billing 9.1.0**, soporte dual de productos, consultas asíncronas silenciosas estilo *C-PIC Compiler*, y optimización exhaustiva de código DEX con **R8 / ProGuard** superando los requisitos de Android Vitals y Google Play Automatic Protection.

---

## 🛠️ Correcciones y Mejoras Principales

### 💳 1. Robustez de Google Play Billing y Compras Pro
* **Compatibilidad Dual de Productos In-App:** Soporte simultáneo para el identificador `remove_ads_pro` y retrocompatibilidad con `remove_ads`, garantizando que la aplicación reconozca el producto independientemente de la configuración activa en Google Play Console.
* **Consultas Silenciosas en Segundo Plano (Estilo C-PIC):** Se desacopló la consulta de productos de la consola de programación. Los estados de carga transitorios o demoras de red de Google Play (`Failed to load product details`) ya no se notifican en la pantalla de terminal del programador, manteniéndola 100% limpia para operaciones de microcontroladores PIC.
* **Reintento Dinámico en UI (`ProAccessDialog`):** Al presionar el botón de compra permanente, si Google Play aún está sincronizando los detalles de precios, el diálogo no se cierra; reintenta la consulta en segundo plano y muestra un Toast amigable.
* **Permiso Explícito de Facturación:** Se añadió explícitamente `<uses-permission android:name="com.android.vending.BILLING" />` en `AndroidManifest.xml`.

### ⚡ 2. Optimización DEX y Reducción de Tamaño (Android Vitals)
* **Ofuscación y Reducción con R8:** Reducción del tamaño del paquete de 21 MB a 14.3 MB en APK y 14.8 MB en AAB (~30% de reducción), superando con creces el umbral mínimo del 25% de ofuscación de Android Vitals.
* **minSdkVersion 24 (Android 7.0+):** Habilitada para soportar de forma nativa la Protección Automática de Integridad en Google Play.

### 🛡️ 3. Protección de Funciones Gratuitas
* Grabación completa y selectiva de Flash ROM, EEPROM y Fuses, Detección en Zócalo ZIF, Borrado Masivo, Blank Check Dual y Exportación de Dumps (HEX/BIN) siguen siendo 100% gratuitas e ilimitadas.

---

## 🇺🇸 English Release Notes

### 💳 1. Google Play Billing Robustness & Pro Purchase Fix
* **Dual In-App Product Support:** Seamless compatibility with both `remove_ads_pro` and `remove_ads` one-time products.
* **Silent Background Querying:** Transient Google Play billing query states (`Failed to load product details`) are logged internally to Logcat and no longer printed to the microcontroller terminal.
* **Dynamic Retry Flow:** If product details are still propagating, the Pro access dialog remains open, triggers an automatic reload, and informs the user via Toast.
* **Explicit Permission:** Added `com.android.vending.BILLING` to `AndroidManifest.xml`.

### ⚡ 2. DEX Optimization & App Size Reduction (Android Vitals)
* **R8 / ProGuard Minification:** Application size reduced from 21 MB to 14.3 MB APK / 14.8 MB AAB (~30% reduction), satisfying Android Vitals standards.
* **minSdkVersion 24 (Android 7.0+):** Enabled for Google Play Automatic Protection.
