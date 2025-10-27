package com.diamon.tutorial;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.diamon.datos.CargardorDeArchivos;
import com.diamon.pic.R;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class TutorialGputilsActivity extends AppCompatActivity {

    private TextView tutorialTextView;
    private ScrollView scrollView;
    private Spinner languageSpinner;
    private Button copyButton;
    private ImageView tutorialImageView;
    private CargardorDeArchivos fileLoader;
    private String currentLanguage = "es";
    private String tutorialText = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial_gputils);

        // Inicializar componentes
        tutorialTextView = findViewById(R.id.tutorialTextView);
        scrollView = findViewById(R.id.tutorialScrollView);
        languageSpinner = findViewById(R.id.languageSpinner);
        copyButton = findViewById(R.id.btnCopyTutorial);
        tutorialImageView = findViewById(R.id.tutorialImageView);

        // Inicializar cargador de archivos
        fileLoader = new CargardorDeArchivos(this);

        // Configurar spinner de idiomas
        setupLanguageSpinner();

        // Cargar tutorial inicial en español
        loadTutorial("es");

        // Configurar eventos
        copyButton.setOnClickListener(v -> copyTutorialText());
    }

    private void setupLanguageSpinner() {
        String[] languages = {"Español", "English"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, languages);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        languageSpinner.setAdapter(adapter);

        languageSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view,
                                       int position, long id) {
                String selectedLanguage = position == 0 ? "es" : "en";
                if (!currentLanguage.equals(selectedLanguage)) {
                    currentLanguage = selectedLanguage;
                    loadTutorial(selectedLanguage);
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });
    }

    private void loadTutorial(String language) {
        String fileName = language.equals("es") ? "tutorial_gputils_es.txt" : "tutorial_gputils_en.txt";

        try {
            InputStream inputStream = fileLoader.leerAsset(fileName);
            byte[] buffer = new byte[inputStream.available()];
            inputStream.read(buffer);
            inputStream.close();

            tutorialText = new String(buffer, StandardCharsets.UTF_8);

            // Procesar y mostrar el texto con formatos especiales
            displayFormattedTutorial(tutorialText);

            // Cargar imagen
            loadTutorialImage();

        } catch (IOException e) {
            Toast.makeText(this, "Error al cargar tutorial: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void displayFormattedTutorial(String text) {
        // Mostrar el texto en el TextView
        tutorialTextView.setText(text);

        // Hacer que el texto sea seleccionable
        tutorialTextView.setTextIsSelectable(true);
    }

    private void loadTutorialImage() {
        try {
            // Cargar la imagen desde assets usando BitmapFactory
            InputStream inputStream = fileLoader.leerAsset("compilacion.jpg");
            android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(inputStream);
            inputStream.close();

            tutorialImageView.setImageBitmap(bitmap);

        } catch (IOException e) {
            // Si la imagen no existe, mostrar un icono de marcador de posición
            tutorialImageView.setImageDrawable(ContextCompat.getDrawable(this,
                    android.R.drawable.ic_menu_gallery));
        }
    }

    private void copyTutorialText() {
        if (!tutorialText.isEmpty()) {
            android.content.ClipboardManager clipboard =
                    (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText(
                    "tutorial", tutorialText);
            clipboard.setPrimaryClip(clip);

            Toast.makeText(this, currentLanguage.equals("es") ?
                    "Tutorial copiado al portapapeles" :
                    "Tutorial copied to clipboard", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Método estático para copiar código específico al portapapeles
     *
     * @param context Contexto de la aplicación
     * @param code Código a copiar
     */
    public static void copyCodeToClipboard(android.content.Context context, String code) {
        android.content.ClipboardManager clipboard =
                (android.content.ClipboardManager) context.getSystemService(
                        android.content.Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText("code", code);
        clipboard.setPrimaryClip(clip);

        Toast.makeText(context, "Código copiado", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Mantener el idioma seleccionado al cambiar orientación
    }
}