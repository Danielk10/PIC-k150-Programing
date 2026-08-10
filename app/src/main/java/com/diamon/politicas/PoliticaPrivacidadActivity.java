package com.diamon.politicas;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.webkit.WebView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.diamon.utilidades.GestorPantalla;

public class PoliticaPrivacidadActivity extends AppCompatActivity {

    private WebView pagina;

    private LinearLayout diseno;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Configurar edge-to-edge para Android 15+
        GestorPantalla gestorPantalla = new GestorPantalla(this);
        gestorPantalla.habilitarEdgeToEdge();

        diseno = new LinearLayout(this);

        diseno.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams parametros = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);

        pagina = new WebView(this);

        diseno.addView(pagina, parametros);

        setContentView(diseno);

        // Aplicar window insets al layout para evitar que se oculte contenido
        gestorPantalla.aplicarWindowInsets(diseno);

        pagina.getSettings().setJavaScriptEnabled(true);

        pagina.loadUrl(
                "https://politicasdeprivacidaspickprograming.blogspot.com/2024/12/pic-k150-programming.html?m=1");
    }
}
