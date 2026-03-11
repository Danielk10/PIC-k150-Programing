package com.diamon.managers;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Gestor de exportación de archivos HEX/BIN al almacenamiento del dispositivo.
 *
 * <p>
 * Permite exportar datos leídos de la memoria del PIC (ROM, EEPROM, Config)
 * como archivos Intel HEX o binarios al directorio que el usuario elija.
 *
 * @author Danielk10
 * @version 1.0
 * @since 2025
 */
public class HexExportManager {

    private final Context context;
    private final AppCompatActivity activity;
    private ExportListener exportListener;
    private ActivityResultLauncher<String> createDocumentLauncher;
    private byte[] pendingExportDataBinary;
    private String pendingExportDataText;
    private boolean pendingIsBinaryFile;

    /** Interfaz para manejar eventos de exportación */
    public interface ExportListener {
        void onExportSuccess(String fileName);

        void onExportError(String errorMessage);
    }

    public HexExportManager(AppCompatActivity activity) {
        this.activity = activity;
        this.context = activity;
    }

    /**
     * Inicializa el launcher para crear documentos.
     * DEBE llamarse antes de onStart() de la Activity.
     */
    public void initialize() {
        createDocumentLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.CreateDocument("application/octet-stream"),
                uri -> {
                    if (uri != null) {
                        if (pendingIsBinaryFile && pendingExportDataBinary != null) {
                            writeDataToUri(uri, pendingExportDataBinary, null);
                        } else if (!pendingIsBinaryFile && pendingExportDataText != null) {
                            writeDataToUri(uri, null, pendingExportDataText);
                        }
                    }
                    pendingExportDataBinary = null;
                    pendingExportDataText = null;
                });
    }

    public void setExportListener(ExportListener listener) {
        this.exportListener = listener;
    }

    /**
     * Exporta datos de memoria como archivo Intel HEX.
     *
     * @param data          Bytes crudos de la memoria leída
     * @param suggestedName Nombre sugerido para el archivo (sin extensión)
     */
    public void exportAsHex(byte[] data, String suggestedName) {
        exportAsHexWithAddress(data, 0, suggestedName);
    }

    /**
     * Exporta datos de memoria como archivo Intel HEX iniciando en una direccion
     * específica.
     */
    public void exportAsHexWithAddress(byte[] data, int startAddress, String suggestedName) {
        if (data == null || data.length == 0) {
            notifyError(context.getString(com.diamon.pic.R.string.no_hay_datos_para_exportar));
            return;
        }

        if (createDocumentLauncher == null) {
            notifyError(context.getString(com.diamon.pic.R.string.error_generico_detalle, "HexExportManager no inicializado"));
            return;
        }

        pendingExportDataText = convertToIntelHexWithAddress(data, startAddress);
        pendingExportDataBinary = null;
        pendingIsBinaryFile = false;
        createDocumentLauncher.launch(suggestedName + ".hex");
    }

    /**
     * Exporta ROM, EEPROM y Config en un único dump HEX.
     */
    public void exportFullDumpAsHex(byte[] romData, byte[] eepromData, byte[] configData,
            int eepromAddress, int configAddress, String suggestedName) {

        if (createDocumentLauncher == null) {
            notifyError(context.getString(com.diamon.pic.R.string.error_generico_detalle, "HexExportManager no inicializado"));
            return;
        }

        StringBuilder fullHex = new StringBuilder();

        if (romData != null && romData.length > 0) {
            fullHex.append(convertSegmentToIntelHex(romData, 0));
        }

        if (configData != null && configData.length > 0) {
            fullHex.append(convertSegmentToIntelHex(configData, configAddress));
        }

        if (eepromData != null && eepromData.length > 0) {
            fullHex.append(convertSegmentToIntelHex(eepromData, eepromAddress));
        }

        // End of File Record
        fullHex.append(":00000001FF\r\n");

        pendingExportDataText = fullHex.toString();
        pendingExportDataBinary = null;
        pendingIsBinaryFile = false;

        createDocumentLauncher.launch(suggestedName + ".hex");
    }

    /**
     * Exporta ROM, EEPROM y Config (con segmentos separados de ID y Fuses)
     * en un único dump HEX. Esta versión escribe User ID y Fuses en sus
     * direcciones correctas del mapa de memoria PIC, en vez de como un
     * bloque contiguo.
     *
     * @param romData       Datos ROM ya formateados
     * @param eepromData    Datos EEPROM ya formateados
     * @param idData        Datos User ID ya formateados (byte-swapped)
     * @param idAddress     Dirección base de User ID (0x4000 para 14-bit, 0x200000 para 16-bit)
     * @param fuseData      Datos de fuses ya formateados (byte-swapped)
     * @param fuseAddress   Dirección base de fuses (0x400E para 14-bit, 0x300000 para 16-bit)
     * @param eepromAddress Dirección base de EEPROM
     * @param suggestedName Nombre sugerido del archivo
     */
    public void exportFullDumpAsHexWithSplitConfig(byte[] romData, byte[] eepromData,
            byte[] idData, int idAddress, byte[] fuseData, int fuseAddress,
            int eepromAddress, String suggestedName) {

        if (createDocumentLauncher == null) {
            notifyError(context.getString(com.diamon.pic.R.string.error_generico_detalle, "HexExportManager no inicializado"));
            return;
        }

        StringBuilder fullHex = new StringBuilder();

        // ROM
        if (romData != null && romData.length > 0) {
            fullHex.append(convertSegmentToIntelHex(romData, 0));
        }

        // User ID en su dirección correcta
        if (idData != null && idData.length > 0) {
            fullHex.append(convertSegmentToIntelHex(idData, idAddress));
        }

        // Fuses en su dirección correcta
        if (fuseData != null && fuseData.length > 0) {
            fullHex.append(convertSegmentToIntelHex(fuseData, fuseAddress));
        }

        // EEPROM
        if (eepromData != null && eepromData.length > 0) {
            fullHex.append(convertSegmentToIntelHex(eepromData, eepromAddress));
        }

        // End of File Record
        fullHex.append(":00000001FF\r\n");

        pendingExportDataText = fullHex.toString();
        pendingExportDataBinary = null;
        pendingIsBinaryFile = false;

        createDocumentLauncher.launch(suggestedName + ".hex");
    }

    /**
     * Exporta datos de configuración como HEX con segmentos separados
     * de User ID y Fuses en sus direcciones correctas.
     */
    public void exportConfigAsHexSplit(byte[] idData, int idAddress,
            byte[] fuseData, int fuseAddress, String suggestedName) {

        if (createDocumentLauncher == null) {
            notifyError(context.getString(com.diamon.pic.R.string.error_generico_detalle, "HexExportManager no inicializado"));
            return;
        }

        StringBuilder hexContent = new StringBuilder();

        if (idData != null && idData.length > 0) {
            hexContent.append(convertSegmentToIntelHex(idData, idAddress));
        }

        if (fuseData != null && fuseData.length > 0) {
            hexContent.append(convertSegmentToIntelHex(fuseData, fuseAddress));
        }

        hexContent.append(":00000001FF\r\n");

        pendingExportDataText = hexContent.toString();
        pendingExportDataBinary = null;
        pendingIsBinaryFile = false;

        createDocumentLauncher.launch(suggestedName + ".hex");
    }

    /**
     * Exporta datos de memoria como archivo binario.
     *
     * @param data          Bytes crudos de la memoria leída
     * @param suggestedName Nombre sugerido para el archivo (sin extensión)
     */
    public void exportAsBinary(byte[] data, String suggestedName) {
        if (data == null || data.length == 0) {
            notifyError(context.getString(com.diamon.pic.R.string.no_hay_datos_para_exportar));
            return;
        }

        if (createDocumentLauncher == null) {
            notifyError(context.getString(com.diamon.pic.R.string.error_generico_detalle, "HexExportManager no inicializado"));
            return;
        }

        pendingExportDataBinary = data;
        pendingExportDataText = null;
        pendingIsBinaryFile = true;
        createDocumentLauncher.launch(suggestedName + ".bin");
    }

    /**
     * Exporta un string hexadecimal (como el retornado por leerMemoriaROMDelPic)
     * como archivo Intel HEX.
     *
     * @param hexString     String hexadecimal con los datos de memoria
     * @param suggestedName Nombre sugerido para el archivo
     */
    public void exportHexStringAsFile(String hexString, String suggestedName) {
        if (hexString == null || hexString.isEmpty() || hexString.startsWith("Error")) {
            notifyError(context.getString(com.diamon.pic.R.string.no_hay_datos_validos_para_exportar));
            return;
        }

        byte[] data = hexStringToBytes(hexString);
        if (data == null) {
            notifyError(context.getString(com.diamon.pic.R.string.error_convirtiendo_datos_hex));
            return;
        }

        exportAsHex(data, suggestedName);
    }

    /**
     * Exporta un string hexadecimal como archivo binario crudo.
     *
     * @param hexString     String hexadecimal con los datos de memoria
     * @param suggestedName Nombre sugerido para el archivo
     */
    public void exportBinStringAsFile(String hexString, String suggestedName) {
        if (hexString == null || hexString.isEmpty() || hexString.startsWith("Error")) {
            notifyError(context.getString(com.diamon.pic.R.string.no_hay_datos_validos_para_exportar));
            return;
        }

        byte[] data = hexStringToBytes(hexString);
        if (data == null) {
            notifyError(context.getString(com.diamon.pic.R.string.error_convirtiendo_datos_hex));
            return;
        }

        exportAsBinary(data, suggestedName);
    }

    /**
     * Escribe los datos al URI seleccionado por el usuario.
     */
    private void writeDataToUri(Uri uri, byte[] binData, String txtData) {
        try {
            OutputStream outputStream = context.getContentResolver().openOutputStream(uri);
            if (outputStream == null) {
                notifyError(context.getString(com.diamon.pic.R.string.error_creando_archivo_salida));
                return;
            }

            if (binData != null) {
                // Escritura binaria directa
                outputStream.write(binData);
            } else if (txtData != null) {
                // Formato de texto (HEX)
                outputStream.write(txtData.getBytes(StandardCharsets.US_ASCII));
            }

            outputStream.flush();
            outputStream.close();

            if (exportListener != null) {
                exportListener.onExportSuccess(uri.getLastPathSegment());
            }

        } catch (IOException e) {
            notifyError(context.getString(com.diamon.pic.R.string.error_escribiendo_archivo_detalle, e.getMessage()));
        } catch (Exception e) {
            notifyError(context.getString(com.diamon.pic.R.string.error_inesperado_leyendo_el_ar) + ": " + e.getMessage());
        }
    }

    /**
     * Convierte un array de bytes a formato Intel HEX.
     *
     * <p>
     * Genera registros tipo:
     * <ul>
     * <li>Tipo 00: Data Record (16 bytes por línea)
     * <li>Tipo 04: Extended Linear Address (cuando la dirección supera 0xFFFF)
     * <li>Tipo 01: End of File Record
     * </ul>
     *
     * @param data Bytes a convertir
     * @return String en formato Intel HEX
     */
    public static String convertToIntelHex(byte[] data) {
        return convertToIntelHexWithAddress(data, 0);
    }

    public static String convertToIntelHexWithAddress(byte[] data, int startAddress) {
        String segments = convertSegmentToIntelHex(data, startAddress);
        return segments + ":00000001FF\r\n";
    }

    /**
     * Formatea los datos devueltos por ProtocoloP18A al estándar Little Endian de
     * Microchip
     * requerido por los archivos Intel HEX.
     * 
     * @param data     Datos crudos
     * @param coreBits 14 o 16 (bits del núcleo)
     * @param isEeprom Si es verdadero, aplica el padding necesario para EEPROM de
     *                 14-bit
     * @return Arreglo de bytes listo para exportar a HEX
     */
    public static byte[] formatForHexExport(byte[] data, int coreBits, boolean isEeprom) {
        if (data == null || data.length == 0)
            return data;

        if (isEeprom) {
            if (coreBits == 16) {
                // PIC18: EEPROM es byte oriented, no necesita padding/swabbing
                return data.clone();
            } else {
                // PIC14: EEPROM usa 1 byte de dato por cada palabra de 16-bits en el .hex
                // Estándar Microchip Little Endian: [Dato, 0x00]
                byte[] padded = new byte[data.length * 2];
                for (int i = 0; i < data.length; i++) {
                    padded[i * 2] = data[i]; // LSB (Dato útil)
                    padded[i * 2 + 1] = 0x00; // MSB (Padding)
                }
                return padded;
            }
        } else {
            // ROM y Configuración:
            // K150 devuelve los datos en orden Big Endian aparente [MSB, LSB] con respecto
            // a la
            // codificación esperada en el Hex de Microchip (Little Endian [LSB, MSB]).
            // Hacemos swap.
            byte[] swapped = new byte[data.length];
            for (int i = 0; i < data.length; i += 2) {
                if (i + 1 < data.length) {
                    swapped[i] = data[i + 1];
                    swapped[i + 1] = data[i];
                } else {
                    swapped[i] = data[i];
                }
            }
            return swapped;
        }
    }

    /**
     * Convierte un array de bytes a formato Intel HEX sin el EOF record.
     */
    public static String convertSegmentToIntelHex(byte[] data, int startAddress) {
        StringBuilder hex = new StringBuilder();
        int bytesPerLine = 16;
        int currentExtendedAddress = -1; // -1 to force writing on first run if startAddress > 0xFFFF

        for (int offset = 0; offset < data.length; offset += bytesPerLine) {
            int fullAddress = startAddress + offset;
            int extendedAddress = (fullAddress >> 16) & 0xFFFF;

            // Emitir Extended Linear Address record si cambió
            if (extendedAddress != currentExtendedAddress) {
                currentExtendedAddress = extendedAddress;
                hex.append(buildExtendedAddressRecord(extendedAddress));
            }

            // Calcular cuántos bytes quedan en esta línea
            int count = Math.min(bytesPerLine, data.length - offset);
            int lineAddress = fullAddress & 0xFFFF;

            // Construir Data Record (tipo 00)
            hex.append(buildDataRecord(lineAddress, data, offset, count));
        }

        return hex.toString();
    }

    /**
     * Construye un registro de dirección extendida (tipo 04).
     */
    private static String buildExtendedAddressRecord(int extendedAddress) {
        int byteCount = 2;
        int recordType = 4;
        int address = 0;
        int hi = (extendedAddress >> 8) & 0xFF;
        int lo = extendedAddress & 0xFF;

        int checksum = byteCount + (address >> 8) + (address & 0xFF) + recordType + hi + lo;
        checksum = (~checksum + 1) & 0xFF;

        return String.format(":%02X%04X%02X%02X%02X%02X\r\n",
                byteCount, address, recordType, hi, lo, checksum);
    }

    /**
     * Construye un registro de datos (tipo 00).
     */
    private static String buildDataRecord(int address, byte[] data, int offset, int count) {
        StringBuilder record = new StringBuilder();
        int recordType = 0;

        record.append(String.format(":%02X%04X%02X", count, address, recordType));

        int checksum = count + ((address >> 8) & 0xFF) + (address & 0xFF) + recordType;

        for (int i = 0; i < count; i++) {
            int b = data[offset + i] & 0xFF;
            record.append(String.format("%02X", b));
            checksum += b;
        }

        checksum = (~checksum + 1) & 0xFF;
        record.append(String.format("%02X\r\n", checksum));

        return record.toString();
    }

    /**
     * Convierte un string hexadecimal a array de bytes.
     */
    private byte[] hexStringToBytes(String hexStr) {
        try {
            String clean = hexStr.replaceAll("\\s+", "");
            if (clean.length() % 2 != 0) {
                return null;
            }

            byte[] result = new byte[clean.length() / 2];
            for (int i = 0; i < result.length; i++) {
                result[i] = (byte) Integer.parseInt(clean.substring(i * 2, i * 2 + 2), 16);
            }
            return result;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void notifyError(String errorMessage) {
        if (exportListener != null) {
            exportListener.onExportError(errorMessage);
        }
    }
}
