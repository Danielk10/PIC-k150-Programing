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
 * Utiliza el Storage Access Framework (SAF) de Android con
 * ACTION_CREATE_DOCUMENT
 * para abrir el explorador de archivos del sistema, permitiendo al usuario
 * elegir
 * la ubicación y modificar el nombre del archivo antes de guardar.
 *
 * @author Danielk10
 * @version 2.0
 * @since 2025
 */
public class HexExportManager {

    private final Context context;
    private final AppCompatActivity activity;
    private ExportListener exportListener;
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
     * Inicializa el gestor.
     * DEBE llamarse antes de onStart() de la Activity.
     */
    public void initialize() {
        // Ya no se requiere inicializar ActionDocument porque usamos modo archivo
        // directo
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
     * Exporta datos de memoria como archivo Intel HEX iniciando en una dirección
     * específica.
     */
    public void exportAsHexWithAddress(byte[] data, int startAddress, String suggestedName) {
        if (data == null || data.length == 0) {
            notifyError(context.getString(com.diamon.pic.R.string.no_hay_datos_para_exportar_archivo));
            return;
        }

        String hexText = convertToIntelHexWithAddress(data, startAddress);
        lanzarSelectorArchivo(suggestedName, ".hex", null, hexText);
    }

    /**
     * Exporta ROM, EEPROM y Config en un único dump HEX.
     */
    public void exportFullDumpAsHex(byte[] romData, byte[] eepromData, byte[] configData,
            int eepromAddress, int coreBits, String suggestedName) {

        int configAddress = (coreBits == 16) ? 0x300000 : 0x4000;

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

        // Registro de fin de archivo (EOF).
        fullHex.append(":00000001FF\r\n");

        lanzarSelectorArchivo(suggestedName, ".hex", null, fullHex.toString());
    }

    /**
     * Exporta datos de memoria como archivo binario.
     *
     * @param data          Bytes crudos de la memoria leída
     * @param suggestedName Nombre sugerido para el archivo (sin extensión)
     */
    public void exportAsBinary(byte[] data, String suggestedName) {
        if (data == null || data.length == 0) {
            notifyError(context.getString(com.diamon.pic.R.string.no_hay_datos_para_exportar_archivo));
            return;
        }

        lanzarSelectorArchivo(suggestedName, ".bin", data, null);
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

    public void lanzarSelectorArchivo(String defaultName, String extension, final byte[] binData,
            final String txtData) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(activity);
        builder.setTitle(context.getString(com.diamon.pic.R.string.exportar_memoria) + " (" + extension + ")");

        final android.widget.EditText input = new android.widget.EditText(context);
        input.setText(defaultName);
        input.setSingleLine(true);
        // padding
        int padding = (int) (16 * context.getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding, padding, padding);
        builder.setView(input);

        builder.setPositiveButton(context.getString(com.diamon.pic.R.string.aceptar), (dialog, which) -> {
            String fileName = input.getText().toString().trim();
            if (fileName.isEmpty()) {
                notifyError("El nombre no puede estar vacío");
                return;
            }
            if (!fileName.toLowerCase().endsWith(extension)) {
                fileName += extension;
            }

            java.io.File dir = android.os.Environment
                    .getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            final java.io.File file = new java.io.File(dir, fileName);

            if (file.exists()) {
                new android.app.AlertDialog.Builder(activity)
                        .setTitle("Reemplazar archivo")
                        .setMessage("El archivo '" + fileName + "' ya existe. ¿Desea reemplazarlo?")
                        .setPositiveButton("Sí", (d, w) -> writeToFile(file, binData, txtData))
                        .setNegativeButton("No", null)
                        .show();
            } else {
                writeToFile(file, binData, txtData);
            }
        });
        builder.setNegativeButton(context.getString(com.diamon.pic.R.string.cancelar), null);
        builder.show();
    }

    private void writeToFile(java.io.File file, byte[] binData, String txtData) {
        try {
            java.io.FileOutputStream fos = new java.io.FileOutputStream(file);

            if (binData != null) {
                // Escritura binaria directa
                fos.write(binData);
            } else if (txtData != null) {
                // Formato de texto (HEX)
                fos.write(txtData.getBytes(StandardCharsets.US_ASCII));
            }
            fos.flush();
            fos.close();

            // Notificar a MediaScanner para que aparezca rápido
            android.media.MediaScannerConnection.scanFile(context, new String[] { file.getAbsolutePath() }, null, null);

            if (exportListener != null) {
                exportListener.onExportSuccess(file.getName());
            }

            android.widget.Toast.makeText(context, "Archivo guardado en Descargas: " + file.getName(),
                    android.widget.Toast.LENGTH_LONG).show();

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
     * <li>Tipo 00: Registro de datos (16 bytes por línea)
     * <li>Tipo 04: Dirección lineal extendida (cuando la dirección supera 0xFFFF)
     * <li>Tipo 01: Registro de fin de archivo
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
     * Convierte el bloque de configuración de 26 bytes del K150
     * a registros Intel HEX en sus direcciones correctas.
     * Estructura K150: [0-1] chip_id, [2-9] user_id (4 words), [10-23] fuses (7
     * words), [24-25] calibrate
     */
    public static String convertConfigSegmentToIntelHex(byte[] configBytes, int coreBits) {
        if (configBytes == null || configBytes.length < 26) {
            return "";
        }

        StringBuilder hex = new StringBuilder();

        if (coreBits == 14 || coreBits == 12) {
            // User ID (4 words) -> a partir de 0x4000
            byte[] userIds = new byte[8];
            System.arraycopy(configBytes, 2, userIds, 0, 8);
            // K150 devuelve Big Endian [MSB, LSB], para el HEX necesitamos Little Endian
            // [LSB, MSB]
            userIds = formatForHexExport(userIds, coreBits, false);
            hex.append(buildDataRecord(0x4000, userIds, 0, 8));

            // Fuses (típicamente 1 word en 0x2007 -> 0x400E para 14-bit)
            // Extraer solo la primera palabra de fuse (que corresponde a 0x2007)
            byte[] fuse0 = new byte[2];
            System.arraycopy(configBytes, 10, fuse0, 0, 2);
            fuse0 = formatForHexExport(fuse0, coreBits, false);
            hex.append(buildDataRecord(0x400E, fuse0, 0, 2));

        } else if (coreBits == 16) {
            // User IDs PIC18 -> 0x200000 (8 bytes)
            hex.append(buildExtendedAddressRecord(0x0020));
            byte[] userIds = new byte[8];
            System.arraycopy(configBytes, 2, userIds, 0, 8);
            userIds = formatForHexExport(userIds, coreBits, false);
            hex.append(buildDataRecord(0x0000, userIds, 0, 8));

            // Config words PIC18 -> 0x300000 (14 bytes)
            hex.append(buildExtendedAddressRecord(0x0030));
            byte[] fuses = new byte[14];
            System.arraycopy(configBytes, 10, fuses, 0, 14);
            fuses = formatForHexExport(fuses, coreBits, false);
            hex.append(buildDataRecord(0x0000, fuses, 0, 14));
        }

        return hex.toString();
    }

    /**
     * Convierte un array de bytes a formato Intel HEX sin el registro EOF.
     */
    public static String convertSegmentToIntelHex(byte[] data, int startAddress) {
        StringBuilder hex = new StringBuilder();
        int bytesPerLine = 16;
        int currentExtendedAddress = 0; // Iniciar en 0 evita emitir ELA 0000 innecesariamente

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
