package com.diamon.tutorial;

import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;

import com.diamon.datos.CargardorDeArchivos;
import com.diamon.pic.R;
import com.diamon.tutorial.utilidadestutorial.TutorialContentParser;
import com.diamon.utilidades.PantallaCompleta;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;

public class TutorialGputilsActivity extends AppCompatActivity {

    private LinearLayout tutorialContentLayout;
    private NestedScrollView scrollView;
    private Spinner languageSpinner;
    private MaterialButton copyButton;
    private ImageView tutorialImageView;
    private TextView languageInfoTextView;
    private FloatingActionButton fabScrollTop;
    private ProgressBar loadingProgress;

    private CargardorDeArchivos fileLoader;
    private String currentLanguage = "es";
    private String tutorialText = "";
    private TutorialContentParser contentParser;
    private boolean isLoading = false;
    private PantallaCompleta pantallaCompleta;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial_gputils);

        pantallaCompleta = new PantallaCompleta(this);

        pantallaCompleta.pantallaCompleta();

        pantallaCompleta.ocultarBotonesVirtuales();

        initializeViews();

        fileLoader = new CargardorDeArchivos(this);
        contentParser = new TutorialContentParser(this, tutorialContentLayout);

        setupLanguageSpinner();
        setupEventListeners();
        setupScrollListener();

        // Cargar tutorial inicial
        loadTutorial("es");
        updateLanguageInfo("es");
    }

    private void initializeViews() {
        tutorialContentLayout = findViewById(R.id.tutorialContentLayout);
        scrollView = findViewById(R.id.tutorialScrollView);
        languageSpinner = findViewById(R.id.languageSpinner);
        copyButton = findViewById(R.id.btnCopyTutorial);
        tutorialImageView = findViewById(R.id.tutorialImageView);
        languageInfoTextView = findViewById(R.id.languageInfoTextView);
        fabScrollTop = findViewById(R.id.fabScrollTop);
        loadingProgress = findViewById(R.id.loadingProgress);
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
                if (!currentLanguage.equals(selectedLanguage) && !isLoading) {
                    currentLanguage = selectedLanguage;
                    loadTutorial(selectedLanguage);
                    updateLanguageInfo(selectedLanguage);
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });
    }

    private void setupEventListeners() {
        copyButton.setOnClickListener(v -> copyTutorialText());
        fabScrollTop.setOnClickListener(v -> scrollView.smoothScrollTo(0, 0));
    }

    private void setupScrollListener() {
        scrollView.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {
            @Override
            public void onScrollChange(NestedScrollView v, int scrollX, int scrollY,
                                       int oldScrollX, int oldScrollY) {
                if (scrollY > 300) {
                    fabScrollTop.show();
                } else {
                    fabScrollTop.hide();
                }
            }
        });
    }

    /**
     * Carga tutorial de forma optimizada con AsyncTask
     */
    private void loadTutorial(String language) {
        if (isLoading) return;

        new LoadTutorialTask(this, language).execute();
    }

    /**
     * AsyncTask para cargar tutorial en background
     */
    private static class LoadTutorialTask extends AsyncTask<Void, Void, TutorialData> {
        private WeakReference<TutorialGputilsActivity> activityRef;
        private String language;

        LoadTutorialTask(TutorialGputilsActivity activity, String language) {
            this.activityRef = new WeakReference<>(activity);
            this.language = language;
        }

        @Override
        protected void onPreExecute() {
            TutorialGputilsActivity activity = activityRef.get();
            if (activity != null) {
                activity.isLoading = true;
                activity.loadingProgress.setVisibility(View.VISIBLE);
                activity.tutorialContentLayout.setVisibility(View.GONE);
            }
        }

        @Override
        protected TutorialData doInBackground(Void... voids) {
            TutorialGputilsActivity activity = activityRef.get();
            if (activity == null) return null;

            TutorialData data = new TutorialData();
            String fileName = language.equals("es") ?
                    "tutorial_gputils_es.txt" : "tutorial_gputils_en.txt";

            try {
                InputStream inputStream = activity.fileLoader.leerAsset(fileName);
                byte[] buffer = new byte[inputStream.available()];
                inputStream.read(buffer);
                inputStream.close();
                data.tutorialText = new String(buffer, StandardCharsets.UTF_8);
                data.success = true;
            } catch (IOException e) {
                data.error = e.getMessage();
                data.success = false;
            }

            return data;
        }

        @Override
        protected void onPostExecute(TutorialData data) {
            TutorialGputilsActivity activity = activityRef.get();
            if (activity == null || data == null) return;

            activity.loadingProgress.setVisibility(View.GONE);
            activity.tutorialContentLayout.setVisibility(View.VISIBLE);

            if (data.success) {
                activity.tutorialText = data.tutorialText;

                // Limpiar y parsear
                activity.tutorialContentLayout.removeAllViews();
                activity.contentParser.parseTutorial(data.tutorialText, language);

                // Cargar imagen
                activity.loadTutorialImage();
            } else {
                Toast.makeText(activity, "Error: " + data.error, Toast.LENGTH_SHORT).show();
            }

            activity.isLoading = false;
        }
    }

    /**
     * Clase para datos del tutorial
     */
    private static class TutorialData {
        String tutorialText;
        String error;
        boolean success;
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
            languageInfoTextView.setText("🌐 Idioma: Español");
        } else {
            languageInfoTextView.setText("🌐 Language: English");
        }
    }

    private void copyTutorialText() {
        if (!tutorialText.isEmpty()) {
            android.content.ClipboardManager clipboard =
                    (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText(
                    "tutorial", tutorialText);
            clipboard.setPrimaryClip(clip);

            String message = currentLanguage.equals("es") ?
                    "Tutorial copiado al portapapeles" :
                    "Tutorial copied to clipboard";
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (!isLoading) {
            loadTutorial(currentLanguage);
            updateLanguageInfo(currentLanguage);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tutorialContentLayout != null) {
            tutorialContentLayout.removeAllViews();
        }
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