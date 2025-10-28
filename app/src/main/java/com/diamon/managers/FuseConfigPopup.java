package com.diamon.managers;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.diamon.chip.ChipPic;
import com.diamon.chip.ChipinfoEntry;
import com.diamon.datos.DatosPicProcesados;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * FuseConfigPopup - Gestor del PopupWindow para configuración de fusibles
 *
 * <p>Esta clase maneja el diálogo de configuración de fusibles del PIC, permitiendo: - Visualizar y
 * editar fusibles del chip seleccionado - Restaurar fusibles desde datos del PIC o del archivo HEX
 * - Configurar ID personalizado - Aplicar o cancelar cambios
 */
public class FuseConfigPopup {

    public interface FuseConfigListener {
        void onFusesApplied(List<Integer> fuses, byte[] idData, Map<String, String> configuration);

        void onFusesCancelled();
    }

    private Context context;
    private Dialog dialog;
    private FuseConfigListener listener;

    // Referencias a componentes
    private ChipPic currentChip;
    private ChipinfoEntry currentChipFuses;
    private DatosPicProcesados hexData;

    // Vistas
    private TextView chipNameTextView;
    private TextView chipInfoTextView;
    private LinearLayout fuseContainer;
    private TextView logTextView;
    private ScrollView logScrollView;
    private EditText customIdEditText;
    private Button btnApply;
    private Button btnRestoreFromChip;
    private Button btnRestoreFromHex;
    private Button btnCancel;

    // Datos
    private Map<String, Spinner> fuseSpinners = new HashMap<>();
    private Map<String, String> lastConfiguration;
    private byte[] currentIDData = new byte[] {0};

    public FuseConfigPopup(Context context, FuseConfigListener listener) {
        this.context = context;
        this.listener = listener;
    }

    /** Muestra el popup de configuración de fusibles */
    public void show(
            ChipPic chip,
            ChipinfoEntry chipFuses,
            DatosPicProcesados hexData,
            Map<String, String> lastConfig) {
        this.currentChip = chip;
        this.currentChipFuses = chipFuses;
        this.hexData = hexData;
        this.lastConfiguration = lastConfig;

        createDialog();
        dialog.show();
    }

    /** Crea y configura el diálogo */
    private void createDialog() {
        dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        // Layout principal
        LinearLayout mainLayout = new LinearLayout(context);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setBackgroundColor(Color.parseColor("#F5F5F5"));
        mainLayout.setPadding(0, 0, 0, 0);

        // Título
        TextView titleView = createTitle();
        mainLayout.addView(titleView);

        // ScrollView para el contenido
        ScrollView contentScrollView = new ScrollView(context);
        LinearLayout contentLayout = new LinearLayout(context);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setPadding(16, 16, 16, 16);

        // Información del chip
        contentLayout.addView(createChipInfoSection());

        // Editor de fusibles
        contentLayout.addView(createFuseEditorSection());

        // ID personalizado
        contentLayout.addView(createCustomIdSection());

        // Log
        contentLayout.addView(createLogSection());

        contentScrollView.addView(contentLayout);

        LinearLayout.LayoutParams scrollParams =
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0);
        scrollParams.weight = 1;
        contentScrollView.setLayoutParams(scrollParams);

        mainLayout.addView(contentScrollView);

        // Botones
        mainLayout.addView(createButtonBar());

