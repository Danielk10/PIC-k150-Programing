package com.diamon.managers;

import com.diamon.chip.ChipPic;
import com.diamon.excepciones.ChipConfigurationException;
import com.diamon.nucleo.Protocolo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Pipeline de verificación post-programación.
 *
 * <p>
 * Implementa la lógica de _verify_pipeline del Python picpro:
 * <ul>
 * <li>Lee ROM después de programar y compara byte-a-byte
 * <li>Lee EEPROM después de programar y compara byte-a-byte
 * <li>Detecta si la ROM está locked (todo ceros)
 * <li>Lee y decodifica la configuración de fuses del chip
 * </ul>
 *
 * @author Danielk10
 * @version 1.0
 * @since 2025
 */
public class VerificationManager {

    /**
     * Resultado de la verificación post-programación.
     */
    public static class VerificationResult {
        public final boolean romVerified;
        public final boolean eepromVerified;
        public final boolean romMaybeLocked;
        public final String chipIdHex;
        public final String calibrationHex;
        public final Map<String, String> decodedFuses;
        public final List<String> messages;

        public VerificationResult(boolean romVerified, boolean eepromVerified,
                boolean romMaybeLocked, String chipIdHex,
                String calibrationHex, Map<String, String> decodedFuses,
                List<String> messages) {
            this.romVerified = romVerified;
            this.eepromVerified = eepromVerified;
            this.romMaybeLocked = romMaybeLocked;
            this.chipIdHex = chipIdHex;
            this.calibrationHex = calibrationHex;
            this.decodedFuses = decodedFuses;
            this.messages = messages;
        }

        public boolean isFullyVerified() {
            return romVerified && eepromVerified;
        }

        /**
         * Genera un resumen de la verificación legible por el usuario.
         */
        public String toSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append("=== Resultado de Verificación ===\n");

            if (chipIdHex != null) {
                sb.append("Chip ID: ").append(chipIdHex).append("\n");
            }
            if (calibrationHex != null) {
                sb.append("Calibración: ").append(calibrationHex).append("\n");
            }

            sb.append("ROM: ").append(romVerified ? "✓ Verificada" : "✗ Falló");
            if (romMaybeLocked) {
                sb.append(" (posiblemente locked para lectura)");
            }
            sb.append("\n");

            sb.append("EEPROM: ").append(eepromVerified ? "✓ Verificada" : "N/A").append("\n");

            if (decodedFuses != null && !decodedFuses.isEmpty()) {
                sb.append("\nFuses decodificados:\n");
                for (Map.Entry<String, String> entry : decodedFuses.entrySet()) {
                    sb.append("  ").append(entry.getKey())
                            .append(" = ").append(entry.getValue()).append("\n");
                }
            }

            for (String msg : messages) {
                sb.append("  ").append(msg).append("\n");
            }

