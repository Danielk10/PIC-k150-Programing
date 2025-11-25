package com.diamon.politicas;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.webkit.WebView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.diamon.utilidades.PantallaCompleta;

public class Politicas extends AppCompatActivity {

    private WebView pagina;

    private LinearLayout diseno;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Configurar edge-to-edge para Android 15+
        PantallaCompleta pantallaCompleta = new PantallaCompleta(this);
        pantallaCompleta.habilitarEdgeToEdge();

        diseno = new LinearLayout(this);

        diseno.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams parametros = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);

        pagina = new WebView(this);

        diseno.addView(pagina, parametros);

        setContentView(diseno);

        // Aplicar window insets al layout para evitar que se oculte contenido
        pantallaCompleta.aplicarWindowInsets(diseno);

        pagina.getSettings().setJavaScriptEnabled(true);

        pagina.loadUrl(
                "https://politicasdeprivacidaspickprograming.blogspot.com/2024/12/pic-k150-programming.html?m=1");
    }
}
