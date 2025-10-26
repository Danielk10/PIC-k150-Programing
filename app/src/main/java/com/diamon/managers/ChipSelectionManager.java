package com.diamon.managers;

import android.content.Context;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;
import com.diamon.chip.ChipPic;
import com.diamon.datos.ChipinfoReader;

import com.diamon.excepciones.ChipConfigurationException;
import java.util.ArrayList;
import java.util.List;

/**
 * Gestor de seleccion de chips PIC MEJORADO. Maneja el spinner de seleccion y la informacion
 * detallada de los chips disponibles. Muestra: Modelo, Palabras ROM, Tamano ROM, Tamano EEPROM, Pin
 * 1
 */
public class ChipSelectionManager {

    private final Context context;
    private final AppCompatActivity actividad;
    private ChipinfoReader chipReader;
    private List<String> chipModels;
    private ChipPic selectedChip;

    // Listener para notificar cambios en la seleccion
    private ChipSelectionListener selectionListener;

    /** Interfaz para manejar eventos de seleccion de chips */
    public interface ChipSelectionListener {
        void onChipSelected(ChipPic chip, String model);

        void onChipSelectionError(String errorMessage);
    }

    /**
     * Constructor del gestor de seleccion de chips
     *
     * @param context Contexto de la aplicacion
     */
    public ChipSelectionManager(AppCompatActivity actividad) {
        this.actividad = actividad;
        this.context = actividad.getApplicationContext();
        this.chipModels = new ArrayList<>();
        initializeChipReader();
    }

    /** Inicializa el lector de informacion de chips */
    private void initializeChipReader() {
        try {
            chipReader = new ChipinfoReader(actividad);
            chipModels = chipReader.getModelosPic();
        } catch (Exception e) {
            notifyError("Error inicializando base de datos de chips: " + e.getMessage());
        }
    }

    /**
     * Establece el listener para eventos de seleccion
     *
     * @param listener Listener que sera notificado
     */
    public void setSelectionListener(ChipSelectionListener listener) {
        this.selectionListener = listener;
    }

    /**
     * Configura un spinner con la lista de chips disponibles
     *
     * @param spinner Spinner a configurar
     */
    public void setupSpinner(Spinner spinner) {
        if (chipModels.isEmpty()) {
            notifyError("No hay modelos de chips disponibles");
            return;
        }

        // Crear array de modelos
        String[] modelsArray = chipModels.toArray(new String[0]);

        // Crear adaptador
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        context, android.R.layout.simple_spinner_dropdown_item, modelsArray);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        // Configurar listener de seleccion
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

    /**
     * Selecciona un chip por su modelo
     *
     * @param model Modelo del chip a seleccionar
     */
    private void selectChipByModel(String model) {
        if (model == null || model.trim().isEmpty()) {
            notifyError("Modelo de chip invalido");
            return;
        }

        try {
            ChipPic chip = chipReader.getChipEntry(model);

            if (chip == null) {
                notifyError("Chip no encontrado en base de datos: " + model);
                return;
            }

            selectedChip = chip;

            // Configurar modo ICSP si es necesario
            String pinLocation = chip.getUbicacionPin1DelPic();
            boolean isIcspOnly = "null".equals(pinLocation);
            chip.setActivarICSP(isIcspOnly);

            // Notificar seleccion
            if (selectionListener != null) {
                selectionListener.onChipSelected(chip, model);
            }

        } catch (Exception e) {
            notifyError("Error procesando chip: " + e.getMessage());
        }
    }

    /**
     * Obtiene el chip actualmente seleccionado
     *
     * @return Chip seleccionado o null si no hay seleccion
     */
    public ChipPic getSelectedChip() {
        return selectedChip;
    }

    /**
     * Obtiene la lista de modelos disponibles
     *
     * @return Lista de modelos de chips
     */
    public List<String> getChipModels() {
        return new ArrayList<>(chipModels);
    }

    /**
     * Obtiene informacion COMPLETA formateada del chip seleccionado Incluye: Modelo, Palabras ROM,
     * Tamano ROM (bytes), EEPROM (con indicacion si no tiene), Pin 1
     *
     * @return String con informacion completa del chip
     */
    public String getSelectedChipInfo() {
        if (selectedChip == null) {
            return "No hay chip seleccionado";
        }

        StringBuilder info = new StringBuilder();

        // Modelo
        info.append("Modelo: ").append(selectedChip.getNombreDelPic()).append("\n");
        try {
            // Palabras de ROM
            int romWords = selectedChip.getTamanoROM();
            info.append("Palabras ROM: ").append(romWords).append("\n");

            // Tamano ROM en bytes (cada palabra = 2 bytes para PICs de 14 bits, 4 para 16 bits)
            int wordSize = selectedChip.getTipoDeNucleoBit() == 16 ? 4 : 2;
            int romBytes = romWords * wordSize;
            info.append("Tamano ROM: ").append(romBytes).append(" bytes\n");

            // EEPROM - Indicar si tiene o no
            if (selectedChip.isTamanoValidoDeEEPROM()) {
                int eepromSize = selectedChip.getTamanoEEPROM();
                info.append("EEPROM: ").append(eepromSize).append(" bytes\n");
            } else {
                info.append("EEPROM: No disponible\n");
            }

        } catch (ChipConfigurationException e) {
        }

        // Pin 1 o modo ICSP
        String pinLocation = selectedChip.getUbicacionPin1DelPic();
        if ("null".equals(pinLocation)) {
            info.append("Modo: ICSP solamente");
        } else {
            info.append("Pin 1: ").append(pinLocation);
        }

        return info.toString();
    }

    /**
     * Verifica si hay un chip seleccionado
     *
     * @return true si hay un chip seleccionado, false en caso contrario
     */
    public boolean hasChipSelected() {
        return selectedChip != null;
    }

    /**
     * Notifica un error al listener
     *
     * @param errorMessage Mensaje de error
     */
    private void notifyError(String errorMessage) {
        if (selectionListener != null) {
            selectionListener.onChipSelectionError(errorMessage);
        }
    }
}
