package com.diamon.nucleo;

public interface Musica {

    public void reproducir();

    public void pausar();

    public void terminar();

    public void setRepetir(boolean repetir);

    public void setVolumen(float volumen);

    public boolean isReproduciendo();

    public boolean isRepitiendo();

    public boolean isTerminado();

    public void liberarRecurso();
}
