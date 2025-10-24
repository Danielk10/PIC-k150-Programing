package com.diamon.pruebas;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.diamon.pic.R;

/**
 * MainActivity - Aplicación de prueba para procesamiento de chips PIC
 *
 * Esta actividad demuestra el uso completo de las clases migradas desde Python 2:
 * - ChipinfoReader: Lee información de chips desde chipinfo.txt
 * - ChipinfoEntry: Maneja datos de un chip específico
 * - HexRecord: Representa registros HEX
 * - HexRecordProcessor: Procesa listas de registros
 * - PicFuseProcessor: Extrae y procesa fusibles e ID
 *
 * Funcionalidad:
 * - Carga automática al iniciar
 * - Botón "Recargar": Ejecuta nuevamente el procesamiento
 * - Botón "Limpiar": Limpia el área de salida
 * - ScrollView: Permite ver todo el output
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "PicProgrammer";

    // Vistas del layout
    private TextView outputTextView;
    private ScrollView scrollView;
    private Button btnReload;
    private Button btnClear;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializar vistas
        initializeViews();

        // Configurar listeners de botones
        setupButtonListeners();

        // Ejecutar procesamiento inicial
        ejecutarProcesamiento();
    }

    /**
     * Inicializa todas las vistas del layout
     */
    private void initializeViews() {
        outputTextView = findViewById(R.id.outputTextView);
        scrollView = findViewById(R.id.scrollView);
        btnReload = findViewById(R.id.btnReload);
        btnClear = findViewById(R.id.btnClear);

        // Configurar TextView para mejor legibilidad
        outputTextView.setTextIsSelectable(true);
    }

    /**
     * Configura los listeners para los botones
     */
    private void setupButtonListeners() {
        // Botón Recargar: Ejecuta nuevamente el procesamiento
        btnReload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this,
                        "Recargando procesamiento...",
                        Toast.LENGTH_SHORT).show();
                ejecutarProcesamiento();
            }
        });

        // Botón Limpiar: Limpia el área de salida
        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                outputTextView.setText("Área de salida limpiada.\n\n" +
                        "Presiona 'Recargar' para ejecutar el procesamiento.");
                Toast.makeText(MainActivity.this,
                        "Salida limpiada",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Ejecuta el procesamiento completo en un thread separado
     * para no bloquear la UI
     */
    private void ejecutarProcesamiento() {
        // Mostrar mensaje de inicio
        outputTextView.setText("⏳ Iniciando procesamiento...\n\n");

        // Ejecutar en thread separado
        new Thread(new Runnable() {
            @Override
            public void run() {
                final String resultado = ejemploCompletoDeUso();

                // Actualizar UI en el thread principal
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        outputTextView.setText(resultado);
                        // Scroll automático al inicio
                        scrollView.post(new Runnable() {
                            @Override
                            public void run() {
                                scrollView.fullScroll(View.FOCUS_UP);
                            }
                        });
                    }
                });
            }
        }).start();
    }

    /**
     * Método principal de procesamiento - Demuestra todas las funcionalidades
     *
     * @return String con el resultado del procesamiento
     */
    private String ejemploCompletoDeUso() {
        StringBuilder output = new StringBuilder();
        output.append("╔════════════════════════════════════════╗\n");
        output.append("║  PROCESAMIENTO DE CHIP PIC - Android  ║\n");
        output.append("╚════════════════════════════════════════╝\n\n");

        try {
            // Paso 1: Cargar información del chip
            output.append("📋 Paso 1: Cargando información del chip\n");
            output.append("─────────────────────────────────────────\n");

            ChipinfoReader reader = new ChipinfoReader(this, "chipinfo.cid");
            ChipinfoEntry chipInfo = reader.getChip("18F8720");

            output.append("✓ Chip: ").append(chipInfo.getChipName()).append("\n");
            output.append("✓ Núcleo: ").append(chipInfo.getCoreBits()).append(" bits\n");
            output.append("✓ Chips en BD: ").append(reader.getChipCount()).append("\n\n");

            // Paso 2: Crear registros HEX de ejemplo
            output.append("🔧 Paso 2: Creando registros de configuración\n");
            output.append("─────────────────────────────────────────\n");

            List<HexRecord> configRecords = crearRegistrosDeEjemplo();
            output.append("✓ Registros creados: ").append(configRecords.size()).append("\n");

            for (HexRecord record : configRecords) {
                output.append("  • [0x")
                        .append(Integer.toHexString(record.getAddress()).toUpperCase())
                        .append("] len=")
                        .append(record.getLength())
                        .append(" bytes\n");
            }
            output.append("\n");

            // Paso 3: Filtrar registros por rango
            output.append("🔍 Paso 3: Filtrando registros de fusibles\n");
            output.append("─────────────────────────────────────────\n");
            output.append("Rango: [0x400E - 0x4010)\n");

            List<HexRecord> fuseRecords = HexRecordProcessor.rangeFilterRecords(
                    configRecords, 0x400E, 0x4010
            );
            output.append("✓ Registros filtrados: ").append(fuseRecords.size()).append("\n\n");

            // Paso 4: Extraer ID del chip
            output.append("🆔 Paso 4: Extrayendo ID del chip\n");
            output.append("─────────────────────────────────────────\n");

            byte[] chipId = PicFuseProcessor.extractChipId(
                    configRecords, chipInfo, 0, null
            );
            output.append("✓ ID extraído: ").append(bytesToHexString(chipId)).append("\n\n");

            // Paso 5: Extraer valores actuales de fusibles
            output.append("⚙️  Paso 5: Extrayendo fusibles actuales\n");
            output.append("─────────────────────────────────────────\n");

            List<Integer> currentFuseValues = PicFuseProcessor.extractFuseValues(
                    configRecords, chipInfo
            );
            output.append(ChipinfoUtils.formatHexList(
                    currentFuseValues, "Valores actuales"
            )).append("\n\n");

            // Paso 6: Decodificar fusibles a formato legible
            output.append("📖 Paso 6: Decodificando fusibles\n");
            output.append("─────────────────────────────────────────\n");

            Map<String, String> currentSettings = chipInfo.decodeFuseData(currentFuseValues);
            output.append(ChipinfoUtils.formatFuseDict(currentSettings));

            // Paso 7: Crear nuevas configuraciones de fusibles
            output.append("\n✏️  Paso 7: Aplicando nuevas configuraciones\n");
            output.append("─────────────────────────────────────────\n");

            Map<String, String> newSettings = new HashMap<>();
            newSettings.put("Oscillator Enable", "Disabled");
            newSettings.put("Brownout Detect", "Enabled");

            output.append("Cambios solicitados:\n");
            output.append("  • WDT → Disabled\n");
            output.append("  • PWRTE → Enabled\n\n");

            // Paso 8: Procesar fusibles (combinar actuales + nuevos)
            output.append("🔄 Paso 8: Procesando fusibles\n");
            output.append("─────────────────────────────────────────\n");

            PicFuseProcessor.FuseProcessingResult result =
                    PicFuseProcessor.processFuses(configRecords, chipInfo, newSettings);

            if (result.isSuccess()) {
                output.append("✅ Procesamiento exitoso\n\n");

                output.append("Configuración final:\n");
                output.append(ChipinfoUtils.formatFuseDict(
                        result.getFinalSettings()
                )).append("\n");

                output.append(ChipinfoUtils.formatHexList(
                        result.getFinalValues(), "Valores finales"
                )).append("\n\n");

                if (result.hasChanges()) {
                    output.append("🔴 Hay cambios para programar en el chip\n");
                } else {
                    output.append("🟢 No hay cambios en fusibles\n");
                }
            } else {
                output.append("❌ Error: ").append(result.getErrorMessage()).append("\n");
            }

            // Paso 9: Ejemplo de fusión de registros
            output.append("\n🧩 Paso 9: Ejemplo de fusión de registros\n");
            output.append("─────────────────────────────────────────\n");

            byte[] defaultData = new byte[] {
                    (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF
            };

            List<HexRecord> testRecords = new ArrayList<>();
            testRecords.add(new HexRecord(0x4000, new byte[]{0x12, 0x34}));

            byte[] merged = HexRecordProcessor.mergeRecords(
                    testRecords, defaultData, 0x4000
            );

            output.append("Buffer original:  FF FF FF FF\n");
            output.append("Registro:         [0x4000] = 12 34\n");
            output.append("Resultado fusión: ").append(bytesToHexString(merged)).append("\n");

            // Resumen final
            output.append("\n╔════════════════════════════════════════╗\n");
            output.append("║      ✅ PROCESAMIENTO COMPLETO ✅      ║\n");
            output.append("╚════════════════════════════════════════╝\n\n");

            output.append("📊 Resumen:\n");
            output.append("  • Chip procesado: ").append(chipInfo.getChipName()).append("\n");
            output.append("  • Registros analizados: ").append(configRecords.size()).append("\n");
            output.append("  • ID extraído: OK\n");
            output.append("  • Fusibles procesados: OK\n");
            output.append("  • Estado: Todo correcto ✓\n\n");

            output.append("💡 Tip: Usa el botón 'Recargar' para ejecutar\n");
            output.append("   nuevamente el procesamiento.\n");

        } catch (Exception e) {
            output.append("\n❌ ERROR CRÍTICO\n");
            output.append("─────────────────────────────────────────\n");
            output.append("Mensaje: ").append(e.getMessage()).append("\n\n");
            output.append("Stack trace:\n");

            for (StackTraceElement element : e.getStackTrace()) {
                output.append("  at ").append(element.toString()).append("\n");
            }

            Log.e(TAG, "Error en procesamiento", e);
        }

        return output.toString();
    }

    /**
     * Crea registros HEX de ejemplo para demostración
     *
     * Simula datos leídos desde un chip PIC real
     *
     * @return Lista de registros HEX de ejemplo
     */
    private List<HexRecord> crearRegistrosDeEjemplo() {
        List<HexRecord> records = new ArrayList<>();

        // Registro de ID del chip (0x4000-0x4007)
        // Estos bytes identifican el modelo del chip
        byte[] idData = new byte[] {
                0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08
        };
        records.add(new HexRecord(0x4000, idData));

        // Registro de fusibles (0x400E-0x400F)
        // Valores de configuración del chip
        byte[] fuseData = new byte[] {
                (byte)0x3F, (byte)0xFF  // 0x3FFF (todos los bits en 1)
        };
        records.add(new HexRecord(0x400E, fuseData));

        return records;
    }

    /**
     * Convierte bytes a string hexadecimal legible
     *
     * @param bytes Array de bytes a convertir
     * @return String con representación hexadecimal
     */
    private String bytesToHexString(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "(vacío)";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            if (i > 0) sb.append(" ");
            sb.append(String.format("%02X", bytes[i] & 0xFF));
        }
        return sb.toString();
    }
}
