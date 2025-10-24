package com.diamon.pruebas;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.diamon.pic.R;

/**
 * MainActivity - Editor dinámico de fusibles para chips PIC
 *
 * Características:
 * - Selector dinámico de chips desde chipinfo.cid
 * - Editor de fusibles generado automáticamente según el chip
 * - Opción de agregar fusibles personalizados
 * - Procesamiento y codificación de fusibles
 * - Log de operaciones en tiempo real
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "PicProgrammer";

    // ==================== VISTAS ====================
    private Spinner chipSpinner;
    private Button btnLoadChip, btnProcess, btnReset, btnClearLog, btnAddCustomFuse;
    private TextView chipInfoTextView, logTextView, emptyFuseMessage;
    private LinearLayout fuseContainer;
    private ScrollView logScrollView;

    // ==================== DATOS ====================
    private ChipinfoReader chipReader;
    private ChipinfoEntry currentChip;
    private Map<String, Spinner> fuseSpinners = new HashMap<>();
    private Map<String, String> customFuses = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupListeners();
        loadChipDatabase();
    }

    /**
     * Inicializa todas las vistas del layout
     */
    private void initializeViews() {
        chipSpinner = findViewById(R.id.chipSpinner);
        btnLoadChip = findViewById(R.id.btnLoadChip);
        btnProcess = findViewById(R.id.btnProcess);
        btnReset = findViewById(R.id.btnReset);
        btnClearLog = findViewById(R.id.btnClearLog);
        btnAddCustomFuse = findViewById(R.id.btnAddCustomFuse);
        chipInfoTextView = findViewById(R.id.chipInfoTextView);
        logTextView = findViewById(R.id.logTextView);
        emptyFuseMessage = findViewById(R.id.emptyFuseMessage);
        fuseContainer = findViewById(R.id.fuseContainer);
        logScrollView = findViewById(R.id.logScrollView);
    }

    /**
     * Configura los listeners para todos los botones
     */
    private void setupListeners() {
        btnLoadChip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadSelectedChip();
            }
        });

        btnProcess.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                processFuses();
            }
        });

        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetFuses();
            }
        });

        btnClearLog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearLog();
            }
        });

        btnAddCustomFuse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCustomFuseDialog();
            }
        });
    }

    /**
     * Carga la base de datos de chips desde chipinfo.cid
     * Se ejecuta en thread separado para no bloquear la UI
     */
    private void loadChipDatabase() {
        logMessage("⏳ Cargando base de datos...");

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    chipReader = new ChipinfoReader(MainActivity.this, "chipinfo.cid");
                    final List<String> chipNames = chipReader.getAvailableChips();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                    MainActivity.this,
                                    android.R.layout.simple_spinner_item,
                                    chipNames
                            );
                            adapter.setDropDownViewResource(
                                    android.R.layout.simple_spinner_dropdown_item
                            );
                            chipSpinner.setAdapter(adapter);

                            logMessage("✓ Base de datos cargada: " +
                                    chipNames.size() + " chips disponibles");
                        }
                    });

                } catch (Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            logMessage("❌ Error cargando BD: " + e.getMessage());
                            Toast.makeText(MainActivity.this,
                                    "Error cargando base de datos",
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                    Log.e(TAG, "Error loading database", e);
                }
            }
        }).start();
    }

    /**
     * Carga el chip seleccionado del Spinner
     * Genera dinámicamente el editor de fusibles
     */
    private void loadSelectedChip() {
        final String chipName = (String) chipSpinner.getSelectedItem();
        if (chipName == null) {
            Toast.makeText(this, "Selecciona un chip", Toast.LENGTH_SHORT).show();
            return;
        }

        logMessage("⏳ Cargando chip: " + chipName);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    currentChip = chipReader.getChip(chipName);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            displayChipInfo();
                            buildFuseEditor();
                            btnProcess.setEnabled(true);
                            btnReset.setEnabled(true);
                            logMessage("✓ Chip cargado: " + chipName);
                        }
                    });

                } catch (Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            logMessage("❌ Error cargando chip: " + e.getMessage());
                            Toast.makeText(MainActivity.this,
                                    "Error cargando chip",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                    Log.e(TAG, "Error loading chip", e);
                }
            }
        }).start();
    }

    /**
     * Muestra información del chip cargado
     */
    private void displayChipInfo() {
        StringBuilder info = new StringBuilder();
        info.append("Chip: ").append(currentChip.getChipName()).append("\n");
        info.append("Núcleo: ").append(currentChip.getCoreBits()).append(" bits\n");

        Integer romSize = (Integer) currentChip.getVar("rom_size");
        if (romSize != null) {
            info.append("ROM: 0x")
                    .append(Integer.toHexString(romSize).toUpperCase())
                    .append("\n");
        }

        info.append("EEPROM: ").append(currentChip.hasEeprom() ? "Sí" : "No");

        chipInfoTextView.setText(info.toString());
    }

    /**
     * Construye dinámicamente el editor de fusibles
     * según los fusibles disponibles en el chip
     */
    @SuppressWarnings("unchecked")
    private void buildFuseEditor() {
        fuseContainer.removeAllViews();
        fuseSpinners.clear();
        emptyFuseMessage.setVisibility(View.GONE);

        Map<String, Map<String, List<ChipinfoEntry.FuseValue>>> fuses =
                (Map<String, Map<String, List<ChipinfoEntry.FuseValue>>>)
                        currentChip.getVar("fuses");

        if (fuses == null || fuses.isEmpty()) {
            emptyFuseMessage.setVisibility(View.VISIBLE);
            emptyFuseMessage.setText("Este chip no tiene fusibles configurables");
            return;
        }

        for (Map.Entry<String, Map<String, List<ChipinfoEntry.FuseValue>>> entry :
                fuses.entrySet()) {
            String fuseName = entry.getKey();
            Map<String, List<ChipinfoEntry.FuseValue>> fuseOptions = entry.getValue();

            addFuseRow(fuseName, new ArrayList<>(fuseOptions.keySet()));
        }

        logMessage("✓ Editor construido: " + fuses.size() + " fusibles");
    }

    /**
     * Agrega una fila de fusible al editor
     *
     * @param fuseName Nombre del fusible
     * @param options Lista de opciones disponibles
     */
    private void addFuseRow(String fuseName, List<String> options) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(8, 8, 8, 8);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 8);
        row.setLayoutParams(params);

        // Label del fusible
        TextView label = new TextView(this);
        label.setText(fuseName + ":");
        label.setTextSize(14);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f
        );
        label.setLayoutParams(labelParams);
        row.addView(label);

        // Spinner con opciones
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                options
        );
        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );
        spinner.setAdapter(adapter);
        LinearLayout.LayoutParams spinnerParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.5f
        );
        spinner.setLayoutParams(spinnerParams);
        row.addView(spinner);

        fuseSpinners.put(fuseName, spinner);
        fuseContainer.addView(row);
    }

    /**
     * Muestra diálogo para agregar fusible personalizado
     */
    private void showCustomFuseDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Agregar Fusible Personalizado");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText nameInput = new EditText(this);
        nameInput.setHint("Nombre del fusible");
        layout.addView(nameInput);

        final EditText valueInput = new EditText(this);
        valueInput.setHint("Valor");
        layout.addView(valueInput);

        builder.setView(layout);

        builder.setPositiveButton("Agregar", new android.content.DialogInterface.OnClickListener() {
            @Override
            public void onClick(android.content.DialogInterface dialog, int which) {
                String name = nameInput.getText().toString().trim();
                String value = valueInput.getText().toString().trim();

                if (!name.isEmpty() && !value.isEmpty()) {
                    customFuses.put(name, value);
                    List<String> valueList = new ArrayList<>();
                    valueList.add(value);
                    addFuseRow(name, valueList);
                    logMessage("✓ Fusible custom agregado: " + name + " = " + value);
                    Toast.makeText(MainActivity.this,
                            "Fusible agregado",
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this,
                            "Completa todos los campos",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    /**
     * Procesa los fusibles seleccionados
     * Codifica las configuraciones a valores hexadecimales
     */
    private void processFuses() {
        if (currentChip == null) {
            Toast.makeText(this, "Carga un chip primero", Toast.LENGTH_SHORT).show();
            return;
        }

        logMessage("⏳ Procesando fusibles...");

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Recopilar configuración actual
                    final Map<String, String> fuseConfig = new HashMap<>();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            for (Map.Entry<String, Spinner> entry : fuseSpinners.entrySet()) {
                                String fuseName = entry.getKey();
                                String fuseValue = (String) entry.getValue().getSelectedItem();
                                if (fuseValue != null) {
                                    fuseConfig.put(fuseName, fuseValue);
                                }
                            }
                            // Agregar fusibles personalizados
                            fuseConfig.putAll(customFuses);
                        }
                    });

                    // Esperar a que se complete la recopilación
                    Thread.sleep(100);

                    // Codificar fusibles
                    final List<Integer> encodedValues = currentChip.encodeFuseData(fuseConfig);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            StringBuilder result = new StringBuilder();
                            result.append("✅ Procesamiento exitoso\n");
                            result.append("Configuración aplicada:\n");
                            for (Map.Entry<String, String> e : fuseConfig.entrySet()) {
                                result.append("  • ").append(e.getKey())
                                        .append(" = ").append(e.getValue()).append("\n");
                            }
                            result.append("\nValores hex: ");
                            for (Integer val : encodedValues) {
                                result.append(String.format("0x%04X ", val));
                            }

                            logMessage(result.toString());
                            Toast.makeText(MainActivity.this,
                                    "Fusibles procesados correctamente",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });

                } catch (Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            logMessage("❌ Error procesando: " + e.getMessage());
                            Toast.makeText(MainActivity.this,
                                    "Error: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                    Log.e(TAG, "Error processing fuses", e);
                }
            }
        }).start();
    }

    /**
     * Restablece el editor de fusibles a su estado inicial
     */
    private void resetFuses() {
        if (currentChip == null) return;

        buildFuseEditor();
        customFuses.clear();
        logMessage("↻ Fusibles restablecidos");
        Toast.makeText(this, "Fusibles restablecidos", Toast.LENGTH_SHORT).show();
    }

    /**
     * Limpia el área de log
     */
    private void clearLog() {
        logTextView.setText("Log limpiado\n");
    }

    /**
     * Agrega un mensaje al log con timestamp
     *
     * @param message Mensaje a agregar
     */
    private void logMessage(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
                String timestamp = sdf.format(new Date());
                String logEntry = "[" + timestamp + "] " + message + "\n";

                logTextView.append(logEntry);

                // Auto-scroll al final
                logScrollView.post(new Runnable() {
                    @Override
                    public void run() {
                        logScrollView.fullScroll(View.FOCUS_DOWN);
                    }
                });
            }
        });
    }
}
