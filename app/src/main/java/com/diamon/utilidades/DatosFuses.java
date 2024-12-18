package com.diamon.utilidades;

import java.util.ArrayList;

public class DatosFuses {

    private String titulo;

    private ArrayList<String> description;

    private ArrayList<String> valor;

    public DatosFuses() {

        titulo = new String();

        description = new ArrayList<String>();

        valor = new ArrayList<String>();
    }

    public String getTitulo() {
        return this.titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public ArrayList<String> getDescription() {
        return this.description;
    }

    public void setDescription(String descripcion) {
        this.description.add(descripcion);
    }

    public ArrayList<String> getValor() {
        return this.valor;
    }

    public void setValor(String valor) {
        this.valor.add(valor);
    }
}
