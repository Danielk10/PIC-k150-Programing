package com.diamon.managers;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.diamon.pic.R;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

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

    /** Abre el selector de documentos con tipos HEX/BIN compatibles. */
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

    /** Valida extensión y carga el contenido según tipo de archivo. */
    private void processSelectedFile(Uri uri) {
        String fileName = getFileName(uri);

        if (fileName == null) {
            notifyError(context.getString(R.string.no_se_pudo_obtener_el_nombre_d));
            return;
        }

        String lowerFileName = fileName.toLowerCase(Locale.ROOT);
        if (!lowerFileName.endsWith(".bin") && !lowerFileName.endsWith(".hex")) {
            notifyError(context.getString(R.string.seleccione_un_archivo_binario_));
            return;
        }

        if (lowerFileName.endsWith(".hex")) {
            hexFileContent = leerArchivoHex(uri, fileName);
        } else {
            hexFileContent = leerArchivoBinarioComoHex(uri, fileName);
        }
    }

    /** Lee un archivo HEX ignorando líneas de comentario iniciadas con ';'. */
    private String leerArchivoHex(Uri uri, String fileName) {
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
            if (inputStream == null) {
                notifyError(context.getString(R.string.error_abriendo_el_archivo_sele));
                return "";
            }

            StringBuilder fileContent = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.length() > 0 && line.charAt(0) == ';') {
                        continue;
                    }
                    fileContent.append(line).append("\n");
                }
            }

            String content = fileContent.toString();
            if (content.trim().isEmpty()) {
                notifyError(context.getString(R.string.el_archivo_seleccionado_esta_v));
                return "";
            }

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

    /** Lee un BIN y lo convierte a Intel HEX para el flujo de procesado existente. */
    private String leerArchivoBinarioComoHex(Uri uri, String fileName) {
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
            if (inputStream == null) {
                notifyError(context.getString(R.string.error_abriendo_el_archivo_sele));
                return "";
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int leidos;
            while ((leidos = inputStream.read(buffer)) != -1) {
                baos.write(buffer, 0, leidos);
            }

            byte[] rawData = baos.toByteArray();
            if (rawData.length == 0) {
                notifyError(context.getString(R.string.el_archivo_seleccionado_esta_v));
                return "";
            }

            String intelHex = HexExportManager.convertToIntelHex(rawData);
            notifyFileLoaded(intelHex, fileName);
            return intelHex;

        } catch (IOException e) {
            notifyError(context.getString(R.string.error_leyendo_el_archivo) + ": " + e.getMessage());
            return "";
        } catch (Exception e) {
            notifyError(context.getString(R.string.error_inesperado_leyendo_el_ar));
            return "";
        }
    }

    private String getFileName(Uri uri) {
        String fileName = null;
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIndex != -1) {
                    fileName = cursor.getString(nameIndex);
                }
            }
        } catch (Exception e) {
            fileName = uri.getLastPathSegment();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
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
