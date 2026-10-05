package com.diamon.managers;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.diamon.chip.ChipPic;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import com.diamon.pic.R;

/**
 * Gestiona la selección y lectura de archivos HEX/BIN desde el selector del sistema.
 */
public class FileManager {

    private final Context context;
    private final AppCompatActivity activity;
    private FileLoadListener fileLoadListener;
    private String hexFileContent = "";
    private ActivityResultLauncher<String[]> filePickerLauncher;
    private ChipPic currentChip;

    public interface FileLoadListener {
        void onFileLoaded(String content, String fileName);

        void onFileLoadError(String errorMessage);
    }

    public void setCurrentChip(ChipPic chip) {
        this.currentChip = chip;
    }

    public FileManager(AppCompatActivity activity) {
        this.activity = activity;
        this.context = activity;
    }

    public void initialize() {
        filePickerLauncher =
                activity.registerForActivityResult(
                        new ActivityResultContracts.OpenDocument(),
                        uri -> {
                            if (uri != null) {
                                processSelectedFile(uri);
                            }
                        });
    }

    public void setFileLoadListener(FileLoadListener listener) {
        this.fileLoadListener = listener;
    }

    /** Abre el selector de documentos con los MIME types soportados. */
    public void openFilePicker() {
        if (filePickerLauncher == null) {
            notifyError(context.getString(R.string.filemanager_no_inicializado));
            return;
        }

        String[] mimeTypes = {
                "application/octet-stream",
                "application/x-binary",
                "text/plain",
                "application/hex"
        };

        filePickerLauncher.launch(mimeTypes);
    }

    /** Valida el archivo seleccionado y dispara la lectura de contenido. */
    private void processSelectedFile(Uri uri) {
        String fileName = getFileName(uri);

        if (fileName == null) {
            notifyError(context.getString(R.string.no_se_pudo_obtener_el_nombre_d));
            return;
        }

        // Validar extensión .hex o .bin.
        String lowerFileName = fileName.toLowerCase();
        if (!lowerFileName.endsWith(".bin") && !lowerFileName.endsWith(".hex")) {
            notifyError(context.getString(R.string.seleccione_un_archivo_binario_));
            return;
        }

        hexFileContent = readFileContent(uri, lowerFileName.endsWith(".bin"), currentChip);
    }

    /** Lee archivo HEX o BIN y lo retorna en formato Intel HEX textual. */
    private String readFileContent(Uri uri, boolean isBinary, ChipPic chip) {
        if (isBinary) {
            return readBinaryAsIntelHex(uri, chip);
        }
        return readHexText(uri);
    }

    private String readFileContent(Uri uri, boolean isBinary) {
        return readFileContent(uri, isBinary, null);
    }

