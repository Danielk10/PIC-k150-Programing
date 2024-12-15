package com.diamon.datos;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.BufferedReader;
import android.os.Environment;
import java.io.File;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.ArrayList;

public class DatosDePic {

    private Context contexto;

    private CargardorArchivos datos;

    public DatosDePic(Activity actividad) {

        contexto = ((AppCompatActivity) actividad).getApplicationContext();

        datos = new CargardorArchivos(actividad);
    }

    public ArrayList<String> getInformacionPic() {

        final ArrayList<String> texto = new ArrayList<String>();

        BufferedReader buferarchivoLeer = null;

        try {

            buferarchivoLeer =
                    new BufferedReader(
                            new InputStreamReader(
                                    datos.leerAsset(CargardorArchivos.DATOS), "UTF-8"));

            String lineas = "";

            while ((lineas = buferarchivoLeer.readLine()) != null) {

                texto.add(lineas);
            }

        } catch (IOException e) {

        } finally {
            try {
                if (buferarchivoLeer != null) {

                    buferarchivoLeer.close();
                }

            } catch (IOException e) {

            }
        }

        return texto;
    }
}
