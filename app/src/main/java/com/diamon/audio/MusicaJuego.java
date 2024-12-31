package com.diamon.audio;

import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;

import com.diamon.nucleo.Musica;

public class MusicaJuego implements Musica, OnCompletionListener {

    private MediaPlayer mediaPlayer;

    private boolean preparado;

    public MusicaJuego(AssetFileDescriptor descriptor) {

        preparado = false;

        mediaPlayer = new MediaPlayer();

        try {
            mediaPlayer.setDataSource(
                    descriptor.getFileDescriptor(),
                    descriptor.getStartOffset(),
                    descriptor.getLength());

            mediaPlayer.prepare();

            preparado = true;

            mediaPlayer.setOnCompletionListener(this);

        } catch (Exception e) {

        }
    }

    @Override
    public void reproducir() {

        if (mediaPlayer.isPlaying()) {

            return;
        }

        try {

            synchronized (this) {
                if (!preparado) {

                    mediaPlayer.prepare();
                }

                mediaPlayer.start();
            }

        } catch (Exception e) {

        }
    }

    @Override
    public void pausar() {

        if (mediaPlayer.isPlaying()) {

            mediaPlayer.pause();
        }
    }

    @Override
    public void terminar() {

        mediaPlayer.stop();

        synchronized (this) {
            preparado = false;
        }
    }

    @Override
    public void setRepetir(boolean repetir) {

        mediaPlayer.setLooping(repetir);
    }

    @Override
    public void setVolumen(float volumen) {

        mediaPlayer.setVolume(volumen, volumen);
    }

    @Override
    public boolean isReproduciendo() {

        return mediaPlayer.isPlaying();
    }

    @Override
    public boolean isRepitiendo() {

        return mediaPlayer.isLooping();
    }

    @Override
    public boolean isTerminado() {

        return !preparado;
    }

    @Override
    public void liberarRecurso() {

        if (mediaPlayer.isPlaying()) {

            mediaPlayer.stop();
        }

        mediaPlayer.release();
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {

        synchronized (this) {
            preparado = false;
        }
    }
}