    /** Lee archivo .hex de texto hasta comentario ';' o EOF. */
    private String readHexText(Uri uri) {
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
            if (inputStream == null) {
                notifyError(context.getString(R.string.error_abriendo_el_archivo_sele));
                return "";
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream, java.nio.charset.StandardCharsets.US_ASCII), 8192)) {
                StringBuilder fileContent = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    // En formato HEX, una línea iniciada con ';' se considera comentario.
                    if (line.length() > 0 && line.charAt(0) == ';') {
                        break;
                    }
                    fileContent.append(line).append("\n");
                }

                String content = fileContent.toString();

                if (content.trim().isEmpty()) {
                    notifyError(context.getString(R.string.el_archivo_seleccionado_esta_v));
                    return "";
                }

                String fileName = getFileName(uri);
                notifyFileLoaded(content, fileName);

                return content;
            }

        } catch (IOException e) {
            notifyError(context.getString(R.string.error_leyendo_el_archivo) + ": " + e.getMessage());
            return "";
        } catch (Exception e) {
            notifyError(context.getString(R.string.error_inesperado_leyendo_el_ar));
            return "";
        }
    }

    /** Lee archivo .bin y lo convierte a Intel HEX de forma segura. */
    private String readBinaryAsIntelHex(Uri uri, ChipPic chip) {
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
            if (inputStream == null) {
                notifyError(context.getString(R.string.error_abriendo_el_archivo_sele));
                return "";
            }

            byte[] data = readAllBytes(inputStream);

            if (data.length == 0) {
                notifyError(context.getString(R.string.el_archivo_seleccionado_esta_v));
                return "";
            }

            String content = binaryToIntelHex(data, chip);
            String fileName = getFileName(uri);
            notifyFileLoaded(content, fileName);
            return content;

        } catch (IOException e) {
            notifyError(context.getString(R.string.error_leyendo_el_archivo) + ": " + e.getMessage());
            return "";
        } catch (Exception e) {
            notifyError(context.getString(R.string.error_inesperado_leyendo_el_ar));
            return "";
        }
    }

    private String readBinaryAsIntelHex(Uri uri) {
        return readBinaryAsIntelHex(uri, null);
    }

    private byte[] readAllBytes(InputStream inputStream) throws IOException {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            baos.write(buffer, 0, read);
        }
        return baos.toByteArray();
    }

    private String binaryToIntelHex(byte[] data) {
        return binaryToIntelHex(data, null);
    }

    public String binaryToIntelHex(byte[] data, ChipPic chip) {
        if (chip == null) {
            return binaryToLinearIntelHex(data, 0);
        }

        try {
            int coreBits = chip.getTipoDeNucleoBit();
            int romSize = chip.getTamanoROM() * 2; // en bytes
            int eepromSize = chip.isTamanoValidoDeEEPROM() ? chip.getTamanoEEPROM() : 0;
            int configSize = 26; // Tamaño estándar del buffer de configuración
            int fullDumpSize = romSize + configSize + eepromSize;

            int eepromHexAddr = (coreBits == 16) ? 0xF000 : 0x4200;
            int idHexAddr = (coreBits == 16) ? 0x200000 : 0x4000;
            int fuseHexAddr = (coreBits == 16) ? 0x300000 : 0x400E;

            StringBuilder out = new StringBuilder();

            if (data.length == fullDumpSize) {
                // Caso 1: Volcado completo (Full Dump)
                // 1. ROM
                byte[] romBytes = java.util.Arrays.copyOfRange(data, 0, romSize);
                out.append(HexExportManager.convertSegmentToIntelHex(romBytes, 0));

                // 2. Configuración (ID y Fuses)
                byte[] configBytes = java.util.Arrays.copyOfRange(data, romSize, romSize + configSize);
                int idLen = (coreBits == 16) ? 8 : 4;
                int idStart = 2; // Omitir chip_id
                int fuseCount = Math.max(1, chip.getFuseBlank().length);
                int fuseByteLen = fuseCount * 2;
                int fuseStart = 10;

                byte[] idBytes = java.util.Arrays.copyOfRange(configBytes, idStart, idStart + idLen);
                byte[] fuseBytes = java.util.Arrays.copyOfRange(configBytes, fuseStart, fuseStart + fuseByteLen);

                idBytes = HexExportManager.formatForHexExport(idBytes, coreBits, false);
                fuseBytes = HexExportManager.formatForHexExport(fuseBytes, coreBits, false);

                out.append(HexExportManager.convertSegmentToIntelHex(idBytes, idHexAddr));
                out.append(HexExportManager.convertSegmentToIntelHex(fuseBytes, fuseHexAddr));

                // 3. EEPROM
                if (eepromSize > 0) {
                    byte[] eepromBytes = java.util.Arrays.copyOfRange(data, romSize + configSize, data.length);
                    eepromBytes = HexExportManager.formatForHexExport(eepromBytes, coreBits, true);
                    out.append(HexExportManager.convertSegmentToIntelHex(eepromBytes, eepromHexAddr));
                }

                out.append(":00000001FF\n");
                return out.toString();

            } else if (data.length == romSize) {
                // Caso 2: Solo ROM
                byte[] romBytes = java.util.Arrays.copyOfRange(data, 0, romSize);
                out.append(HexExportManager.convertSegmentToIntelHex(romBytes, 0));
                out.append(":00000001FF\n");
                return out.toString();

            } else if (data.length == eepromSize && eepromSize > 0) {
                // Caso 3: Solo EEPROM
                byte[] eepromBytes = HexExportManager.formatForHexExport(data, coreBits, true);
                out.append(HexExportManager.convertSegmentToIntelHex(eepromBytes, eepromHexAddr));
                out.append(":00000001FF\n");
                return out.toString();

            } else if (data.length == configSize) {
                // Caso 4: Solo Configuración
                int idLen = (coreBits == 16) ? 8 : 4;
                int idStart = 2;
                int fuseCount = Math.max(1, chip.getFuseBlank().length);
                int fuseByteLen = fuseCount * 2;
                int fuseStart = 10;

                byte[] idBytes = java.util.Arrays.copyOfRange(data, idStart, idStart + idLen);
                byte[] fuseBytes = java.util.Arrays.copyOfRange(data, fuseStart, fuseStart + fuseByteLen);

                idBytes = HexExportManager.formatForHexExport(idBytes, coreBits, false);
                fuseBytes = HexExportManager.formatForHexExport(fuseBytes, coreBits, false);

                out.append(HexExportManager.convertSegmentToIntelHex(idBytes, idHexAddr));
                out.append(HexExportManager.convertSegmentToIntelHex(fuseBytes, fuseHexAddr));
                out.append(":00000001FF\n");
                return out.toString();

            } else {
                return binaryToLinearIntelHex(data, 0);
            }

        } catch (Exception e) {
            return binaryToLinearIntelHex(data, 0);
        }
    }

    private String binaryToLinearIntelHex(byte[] data, int startAddress) {
        return HexExportManager.convertToIntelHexWithAddress(data, startAddress);
    }

    private String getFileName(Uri uri) {
        String fileName = null;
        try {
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIndex != -1) {
                    fileName = cursor.getString(nameIndex);
                }
                cursor.close();
            }
        } catch (Exception e) {
            fileName = uri.getLastPathSegment();
        }
        return fileName;
    }

    public String getHexFileContent() {
        return hexFileContent;
    }

    public boolean hasFileLoaded() {
        return hexFileContent != null && !hexFileContent.isEmpty();
    }

    public void clearFileContent() {
        hexFileContent = "";
    }

    private void notifyFileLoaded(String content, String fileName) {
        if (fileLoadListener != null) {
            fileLoadListener.onFileLoaded(content, fileName);
        }
    }

    /**
     * Copia de archivos Zero-Copy utilizando FileChannel.transferTo() a nivel de kernel de Linux.
     */
    public static void copyFileZeroCopy(java.io.File source, java.io.File dest) throws IOException {
        try {
            try (java.io.FileInputStream fis = new java.io.FileInputStream(source);
                 java.io.FileOutputStream fos = new java.io.FileOutputStream(dest);
                 java.nio.channels.FileChannel srcChannel = fis.getChannel();
                 java.nio.channels.FileChannel dstChannel = fos.getChannel()) {
                long size = srcChannel.size();
                long position = 0;
                while (position < size) {
                    position += srcChannel.transferTo(position, size - position, dstChannel);
                }
            }
        } catch (IOException e) {
            // Fallback a copiado tradicional por streams si el sistema de archivos no soporta sendfile
            try (java.io.InputStream in = new java.io.BufferedInputStream(new java.io.FileInputStream(source), 8192);
                 java.io.OutputStream out = new java.io.BufferedOutputStream(new java.io.FileOutputStream(dest), 8192)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
                out.flush();
            }
        }
    }

    private void notifyError(String errorMessage) {
        if (fileLoadListener != null) {
            fileLoadListener.onFileLoadError(errorMessage);
        }
    }
}
