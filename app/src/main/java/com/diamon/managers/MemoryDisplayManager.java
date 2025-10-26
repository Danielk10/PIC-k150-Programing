package com.diamon.managers;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Gestor de visualizacion de memoria COMPLETAMENTE CORREGIDO
 * - ROM y EEPROM en UN popup
 * - Verde para datos, BLANCO para vacio (diferencia clara)
 * - ScrollView funcional en ambos
 * - NO se sale de pantalla
 */
public class MemoryDisplayManager {

    private final Context context;
    private PopupWindow memoryPopup;

    public MemoryDisplayManager(Context context) {
        this.context = context;
    }

    /**
     * Muestra ROM y EEPROM en UN popup - EXACTO como el original
     */
    public void showMemoryDataPopup(String romData, int romSize, String eepromData, int eepromSize, boolean hasEeprom) {
        // Contenedor principal
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
        title.setText("Datos de Memoria");
        title.setTextColor(Color.BLACK);
        title.setTextSize(20);
        title.setTypeface(null, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, dpToPx(16));
        container.addView(title);

        // Etiqueta ROM
        TextView romLabel = new TextView(context);
        romLabel.setText("Memoria ROM");
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

        // Contenedor ROM con fondo negro
        LinearLayout romContainer = new LinearLayout(context);
        romContainer.setOrientation(LinearLayout.VERTICAL);
        romContainer.setBackgroundColor(Color.BLACK);
        romContainer.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));

        // Crear background con bordes
        GradientDrawable romBg = new GradientDrawable();
        romBg.setColor(Color.BLACK);
        romBg.setCornerRadius(8f);
        romContainer.setBackground(romBg);

        displayData(romContainer, romData != null ? romData : "", 4, 8);
        romScrollView.addView(romContainer);
        container.addView(romScrollView);

        // Si tiene EEPROM
        if (hasEeprom) {
            // Etiqueta EEPROM
            TextView eepromLabel = new TextView(context);
            eepromLabel.setText("Memoria EEPROM");
            eepromLabel.setTextColor(Color.parseColor("#4CAF50"));
            eepromLabel.setTextSize(16);
            eepromLabel.setTypeface(null, Typeface.BOLD);
            eepromLabel.setPadding(0, dpToPx(12), 0, dpToPx(8));
            container.addView(eepromLabel);

            // ScrollView EEPROM
            ScrollView eepromScrollView = new ScrollView(context);
            LinearLayout.LayoutParams eepromScrollParams = new LinearLayout.LayoutParams(
                    dpToPx(320),
                    dpToPx(150)
            );
            eepromScrollView.setLayoutParams(eepromScrollParams);

            // Contenedor EEPROM
            LinearLayout eepromContainer = new LinearLayout(context);
            eepromContainer.setOrientation(LinearLayout.VERTICAL);
            eepromContainer.setBackgroundColor(Color.BLACK);
            eepromContainer.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));

            GradientDrawable eepromBg = new GradientDrawable();
            eepromBg.setColor(Color.BLACK);
            eepromBg.setCornerRadius(8f);
            eepromContainer.setBackground(eepromBg);

            displayData(eepromContainer, eepromData != null ? eepromData : "", 2, 8);
            eepromScrollView.addView(eepromContainer);
            container.addView(eepromScrollView);
        }

        // Boton cerrar
        TextView closeButton = new TextView(context);
        closeButton.setText("Cerrar");
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
     * LOGICA EXACTA del original para mostrar datos
     * Verde para datos, BLANCO para vacio
     */
    private void displayData(LinearLayout container, String data, int groupSize, int columns) {
        int address = 0;
        StringBuilder formattedRow = new StringBuilder();

        for (int i = 0; i < data.length(); i += groupSize * columns) {
            // Direccion
            String addressHex = String.format("%04X", address);
            formattedRow.append(addressHex).append(": ");

            // Datos
            for (int j = 0; j < columns; j++) {
                int start = i + j * groupSize;
                int end = Math.min(start + groupSize, data.length());

                if (start < data.length()) {
                    formattedRow.append(data.substring(start, end)).append(" ");
                }
            }

            // Crear fila
            TextView rowTextView = new TextView(context);
            rowTextView.setText(formattedRow.toString().trim());
            rowTextView.setTextSize(11);
            rowTextView.setTextColor(Color.parseColor("#4CAF50")); // VERDE para datos
            rowTextView.setTypeface(Typeface.MONOSPACE);
            rowTextView.setPadding(4, 2, 4, 2);
            container.addView(rowTextView);

            address += 8;
            formattedRow.setLength(0);
        }

        // Agregar filas BLANCAS para memoria vacia
        // Calcular cuantas filas faltan
        int totalRows = (int) Math.ceil(data.length() / (double) (groupSize * columns));
        int maxRows = 50; // Maximo estimado

        if (totalRows < maxRows) {
            for (int i = totalRows; i < maxRows && i < totalRows + 10; i++) {
                String addressHex = String.format("%04X", address);
                StringBuilder emptyRow = new StringBuilder();
                emptyRow.append(addressHex).append(": ");

                for (int j = 0; j < columns; j++) {
                    for (int k = 0; k < groupSize; k++) {
                        emptyRow.append("F");
                    }
                    emptyRow.append(" ");
                }

                TextView emptyTextView = new TextView(context);
                emptyTextView.setText(emptyRow.toString().trim());
                emptyTextView.setTextSize(11);
                emptyTextView.setTextColor(Color.WHITE); // BLANCO para vacio
                emptyTextView.setTypeface(Typeface.MONOSPACE);
                emptyTextView.setPadding(4, 2, 4, 2);
                container.addView(emptyTextView);

                address += 8;
            }
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
