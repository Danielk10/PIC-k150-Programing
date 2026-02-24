package com.diamon.datos;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import com.diamon.nucleo.Datos;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class CargardorDeArchivos implements Datos {

    public static final String DATOS = "chipinfo.cid";

    private AssetManager manejador;

    private Activity actividad;

    public CargardorDeArchivos(Activity actividad) {

        this.actividad = actividad;

        this.manejador = actividad.getAssets();
    }

    @Override
    public InputStream leerDatoInterno(String nombre) throws IOException {
        return actividad.openFileInput(nombre);
    }

    @Override
    public OutputStream escribirDatoInterno(String nombre) throws IOException {
        return actividad.openFileOutput(nombre, Context.MODE_PRIVATE);
    }

    @Override
    public InputStream leerAsset(String nombre) throws IOException {

        return manejador.open(nombre);
    }
}
