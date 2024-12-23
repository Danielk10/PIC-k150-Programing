package com.diamon.nucleo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface Datos {

    public InputStream leerDatoExterno(String nombre) throws IOException;

    public OutputStream escribirDatoExterno(String nombre) throws IOException;

    public InputStream leerDatoInterno(String nombre) throws IOException;

    public OutputStream escribirDatoInterno(String nombre) throws IOException;

    public InputStream leerAsset(String nombre) throws IOException;
}
