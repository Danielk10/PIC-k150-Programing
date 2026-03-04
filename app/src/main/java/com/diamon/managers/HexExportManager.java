package com.diamon.managers;

import android.content.Context;
import android.net.Uri;
import android.widget.EditText;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;

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
    private PendingExportData pendingExportData;
    private ActivityResultLauncher<Uri> selectorCarpetaLauncher;

    /** Estructura temporal para completar la exportación después de elegir carpeta. */
    private static class PendingExportData {
        final String baseName;
        final String extension;
        final byte[] binaryData;
        final String textData;

        PendingExportData(String baseName, String extension, byte[] binaryData, String textData) {
            this.baseName = baseName;
            this.extension = extension;
            this.binaryData = binaryData;
            this.textData = textData;
        }
    }

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
     */
    public void initialize() {
        selectorCarpetaLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.OpenDocumentTree(),
                this::procesarCarpetaSeleccionada);
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
            notifyError(context.getString(com.diamon.pic.R.string.no_hay_datos_para_exportar_archivo));
            return;
        }

        String hexText = convertToIntelHexWithAddress(data, startAddress);
        saveWithDialog(suggestedName, ".hex", null, hexText);
    }

    /**
     * Exporta ROM, EEPROM y Config en un único dump HEX.
     */
    public void exportFullDumpAsHex(byte[] romData, byte[] eepromData, byte[] configData,
            int eepromAddress, int configAddress, String suggestedName) {

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

        saveWithDialog(suggestedName, ".hex", null, fullHex.toString());
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

        saveWithDialog(suggestedName, ".bin", data, null);
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

    private void saveWithDialog(String defaultName, String extension, final byte[] binData, final String txtData) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(activity);
        builder.setTitle(context.getString(com.diamon.pic.R.string.exportar_memoria) + " (" + extension + ")");

        final EditText input = new EditText(context);
        input.setText(defaultName);
        input.setSingleLine(true);
        // padding
        int padding = (int) (16 * context.getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding, padding, padding);
        builder.setView(input);

        builder.setPositiveButton(context.getString(com.diamon.pic.R.string.aceptar), (dialog, which) -> {
            String baseName = input.getText().toString().trim();
            if (baseName.isEmpty()) {
                notifyError(context.getString(com.diamon.pic.R.string.error_nombre_archivo_vacio));
                return;
            }
            pendingExportData = new PendingExportData(baseName, extension, binData, txtData);
            abrirSelectorDeCarpeta();
        });
        builder.setNegativeButton(context.getString(com.diamon.pic.R.string.cancelar), null);
        builder.show();
    }

    /** Abre el selector de carpeta del sistema para exportación. */
    private void abrirSelectorDeCarpeta() {
        if (selectorCarpetaLauncher == null) {
            notifyError(context.getString(com.diamon.pic.R.string.error_exportador_no_inicializado));
            pendingExportData = null;
            return;
        }
        selectorCarpetaLauncher.launch(null);
    }

    /** Procesa la carpeta elegida por el usuario y completa la exportación pendiente. */
    private void procesarCarpetaSeleccionada(Uri treeUri) {
        if (pendingExportData == null) {
            notifyError(context.getString(com.diamon.pic.R.string.error_no_hay_exportacion_pendiente));
            return;
        }

        if (treeUri == null) {
            notifyError(context.getString(com.diamon.pic.R.string.seleccion_carpeta_cancelada));
            pendingExportData = null;
            return;
        }

        try {
            context.getContentResolver().takePersistableUriPermission(
                    treeUri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                            | android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        } catch (SecurityException ignored) {
            // Algunos proveedores no permiten persistir permisos; continuamos con permiso temporal.
        }

        DocumentFile carpeta = DocumentFile.fromTreeUri(context, treeUri);
        if (carpeta == null || !carpeta.canWrite()) {
            notifyError(context.getString(com.diamon.pic.R.string.error_carpeta_no_accesible));
            pendingExportData = null;
            return;
        }

        String fileName = pendingExportData.baseName;
        if (!fileName.toLowerCase().endsWith(pendingExportData.extension)) {
            fileName += pendingExportData.extension;
        }

        DocumentFile archivoExistente = carpeta.findFile(fileName);
        if (archivoExistente != null) {
            final String finalFileName = fileName;
            new android.app.AlertDialog.Builder(activity)
                    .setTitle(context.getString(com.diamon.pic.R.string.reemplazar_archivo_titulo))
                    .setMessage(context.getString(com.diamon.pic.R.string.reemplazar_archivo_mensaje, finalFileName))
                    .setPositiveButton(context.getString(com.diamon.pic.R.string.si), (dialog, which) -> {
                        if (!archivoExistente.delete()) {
                            notifyError(context.getString(com.diamon.pic.R.string.error_reemplazando_archivo));
                            pendingExportData = null;
                            return;
                        }
                        crearYEscribirArchivo(carpeta, finalFileName);
                    })
                    .setNegativeButton(context.getString(com.diamon.pic.R.string.no), (dialog, which) -> pendingExportData = null)
                    .show();
            return;
        }

        crearYEscribirArchivo(carpeta, fileName);
    }

    private void crearYEscribirArchivo(DocumentFile carpeta, String fileName) {
        if (pendingExportData == null) {
            notifyError(context.getString(com.diamon.pic.R.string.error_no_hay_exportacion_pendiente));
            return;
        }

        String mimeType = pendingExportData.extension.equalsIgnoreCase(".hex")
                ? "text/plain"
                : "application/octet-stream";

        DocumentFile nuevoArchivo = carpeta.createFile(mimeType, fileName);
        if (nuevoArchivo == null || nuevoArchivo.getUri() == null) {
            notifyError(context.getString(com.diamon.pic.R.string.error_creando_archivo_salida));
            pendingExportData = null;
            return;
        }

        writeToUri(nuevoArchivo.getUri(), pendingExportData.binaryData, pendingExportData.textData, fileName);
        pendingExportData = null;
    }

    private void writeToUri(Uri fileUri, byte[] binData, String txtData, String fileName) {
        try {
            OutputStream outputStream = context.getContentResolver().openOutputStream(fileUri, "wt");
            if (outputStream == null) {
                notifyError(context.getString(com.diamon.pic.R.string.error_abriendo_archivo_escritura));
                return;
            }

            if (binData != null) {
                outputStream.write(binData);
            } else if (txtData != null) {
                outputStream.write(txtData.getBytes(StandardCharsets.US_ASCII));
            }
            outputStream.flush();
            outputStream.close();

            // No se requiere MediaScanner al usar SAF.

            if (exportListener != null) {
                exportListener.onExportSuccess(fileName);
            }
            android.widget.Toast.makeText(context,
                    context.getString(com.diamon.pic.R.string.archivo_guardado, fileName),
                    android.widget.Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            notifyError(context.getString(com.diamon.pic.R.string.error_escribiendo_archivo_detalle, e.getMessage()));
        } catch (Exception e) {
            notifyError(context.getString(com.diamon.pic.R.string.error_inesperado_exportar_detalle, e.getMessage()));
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
     * Convierte un array de bytes a formato Intel HEX sin el registro EOF.
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
