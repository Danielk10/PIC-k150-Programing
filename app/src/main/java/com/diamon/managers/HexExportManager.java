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
            notifyError("No hay datos para exportar");
            return;
        }

        if (createDocumentLauncher == null) {
            notifyError("HexExportManager no inicializado");
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
            notifyError("HexExportManager no inicializado");
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
        fullHex.append(":00000001FF\n");

        pendingExportDataText = fullHex.toString();
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
            notifyError("No hay datos para exportar");
            return;
        }

        if (createDocumentLauncher == null) {
            notifyError("HexExportManager no inicializado");
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
            notifyError("No hay datos válidos para exportar");
            return;
        }

        byte[] data = hexStringToBytes(hexString);
        if (data == null) {
            notifyError("Error convirtiendo datos hexadecimales");
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
            notifyError("No hay datos válidos para exportar");
            return;
        }

        byte[] data = hexStringToBytes(hexString);
        if (data == null) {
            notifyError("Error convirtiendo datos hexadecimales");
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
                notifyError("Error abriendo archivo para escritura");
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
            notifyError("Error escribiendo archivo: " + e.getMessage());
        } catch (Exception e) {
            notifyError("Error inesperado al exportar: " + e.getMessage());
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
        return segments + ":00000001FF\n";
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

        return String.format(":%02X%04X%02X%02X%02X%02X\n",
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
        record.append(String.format("%02X\n", checksum));

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
