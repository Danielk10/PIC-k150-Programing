package com.diamon.managers;

import android.content.Context;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;

import com.diamon.chip.ChipPic;
import com.diamon.chip.ChipinfoEntry;
import com.diamon.datos.ChipinfoReader;
import com.diamon.datos.ChipFusesReader;
import com.diamon.excepciones.ChipConfigurationException;

import java.util.ArrayList;
import java.util.List;
import com.diamon.pic.R;

/**
 * Gestor de seleccion de chips PIC - VERSION FINAL - Info completa con metodos EXACTOS de ChipPic -
 * Colores: Etiquetas en BLANCO, Datos en VERDE - Tipo de nucleo incluido
 */
public class ChipSelectionManager {

    private final Context context;

    private final AppCompatActivity activity;
    private ChipinfoReader chipReader;
    private ChipFusesReader chipfuses;
    private List<String> chipModels;
    private ChipPic selectedChip;
    private ChipinfoEntry chipFusesSelected;

    private ChipSelectionListener selectionListener;

    public interface ChipSelectionListener {
        void onChipSelected(ChipPic chip, String model);

        void onChipSelectionError(String errorMessage);
    }

    public ChipSelectionManager(AppCompatActivity activity) {
        this.activity = activity;
        this.context = activity.getApplicationContext();
        this.chipModels = new ArrayList<>();
        initializeChipReader();
    }

    private void initializeChipReader() {
        try {
            chipReader = new ChipinfoReader(this.activity);
            chipfuses = new ChipFusesReader(this.activity, "chipinfo.cid");
            chipModels = chipReader.getModelosPic();
        } catch (Exception e) {
            notifyError(
                    context.getString(R.string.error_inicializando_base_de_da)
                            + ": "
                            + e.getMessage());
        }
    }

    public void setSelectionListener(ChipSelectionListener listener) {
        this.selectionListener = listener;
    }

