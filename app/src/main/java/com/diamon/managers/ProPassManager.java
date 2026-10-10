package com.diamon.managers;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.diamon.billing.BillingManager;

import java.util.concurrent.TimeUnit;

/**
 * Gestor del Pase Temporal K150 Pro (12 Horas).
 * Otorga acceso a las herramientas Pro tras ver un anuncio recompensado (Rewarded Ad).
 */
public class ProPassManager {

    public static final String PREFS_NAME = "pro_pass_prefs";
    public static final String KEY_EXPIRE_TIME = "pro_pass_expire_time";

    /** Duración estándar del pase libre: 12 horas en milisegundos. */
    public static final long DURACION_PASE_MS = 12 * 60 * 60 * 1000L;

    private final SharedPreferences prefs;

    public ProPassManager(@NonNull Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    @VisibleForTesting
    public ProPassManager(@NonNull SharedPreferences prefs) {
        this.prefs = prefs;
    }

    /**
     * Activa el pase temporal por 12 horas a partir del momento actual.
     */
    public void activarPase12Horas() {
        long nuevoVencimiento = System.currentTimeMillis() + DURACION_PASE_MS;
        prefs.edit().putLong(KEY_EXPIRE_TIME, nuevoVencimiento).apply();
    }

    /**
     * Retorna true si el pase temporal de 12 horas está actualmente activo.
     */
    public boolean hasActiveProPass() {
        return System.currentTimeMillis() < getExpireTime();
    }

    /**
     * Alias retrocompatible para verificar si el pase está activo.
     */
    public boolean isPaseActivo() {
        return hasActiveProPass();
    }

    /**
     * Retorna la marca de tiempo (timestamp en ms) de expiración del pase.
     */
    public long getExpireTime() {
        return prefs.getLong(KEY_EXPIRE_TIME, 0L);
    }

    /**
     * Alias retrocompatible para obtener la marca de tiempo de expiración.
     */
    public long getExpirationTimestamp() {
        return getExpireTime();
    }

    /**
     * Retorna los milisegundos restantes del pase activo (0 si ha expirado).
     */
    public long getTiempoRestanteMs() {
        return Math.max(0, getExpireTime() - System.currentTimeMillis());
    }

    /**
     * Retorna un texto formateado del tiempo restante (ej: "11h 45m" o "Expirado").
     */
    public String getTiempoRestanteFormateado() {
        long restante = getTiempoRestanteMs();
        if (restante <= 0) {
            return "Expirado";
        }

        long horas = TimeUnit.MILLISECONDS.toHours(restante);
        long minutos = TimeUnit.MILLISECONDS.toMinutes(restante) % 60;

        if (horas > 0) {
            return horas + "h " + minutos + "m";
        } else if (minutos > 0) {
            return minutos + "m";
        } else {
            long segundos = TimeUnit.MILLISECONDS.toSeconds(restante);
            return Math.max(1, segundos) + "s";
        }
    }

    /**
     * Alias retrocompatible para formatear el tiempo restante.
     */
    public String formatearTiempoRestante() {
        return getTiempoRestanteFormateado();
    }

    /**
     * Evalúa si las funciones Pro están habilitadas, ya sea por compra permanente
     * de Google Play Billing o por un pase temporal de 12 horas activo.
     *
     * @param billingManager Gestor de facturación permanente (opcional).
     * @return true si el usuario tiene acceso Pro activo.
     */
    public boolean isProActive(@Nullable BillingManager billingManager) {
        return (billingManager != null && billingManager.isAdsRemoved()) || hasActiveProPass();
    }

    /**
     * Permite fijar directamente la marca de tiempo de expiración (para pruebas unitarias).
     */
    @VisibleForTesting
    public void setExpireTime(long expireTime) {
        prefs.edit().putLong(KEY_EXPIRE_TIME, expireTime).apply();
    }

    /**
     * Limpia el pase pro en preferencias (útil para pruebas unitarias o reseteo).
     */
    @VisibleForTesting
    public void limpiarPase() {
        prefs.edit().remove(KEY_EXPIRE_TIME).apply();
    }
}
