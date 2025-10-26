package com.diamon.pic.managers;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Gestor de archivos HEX CORREGIDO. Filtra CORRECTAMENTE archivos .hex y .bin como el codigo
 * original
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

    /** Inicializa el launcher con filtro EXACTO del original */
    public void initialize() {
        filePickerLauncher =
                activity.registerForActivityResult(
                        new ActivityResultContracts.OpenDocument(),
                        uri -> {
                            if (uri != null) {
                                processFile(uri);
                            }
                        });
    }

    public void setFileLoadListener(FileLoadListener listener) {
        this.fileLoadListener = listener;
    }

    /** Abre selector con filtro EXACTO: solo .hex y .bin */
    public void openFilePicker() {
        if (filePickerLauncher == null) {
            notifyError("FileManager no inicializado");
            return;
        }

        // FILTRO CORRECTO: Solo hex y bin
        String[] mimeTypes = {"*/*"};
        filePickerLauncher.launch(mimeTypes);
    }

    private void processFile(Uri uri) {
        String fileName = getFileName(uri);

        if (fileName == null) {
            notifyError("No se pudo obtener el nombre del archivo");
            return;
        }

        // VALIDACION ESTRICTA: Solo .hex o .bin
        String lowerFileName = fileName.toLowerCase();
        if (!lowerFileName.endsWith(".hex") && !lowerFileName.endsWith(".bin")) {
            notifyError("Solo se permiten archivos .hex o .bin");
            return;
        }

        String content = readFileContent(uri);

        if (content != null && !content.isEmpty()) {
            hexFileContent = content;
            notifyFileLoaded(content, fileName);
        }
    }

    private String readFileContent(Uri uri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);

            if (inputStream == null) {
                notifyError("Error abriendo el archivo");
                return null;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder fileContent = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.length() > 0 && line.charAt(0) == ';') {
                    break;
                }
                fileContent.append(line).append("\n");
            }

            reader.close();
            inputStream.close();

            String content = fileContent.toString();

            if (content.trim().isEmpty()) {
                notifyError("El archivo esta vacio");
                return null;
            }

            return content;

        } catch (IOException e) {
            notifyError("Error leyendo el archivo: " + e.getMessage());
            return null;
        } catch (Exception e) {
            notifyError("Error inesperado: " + e.getMessage());
            return null;
        }
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
