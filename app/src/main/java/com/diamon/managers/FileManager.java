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

/**
 * FileManager CORREGIDO - Filtro EXACTO del original
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
        filePickerLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri != null) {
                        processSelectedFile(uri);
                    }
                }
        );
    }

    public void setFileLoadListener(FileLoadListener listener) {
        this.fileLoadListener = listener;
    }

    /**
     * Filtro EXACTO del original
     */
    public void openFilePicker() {
        if (filePickerLauncher == null) {
            notifyError("FileManager no inicializado");
            return;
        }

        // FILTROS EXACTOS del codigo original
        String[] mimeTypes = {
                "application/octet-stream",
                "application/x-binary"
        };

        filePickerLauncher.launch(mimeTypes);
    }

    /**
     * Procesa archivo seleccionado - LOGICA EXACTA del original
     */
    private void processSelectedFile(Uri uri) {
        String fileName = getFileName(uri);

        if (fileName == null) {
            notifyError("No se pudo obtener el nombre del archivo");
            return;
        }

        // Validar extension .hex o .bin (case-insensitive)
        String lowerFileName = fileName.toLowerCase();
        if (!lowerFileName.endsWith(".bin") && !lowerFileName.endsWith(".hex")) {
            notifyError("Seleccione un archivo binario valido (.hex o .bin)");
            return;
        }

        // Leer archivo
        hexFileContent = readHexFile(uri);
    }


    /**
     * Lee archivo HEX - LOGICA EXACTA del original
     */
    private String readHexFile(Uri uri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);

            if (inputStream == null) {
                notifyError("Error abriendo el archivo seleccionado");
                return "";
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder fileContent = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                // Parar en comentarios (lineas con ';')
                if (line.length() > 0 && line.charAt(0) == ';') {
                    break;
                }
                fileContent.append(line).append("\n");
            }

            reader.close();
            inputStream.close();

            String content = fileContent.toString();

            if (content.trim().isEmpty()) {
                notifyError("El archivo seleccionado esta vacio");
                return "";
            }

            // Notificar exito
            String fileName = getFileName(uri);
            notifyFileLoaded(content, fileName);

            return content;

        } catch (IOException e) {
            notifyError("Error leyendo el archivo: " + e.getMessage());
            return "";
        } catch (Exception e) {
            notifyError("Error inesperado leyendo el archivo");
            return "";
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