            return sb.toString();
        }
    }

    /**
     * Ejecuta la pipeline de verificación post-programación.
     * Equivalente a Python _verify_pipeline() + _print_chip_config().
     *
     * @param protocolo      Protocolo de comunicación activo
     * @param chipPIC        Configuración del chip
     * @param expectedRom    ROM que fue programada (string hex), null para saltar
     *                       verificación ROM
     * @param expectedEeprom EEPROM que fue programada (string hex), null para
     *                       saltar verificación EEPROM
     * @return Resultado de la verificación
     */
    public static VerificationResult verify(Protocolo protocolo, ChipPic chipPIC,
            String expectedRom, String expectedEeprom) {

        List<String> messages = new ArrayList<>();
        boolean romVerified = false;
        boolean eepromVerified = false;
        boolean romMaybeLocked = false;
        String chipIdHex = null;
        String calibrationHex = null;
        Map<String, String> decodedFuses = null;

        // 1. Leer configuración del chip (equivalente a read_config +
        // _print_chip_config)
        try {
            String configData = protocolo.leerDatosDeConfiguracionDelPic();
            if (configData != null && !configData.startsWith("Error") && configData.length() >= 4) {
                // Config tiene 26 bytes = 52 chars hex
                // Bytes 0-1: Chip ID
                if (configData.length() >= 4) {
                    chipIdHex = "0x" + configData.substring(0, 4);
                }
                // Últimos 2 bytes: Calibración
                if (configData.length() >= 52) {
                    calibrationHex = "0x" + configData.substring(48, 52);
                }

                // Intentar decodificar fuses desde la config leída
                try {
                    // Extraer valores de fuse de los bytes de config
                    // Bytes 10-23 contienen los 7 fuses (cada uno 2 bytes LE)
                    if (configData.length() >= 48) {
                        List<Integer> fuseValues = new ArrayList<>();
                        int tipoNucleo = chipPIC.getTipoDeNucleoBit();
                        int numFuses = (tipoNucleo == 16) ? 7 : 1;

                        for (int i = 0; i < numFuses && (20 + i * 4) <= configData.length() - 4; i++) {
                            int startIdx = 20 + i * 4;
                            String fuseHex = configData.substring(startIdx, startIdx + 4);
                            // Little-endian: swap bytes
                            String swapped = fuseHex.substring(2, 4) + fuseHex.substring(0, 2);
                            fuseValues.add(Integer.parseInt(swapped, 16));
                        }

                        if (!fuseValues.isEmpty()) {
                            decodedFuses = chipPIC.decodeFuseData(fuseValues);
                            messages.add("Fuses decodificados correctamente");
                        }
                    }
                } catch (Exception e) {
                    messages.add("No se pudieron decodificar fuses: " + e.getMessage());
                }
            } else {
                messages.add("No se pudo leer la configuración del chip");
            }
        } catch (Exception e) {
            messages.add("Error leyendo configuración: " + e.getMessage());
        }

        // 2. Verificar ROM (equivalente a _verify_pipeline ROM section)
        if (expectedRom != null && !expectedRom.isEmpty()) {
            try {
                messages.add("Verificando ROM...");
                String actualRom = protocolo.leerMemoriaROMDelPic(chipPIC);

                if (actualRom != null && !actualRom.startsWith("Error")) {
                    // Normalizar para comparar
                    String normalizedExpected = expectedRom.toUpperCase().trim();
                    String normalizedActual = actualRom.toUpperCase().trim();

                    if (normalizedExpected.equals(normalizedActual)) {
                        romVerified = true;
                        messages.add("ROM verificada correctamente ✓");
                    } else {
                        // Detectar si está locked (todo ceros)
                        String allZeros = normalizedActual.replace("0", "");
                        boolean calWord = chipPIC.isFlagCalibration();

                        if (calWord) {
                            // Si tiene cal_word, los últimos 2 bytes no serán cero
                            romMaybeLocked = allZeros.length() <= 4; // solo cal word no es cero
                        } else {
                            romMaybeLocked = allZeros.isEmpty();
                        }

                        if (romMaybeLocked) {
                            messages.add("ROM verificación falló — posiblemente locked para lectura");
                        } else {
                            messages.add("ROM verificación falló — datos no coinciden");
                        }
                    }
                } else {
                    messages.add("Error leyendo ROM para verificación");
                }
            } catch (Exception e) {
                messages.add("Error en verificación ROM: " + e.getMessage());
            }
        }

        // 3. Verificar EEPROM (equivalente a _verify_pipeline EEPROM section)
        if (expectedEeprom != null && !expectedEeprom.isEmpty()) {
            try {
                if (chipPIC.isTamanoValidoDeEEPROM()) {
                    messages.add("Verificando EEPROM...");
                    String actualEeprom = protocolo.leerMemoriaEEPROMDelPic(chipPIC);

                    if (actualEeprom != null && !actualEeprom.startsWith("Error")) {
                        String normalizedExpected = expectedEeprom.toUpperCase().trim();
                        String normalizedActual = actualEeprom.toUpperCase().trim();

                        if (normalizedExpected.equals(normalizedActual)) {
                            eepromVerified = true;
                            messages.add("EEPROM verificada correctamente ✓");
                        } else {
                            messages.add("EEPROM verificación falló — datos no coinciden");
                        }
                    } else {
                        messages.add("Error leyendo EEPROM para verificación");
                    }
                } else {
                    eepromVerified = true; // No EEPROM = OK
                    messages.add("Chip sin EEPROM, verificación no aplica");
                }
            } catch (Exception e) {
                messages.add("Error en verificación EEPROM: " + e.getMessage());
            }
        } else {
            eepromVerified = true; // No se esperaba EEPROM
        }

        return new VerificationResult(romVerified, eepromVerified, romMaybeLocked,
                chipIdHex, calibrationHex, decodedFuses, messages);
    }

    /**
     * Genera info del HEX cargado.
     * Equivalente a Python hex_info command.
     *
     * @param chipPIC    Configuración del chip
     * @param romData    Datos ROM procesados (bytes)
     * @param eepromData Datos EEPROM procesados (bytes), puede ser null
     * @return String con información detallada del HEX
     */
    public static String getHexInfo(ChipPic chipPIC, byte[] romData, byte[] eepromData) {
        StringBuilder info = new StringBuilder();
        info.append("=== Información del HEX ===\n");

        try {
            int romSizeChip = chipPIC.getTamanoROM();
            int eepromSizeChip = chipPIC.getTamanoEEPROM();

            // ROM info
            int romWordsUsed = (romData != null) ? romData.length / 2 : 0;
            int romWordsFree = romSizeChip - romWordsUsed;
            info.append(String.format("ROM: %d palabras usadas, %d palabras libres en chip\n",
                    romWordsUsed, Math.max(0, romWordsFree)));
            info.append(String.format("ROM total chip: %d palabras (%d bytes)\n",
                    romSizeChip, romSizeChip * 2));

            // EEPROM info
            if (chipPIC.isTamanoValidoDeEEPROM()) {
                int eepromBytesUsed = (eepromData != null) ? eepromData.length : 0;
                int eepromBytesFree = eepromSizeChip - eepromBytesUsed;
                info.append(String.format("EEPROM: %d bytes usados, %d bytes libres en chip\n",
                        eepromBytesUsed, Math.max(0, eepromBytesFree)));
                info.append(String.format("EEPROM total chip: %d bytes\n", eepromSizeChip));
            } else {
                info.append("EEPROM: No disponible en este chip\n");
            }

            // Chip info general
            info.append(String.format("\nChip: %s\n", chipPIC.getNombreDelPic()));
            info.append(String.format("Core: %d bits\n", chipPIC.getTipoDeNucleoBit()));
            info.append(String.format("Pin 1: %s\n", chipPIC.getUbicacionPin1DelPic()));
            info.append(String.format("Flash: %s\n", chipPIC.isFlashChip() ? "Sí" : "No"));
            info.append(String.format("ICSP only: %s\n", chipPIC.isICSPonly() ? "Sí" : "No"));

        } catch (ChipConfigurationException e) {
            info.append("Error obteniendo info del chip: ").append(e.getMessage());
        }

        return info.toString();
    }
}
