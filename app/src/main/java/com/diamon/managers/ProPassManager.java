package com.diamon.managers;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.diamon.billing.BillingManager;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Gestor del Pase Temporal K150 Pro (12 Horas).
 * Otorga acceso a las herramientas Pro tras ver un anuncio recompensado (Rewarded Ad).
 * Incluye respaldo firmado en almacenamiento persistente, sincronización cloud en Firebase Firestore
 * y Google Cloud Backup para que el usuario no pierda sus 12 horas al desinstalar o reinstalar la app.
 */
public class ProPassManager {

    private static final String TAG = "ProPassManager";
    public static final String PREFS_NAME = "pro_pass_prefs";
    public static final String KEY_EXPIRE_TIME = "pro_pass_expire_time";
    private static final String BACKUP_FILENAME = ".k150_pro_pass.dat";

    /** Duración estándar del pase libre: 12 horas en milisegundos. */
    public static final long DURACION_PASE_MS = 12 * 60 * 60 * 1000L;

    public interface OnPassRestoredListener {
        void onPassRestored(long expireTime);
    }

    private final SharedPreferences prefs;
    private final Context context;
    private OnPassRestoredListener onPassRestoredListener;

    public ProPassManager(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (!hasActiveProPass()) {
            intentarRestaurarRespaldo();
            intentarRestaurarDesdeFirestore(null);
        }
    }

    @VisibleForTesting
    public ProPassManager(@NonNull SharedPreferences prefs) {
        this.context = null;
        this.prefs = prefs;
    }

    public void setOnPassRestoredListener(@Nullable OnPassRestoredListener listener) {
        this.onPassRestoredListener = listener;
    }

    /**
     * Activa el pase temporal por 12 horas a partir del momento actual.
     */
    public void activarPase12Horas() {
        long nuevoVencimiento = System.currentTimeMillis() + DURACION_PASE_MS;
        prefs.edit().putLong(KEY_EXPIRE_TIME, nuevoVencimiento).apply();
        guardarRespaldoPersistente(nuevoVencimiento);
        guardarRespaldoEnFirestore(nuevoVencimiento);
    }

    private String getDeviceId() {
        if (context == null) return "test_device";
        try {
            String id = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
            return (id != null) ? id : "unknown_device";
        } catch (Exception e) {
            return "unknown_device";
        }
    }

    private String calcularHash(String deviceId, long expireTime) {
        try {
            String payload = deviceId + ":" + expireTime + ":PIC_K150_PRO_PASS_SALT";
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private void guardarRespaldoPersistente(long expireTime) {
        if (context == null) return;
        new Thread(() -> {
            try {
                File privateDir = context.getFilesDir();
                if (privateDir != null && !privateDir.exists()) {
                    privateDir.mkdirs();
                }
                File backupFile = new File(privateDir, BACKUP_FILENAME);
                String hash = calcularHash(getDeviceId(), expireTime);
                FileWriter writer = new FileWriter(backupFile);
                writer.write(expireTime + ":" + hash);
                writer.flush();
                writer.close();
            } catch (Exception ignored) {
            }
        }, "ProPassBackupWorker").start();
    }

    private void intentarRestaurarRespaldo() {
        if (context == null) return;
        try {
            File privateDir = context.getFilesDir();
            File backupFile = new File(privateDir, BACKUP_FILENAME);
            if (!backupFile.exists() || !backupFile.canRead()) {
                return;
            }
            BufferedReader reader = new BufferedReader(new FileReader(backupFile));
            String line = reader.readLine();
            reader.close();
            if (line == null || !line.contains(":")) {
                return;
            }
            String[] parts = line.split(":");
            if (parts.length != 2) {
                return;
            }
            long expireTime = Long.parseLong(parts[0]);
            String expectedHash = parts[1];
            String actualHash = calcularHash(getDeviceId(), expireTime);
            if (expectedHash.equals(actualHash) && System.currentTimeMillis() < expireTime) {
                prefs.edit().putLong(KEY_EXPIRE_TIME, expireTime).apply();
            }
        } catch (Exception ignored) {
        }
    }

    private void guardarRespaldoEnFirestore(long expireTime) {
        if (context == null) return;
        new Thread(() -> {
            try {
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                Map<String, Object> data = new HashMap<>();
                data.put("deviceId", getDeviceId());
                data.put("expireTime", expireTime);
                data.put("updatedAt", FieldValue.serverTimestamp());
                data.put("active", true);

                db.collection("pro_passes")
                        .document(getDeviceId())
                        .set(data, SetOptions.merge())
                        .addOnSuccessListener(aVoid -> Log.d(TAG, "Pase Pro respaldado en Firestore exitosamente"))
                        .addOnFailureListener(e -> Log.w(TAG, "Error al respaldar Pase Pro en Firestore: " + e.getMessage()));
            } catch (Throwable t) {
                Log.w(TAG, "Firestore no disponible o error al guardar: " + t.getMessage());
            }
        }, "ProPassFirestoreBackup").start();
    }

    /**
     * Consulta Firestore en la nube para sincronizar o restaurar el pase de 12 horas.
     * Útil tras desinstalar y reinstalar la aplicación en el mismo dispositivo.
     */
    public void intentarRestaurarDesdeFirestore(@Nullable OnPassRestoredListener listener) {
        if (context == null) return;
        try {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("pro_passes")
                    .document(getDeviceId())
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        if (snapshot != null && snapshot.exists()) {
                            Long expireTime = snapshot.getLong("expireTime");
                            if (expireTime != null && expireTime > System.currentTimeMillis()) {
                                if (expireTime > getExpireTime()) {
                                    prefs.edit().putLong(KEY_EXPIRE_TIME, expireTime).apply();
                                    guardarRespaldoPersistente(expireTime);
                                    Log.i(TAG, "Pase Pro recuperado de Firebase Firestore (expira en: " + expireTime + ")");
                                    if (listener != null) {
                                        listener.onPassRestored(expireTime);
                                    }
                                    if (onPassRestoredListener != null) {
                                        onPassRestoredListener.onPassRestored(expireTime);
                                    }
                                }
                            }
                        }
                    })
                    .addOnFailureListener(e -> Log.w(TAG, "No se pudo recuperar Pase Pro de Firestore: " + e.getMessage()));
        } catch (Throwable t) {
            Log.w(TAG, "Firestore no inicializado o sin conectividad: " + t.getMessage());
        }
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