        // Configurar diálogo
        dialog.setContentView(mainLayout);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(
                    (int) (context.getResources().getDisplayMetrics().widthPixels * 0.95),
                    (int) (context.getResources().getDisplayMetrics().heightPixels * 0.90));
        }

        // Cargar datos iniciales
        loadInitialData();
    }

    /** Crea el título del diálogo */
    private TextView createTitle() {
        TextView title = new TextView(context);
        title.setText("Configuraciones de Fusibles del PIC");
        title.setTextColor(Color.WHITE);
        title.setBackgroundColor(Color.parseColor("#2196F3"));
        title.setTextSize(18);
        title.setGravity(Gravity.CENTER);
        title.setPadding(16, 16, 16, 16);
        title.setLayoutParams(
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return title;
    }

    /** Crea la sección de información del chip */
    private LinearLayout createChipInfoSection() {
        LinearLayout section = new LinearLayout(context);
        section.setOrientation(LinearLayout.VERTICAL);
        section.setBackgroundColor(Color.WHITE);
        section.setPadding(12, 12, 12, 12);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 12);
        section.setLayoutParams(params);

        TextView label = new TextView(context);
        label.setText("Chip Seleccionado");
        label.setTextColor(Color.parseColor("#424242"));
        label.setTextSize(14);
        label.setTypeface(null, android.graphics.Typeface.BOLD);
        section.addView(label);

        chipNameTextView = new TextView(context);
        chipNameTextView.setText(currentChip != null ? currentChip.getNombreDelPic() : "Sin chip");
        chipNameTextView.setTextColor(Color.parseColor("#1976D2"));
        chipNameTextView.setTextSize(16);
        chipNameTextView.setTypeface(null, android.graphics.Typeface.BOLD);
        chipNameTextView.setPadding(0, 8, 0, 8);
        section.addView(chipNameTextView);

        chipInfoTextView = new TextView(context);
        chipInfoTextView.setBackgroundColor(Color.parseColor("#E3F2FD"));
        chipInfoTextView.setPadding(12, 12, 12, 12);
        chipInfoTextView.setTextSize(12);
        chipInfoTextView.setTypeface(android.graphics.Typeface.MONOSPACE);
        chipInfoTextView.setTextColor(Color.parseColor("#1976D2"));
        section.addView(chipInfoTextView);

        return section;
    }

    /** Crea la sección de edición de fusibles */
    private LinearLayout createFuseEditorSection() {
        LinearLayout section = new LinearLayout(context);
        section.setOrientation(LinearLayout.VERTICAL);
        section.setBackgroundColor(Color.WHITE);
        section.setPadding(12, 12, 12, 12);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 12);
        section.setLayoutParams(params);

        TextView label = new TextView(context);
        label.setText("Configuración de Fusibles");
        label.setTextColor(Color.parseColor("#424242"));
        label.setTextSize(16);
        label.setTypeface(null, android.graphics.Typeface.BOLD);
        label.setPadding(0, 0, 0, 12);
        section.addView(label);

        ScrollView fuseScrollView = new ScrollView(context);
        LinearLayout.LayoutParams scrollParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        (int) (context.getResources().getDisplayMetrics().heightPixels * 0.25));
        fuseScrollView.setLayoutParams(scrollParams);

        fuseContainer = new LinearLayout(context);
        fuseContainer.setOrientation(LinearLayout.VERTICAL);
        fuseScrollView.addView(fuseContainer);

        section.addView(fuseScrollView);

        return section;
    }

    /** Crea la sección de ID personalizado */
    private LinearLayout createCustomIdSection() {
        LinearLayout section = new LinearLayout(context);
        section.setOrientation(LinearLayout.VERTICAL);
        section.setBackgroundColor(Color.WHITE);
        section.setPadding(12, 12, 12, 12);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 12);
        section.setLayoutParams(params);

        TextView label = new TextView(context);
        label.setText("ID Personalizado (hex)");
        label.setTextColor(Color.parseColor("#424242"));
        label.setTextSize(14);
        label.setTypeface(null, android.graphics.Typeface.BOLD);
        section.addView(label);

        customIdEditText = new EditText(context);
        customIdEditText.setHint("Ejemplo: 00 01 02 03 04 05 06 07");
        customIdEditText.setInputType(InputType.TYPE_CLASS_TEXT);
        customIdEditText.setBackgroundColor(Color.parseColor("#F5F5F5"));
        customIdEditText.setPadding(12, 12, 12, 12);
        customIdEditText.setTextSize(12);
        customIdEditText.setTypeface(android.graphics.Typeface.MONOSPACE);
        section.addView(customIdEditText);

        return section;
    }

    /** Crea la sección de log */
    private LinearLayout createLogSection() {
        LinearLayout section = new LinearLayout(context);
        section.setOrientation(LinearLayout.VERTICAL);
        section.setBackgroundColor(Color.parseColor("#FAFAFA"));
        section.setPadding(12, 12, 12, 12);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        (int) (context.getResources().getDisplayMetrics().heightPixels * 0.15));
        params.setMargins(0, 0, 0, 12);
        section.setLayoutParams(params);

        TextView label = new TextView(context);
        label.setText("Log de Operaciones");
        label.setTextColor(Color.parseColor("#424242"));
        label.setTextSize(14);
        label.setTypeface(null, android.graphics.Typeface.BOLD);
        label.setPadding(0, 0, 0, 8);
        section.addView(label);

        logScrollView = new ScrollView(context);
        LinearLayout.LayoutParams scrollParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        logScrollView.setLayoutParams(scrollParams);

        logTextView = new TextView(context);
        logTextView.setTextSize(10);
        logTextView.setTypeface(android.graphics.Typeface.MONOSPACE);
        logTextView.setTextColor(Color.parseColor("#616161"));
        logTextView.setText("Log iniciado...\n");
        logScrollView.addView(logTextView);

        section.addView(logScrollView);

        return section;
    }

    /** Crea la barra de botones */
    private LinearLayout createButtonBar() {
        LinearLayout buttonBar = new LinearLayout(context);
        buttonBar.setOrientation(LinearLayout.VERTICAL);
        buttonBar.setBackgroundColor(Color.WHITE);
        buttonBar.setPadding(12, 12, 12, 12);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        buttonBar.setLayoutParams(params);

        // Primera fila: Restaurar desde PIC y HEX
        LinearLayout row1 = new LinearLayout(context);
        row1.setOrientation(LinearLayout.HORIZONTAL);

        btnRestoreFromChip = createButton("Restaurar desde PIC", "#FF9800");
        btnRestoreFromHex = createButton("Restaurar desde HEX", "#FF9800");

        LinearLayout.LayoutParams btnParams =
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        btnParams.setMargins(0, 0, 4, 0);
        btnRestoreFromChip.setLayoutParams(btnParams);

        btnParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        btnParams.setMargins(4, 0, 0, 0);
        btnRestoreFromHex.setLayoutParams(btnParams);

        row1.addView(btnRestoreFromChip);
        row1.addView(btnRestoreFromHex);
        buttonBar.addView(row1);

        // Espacio
        View space = new View(context);
        LinearLayout.LayoutParams spaceParams =
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 8);
        space.setLayoutParams(spaceParams);
        buttonBar.addView(space);

        // Segunda fila: Aplicar y Cancelar
        LinearLayout row2 = new LinearLayout(context);
        row2.setOrientation(LinearLayout.HORIZONTAL);

        btnApply = createButton("Aplicar Fuses", "#4CAF50");
        btnCancel = createButton("Cancelar", "#9E9E9E");

        btnParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        btnParams.setMargins(0, 0, 4, 0);
        btnApply.setLayoutParams(btnParams);

        btnParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        btnParams.setMargins(4, 0, 0, 0);
        btnCancel.setLayoutParams(btnParams);

        row2.addView(btnApply);
        row2.addView(btnCancel);
        buttonBar.addView(row2);

        // Configurar listeners
        setupButtonListeners();

        return buttonBar;
    }

    /** Crea un botón con estilo */
    private Button createButton(String text, String color) {
        Button button = new Button(context);
        button.setText(text);
        button.setTextColor(Color.WHITE);
        button.setBackgroundColor(Color.parseColor(color));
        button.setAllCaps(false);
        return button;
    }

    /** Configura los listeners de los botones */
    private void setupButtonListeners() {
        btnRestoreFromChip.setOnClickListener(v -> restoreFromChip());
        btnRestoreFromHex.setOnClickListener(v -> restoreFromHex());
        btnApply.setOnClickListener(v -> applyFuses());
        btnCancel.setOnClickListener(v -> cancel());
    }

    /** Carga los datos iniciales */
    private void loadInitialData() {
        if (currentChip == null || currentChipFuses == null) {
            logMessage("❌ Error: No hay chip seleccionado");
            return;
        }

        // Mostrar información del chip
        displayChipInfo();

        // Construir editor de fusibles
        buildFuseEditor();

        // Restaurar última configuración si existe
        if (lastConfiguration != null && !lastConfiguration.isEmpty()) {
            restoreConfiguration(lastConfiguration);
            logMessage("✓ Última configuración restaurada");
        } else {
            // Si no hay configuración previa, intentar cargar desde HEX
            restoreFromHex();
        }
    }

    /** Muestra la información del chip */
    private void displayChipInfo() {
        StringBuilder info = new StringBuilder();
        info.append("Chip: ").append(currentChip.getNombreDelPic()).append("\n");

        try {
            info.append("ROM: 0x")
                    .append(Integer.toHexString(currentChip.getTamanoROM()).toUpperCase())
                    .append("\n");

            if (currentChip.isTamanoValidoDeEEPROM()) {
                info.append("EEPROM: 0x")
                        .append(Integer.toHexString(currentChip.getTamanoEEPROM()).toUpperCase())
                        .append("\n");
            }
        } catch (Exception e) {
            info.append("Error obteniendo info: ").append(e.getMessage());
        }

        chipInfoTextView.setText(info.toString());
    }

    /** Construye el editor de fusibles dinámicamente */
    @SuppressWarnings("unchecked")
    private void buildFuseEditor() {
        fuseContainer.removeAllViews();
        fuseSpinners.clear();

        Map<String, Map<String, List<Integer>>> fuses =
                (Map<String, Map<String, List<Integer>>>) currentChipFuses.getVar("fuses");

        if (fuses == null || fuses.isEmpty()) {
            TextView empty = new TextView(context);
            empty.setText("Este chip no tiene fusibles configurables");
            empty.setTextColor(Color.parseColor("#757575"));
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(32, 32, 32, 32);
            fuseContainer.addView(empty);
            logMessage("⚠ Este chip no tiene fusibles configurables");
            return;
        }

        for (Map.Entry<String, Map<String, List<Integer>>> entry : fuses.entrySet()) {
            String fuseName = entry.getKey();
            Map<String, List<Integer>> fuseOptions = entry.getValue();
            addFuseRow(fuseName, new ArrayList<>(fuseOptions.keySet()));
        }

        logMessage("✓ Editor construido: " + fuses.size() + " fusibles");
    }

    /** Agrega una fila de fusible al editor */
    private void addFuseRow(String fuseName, List<String> options) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(8, 8, 8, 8);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 8);
        row.setLayoutParams(params);

        // Label
        TextView label = new TextView(context);
        label.setText(fuseName + ":");
        label.setTextSize(14);
        label.setTextColor(Color.parseColor("#424242"));

        LinearLayout.LayoutParams labelParams =
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        label.setLayoutParams(labelParams);
        row.addView(label);

        // Spinner
        Spinner spinner = new Spinner(context);
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        LinearLayout.LayoutParams spinnerParams =
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.5f);
        spinner.setLayoutParams(spinnerParams);
        row.addView(spinner);

        fuseSpinners.put(fuseName, spinner);
        fuseContainer.addView(row);
    }

    /** Restaura fusibles desde los datos del chip (base de datos) */
    private void restoreFromChip() {
        try {
            logMessage("⏳ Restaurando fusibles desde datos del chip...");

            // Obtener configuración por defecto del chip desde la base de datos
            // Aquí podríamos leer los valores por defecto si existieran en ChipinfoEntry

            // Por ahora, establecer en el primer valor disponible
            for (Map.Entry<String, Spinner> entry : fuseSpinners.entrySet()) {
                Spinner spinner = entry.getValue();
                if (spinner.getCount() > 0) {
                    spinner.setSelection(0);
                }
            }

            customIdEditText.setText("");
            currentIDData = new byte[] {0};

            logMessage("✓ Fusibles restaurados desde datos del chip");
            Toast.makeText(context, "Fusibles restaurados desde chip", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            logMessage("❌ Error restaurando desde chip: " + e.getMessage());
            Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /** Restaura fusibles desde el archivo HEX cargado */
    private void restoreFromHex() {
        try {
            if (hexData == null) {
                logMessage("⚠ No hay datos HEX cargados");
                Toast.makeText(context, "No hay archivo HEX cargado", Toast.LENGTH_SHORT).show();
                return;
            }

            logMessage("⏳ Restaurando fusibles desde archivo HEX...");

            // Obtener fuses del HEX
            int[] hexFuses = hexData.obtenerValoresIntHexFusesPocesado();
            byte[] hexID = hexData.obtenerVsloresBytesHexIDPocesado();

            if (hexFuses == null || hexFuses.length == 0) {
                logMessage("⚠ El archivo HEX no contiene datos de fusibles");
                Toast.makeText(context, "El HEX no contiene fusibles", Toast.LENGTH_SHORT).show();
                return;
            }

            // Convertir a List<Integer>
            List<Integer> fuseList = new ArrayList<>();
            for (int fuse : hexFuses) {
                fuseList.add(fuse);
            }

            // Decodificar fusibles usando ChipinfoEntry
            Map<String, String> decodedFuses = currentChipFuses.decodeFuseData(fuseList);

            // Aplicar configuración decodificada
            restoreConfiguration(decodedFuses);

            // Actualizar ID
            if (hexID != null && hexID.length > 0) {
                currentIDData = hexID;
                StringBuilder idStr = new StringBuilder();
                for (byte b : hexID) {
                    idStr.append(String.format("%02X ", b & 0xFF));
                }
                customIdEditText.setText(idStr.toString().trim());
            }

            logMessage("✓ Fusibles restaurados desde HEX");
            logMessage("  Fuses: " + decodedFuses.toString());
            Toast.makeText(context, "Fusibles restaurados desde HEX", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            logMessage("❌ Error restaurando desde HEX: " + e.getMessage());
            Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /** Restaura una configuración de fusibles */
    private void restoreConfiguration(Map<String, String> config) {
        for (Map.Entry<String, String> entry : config.entrySet()) {
            String fuseName = entry.getKey();
            String fuseValue = entry.getValue();

            Spinner spinner = fuseSpinners.get(fuseName);
            if (spinner != null) {
                ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinner.getAdapter();
                for (int i = 0; i < adapter.getCount(); i++) {
                    if (adapter.getItem(i).equals(fuseValue)) {
                        spinner.setSelection(i);
                        break;
                    }
                }
            }
        }
    }

    /** Aplica los fusibles configurados */
    private void applyFuses() {
        try {
            logMessage("⏳ Aplicando configuración de fusibles...");

            // Recopilar configuración actual
            Map<String, String> fuseConfig = new HashMap<>();
            for (Map.Entry<String, Spinner> entry : fuseSpinners.entrySet()) {
                String fuseName = entry.getKey();
                String fuseValue = (String) entry.getValue().getSelectedItem();
                if (fuseValue != null) {
                    fuseConfig.put(fuseName, fuseValue);
                }
            }

            // Codificar fusibles
            List<Integer> encodedFuses = currentChipFuses.encodeFuseData(fuseConfig);

            // Procesar ID personalizado
            byte[] idData = parseCustomId();

            // Log de resultados
            StringBuilder result = new StringBuilder();
            result.append("✅ Configuración aplicada exitosamente\n");
            result.append("Fusibles configurados:\n");
            for (Map.Entry<String, String> e : fuseConfig.entrySet()) {
                result.append("  • ")
                        .append(e.getKey())
                        .append(" = ")
                        .append(e.getValue())
                        .append("\n");
            }
            result.append("\nValores hex: ");
            for (Integer val : encodedFuses) {
                result.append(String.format("0x%04X ", val));
            }

            logMessage(result.toString());

            // Notificar al listener
            if (listener != null) {
                listener.onFusesApplied(encodedFuses, idData, fuseConfig);
            }

            Toast.makeText(context, "Fusibles aplicados correctamente", Toast.LENGTH_SHORT).show();
            dialog.dismiss();

        } catch (Exception e) {
            logMessage("❌ Error aplicando fusibles: " + e.getMessage());
            Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /** Parsea el ID personalizado ingresado */
    private byte[] parseCustomId() {
        String idText = customIdEditText.getText().toString().trim();

        if (idText.isEmpty()) {
            return currentIDData != null ? currentIDData : new byte[] {0};
        }

        try {
            String[] bytes = idText.split("\\s+");
            byte[] result = new byte[bytes.length];

            for (int i = 0; i < bytes.length; i++) {
                result[i] = (byte) Integer.parseInt(bytes[i], 16);
            }

            return result;
        } catch (Exception e) {
            logMessage("⚠ Error parseando ID personalizado, usando ID anterior");
            return currentIDData != null ? currentIDData : new byte[] {0};
        }
    }

    /** Cancela y cierra el diálogo */
    private void cancel() {
        if (listener != null) {
            listener.onFusesCancelled();
        }
        dialog.dismiss();
    }

    /** Agrega un mensaje al log */
    private void logMessage(String message) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        String timestamp = sdf.format(new Date());
        String logEntry = "[" + timestamp + "] " + message + "\n";

        logTextView.append(logEntry);

        // Auto-scroll
        logScrollView.post(() -> logScrollView.fullScroll(View.FOCUS_DOWN));
    }

    /** Cierra el diálogo */
    public void dismiss() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }
}
