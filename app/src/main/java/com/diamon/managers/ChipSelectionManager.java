package com.diamon.managers;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.diamon.chip.ChipPic;
import com.diamon.chip.ChipinfoEntry;
import com.diamon.datos.ChipinfoReader;
import com.diamon.datos.ChipFusesReader;
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
    private ChipFusesReader chipfuses;
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
                chipfuses = new ChipFusesReader(activity, "chipinfo.cid");
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

    public ChipinfoEntry getSelectedChipFuses() {
        if (currentChip == null || chipfuses == null)
            return null;
        try {
            return chipfuses.getChip(currentChip.getNombreDelPic());
        } catch (Exception e) {
            return null;
        }
    }

    public String getSelectedChipInfo() {
        if (currentChip == null)
            return "";
        try {
            StringBuilder sb = new StringBuilder();

            // Modelo
            sb.append(context.getString(R.string.modelo)).append(": ").append(currentChip.getNombreDelPic())
                    .append("\n");

            // Palabras ROM y Tamaño ROM
            // Asumiendo que getTamanoROM devuelve bytes, calculamos palabras aproximadas
            // (ej. 14-bit core = 2 bytes/word en mapa de memoria)
            int romBytes = currentChip.getTamanoROM();
            int romWords = romBytes / 2;

            sb.append(context.getString(R.string.palabras_rom)).append(": ").append(romWords).append("\n");
            sb.append(context.getString(R.string.tamano_rom)).append(": ").append(romBytes).append(" bytes\n");

            // EEPROM
            sb.append(context.getString(R.string.eeprom)).append(": ");
            if (currentChip.isTamanoValidoDeEEPROM()) {
                sb.append(currentChip.getTamanoEEPROM()).append(" bytes");
            } else {
                sb.append(context.getString(R.string.no_disponible));
            }
            sb.append("\n");

            // Tipo de Nucleo
            sb.append(context.getString(R.string.tipo_de_nucleo)).append(": ").append(currentChip.getTipoDeNucleoBit())
                    .append(" bits\n");

            // Modo (Hardcoded a ICSP solamente por ahora según imagen, o lógica si existe)
            sb.append(context.getString(R.string.modo)).append(": ").append(context.getString(R.string.icsp_solamente));

            return sb.toString();
        } catch (Exception e) {
            return "Error obteniendo info";
        }
    }

    public String getSelectedChipInfoColored() {
        // En una implementación real esto podría devolver un Spannable,
        // pero para simplificar devolvemos el mismo texto que el anterior.
        return getSelectedChipInfo();
    }

    private void notifyError(String message) {
        if (selectionListener != null) {
            selectionListener.onChipSelectionError(message);
        } else {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        }
    }
}