    public void setupSpinner(Spinner spinner) {
        if (chipModels.isEmpty()) {
            notifyError(context.getString(R.string.no_hay_modelos_de_chips_dispon));
            return;
        }

        String[] modelsArray = chipModels.toArray(new String[0]);

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        context, android.R.layout.simple_spinner_dropdown_item, modelsArray);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent, android.view.View view, int position, long id) {
                        if (position >= 0 && position < chipModels.size()) {
                            String model = chipModels.get(position);
                            selectChipByModel(model);
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        // No hacer nada
                    }
                });
    }

    private void selectChipByModel(String model) {
        if (model == null || model.trim().isEmpty()) {
            notifyError(context.getString(R.string.modelo_de_chip_invalido));
            return;
        }

        try {
            ChipPic chip = chipReader.getChipEntry(model);
            // Selecciom de chip
            ChipinfoEntry chipFs = chipfuses.getChip(model);

            if (chip == null) {
                notifyError(
                        context.getString(R.string.chip_no_encontrado_en_base_de_) + ": " + model);
                return;
            }

            selectedChip = chip;
            chipFusesSelected = chipFs;

            // Configurar modo ICSP
            String pinLocation = chip.getUbicacionPin1DelPic();
            boolean isIcspOnly = "null".equals(pinLocation);
            chip.setActivarICSP(isIcspOnly);

            if (selectionListener != null) {
                selectionListener.onChipSelected(chip, model);
            }

        } catch (Exception e) {
            notifyError(context.getString(R.string.error_procesando_chip) + ": " + e.getMessage());
        }
    }

    public ChipPic getSelectedChip() {
        return selectedChip;
    }

    public ChipinfoEntry getSelectedChipFuses() {
        return chipFusesSelected;
    }

    public List<String> getChipModels() {
        return new ArrayList<>(chipModels);
    }

    /**
     * Obtiene informacion COMPLETA con METODOS EXACTOS de ChipPic Formato: Etiquetas en BLANCO,
     * Datos en VERDE
     */
    public SpannableString getSelectedChipInfoColored() {
        if (selectedChip == null) {
            return new SpannableString(context.getString(R.string.no_hay_chip_seleccionado));
        }

        StringBuilder info = new StringBuilder();

        try {
            // Modelo
            String modelo = selectedChip.getNombreDelPic();
            info.append(context.getString(R.string.modelo) + ": ").append(modelo).append("\n");

            // Palabras ROM
            int romWords = selectedChip.getTamanoROM();
            info.append(context.getString(R.string.palabras_rom) + ": ")
                    .append(romWords)
                    .append("\n");

            // Tamano ROM en bytes
            int wordSize = selectedChip.getTipoDeNucleoBit() == 16 ? 4 : 2;
            int romBytes = romWords * wordSize;
            info.append(context.getString(R.string.tamano_rom) + ": ")
                    .append(romBytes)
                    .append(" bytes\n");

            // EEPROM
            if (selectedChip.isTamanoValidoDeEEPROM()) {
                int eepromSize = selectedChip.getTamanoEEPROM();
                info.append(context.getString(R.string.eeprom) + ": ")
                        .append(eepromSize)
                        .append(" bytes\n");
            } else {
                info.append(
                        context.getString(R.string.eeprom)
                                + ": "
                                + context.getString(R.string.no_disponible)
                                + "\n");
            }

            // Tipo de nucleo
            int nucleoBits = selectedChip.getTipoDeNucleoBit();
            info.append(context.getString(R.string.tipo_de_nucleo) + ": ")
                    .append(nucleoBits)
                    .append(" bits\n");

        } catch (ChipConfigurationException e) {
            info.append(context.getString(R.string.error_obteniendo_datos_del_chi) + "\n");
        }

        // Pin 1 o modo ICSP
        String pinLocation = selectedChip.getUbicacionPin1DelPic();
        if ("null".equals(pinLocation)) {
            info.append(
                    context.getString(R.string.modo)
                            + ": "
                            + context.getString(R.string.icsp_solamente));
        } else {
            info.append(context.getString(R.string.pin_1) + ": ").append(pinLocation);
        }

        // Crear SpannableString con colores
        String infoStr = info.toString();
        SpannableString spannableString = new SpannableString(infoStr);

        // Aplicar colores: Etiquetas BLANCO, Datos VERDE
        colorizeInfo(spannableString, infoStr);

        return spannableString;
    }

    /** Aplica colores: Etiquetas en BLANCO, datos en VERDE */
    private void colorizeInfo(SpannableString spannable, String text) {
        String[] lines = text.split("\n");
        int currentPos = 0;

        for (String line : lines) {
            if (line.contains(":")) {
                int colonPos = line.indexOf(":");

                // Etiqueta (antes de :) en BLANCO
                spannable.setSpan(
                        new ForegroundColorSpan(Color.WHITE),
                        currentPos,
                        currentPos + colonPos + 1,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                // Dato (despues de :) en VERDE
                if (colonPos + 2 < line.length()) {
                    spannable.setSpan(
                            new ForegroundColorSpan(Color.parseColor("#4CAF50")),
                            currentPos + colonPos + 2,
                            currentPos + line.length(),
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }

            currentPos += line.length() + 1; // +1 por el \n
        }
    }

    /** Version simple sin colores (para compatibilidad) */
    public String getSelectedChipInfo() {
        if (selectedChip == null) {
            return context.getString(R.string.no_hay_chip_seleccionado);
        }

        StringBuilder info = new StringBuilder();

        try {
            info.append(context.getString(R.string.modelo) + ": ")
                    .append(selectedChip.getNombreDelPic())
                    .append("\n");

            int romWords = selectedChip.getTamanoROM();
            info.append(context.getString(R.string.palabras_rom) + ": ")
                    .append(romWords)
                    .append("\n");

            int wordSize = selectedChip.getTipoDeNucleoBit() == 16 ? 4 : 2;
            int romBytes = romWords * wordSize;
            info.append(context.getString(R.string.tamano_rom) + ": ")
                    .append(romBytes)
                    .append(" bytes\n");

            if (selectedChip.isTamanoValidoDeEEPROM()) {
                int eepromSize = selectedChip.getTamanoEEPROM();
                info.append(context.getString(R.string.eeprom) + " : ")
                        .append(eepromSize)
                        .append(" bytes\n");
            } else {
                info.append(
                        context.getString(R.string.eeprom)
                                + " : "
                                + context.getString(R.string.no_disponible)
                                + "\n");
            }

            int nucleoBits = selectedChip.getTipoDeNucleoBit();
            info.append(context.getString(R.string.tipo_de_nucleo) + ": ")
                    .append(nucleoBits)
                    .append(" bits\n");

        } catch (ChipConfigurationException e) {
            // Ignorar errores
        }

        String pinLocation = selectedChip.getUbicacionPin1DelPic();
        if ("null".equals(pinLocation)) {
            info.append(
                    context.getString(R.string.modo)
                            + ": "
                            + context.getString(R.string.icsp_solamente));
        } else {
            info.append(context.getString(R.string.pin_1) + ": ").append(pinLocation);
        }

        return info.toString();
    }

    public boolean hasChipSelected() {
        return selectedChip != null;
    }

    private void notifyError(String errorMessage) {
        if (selectionListener != null) {
            selectionListener.onChipSelectionError(errorMessage);
        }
    }
}
