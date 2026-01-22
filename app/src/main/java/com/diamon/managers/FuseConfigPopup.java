package com.diamon.managers;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
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
 * FuseConfigPopup - Gestor del PopupWindow para configuracion de fusibles.
 *
 * <p>
 * VERSION REDISENADA: - Layout dinamico que se ajusta automaticamente -
 * Fusibles crecen/encojen
 * segun cantidad - Log simplificado sin scroll (solo mostrara ultimos mensajes)
 * - Componentes
 * fluyen naturalmente - Scroll SOLO en el ScrollView principal
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
    private EditText customIdEditText;
    private Button btnApply;
    private Button btnRestoreFromChip;
    private Button btnRestoreFromHex;
    private Button btnCancel;
    private Button btnAddCustomFuse;

    // Datos
    private Map<String, Spinner> fuseSpinners = new HashMap<>();
    private Map<String, String> customFuses = new HashMap<>();
    private Map<String, String> lastConfiguration;
    private byte[] currentIDData = new byte[] { 0 };

    public FuseConfigPopup(Context context, FuseConfigListener listener) {
        this.context = context;
        this.listener = listener;
    }

    /** Muestra el popup de configuracion de fusibles */
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

    /** Crea y configura el dialogo - DISENO SIMPLIFICADO */
    private void createDialog() {
        dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        // Layout principal (VERTICAL)
        LinearLayout mainLayout = new LinearLayout(context);
        mainLayout.setOrientation(LinearLayout.VERTICAL);

        // Estilo Premium: Fondo oscuro y bordes redondeados
        android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
        shape.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        shape.setCornerRadius(dpToPx(16));
        shape.setColor(Color.parseColor("#505060"));
        shape.setStroke(dpToPx(2), Color.parseColor("#3A3A4E"));
        mainLayout.setBackground(shape);
        mainLayout.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));

        // ========== TITULO (FIJO ARRIBA) ==========
        TextView titleView = createTitle();
        mainLayout.addView(titleView);

        // ========== SCROLL PRINCIPAL (WEIGHT=1) - TODO FLUYE AQUI ==========
        ScrollView mainScrollView = new ScrollView(context);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0);
        scrollParams.weight = 1;
        mainScrollView.setLayoutParams(scrollParams);

        LinearLayout scrollContent = new LinearLayout(context);
        scrollContent.setOrientation(LinearLayout.VERTICAL);
        scrollContent.setPadding(0, dpToPx(8), 0, dpToPx(8));

        // Informacion del chip
        scrollContent.addView(createChipInfoSection());

        // Editor de fusibles (SIN ScrollView interno, solo crece/encoge)
        scrollContent.addView(createFuseEditorSection());

        // Boton agregar fusible personalizado
        scrollContent.addView(createAddCustomFuseButton());

        // ID personalizado
        scrollContent.addView(createCustomIdSection());

        // Log simplificado (SIN ScrollView, texto que se trunca)
        scrollContent.addView(createLogSection());

        mainScrollView.addView(scrollContent);
        mainLayout.addView(mainScrollView);

        // ========== BOTONES (FIJOS ABAJO) ==========
        mainLayout.addView(createButtonBar());

        // Configurar dialogo
        dialog.setContentView(mainLayout);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            window.setElevation(24f);
            window.setLayout(
                    (int) (context.getResources().getDisplayMetrics().widthPixels * 0.95),
                    (int) (context.getResources().getDisplayMetrics().heightPixels * 0.90));
        }

        // Animación de entrada
        applyShowAnimation(mainLayout);

        // Cargar datos iniciales
        loadInitialData();
    }

    private void applyShowAnimation(View view) {
        view.setScaleY(0);
        view.setPivotY(0);

        android.animation.ValueAnimator animator = android.animation.ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(800);
        animator.setInterpolator(new android.view.animation.BounceInterpolator());
        animator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            view.setScaleY(value);
        });
        animator.start();
    }

    private int dpToPx(int dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }

    /** Crea el titulo del dialogo */
    private TextView createTitle() {
        TextView title = new TextView(context);
        title.setText("Configuraciones de Fusibles");
        title.setTextColor(Color.WHITE);
        title.setTextSize(20);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, dpToPx(12));
        title.setLayoutParams(
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return title;
    }

    /** Crea la seccion de informacion del chip */
    private LinearLayout createChipInfoSection() {
        LinearLayout section = new LinearLayout(context);
        section.setOrientation(LinearLayout.VERTICAL);

        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(Color.parseColor("#2A2A3E"));
        bg.setCornerRadius(dpToPx(8));
        section.setBackground(bg);
        section.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
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

    /**
     * CORREGIDO: Crea la seccion de edicion de fusibles SIN ScrollView interno Se
     * ajusta
     * dinamicamente segun cantidad de fusibles
     */
    private LinearLayout createFuseEditorSection() {
        LinearLayout section = new LinearLayout(context);
        section.setOrientation(LinearLayout.VERTICAL);

        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(Color.parseColor("#2A2A3E"));
        bg.setCornerRadius(dpToPx(8));
        section.setBackground(bg);
        section.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT // SE AJUSTA SEGUN CONTENIDO
        );
        params.setMargins(0, 0, 0, 12);
        section.setLayoutParams(params);

        TextView label = new TextView(context);
        label.setText("Configuracion de Fusibles");
        label.setTextColor(Color.parseColor("#424242"));
        label.setTextSize(16);
        label.setTypeface(null, android.graphics.Typeface.BOLD);
        label.setPadding(0, 0, 0, 12);
        section.addView(label);

        // Container de fusibles SIN ScrollView - se ajusta naturalmente
        fuseContainer = new LinearLayout(context);
        fuseContainer.setOrientation(LinearLayout.VERTICAL);
        fuseContainer.setPadding(8, 8, 8, 8);
        fuseContainer.setBackgroundColor(Color.parseColor("#FAFAFA"));

        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT // SE AJUSTA SEGUN CONTENIDO
        );
        fuseContainer.setLayoutParams(containerParams);

        section.addView(fuseContainer);
        return section;
    }

    /** Crea el boton para agregar fusibles personalizados */
    private Button createAddCustomFuseButton() {
        btnAddCustomFuse = new Button(context);
        btnAddCustomFuse.setText("➕ Agregar Fusible Personalizado");
        btnAddCustomFuse.setTextColor(Color.WHITE);
        btnAddCustomFuse.setBackgroundColor(Color.parseColor("#FF9800"));
        btnAddCustomFuse.setAllCaps(false);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 12);
        btnAddCustomFuse.setLayoutParams(params);

        btnAddCustomFuse.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showCustomFuseDialog();
                    }
                });

        return btnAddCustomFuse;
    }

    /** Muestra dialogo para agregar fusible personalizado */
    private void showCustomFuseDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Agregar Fusible Personalizado");

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText nameInput = new EditText(context);
        nameInput.setHint("Nombre del fusible");
        layout.addView(nameInput);

        final EditText valueInput = new EditText(context);
        valueInput.setHint("Valor (ejemplo: ON, OFF, 0x3FFF)");
        layout.addView(valueInput);

        builder.setView(layout);

        builder.setPositiveButton(
                "Agregar",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String name = nameInput.getText().toString().trim();
                        String value = valueInput.getText().toString().trim();

                        if (!name.isEmpty() && !value.isEmpty()) {
                            customFuses.put(name, value);
                            List<String> valueList = new ArrayList<>();
                            valueList.add(value);
                            addFuseRow(name, valueList);
                            logMessage("✓ Fusible personalizado agregado: " + name + " = " + value);
                            Toast.makeText(context, "Fusible agregado", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(context, "Completa todos los campos", Toast.LENGTH_SHORT)
                                    .show();
                        }
                    }
                });

        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    /** Crea la seccion de ID personalizado */
    private LinearLayout createCustomIdSection() {
        LinearLayout section = new LinearLayout(context);
        section.setOrientation(LinearLayout.VERTICAL);

        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(Color.parseColor("#2A2A3E"));
        bg.setCornerRadius(dpToPx(8));
        section.setBackground(bg);
        section.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
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

    /**
     * SIMPLIFICADO: Crea la seccion de log SIN ScrollView Solo muestra ultimos 5
     * mensajes (trunca
     * automaticamente)
     */
    private LinearLayout createLogSection() {
        LinearLayout section = new LinearLayout(context);
        section.setOrientation(LinearLayout.VERTICAL);

        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(Color.parseColor("#2A2A3E"));
        bg.setCornerRadius(dpToPx(8));
        section.setBackground(bg);
        section.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 12);
        section.setLayoutParams(params);

        TextView label = new TextView(context);
        label.setText("Log de Operaciones");
        label.setTextColor(Color.parseColor("#424242"));
        label.setTextSize(14);
        label.setTypeface(null, android.graphics.Typeface.BOLD);
        label.setPadding(0, 0, 0, 8);
        section.addView(label);

        logTextView = new TextView(context);
        logTextView.setTextSize(10);
        logTextView.setTypeface(android.graphics.Typeface.MONOSPACE);
        logTextView.setTextColor(Color.parseColor("#616161"));
        logTextView.setText("Log iniciado...\n");
        logTextView.setPadding(8, 8, 8, 8);
        logTextView.setBackgroundColor(Color.WHITE);

        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        logTextView.setLayoutParams(textParams);
        logTextView.setMaxLines(5); // Solo muestra maximo 5 lineas
        logTextView.setEllipsize(android.text.TextUtils.TruncateAt.END);

        section.addView(logTextView);
        return section;
    }

    /** Crea la barra de botones */
    private LinearLayout createButtonBar() {
        LinearLayout buttonBar = new LinearLayout(context);
        buttonBar.setOrientation(LinearLayout.VERTICAL);
        buttonBar.setPadding(0, dpToPx(12), 0, 0);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        buttonBar.setLayoutParams(params);

        // Primera fila: Restaurar desde PIC y HEX
        LinearLayout row1 = new LinearLayout(context);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setLayoutParams(
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        btnRestoreFromChip = createButton("Restaurar desde PIC", "#FF9800");
        btnRestoreFromHex = createButton("Restaurar desde HEX", "#FF9800");

        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT,
                1.0f);
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
        LinearLayout.LayoutParams spaceParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 8);
        space.setLayoutParams(spaceParams);
        buttonBar.addView(space);

        // Segunda fila: Aplicar y Cancelar
        LinearLayout row2 = new LinearLayout(context);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.setLayoutParams(
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

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

        setupButtonListeners();
        return buttonBar;
    }

    /** Crea un boton con estilo */
    private Button createButton(String text, String color) {
        Button button = new Button(context);
        button.setText(text);
        button.setTextColor(Color.WHITE);

        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(Color.parseColor(color));
        bg.setCornerRadius(dpToPx(8));
        button.setBackground(bg);

        button.setAllCaps(false);
        return button;
    }

    /** Configura los listeners de los botones */
    private void setupButtonListeners() {
        btnRestoreFromChip.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        restoreFromChip();
                    }
                });

        btnRestoreFromHex.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        restoreFromHex();
                    }
                });

        btnApply.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        applyFuses();
                    }
                });

        btnCancel.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        cancel();
                    }
                });
    }

    /** Carga los datos iniciales */
    private void loadInitialData() {
        if (currentChip == null || currentChipFuses == null) {
            logMessage("❌ Error: No hay chip seleccionado");
            return;
        }

        displayChipInfo();
        buildFuseEditor();
        validateHexCompatibility();

        if (lastConfiguration != null && !lastConfiguration.isEmpty()) {
            restoreConfiguration(lastConfiguration);
            logMessage("✓ Ultima configuracion restaurada");
        } else {
            if (hexData != null) {
                restoreFromHex();
            }
        }
    }

    /** Valida compatibilidad del HEX con el chip seleccionado */
    private void validateHexCompatibility() {
        if (hexData == null) {
            logMessage("⚠ No hay archivo HEX cargado");
            return;
        }

        try {
            int[] hexFuses = hexData.obtenerValoresIntHexFusesPocesado();

            if (hexFuses == null || hexFuses.length == 0) {
                logMessage("⚠ El HEX no contiene fusibles");
                return;
            }

            @SuppressWarnings("unchecked")
            Map<String, Map<String, List<Integer>>> chipFuses = (Map<String, Map<String, List<Integer>>>) currentChipFuses
                    .getVar("fuses");

            if (chipFuses != null) {
                int expectedFuseCount = chipFuses.size();

                if (hexFuses.length != expectedFuseCount) {
                    logMessage("⚠️ ADVERTENCIA: Cantidad de fusibles no coincide");
                    logMessage("  HEX: " + hexFuses.length + " fusibles");
                    logMessage("  Chip espera: " + expectedFuseCount);
                } else {
                    logMessage("✓ HEX compatible");
                }
            }

        } catch (Exception e) {
            logMessage("⚠ Error verificando HEX: " + e.getMessage());
        }
    }

    /** Muestra la informacion del chip */
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
            info.append("Error: ").append(e.getMessage());
        }

        chipInfoTextView.setText(info.toString());
    }

    /** Construye el editor de fusibles dinamicamente */
    @SuppressWarnings("unchecked")
    private void buildFuseEditor() {
        fuseContainer.removeAllViews();
        fuseSpinners.clear();
        customFuses.clear();

        Map<String, Map<String, List<Integer>>> fuses = (Map<String, Map<String, List<Integer>>>) currentChipFuses
                .getVar("fuses");

        if (fuses == null || fuses.isEmpty()) {
            TextView empty = new TextView(context);
            empty.setText("Este chip no tiene fusibles configurables");
            empty.setTextColor(Color.parseColor("#9E9E9E"));
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));
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

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 8);
        row.setLayoutParams(params);

        // Label
        TextView label = new TextView(context);
        label.setText(fuseName + ":");
        label.setTextSize(14);
        label.setTextColor(Color.parseColor("#424242"));

        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT,
                1.0f);
        label.setLayoutParams(labelParams);
        row.addView(label);

        // Spinner
        Spinner spinner = new Spinner(context);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        LinearLayout.LayoutParams spinnerParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT,
                1.5f);
        spinner.setLayoutParams(spinnerParams);
        row.addView(spinner);

        fuseSpinners.put(fuseName, spinner);
        fuseContainer.addView(row);
    }

    /** Restaura fusibles desde los datos del chip */
    private void restoreFromChip() {
        try {
            logMessage("⏳ Restaurando desde chip...");

            for (Map.Entry<String, Spinner> entry : fuseSpinners.entrySet()) {
                Spinner spinner = entry.getValue();
                if (spinner.getCount() > 0) {
                    spinner.setSelection(0);
                }
            }

            customIdEditText.setText("");
            currentIDData = new byte[] { 0 };

            logMessage("✓ Restaurado desde chip");
            Toast.makeText(context, "Fusibles restaurados", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            logMessage("❌ Error: " + e.getMessage());
        }
    }

    /** Restaura fusibles desde el archivo HEX */
    private void restoreFromHex() {
        try {
            if (hexData == null) {
                logMessage("⚠ No hay HEX cargado");
                return;
            }

            logMessage("⏳ Restaurando desde HEX...");

            int[] hexFuses = hexData.obtenerValoresIntHexFusesPocesado();
            byte[] hexID = hexData.obtenerVsloresBytesHexIDPocesado();

            if (hexFuses == null || hexFuses.length == 0) {
                logMessage("⚠ HEX sin fusibles");
                return;
            }

            List<Integer> fuseList = new ArrayList<>();
            for (int fuse : hexFuses) {
                fuseList.add(fuse);
            }

            Map<String, String> decodedFuses = currentChipFuses.decodeFuseData(fuseList);
            restoreConfiguration(decodedFuses);

            if (hexID != null && hexID.length > 0) {
                currentIDData = hexID;
                StringBuilder idStr = new StringBuilder();
                for (byte b : hexID) {
                    idStr.append(String.format("%02X ", b & 0xFF));
                }
                customIdEditText.setText(idStr.toString().trim());
            }

            logMessage("✓ Restaurado desde HEX");
            Toast.makeText(context, "Fusibles restaurados desde HEX", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            logMessage("❌ Error: " + e.getMessage());
        }
    }

    /** Restaura una configuracion de fusibles */
    private void restoreConfiguration(Map<String, String> config) {
        for (Map.Entry<String, String> entry : config.entrySet()) {
            String fuseName = entry.getKey();
            String fuseValue = entry.getValue();

            Spinner spinner = fuseSpinners.get(fuseName);
            if (spinner != null) {
                android.widget.SpinnerAdapter adapter = spinner.getAdapter();
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
            logMessage("⏳ Aplicando...");

            Map<String, String> fuseConfig = new HashMap<>();
            for (Map.Entry<String, Spinner> entry : fuseSpinners.entrySet()) {
                String fuseName = entry.getKey();
                String fuseValue = (String) entry.getValue().getSelectedItem();
                if (fuseValue != null) {
                    fuseConfig.put(fuseName, fuseValue);
                }
            }

            fuseConfig.putAll(customFuses);

            List<Integer> encodedFuses = currentChipFuses.encodeFuseData(fuseConfig);
            byte[] idData = parseCustomId();

            logMessage("✅ Aplicado exitosamente");

            if (listener != null) {
                listener.onFusesApplied(encodedFuses, idData, fuseConfig);
            }

            Toast.makeText(context, "Fusibles aplicados", Toast.LENGTH_SHORT).show();
            dialog.dismiss();

        } catch (Exception e) {
            logMessage("❌ Error: " + e.getMessage());
        }
    }

    /** Parsea el ID personalizado */
    private byte[] parseCustomId() {
        String idText = customIdEditText.getText().toString().trim();

        if (idText.isEmpty()) {
            return currentIDData != null ? currentIDData : new byte[] { 0 };
        }

        try {
            String[] bytes = idText.split("\\s+");
            byte[] result = new byte[bytes.length];

            for (int i = 0; i < bytes.length; i++) {
                result[i] = (byte) Integer.parseInt(bytes[i], 16);
            }

            return result;
        } catch (Exception e) {
            logMessage("⚠ Error parseando ID");
            return currentIDData != null ? currentIDData : new byte[] { 0 };
        }
    }

    /** Cancela y cierra el dialogo */
    private void cancel() {
        if (listener != null) {
            listener.onFusesCancelled();
        }
        dialog.dismiss();
    }

    /** Agrega un mensaje al log (truncado a 5 lineas) */
    private void logMessage(String message) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        String timestamp = sdf.format(new Date());
        String logEntry = "[" + timestamp + "] " + message;

        String currentText = logTextView.getText().toString();
        String[] lines = currentText.split("\n");

        // Mantener solo ultimas 4 lineas + nueva
        StringBuilder newText = new StringBuilder();
        int start = Math.max(0, lines.length - 4);
        for (int i = start; i < lines.length; i++) {
            if (!lines[i].isEmpty()) {
                newText.append(lines[i]).append("\n");
            }
        }
        newText.append(logEntry).append("\n");

        logTextView.setText(newText.toString());
    }

    /** Cierra el dialogo */
    public void dismiss() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }
}
