package com.diamon.managers;

import com.diamon.pic.R;
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
        public String toSummary(android.content.Context context) {
            StringBuilder sb = new StringBuilder();
            sb.append("=== ").append(context.getString(R.string.resultado_verificacion)).append(" ===\n");

            if (chipIdHex != null) {
                sb.append(context.getString(R.string.chip_id_label)).append(" ").append(chipIdHex).append("\n");
            }
            if (calibrationHex != null) {
                sb.append(context.getString(R.string.calibracion_label, calibrationHex)).append("\n");
            }

            sb.append(context.getString(R.string.rom_label, romVerified ? "✓ " + context.getString(R.string.verificada) : "✗ " + context.getString(R.string.fallo)));
            if (romMaybeLocked) {
                sb.append(" (").append(context.getString(R.string.rom_posible_locked)).append(")");
            }
            sb.append("\n");

            sb.append(context.getString(R.string.eeprom_label, eepromVerified ? "✓ " + context.getString(R.string.verificada) : context.getString(R.string.not_available))).append("\n");

            if (decodedFuses != null && !decodedFuses.isEmpty()) {
                sb.append("\n").append(context.getString(R.string.seccion_fuses)).append(":\n");
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
    public static VerificationResult verify(android.content.Context context, Protocolo protocolo, ChipPic chipPIC,
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
                            messages.add(context.getString(R.string.fuses_decodificados_exito));
                        }
                    }
                } catch (Exception e) {
                    messages.add(context.getString(R.string.error_decodificar_fuses, e.getMessage()));
                }
            } else {
                messages.add(context.getString(R.string.error_leer_config));
            }
        } catch (Exception e) {
            messages.add(context.getString(R.string.error_leyendo_config_detalle, e.getMessage()));
        }

        // 2. Verificar ROM (equivalente a Python _verify_pipeline ROM section)
        // Python: pic_rom_data = programming_interface.read_rom()
        // if pic_rom_data == flash_data.rom_data: print('ROM verified.')
        if (expectedRomBytes != null && expectedRomBytes.length > 0) {
            try {
                messages.add(context.getString(R.string.verificando_rom_label));
                String actualRomHex = protocolo.leerMemoriaROMDelPic(chipPIC);

                if (actualRomHex != null && !actualRomHex.startsWith("Error")) {
                    // Convertir hex string leído del chip a byte[]
                    byte[] actualRomBytes = hexStringToBytes(actualRomHex);

                    if (actualRomBytes != null) {
                        // Comparar byte-a-byte (como Python)
                        if (bytesEqual(expectedRomBytes, actualRomBytes)) {
                            romVerified = true;
                            messages.add(context.getString(R.string.rom_verificada_exito));
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
                                messages.add(context.getString(R.string.rom_fallo_locked_label));
                            } else {
                                // Agregar info de mismatch para debug
                                int mismatchCount = countMismatches(expectedRomBytes, actualRomBytes);
                                messages.add(context.getString(R.string.rom_fallo_mismatch, mismatchCount, expectedRomBytes.length));
                            }
                        }
                    } else {
                        messages.add(context.getString(R.string.error_convertir_rom));
                    }
                } else {
                    messages.add(context.getString(R.string.error_leyendo_rom_verif));
                }
            } catch (Exception e) {
                messages.add(context.getString(R.string.error_verif_rom_detalle, e.getMessage()));
            }
        }

        // 3. Verificar EEPROM (equivalente a Python _verify_pipeline EEPROM section)
        if (expectedEepromBytes != null && expectedEepromBytes.length > 0) {
            try {
                if (chipPIC.isTamanoValidoDeEEPROM()) {
                    messages.add(context.getString(R.string.verificando_eeprom_label));
                    String actualEepromHex = protocolo.leerMemoriaEEPROMDelPic(chipPIC);

                    if (actualEepromHex != null && !actualEepromHex.startsWith("Error")) {
                        byte[] actualEepromBytes = hexStringToBytes(actualEepromHex);

                        if (actualEepromBytes != null) {
                            if (bytesEqual(expectedEepromBytes, actualEepromBytes)) {
                                eepromVerified = true;
                                messages.add(context.getString(R.string.eeprom_verificada_exito));
                            } else {
                                int mismatchCount = countMismatches(expectedEepromBytes, actualEepromBytes);
                                messages.add(context.getString(R.string.eeprom_fallo_mismatch, mismatchCount, expectedEepromBytes.length));
                            }
                        } else {
                            messages.add(context.getString(R.string.error_convertir_eeprom));
                        }
                    } else {
                        messages.add(context.getString(R.string.error_leyendo_eeprom_verif));
                    }
                } else {
                    eepromVerified = true; // No EEPROM = OK
                    messages.add(context.getString(R.string.chip_sin_eeprom_verif));
                }
            } catch (Exception e) {
                messages.add(context.getString(R.string.error_verif_eeprom_detalle, e.getMessage()));
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
    public static String getHexInfo(android.content.Context context, ChipPic chipPIC, byte[] romData, byte[] eepromData) {
        StringBuilder info = new StringBuilder();
        info.append(context.getString(R.string.titulo_info_hex)).append("\n");

        try {
            int romSizeChip = chipPIC.getTamanoROM();
            int eepromSizeChip = chipPIC.getTamanoEEPROM();

            // ROM info
            int romWordsUsed = (romData != null) ? romData.length / 2 : 0;
            int romWordsFree = romSizeChip - romWordsUsed;
            info.append(context.getString(R.string.rom_info_usage, romWordsUsed, Math.max(0, romWordsFree))).append("\n");
            info.append(context.getString(R.string.rom_total_info, romSizeChip, romSizeChip * 2)).append("\n");

            // EEPROM info
            if (chipPIC.isTamanoValidoDeEEPROM()) {
                int eepromBytesUsed = (eepromData != null) ? eepromData.length : 0;
                int eepromBytesFree = eepromSizeChip - eepromBytesUsed;
                info.append(context.getString(R.string.eeprom_info_usage, eepromBytesUsed, Math.max(0, eepromBytesFree))).append("\n");
                info.append(context.getString(R.string.eeprom_total_info, eepromSizeChip)).append("\n");
            } else {
                info.append(context.getString(R.string.eeprom_no_disponible)).append("\n");
            }

            // Chip info general
            info.append("\n").append(context.getString(R.string.chip_label_info, chipPIC.getNombreDelPic())).append("\n");
            info.append(context.getString(R.string.core_info, chipPIC.getTipoDeNucleoBit())).append("\n");
            info.append(context.getString(R.string.pin1_info, chipPIC.getUbicacionPin1DelPic())).append("\n");
            info.append(context.getString(R.string.flash_info, chipPIC.isFlashChip() ? context.getString(R.string.si) : context.getString(R.string.no))).append("\n");
            info.append(context.getString(R.string.icsp_only_info, chipPIC.isICSPonly() ? context.getString(R.string.si) : context.getString(R.string.no))).append("\n");

        } catch (ChipConfigurationException e) {
            info.append(context.getString(R.string.error_info_chip_detalle, e.getMessage()));
        }

        return info.toString();
    }
}
