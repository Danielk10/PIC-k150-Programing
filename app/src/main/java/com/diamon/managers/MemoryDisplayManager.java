package com.diamon.pic.managers;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Gestor de visualizacion de datos de memoria CORREGIDO. Muestra ROM y EEPROM en UN SOLO
 * PopupWindow con tabs Colores: Verde para datos, Blanco para vacio ScrollView en ambas secciones
 */
public class MemoryDisplayManager {

    private final Context context;
    private PopupWindow memoryPopup;

    public MemoryDisplayManager(Context context) {
        this.context = context;
    }

    /** Muestra datos ROM y EEPROM en UN SOLO popup con dos secciones */
    public void showMemoryDataPopup(
            String romData, int romSize, String eepromData, int eepromSize, boolean hasEeprom) {
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setBackgroundColor(Color.parseColor("#1A1A2E"));
        container.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));

        // Titulo
        TextView titleView = new TextView(context);
        titleView.setText("Datos de Memoria");
        titleView.setTextColor(Color.parseColor("#4CAF50"));
        titleView.setTextSize(18);
        titleView.setTypeface(null, Typeface.BOLD);
        titleView.setGravity(Gravity.CENTER);
        titleView.setPadding(0, 0, 0, dpToPx(12));
        container.addView(titleView);

        // Seccion ROM
        TextView romTitle = new TextView(context);
        romTitle.setText("Memoria ROM");
        romTitle.setTextColor(Color.parseColor("#FFD700"));
        romTitle.setTextSize(16);
        romTitle.setTypeface(null, Typeface.BOLD);
        romTitle.setPadding(0, 0, 0, dpToPx(8));
        container.addView(romTitle);

        // ScrollView ROM con altura fija
        ScrollView romScrollView = new ScrollView(context);
        LinearLayout.LayoutParams romScrollParams =
                new LinearLayout.LayoutParams(dpToPx(350), dpToPx(hasEeprom ? 200 : 350));
        romScrollView.setLayoutParams(romScrollParams);

        LinearLayout romContainer = new LinearLayout(context);
        romContainer.setOrientation(LinearLayout.VERTICAL);
        romContainer.setBackgroundColor(Color.BLACK);
        romContainer.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));

        displayMemoryDataWithColors(romContainer, romData, romSize, 4, 8);
        romScrollView.addView(romContainer);
        container.addView(romScrollView);

        // Seccion EEPROM si existe
        if (hasEeprom && eepromData != null && !eepromData.isEmpty()) {
            TextView eepromTitle = new TextView(context);
            eepromTitle.setText("Memoria EEPROM");
            eepromTitle.setTextColor(Color.parseColor("#FFD700"));
            eepromTitle.setTextSize(16);
            eepromTitle.setTypeface(null, Typeface.BOLD);
            eepromTitle.setPadding(0, dpToPx(12), 0, dpToPx(8));
            container.addView(eepromTitle);

            // ScrollView EEPROM con altura fija
            ScrollView eepromScrollView = new ScrollView(context);
            LinearLayout.LayoutParams eepromScrollParams =
                    new LinearLayout.LayoutParams(dpToPx(350), dpToPx(150));
            eepromScrollView.setLayoutParams(eepromScrollParams);

            LinearLayout eepromContainer = new LinearLayout(context);
            eepromContainer.setOrientation(LinearLayout.VERTICAL);
            eepromContainer.setBackgroundColor(Color.BLACK);
            eepromContainer.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));

            displayMemoryDataWithColors(eepromContainer, eepromData, eepromSize, 2, 16);
            eepromScrollView.addView(eepromContainer);
            container.addView(eepromScrollView);
        }

        // Boton cerrar
        TextView closeButton = new TextView(context);
        closeButton.setText("Cerrar");
        closeButton.setTextColor(Color.WHITE);
        closeButton.setTextSize(16);
        closeButton.setGravity(Gravity.CENTER);
        closeButton.setPadding(dpToPx(40), dpToPx(12), dpToPx(40), dpToPx(12));

        GradientDrawable buttonBg = new GradientDrawable();
        buttonBg.setShape(GradientDrawable.RECTANGLE);
        buttonBg.setColor(Color.parseColor("#2196F3"));
        buttonBg.setCornerRadius(dpToPx(8));
        closeButton.setBackground(buttonBg);

        LinearLayout.LayoutParams buttonParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
        buttonParams.gravity = Gravity.CENTER;
        buttonParams.setMargins(0, dpToPx(16), 0, 0);
        closeButton.setLayoutParams(buttonParams);
        container.addView(closeButton);

        // Crear popup
        if (memoryPopup != null && memoryPopup.isShowing()) {
            memoryPopup.dismiss();
        }

        memoryPopup =
                new PopupWindow(
                        container,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        true);

        memoryPopup.setOutsideTouchable(true);
        memoryPopup.setFocusable(true);
        closeButton.setOnClickListener(v -> memoryPopup.dismiss());

        memoryPopup.showAtLocation(
                ((android.app.Activity) context).findViewById(android.R.id.content),
                Gravity.CENTER,
                0,
                0);
    }

    /** Muestra datos con colores: VERDE para datos, BLANCO para vacio */
    private void displayMemoryDataWithColors(
            LinearLayout container, String data, int totalSize, int groupSize, int columns) {
        if (data == null) data = "";

        int dataLength = data.length();
        int address = 0;

        // Calcular filas necesarias
        int totalGroups = totalSize;
        int groupsPerRow = columns;
        int totalRows = (totalGroups + groupsPerRow - 1) / groupsPerRow;

        for (int row = 0; row < totalRows; row++) {
            LinearLayout rowLayout = new LinearLayout(context);
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            rowLayout.setPadding(0, dpToPx(1), 0, dpToPx(1));

            // Direccion
            TextView addressView = new TextView(context);
            addressView.setText(String.format("%04X: ", address));
            addressView.setTextColor(Color.parseColor("#FFD700"));
            addressView.setTypeface(Typeface.MONOSPACE);
            addressView.setTextSize(11);
            addressView.setMinWidth(dpToPx(50));
            rowLayout.addView(addressView);

            // Datos
            for (int col = 0; col < columns; col++) {
                int dataIndex = (row * columns + col) * groupSize;

                TextView dataView = new TextView(context);
                dataView.setTypeface(Typeface.MONOSPACE);
                dataView.setTextSize(11);
                dataView.setPadding(dpToPx(2), 0, dpToPx(2), 0);

                if (dataIndex < dataLength) {
                    int endIndex = Math.min(dataIndex + groupSize, dataLength);
                    String hexValue = data.substring(dataIndex, endIndex);
                    while (hexValue.length() < groupSize) hexValue += " ";

                    dataView.setText(hexValue + " ");
                    dataView.setTextColor(Color.parseColor("#4CAF50")); // VERDE
                } else {
                    String emptyValue = "";
                    for (int i = 0; i < groupSize; i++) emptyValue += "F";
                    dataView.setText(emptyValue + " ");
                    dataView.setTextColor(Color.WHITE); // BLANCO
                }

                rowLayout.addView(dataView);
            }

            container.addView(rowLayout);
            address += columns;
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
