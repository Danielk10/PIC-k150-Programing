package com.diamon.managers;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;
import com.diamon.pic.R;

/**
 * Gestor de visualizacion de memoria - VERSION FINAL CORREGIDA
 * - Direcciones en DORADO (#FFD700)
 * - Datos cargados en VERDE (#4CAF50)
 * - Datos vacios (FF, 3FFF, FFFF) en ROJO (#F44336)
 * - ROM y EEPROM en UN popup
 * - ScrollView funcional
 * - NO se sale de pantalla
 */
public class MemoryDisplayManager {

    private final Context context;
    private PopupWindow memoryPopup;

    public MemoryDisplayManager(Context context) {
        this.context = context;
    }

    /**
     * Muestra ROM y EEPROM en UN popup con colores correctos
     */
    public void showMemoryDataPopup(String romData, int romSize, String eepromData, int eepromSize, boolean hasEeprom) {
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);

        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setCornerRadius(40f);
        shape.setColor(Color.WHITE);
        container.setBackground(shape);
        container.setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20));

        // Titulo
        TextView title = new TextView(context);
        title.setText(context.getString(R.string.datos_de_memoria));
        title.setTextColor(Color.BLACK);
        title.setTextSize(20);
        title.setTypeface(null, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, dpToPx(16));
        container.addView(title);

        // Etiqueta ROM
        TextView romLabel = new TextView(context);
        romLabel.setText(context.getString(R.string.memoria_rom));
        romLabel.setTextColor(Color.parseColor("#4CAF50"));
        romLabel.setTextSize(16);
        romLabel.setTypeface(null, Typeface.BOLD);
        romLabel.setPadding(0, 0, 0, dpToPx(8));
        container.addView(romLabel);

        // ScrollView ROM
        ScrollView romScrollView = new ScrollView(context);
        LinearLayout.LayoutParams romScrollParams = new LinearLayout.LayoutParams(
                dpToPx(320),
                dpToPx(hasEeprom ? 180 : 350)
        );
        romScrollView.setLayoutParams(romScrollParams);

        LinearLayout romContainer = new LinearLayout(context);
        romContainer.setOrientation(LinearLayout.VERTICAL);
        romContainer.setBackgroundColor(Color.BLACK);
        romContainer.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));

        GradientDrawable romBg = new GradientDrawable();
        romBg.setColor(Color.BLACK);
        romBg.setCornerRadius(8f);
        romContainer.setBackground(romBg);

        displayDataWithColors(romContainer, romData != null ? romData : "", 4, 8, true, romSize);
        romScrollView.addView(romContainer);
        container.addView(romScrollView);

        // EEPROM si existe
        if (hasEeprom) {
            TextView eepromLabel = new TextView(context);
            eepromLabel.setText(context.getString(R.string.memoria_eeprom));
            eepromLabel.setTextColor(Color.parseColor("#4CAF50"));
            eepromLabel.setTextSize(16);
            eepromLabel.setTypeface(null, Typeface.BOLD);
            eepromLabel.setPadding(0, dpToPx(12), 0, dpToPx(8));
            container.addView(eepromLabel);

            ScrollView eepromScrollView = new ScrollView(context);
            LinearLayout.LayoutParams eepromScrollParams = new LinearLayout.LayoutParams(
                    dpToPx(320),
                    dpToPx(150)
            );
            eepromScrollView.setLayoutParams(eepromScrollParams);

            LinearLayout eepromContainer = new LinearLayout(context);
            eepromContainer.setOrientation(LinearLayout.VERTICAL);
            eepromContainer.setBackgroundColor(Color.BLACK);
            eepromContainer.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));

            GradientDrawable eepromBg = new GradientDrawable();
            eepromBg.setColor(Color.BLACK);
            eepromBg.setCornerRadius(8f);
            eepromContainer.setBackground(eepromBg);
            displayDataWithColors(eepromContainer, eepromData != null ? eepromData : "", 2, 8, false, eepromSize);
