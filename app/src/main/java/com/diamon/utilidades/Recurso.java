package com.diamon.utilidad;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.SoundPool;

import com.diamon.audio.MusicaJuego;
import com.diamon.audio.SonidoJuego;
import com.diamon.graficos.Textura2D;
import com.diamon.nucleo.Musica;
import com.diamon.nucleo.Sonido;
import com.diamon.nucleo.Textura;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

public class Recurso {

    private HashMap<String, Textura> texturas;

    private HashMap<String, Sonido> sonidos;

    private HashMap<String, Musica> musicas;

    private Context contexto;

    public Recurso(Context contexto) {

        sonidos = new HashMap<String, Sonido>();

        musicas = new HashMap<String, Musica>();

        texturas = new HashMap<String, Textura>();

        this.contexto = contexto;
    }

    public Textura cargarTextura(String nombre) {

        InputStream entrada = null;

        Textura imagen = null;

        final BitmapFactory.Options options = new BitmapFactory.Options();

        try {
            // Primer paso: obtener solo las dimensiones
            entrada = contexto.getAssets().open(nombre);

            options.inJustDecodeBounds = true;

            BitmapFactory.decodeStream(entrada, null, options);

            entrada.close();

            // Calcular inSampleSize para reducir la imagen si es necesario
            options.inSampleSize = calculateInSampleSize(options, 800);

            options.inJustDecodeBounds = false;

            // Volver a abrir el InputStream para decodificar la imagen completa
            entrada = contexto.getAssets().open(nombre);

            Bitmap bitmap = BitmapFactory.decodeStream(entrada, null, options);

            // Crear la textura con el Bitmap decodificado
            imagen = new Textura2D(bitmap);

            texturas.put(nombre, imagen);

        } catch (IOException e) {

            e.printStackTrace(); // Muestra cualquier excepción que ocurra

        } finally {

            if (entrada != null) {

                try {

                    entrada.close();

                } catch (IOException e) {

                    e.printStackTrace();
                }
            }
        }

        return texturas.get(nombre);
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int maxTextureSize) {

        // Obtener el ancho y alto originales
        final int height = options.outHeight;

        final int width = options.outWidth;

        int inSampleSize = 1;

        // Si las dimensiones originales superan el tamaño máximo permitido
        if (height > maxTextureSize || width > maxTextureSize) {

            final int halfHeight = height / 2;

            final int halfWidth = width / 2;

            // Calcular el valor adecuado de inSampleSize para reducir el bitmap
            while ((halfHeight / inSampleSize) >= maxTextureSize
                    && (halfWidth / inSampleSize) >= maxTextureSize) {

                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public Textura getTextura(String nombre) {

        Textura imagen = texturas.get(nombre);

        if (imagen == null) {

            imagen = cargarTextura(nombre);

            texturas.put(nombre, imagen);
        }

        return imagen;
    }

    public Musica cargarMusica(String nombre) {

        AssetFileDescriptor descriptor = null;

        try {

            descriptor = contexto.getAssets().openFd(nombre);

        } catch (IOException e) {

        }

        Musica musica = new MusicaJuego(descriptor);

        musicas.put(nombre, musica);

        return musicas.get(nombre);
    }

    public Musica getMusica(String nombre) {

        Musica musica = musicas.get(nombre);

        if (musica == null) {

            musica = cargarMusica(nombre);

            musicas.put(nombre, musica);
        }

        return musica;
    }

    public Sonido cargarSonido(String nombre) {

        AssetFileDescriptor descriptor = null;

        try {

            descriptor = contexto.getAssets().openFd(nombre);

        } catch (IOException e) {

        }

        final SoundPool sonidoPool = new SoundPool(200, AudioManager.STREAM_MUSIC, 0);

        int id = sonidoPool.load(descriptor, 0);

        Sonido sonido = new SonidoJuego(id, sonidoPool);

        sonidos.put(nombre, sonido);

        return sonidos.get(nombre);
    }

    public Sonido getSonido(String nombre) {

        Sonido sonido = sonidos.get(nombre);

        if (sonido == null) {

            sonido = cargarSonido(nombre);

            sonidos.put(nombre, sonido);
        }

        return sonido;
    }
}
