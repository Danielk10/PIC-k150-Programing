package com.diamon.managers;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.text.HtmlCompat;

import com.diamon.chip.ChipPic;
import com.diamon.datos.ChipinfoReader;
import com.diamon.pic.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Gestor de selección de chips PIC.
 * Maneja la lectura de la base de datos y la configuración del Spinner.
 */
public class ChipSelectionManager {

    private final AppCompatActivity activity;
    private final Context context;
    private ChipinfoReader chipReader;
    private List<String> chipModels;
    private ChipSelectionListener selectionListener;
    private ChipPic currentChip;

    public interface ChipSelectionListener {
        void onChipSelected(ChipPic chip, String model);

        void onChipSelectionError(String errorMessage);

        void onDatabaseLoaded(); // Notificar cuando la DB esté lista
    }

    public ChipSelectionManager(AppCompatActivity activity) {
        this.activity = activity;
        this.context = activity.getApplicationContext();
        this.chipModels = new ArrayList<>();
    }

    /**
     * Inicializa la base de datos de chips de forma asíncrona para evitar ANR.
     */
    public void initializeAsync() {
        new Thread(() -> {
            try {
                chipReader = new ChipinfoReader(activity);
                List<String> models = chipReader.getModelosPic();

                activity.runOnUiThread(() -> {
                    chipModels = models;
                    if (selectionListener != null) {
                        selectionListener.onDatabaseLoaded();
                    }
                });
            } catch (Exception e) {
                activity.runOnUiThread(() -> {
                    notifyError(context.getString(R.string.error_inicializando_base_de_da) + ": " + e.getMessage());
                });
            }
        }).start();
    }

    public void setSelectionListener(ChipSelectionListener listener) {
        this.selectionListener = listener;
    }

    public void setupSpinner(Spinner spinner) {
        if (chipModels == null || chipModels.isEmpty()) {
            return;
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_item, chipModels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position,
                    long id) {
                String model = chipModels.get(position);
                selectChip(model);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });
    }

    private void selectChip(String model) {
        try {
            ChipPic chip = chipReader.getChipEntry(model);
            if (selectionListener != null) {
                this.currentChip = chip;
                selectionListener.onChipSelected(chip, model);
            }
        } catch (Exception e) {
            notifyError(context.getString(R.string.error_obteniendo_datos_del_chi) + ": " + e.getMessage());
        }
    }

    /**
     * Devuelve el chip actualmente seleccionado (que ya incluye la informacion de
     * fuses).
     * Reemplaza a getSelectedChipFuses() + ChipinfoEntry, ahora todo esta en
     * ChipPic.
     */
    public ChipPic getSelectedChipFuses() {
        return currentChip;
    }

    public CharSequence getSelectedChipInfoColored() {
        if (currentChip == null)
            return "";
        try {
            StringBuilder sb = new StringBuilder();

            // Modelo
            sb.append(formatLabel(context.getString(R.string.modelo))).append(" ").append(currentChip.getNombreDelPic())
                    .append("<br>");

            // Palabras ROM y Tamaño ROM
            int romBytes = currentChip.getTamanoROM();
            int romWords = romBytes / 2;

            sb.append(formatLabel(context.getString(R.string.palabras_rom))).append(" ").append(romWords)
                    .append("<br>");
            sb.append(formatLabel(context.getString(R.string.tamano_rom))).append(" ").append(romBytes)
                    .append(" bytes<br>");

            // EEPROM
            sb.append(formatLabel(context.getString(R.string.eeprom))).append(" ");
            if (currentChip.isTamanoValidoDeEEPROM()) {
                sb.append(currentChip.getTamanoEEPROM()).append(" bytes");
            } else {
                sb.append(context.getString(R.string.no_disponible));
            }
            sb.append("<br>");

            // Tipo de Nucleo
            sb.append(formatLabel(context.getString(R.string.tipo_de_nucleo))).append(" ")
                    .append(currentChip.getTipoDeNucleoBit()).append(" bits<br>");

            // Modo
            sb.append(formatLabel(context.getString(R.string.modo))).append(" ");
            if (currentChip.isICSPOnlyCompatible()) {
                sb.append(context.getString(R.string.icsp_solamente));
            } else {
                sb.append(context.getString(R.string.pin_1)).append(" ").append(currentChip.getUbicacionPin1DelPic());
            }

            return HtmlCompat.fromHtml(sb.toString(), HtmlCompat.FROM_HTML_MODE_LEGACY);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    private String formatLabel(String label) {
        return "<font color='#FFFFFF'><b>" + label + ":</b></font>";
    }

    public String getSelectedChipInfo() {
        return getSelectedChipInfoColored().toString();
    }

    private void notifyError(String message) {
        if (selectionListener != null) {
            selectionListener.onChipSelectionError(message);
        } else {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        }
    }
}