eepromScrollView.addView(eepromContainer);
            container.addView(eepromScrollView);
        }

        // Boton cerrar
        TextView closeButton = new TextView(context);
        closeButton.setText(context.getString(R.string.cerrar));
        closeButton.setTextColor(Color.WHITE);
        closeButton.setTextSize(16);
        closeButton.setGravity(Gravity.CENTER);
        closeButton.setPadding(dpToPx(50), dpToPx(12), dpToPx(50), dpToPx(12));

        GradientDrawable buttonBg = new GradientDrawable();
        buttonBg.setShape(GradientDrawable.RECTANGLE);
        buttonBg.setColor(Color.parseColor("#2196F3"));
        buttonBg.setCornerRadius(dpToPx(8));
        closeButton.setBackground(buttonBg);

        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        buttonParams.gravity = Gravity.CENTER;
        buttonParams.setMargins(0, dpToPx(16), 0, 0);
        closeButton.setLayoutParams(buttonParams);
        container.addView(closeButton);

        // Crear popup
        if (memoryPopup != null && memoryPopup.isShowing()) {
            memoryPopup.dismiss();
        }

        memoryPopup = new PopupWindow(
                container,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                true
        );

        memoryPopup.setOutsideTouchable(true);
        memoryPopup.setFocusable(true);
        closeButton.setOnClickListener(v -> memoryPopup.dismiss());

        memoryPopup.showAtLocation(
                ((android.app.Activity) context).findViewById(android.R.id.content),
                Gravity.CENTER,
                0,
                0
        );
    }

    /**
     * Muestra datos con colores:
     * - DORADO: Direcciones
     * - VERDE: Datos cargados
     * - ROJO: Datos vacios (FF, 3FFF, FFFF)
     */
    private void displayDataWithColors(LinearLayout container, String data, int groupSize, int columns, boolean isROM, int memorySize) {
      int address = 0;

        for (int i = 0; i < data.length(); i += groupSize * columns) {
            StringBuilder rowText = new StringBuilder();

            // Direccion
            String addressHex = String.format("%04X", address);
            rowText.append(addressHex).append(": ");

            // Datos
            for (int j = 0; j < columns; j++) {
                int start = i + j * groupSize;
                int end = Math.min(start + groupSize, data.length());

                if (start < data.length()) {
                    rowText.append(data.substring(start, end)).append(" ");
                }
            }

            // Crear TextView con SpannableString para colores
            TextView rowTextView = createColoredRow(rowText.toString(), addressHex, groupSize, isROM);
            container.addView(rowTextView);

            address += 8;
        }

        // Agregar filas vacias en ROJO hasta el tamano real de memoria
        int totalDataRows = (int) Math.ceil(data.length() / (double) (groupSize * columns));
        int totalMemoryRows = (int) Math.ceil(memorySize / (double) columns);

        for (int i = totalDataRows; i < totalMemoryRows; i++) {
            String addressHex = String.format("%04X", address);
            StringBuilder emptyRow = new StringBuilder();
            emptyRow.append(addressHex).append(": ");

            for (int j = 0; j < columns; j++) {
                for (int k = 0; k < groupSize; k++) {
                    emptyRow.append("F");
                }
                emptyRow.append(" ");
            }

            TextView emptyTextView = createColoredRow(emptyRow.toString(), addressHex, groupSize, isROM);
            container.addView(emptyTextView);

            address += 8;
        }

    }

    /**
     * Crea fila con colores: DORADO (dir), VERDE (datos), ROJO (vacios)
     */
    private TextView createColoredRow(String fullText, String address, int groupSize, boolean isROM) {
        TextView textView = new TextView(context);
        SpannableString spannableString = new SpannableString(fullText);

        // Color DORADO para direccion (0000: )
        int dirEnd = address.length() + 2; // "0000: "
        spannableString.setSpan(
                new ForegroundColorSpan(Color.parseColor("#FFD700")), // DORADO
                0,
                dirEnd,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        // Colores para datos
        int dataStart = dirEnd;
        String dataSection = fullText.substring(dataStart);
        String[] groups = dataSection.trim().split("\\s+");

        int currentPos = dataStart;
        for (String group : groups) {
            if (group.isEmpty()) continue;

            // Detectar si es dato vacio (FF, 3FFF, FFFF)
            boolean isEmpty = isEmptyData(group, isROM);
            int color = isEmpty ? Color.parseColor("#F44336") : Color.parseColor("#4CAF50"); // ROJO : VERDE

            int groupEnd = currentPos + group.length();
            if (groupEnd <= fullText.length()) {
                spannableString.setSpan(
                        new ForegroundColorSpan(color),
                        currentPos,
                        groupEnd,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            }
            currentPos = groupEnd + 1; // +1 para el espacio
        }

        textView.setText(spannableString);
        textView.setTextSize(11);
        textView.setTypeface(Typeface.MONOSPACE);
        textView.setPadding(4, 2, 4, 2);

        return textView;
    }

    /**
     * Detecta si un dato es vacio (FF, 3FFF, FFFF)
     */
    private boolean isEmptyData(String data, boolean isROM) {
        if (data == null || data.isEmpty()) return false;

        // Normalizar a mayusculas
        String upper = data.toUpperCase();

        if (isROM) {
            // ROM: 3FFF o FFFF son vacios
            return upper.equals("3FFF") || upper.equals("FFFF");
        } else {
            // EEPROM: FF es vacio
            return upper.equals("FF");
        }
    }

    public void dismissAllPopups() {
        if (memoryPopup != null && memoryPopup.isShowing()) {
            memoryPopup.dismiss();
        }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }
}
