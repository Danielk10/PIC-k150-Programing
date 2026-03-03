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
 * @version 2.0 - Corregida verificación para comparar bytes procesados
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
     * CORREGIDO: Ahora recibe byte[] procesados del HEX (como hace Python).
     * Python compara: read_rom() bytes == flash_data.rom_data bytes
     * Android ahora: leerMemoriaROMDelPic() → bytes == DatosPicProcesados.romData
     * bytes
     *
     * @param protocolo           Protocolo de comunicación activo
     * @param chipPIC             Configuración del chip
     * @param expectedRomBytes    ROM procesada del HEX (bytes), null para saltar
     * @param expectedEepromBytes EEPROM procesada del HEX (bytes), null para saltar
     * @return Resultado de la verificación
     */
    public static VerificationResult verify(Protocolo protocolo, ChipPic chipPIC,
            byte[] expectedRomBytes, byte[] expectedEepromBytes) {

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

        // 2. Verificar ROM (equivalente a Python _verify_pipeline ROM section)
        // Python: pic_rom_data = programming_interface.read_rom()
        // if pic_rom_data == flash_data.rom_data: print('ROM verified.')
        if (expectedRomBytes != null && expectedRomBytes.length > 0) {
            try {
                messages.add("Verificando ROM...");
                String actualRomHex = protocolo.leerMemoriaROMDelPic(chipPIC);

                if (actualRomHex != null && !actualRomHex.startsWith("Error")) {
                    // Convertir hex string leído del chip a byte[]
                    byte[] actualRomBytes = hexStringToBytes(actualRomHex);

                    if (actualRomBytes != null) {
                        // Comparar byte-a-byte (como Python)
                        if (bytesEqual(expectedRomBytes, actualRomBytes)) {
                            romVerified = true;
                            messages.add("ROM verificada correctamente ✓");
                        } else {
                            // Detectar si está locked (todo ceros) — como Python:
                            // no_of_zeros = pic_rom_data.count(b'\x00')
                            // is_maybe_locked = pic_rom_data_len == no_of_zeros
                            int zeroCount = 0;
                            for (byte b : actualRomBytes) {
                                if (b == 0)
                                    zeroCount++;
                            }
                            boolean calWord = chipPIC.isFlagCalibration();

                            if (calWord) {
                                // Si tiene cal_word, los últimos 2 bytes no serán cero
                                romMaybeLocked = (actualRomBytes.length - 2) == zeroCount;
                            } else {
                                romMaybeLocked = actualRomBytes.length == zeroCount;
                            }

                            if (romMaybeLocked) {
                                messages.add("ROM verificación falló — posiblemente locked para lectura");
                            } else {
                                // Agregar info de mismatch para debug
                                int mismatchCount = countMismatches(expectedRomBytes, actualRomBytes);
                                messages.add("ROM verificación falló — " + mismatchCount
                                        + " bytes no coinciden de " + expectedRomBytes.length);
                            }
                        }
                    } else {
                        messages.add("Error convirtiendo datos ROM leídos");
                    }
                } else {
                    messages.add("Error leyendo ROM para verificación");
                }
            } catch (Exception e) {
                messages.add("Error en verificación ROM: " + e.getMessage());
            }
        }

        // 3. Verificar EEPROM (equivalente a Python _verify_pipeline EEPROM section)
        if (expectedEepromBytes != null && expectedEepromBytes.length > 0) {
            try {
                if (chipPIC.isTamanoValidoDeEEPROM()) {
                    messages.add("Verificando EEPROM...");
                    String actualEepromHex = protocolo.leerMemoriaEEPROMDelPic(chipPIC);

                    if (actualEepromHex != null && !actualEepromHex.startsWith("Error")) {
                        byte[] actualEepromBytes = hexStringToBytes(actualEepromHex);

                        if (actualEepromBytes != null) {
                            if (bytesEqual(expectedEepromBytes, actualEepromBytes)) {
                                eepromVerified = true;
                                messages.add("EEPROM verificada correctamente ✓");
                            } else {
                                int mismatchCount = countMismatches(expectedEepromBytes, actualEepromBytes);
                                messages.add("EEPROM verificación falló — " + mismatchCount
                                        + " bytes no coinciden de " + expectedEepromBytes.length);
                            }
                        } else {
                            messages.add("Error convirtiendo datos EEPROM leídos");
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
     * Compara dos arrays de bytes teniendo en cuenta que pueden tener tamaños
     * ligeramente distintos.
     * Compara hasta el tamaño del menor.
     */
    private static boolean bytesEqual(byte[] expected, byte[] actual) {
        int compareLength = Math.min(expected.length, actual.length);
        for (int i = 0; i < compareLength; i++) {
            if (expected[i] != actual[i]) {
                return false;
            }
        }
        // Si los tamaños difieren, verificar que los bytes extra son blancos (0xFF)
        if (expected.length != actual.length) {
            byte[] longer = expected.length > actual.length ? expected : actual;
            for (int i = compareLength; i < longer.length; i++) {
                if (longer[i] != (byte) 0xFF && longer[i] != 0x00) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Cuenta los bytes que no coinciden entre dos arrays.
     */
    private static int countMismatches(byte[] expected, byte[] actual) {
        int mismatches = 0;
        int maxLen = Math.max(expected.length, actual.length);
        for (int i = 0; i < maxLen; i++) {
            byte e = i < expected.length ? expected[i] : (byte) 0xFF;
            byte a = i < actual.length ? actual[i] : (byte) 0xFF;
            if (e != a)
                mismatches++;
        }
        return mismatches;
    }

    /**
     * Convierte un hex string a byte array.
     */
    private static byte[] hexStringToBytes(String hexStr) {
        if (hexStr == null)
            return null;
        String clean = hexStr.replaceAll("\\s+", "");
        if (clean.length() % 2 != 0)
            return null;

        try {
            byte[] result = new byte[clean.length() / 2];
            for (int i = 0; i < result.length; i++) {
                result[i] = (byte) Integer.parseInt(clean.substring(i * 2, i * 2 + 2), 16);
            }
            return result;
        } catch (NumberFormatException e) {
            return null;
        }
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
