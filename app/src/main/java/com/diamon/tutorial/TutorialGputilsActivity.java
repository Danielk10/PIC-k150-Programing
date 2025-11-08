package com.diamon.tutorial;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.view.KeyEvent;
import android.graphics.Typeface;

import com.diamon.utilidades.PantallaCompleta;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.diamon.datos.CargardorDeArchivos;
import com.diamon.pic.R;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Activity mejorada para mostrar tutoriales de GPUTILS
 * Con formato profesional, comandos en cajas negras, código con numeración
 * y enlaces clickeables
 */
public class TutorialGputilsActivity extends AppCompatActivity {

    private LinearLayout tutorialContainer;
    private ScrollView scrollView;
    private Spinner languageSpinner;
    private Button copyButton;
    private ImageView tutorialImageView;
    private TextView languageInfoTextView;
    
    private CargardorDeArchivos fileLoader;
    private String currentLanguage = "es";
    private String tutorialText = "";
    private PantallaCompleta pantallaCompleta;
    
    private TutorialContentRenderer contentRenderer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial_gputils);

        pantallaCompleta = new PantallaCompleta(this);
        pantallaCompleta.pantallaCompleta();
        pantallaCompleta.ocultarBotonesVirtuales();

        // Inicializar componentes
        tutorialContainer = findViewById(R.id.tutorialContainer);
        scrollView = findViewById(R.id.tutorialScrollView);
        languageSpinner = findViewById(R.id.languageSpinner);
        copyButton = findViewById(R.id.btnCopyTutorial);
        tutorialImageView = findViewById(R.id.tutorialImageView);
        languageInfoTextView = findViewById(R.id.languageInfoTextView);

        // Inicializar cargador de archivos y renderizador
        fileLoader = new CargardorDeArchivos(this);
        contentRenderer = new TutorialContentRenderer(this, tutorialContainer);

        // Configurar spinner de idiomas
        setupLanguageSpinner();

        // Cargar tutorial inicial en español
        loadTutorial("es");
        updateLanguageInfo("es");

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
                    updateLanguageInfo(selectedLanguage);
                    updateCopyButtonText(selectedLanguage);
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

            // Renderizar el tutorial con formato profesional
            contentRenderer.setLanguage(language);
            contentRenderer.renderTutorial(tutorialText);

            // Cargar imagen
            loadTutorialImage();

        } catch (IOException e) {
            Toast.makeText(this, "Error al cargar tutorial: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void loadTutorialImage() {
        try {
            InputStream inputStream = fileLoader.leerAsset("compilacion.jpg");
            android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(inputStream);
            inputStream.close();
            tutorialImageView.setImageBitmap(bitmap);
        } catch (IOException e) {
            tutorialImageView.setImageDrawable(ContextCompat.getDrawable(this,
                    android.R.drawable.ic_menu_gallery));
        }
    }

    private void updateLanguageInfo(String language) {
        if (language.equals("es")) {
            languageInfoTextView.setText("Idioma: Español");
        } else {
            languageInfoTextView.setText("Language: English");
        }
    }

    private void updateCopyButtonText(String language) {
        if (language.equals("es")) {
            copyButton.setText("📋 Copiar Todo");
        } else {
            copyButton.setText("📋 Copy All");
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

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        loadTutorial(currentLanguage);
        updateLanguageInfo(currentLanguage);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            pantallaCompleta.ocultarBotonesVirtuales();
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        pantallaCompleta.ocultarBotonesVirtuales();
        return super.onKeyUp(keyCode, event);
    }
}
