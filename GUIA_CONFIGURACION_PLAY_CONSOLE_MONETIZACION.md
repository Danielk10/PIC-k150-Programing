# Guía de Configuración de Monetización y Google Play Console 🚀
## PIC-k150-Programing — K150 Pro & Pase Libre de 12 Horas

Esta guía documenta en detalle el modelo de monetización híbrido implementado en **PIC-k150-Programing** (`com.diamon.pic`), explicando cómo dar de alta el producto in-app en **Google Play Console**, cómo configurar las licencias de prueba y cómo reemplazar los bloques de anuncios en **Google AdMob**.

---

## 🧭 1. Arquitectura de Monetización Híbrida (Fair Freemium)

La aplicación implementa un modelo **100% ético y técnico**: todas las operaciones fundamentales del programador K150 son libres y gratuitas para siempre, mientras que las herramientas avanzadas de ingeniería inversa y diagnóstico profundo están protegidas mediante un pase temporal de 12 horas o una compra permanente.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                       ESTRUCTURA DE CARACTERÍSTICAS                         │
├──────────────────────────────────────┬──────────────────────────────────────┤
│ 🆓 100% LIBRES (Sin Restricciones)   │ 👑 CARACTERÍSTICAS PRO (Gated)       │
├──────────────────────────────────────┼──────────────────────────────────────┤
│ • Grabación Flash ROM Completa       │ • Editor Hexadecimal en Vivo         │
│ • Grabación Solo Flash ROM           │ • Diagnóstico Hardware & Voltajes    │
│ • Grabación Solo EEPROM              │   (Test VPP 13V y VDD 5V)            │
│ • Grabación Solo Fusibles de Config. │ • Vector de Depuración ICD (24 bits) │
│ • Detección en Zócalo / Device ID    │ • Rescate y Edición Manual OSCCAL    │
│ • Borrado Físico y Blank Check Dual  │ • Cero Publicidad durante 12 horas   │
│ • Volcado y Exportación (HEX / BIN)  │   (o permanente tras compra)         │
└──────────────────────────────────────┴──────────────────────────────────────┘
```

El usuario puede desbloquear las herramientas Pro de dos formas:
1. 🎬 **Ver un Video Recompensado (Pase Libre de 12 Horas):** Al terminar de ver el video de Google AdMob, se otorga acceso ilimitado a todas las funciones Pro y se ocultan los anuncios durante 12 horas completas.
2. 👑 **Compra In-App Permanente:** Pago único que desbloquea para siempre todas las herramientas Pro y elimina la publicidad sin volver a requerir anuncios.

---

## 🛒 2. Configuración del Producto en Google Play Console

### Paso 2.1: Crear el Producto Integrado (In-App Product)
1. Inicia sesión en [Google Play Console](https://play.google.com/console).
2. Selecciona la aplicación **PIC-k150-Programing**.
3. En el menú lateral izquierdo, ve a: **Monetizar con Play** ➔ **Productos** ➔ **Productos integrados en la aplicación**.
4. Haz clic en **Crear producto**.

### Paso 2.2: Parámetros del Producto

| Campo | Valor Requerido | Notas |
| :--- | :--- | :--- |
| **ID de producto** | `remove_ads_pro` | **Debe coincidir exactamente con `BillingManager.PRODUCT_ID_REMOVE_ADS`** |
| **Tipo de producto** | No consumible (One-time product) | Compra permanente de por vida |

### Paso 2.3: Nombre y Descripción Localizados

#### 🇪🇸 Español (es-419 / es-ES) — Idioma Principal:
* **Nombre:**
  ```text
  K150 Pro (Sin Anuncios + Herramientas)
  ```
* **Descripción:**
  ```text
  Desbloquea permanentemente todas las funciones Pro de silicio (Editor Hexadecimal en Vivo, Diagnóstico de Hardware y prueba de voltajes VPP/VDD, Vector de Depuración ICD y calibración OSCCAL) y elimina completamente la publicidad de la aplicación.
  ```

#### 🇺🇸 Inglés (en-US):
* **Name:**
  ```text
  K150 Pro (Ad-Free + Silicon Tools)
  ```
* **Description:**
  ```text
  Permanently unlock all Pro silicon tools (Live Hex Editor, Hardware Diagnostics & VPP/VDD voltage tests, ICD Debug Vector, and OSCCAL manual calibration) with zero ads across the entire app.
  ```

### Paso 2.4: Fijar Precio y Activar
* **Precio Oficial:** `$4.99 USD` (Google Play calculará automáticamente los equivalentes en moneda local para cada país con impuestos incluidos).
* Haz clic en **Guardar** y luego en **Activar producto**.

---

## 🧪 3. Configuración de Cuentas de Prueba (License Testing)

Para probar el flujo de compra in-app sin gastar dinero real y con entrega inmediata de la licencia Pro:

1. En Google Play Console, ve a: **Configuración** ➔ **Licencias de prueba** (License Testing).
2. En la sección **Evaluadores con acceso a licencias**, agrega las direcciones de correo electrónico de tu cuenta de Google (las cuentas configuradas en tu dispositivo de prueba o emulador).
3. En **Respuesta de prueba de licencia**, selecciona:
   - `RESPOND_NORMALLY` (Simula el diálogo real de compra de Google Play, pero emite una tarjeta de prueba que no cobra).
4. Guarda los cambios. Las compras realizadas con estas cuentas devolverán tokens válidos y se confirmarán automáticamente en la app.

---

## 📢 4. Configuración del Bloque Recompensado en Google AdMob

La aplicación utiliza anuncios bonificados (**Rewarded Ads**) para otorgar el **Pase Pro de 12 Horas**.

### Paso 4.1: Crear la Unidad de Anuncios en AdMob
1. Inicia sesión en [Google AdMob](https://admob.google.com/).
2. Selecciona la aplicación **PIC-k150-Programing**.
3. Ve a **Bloques de anuncios** ➔ **Añadir bloque de anuncios**.
4. Selecciona el formato **Bonificado** (Rewarded Video).
5. Configura los parámetros:
   - **Nombre del bloque:** `Pase Pro 12 Horas - K150`
   - **Importe de recompensa:** `1`
   - **Elemento de recompensa:** `Pase Pro`
6. Haz clic en **Crear bloque de anuncios** y copia el ID generado:
   `ca-app-pub-5141499161332805/XXXXXXXXXX`

### Paso 4.2: Estado del ID en el Código Fuente
La unidad real creada en AdMob ya fue integrada directamente en el código de la aplicación:

En [`app/src/main/java/com/diamon/publicidad/GestorPublicidad.java`](file:///home/danielpdiamon/PIC-k150-Programing/app/src/main/java/com/diamon/publicidad/GestorPublicidad.java):
```java
/** ID Real AdMob de producción: "Pase Pro 12 Horas K150" */
public static final String REWARDED_AD_UNIT_ID = "ca-app-pub-5141499161332805/7291519778";
/** ID Oficial de Prueba de Google AdMob para desarrollo y pruebas locales */
public static final String REWARDED_TEST_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917";
public static final String REWARDED_ID = REWARDED_AD_UNIT_ID; // Alias retrocompatible
```

> [!NOTE]
> En la consola de AdMob, la unidad está configurada con:
> * **Nombre de la unidad:** `Pase Pro 12 Horas K150`
> * **Recompensa:** `Pase Pro 12 Horas` (Valor: `1`)
> * **ID de la unidad:** `ca-app-pub-5141499161332805/7291519778`
> 
> Durante pruebas locales en dispositivos de depuración, asegúrate de registrar el dispositivo de prueba con `RequestConfiguration.Builder().setTestDeviceIds(...)` o usar el ID de prueba para no generar impresiones inválidas sobre tu cuenta de AdMob.

---

## 🛠️ 5. Verificación de Funcionamiento y Pruebas Unitarias

La lógica del Pase Pro y de Facturación cuenta con suites de pruebas unitarias automatizadas:

```bash
# Ejecutar pruebas unitarias de monetización y pase de 12 horas
./gradlew testDebugUnitTest --tests "com.diamon.managers.ProPassManagerTest" --tests "com.diamon.billing.BillingManagerTest"
```

### Casos de Prueba Verificados:
* ✅ `activarPase12Horas`: Establece correctamente la expiración a `ahora + 12 horas`.
* ✅ `formatearTiempoRestante`: Muestra `"11h 45m"`, `"45m"` o `"Expirado"` de forma precisa.
* ✅ `isProActive`: Reconoce automáticamente la compra de `BillingManager` o el pase temporal vigente.
* ✅ `onPurchasesUpdated`: Reconoce (acknowledge) compras pendientes para evitar cancelaciones automáticas a los 3 días.
* ✅ Ocultamiento del banner inferior y actualización dinámica del botón `⭐ Modo Pro` / `👑 Pro Permanente` en la Toolbar.
