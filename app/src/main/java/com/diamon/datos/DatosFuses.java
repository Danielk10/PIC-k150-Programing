package com.diamon.datos;

import java.util.ArrayList;

public class DatosFuses {

    private String titulo;

    private ArrayList<String> description;

    private ArrayList<Integer> valor;

    public DatosFuses() {

        titulo = new String();

        description = new ArrayList<String>();

        valor = new ArrayList<Integer>();
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

    public ArrayList<Integer> getValor() {
        return this.valor;
    }

    public void setValor(Integer valor) {
        this.valor.add(valor);
    }
}
