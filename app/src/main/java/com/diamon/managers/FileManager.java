package com.diamon.managers;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

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

    public interface FileLoadListener {
        void onFileLoaded(String content, String fileName);

        void onFileLoadError(String errorMessage);
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

        hexFileContent = readFileContent(uri, lowerFileName.endsWith(".bin"));
    }

    /** Lee archivo HEX o BIN y lo retorna en formato Intel HEX textual. */
    private String readFileContent(Uri uri, boolean isBinary) {
        if (isBinary) {
            return readBinaryAsIntelHex(uri);
        }
        return readHexText(uri);
    }

    /** Lee archivo .hex de texto hasta comentario ';' o EOF. */
    private String readHexText(Uri uri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);

            if (inputStream == null) {
                notifyError(context.getString(R.string.error_abriendo_el_archivo_sele));
                return "";
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder fileContent = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                // En formato HEX, una línea iniciada con ';' se considera comentario.
                if (line.length() > 0 && line.charAt(0) == ';') {
                    break;
                }
                fileContent.append(line).append("\n");
            }

            reader.close();
            inputStream.close();

            String content = fileContent.toString();

            if (content.trim().isEmpty()) {
                notifyError(context.getString(R.string.el_archivo_seleccionado_esta_v));
                return "";
            }

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

    /** Lee archivo .bin y lo convierte a Intel HEX de forma segura. */
    private String readBinaryAsIntelHex(Uri uri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                notifyError(context.getString(R.string.error_abriendo_el_archivo_sele));
                return "";
            }

            byte[] data = readAllBytes(inputStream);
            inputStream.close();

            if (data.length == 0) {
                notifyError(context.getString(R.string.el_archivo_seleccionado_esta_v));
                return "";
            }

            String content = binaryToIntelHex(data);
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

    private byte[] readAllBytes(InputStream inputStream) throws IOException {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            baos.write(buffer, 0, read);
        }
        return baos.toByteArray();
    }

    private String binaryToIntelHex(byte[] data) {
        StringBuilder out = new StringBuilder();
        final int recordSize = 16;
        int currentUpper = -1;

        for (int address = 0; address < data.length; address += recordSize) {
            int upper = (address >>> 16) & 0xFFFF;
            if (upper != currentUpper) {
                currentUpper = upper;
                out.append(buildExtendedLinearAddressRecord(upper)).append('\n');
            }

            int count = Math.min(recordSize, data.length - address);
            int lowAddress = address & 0xFFFF;
            int checksum = count + ((lowAddress >> 8) & 0xFF) + (lowAddress & 0xFF);

            StringBuilder line = new StringBuilder(11 + (count * 2));
            line.append(':');
            line.append(String.format("%02X%04X00", count, lowAddress));

            for (int i = 0; i < count; i++) {
                int b = data[address + i] & 0xFF;
                line.append(String.format("%02X", b));
                checksum += b;
            }

            int finalChecksum = ((~checksum + 1) & 0xFF);
            line.append(String.format("%02X", finalChecksum));
            out.append(line).append('\n');
        }

        out.append(":00000001FF\n");
        return out.toString();
    }

    private String buildExtendedLinearAddressRecord(int upperAddress) {
        int high = (upperAddress >> 8) & 0xFF;
        int low = upperAddress & 0xFF;
        int checksum = (2 + 0 + 0 + 4 + high + low) & 0xFF;
        checksum = ((~checksum + 1) & 0xFF);
        return String.format(":02000004%02X%02X%02X", high, low, checksum);
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

    private void notifyError(String errorMessage) {
        if (fileLoadListener != null) {
            fileLoadListener.onFileLoadError(errorMessage);
        }
    }
}
