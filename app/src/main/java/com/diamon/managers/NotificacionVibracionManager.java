package com.diamon.managers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.diamon.pic.R;

/**
 * Gestor unificado para vibración háptica y notificaciones con sonido
 * del sistema tras operaciones con el programador K150.
 */
public class NotificacionVibracionManager {

    private static final String CHANNEL_ID = "canal_k150_operaciones";
    private static final String CHANNEL_NAME = "Operaciones PIC K150";
    private static final String CHANNEL_DESC = "Notificaciones de fin de grabación y alertas de hardware";
    private static final int NOTIFICATION_ID_BASE = 1050;

    private final Context context;
    private final Vibrator vibrator;
    private final boolean hasVibrator;

    public NotificacionVibracionManager(Context context) {
        this.context = context.getApplicationContext();
        this.vibrator = obtenerVibrator(context);
        this.hasVibrator = (this.vibrator != null && this.vibrator.hasVibrator());
        crearCanalNotificaciones();
    }

    private Vibrator obtenerVibrator(Context ctx) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                VibratorManager vm = (VibratorManager) ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
                if (vm != null) {
                    return vm.getDefaultVibrator();
                }
            }
            return (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
        } catch (Exception e) {
            return null;
        }
    }

    private void crearCanalNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                if (nm != null) {
                    NotificationChannel channel = new NotificationChannel(
                            CHANNEL_ID,
                            CHANNEL_NAME,
                            NotificationManager.IMPORTANCE_HIGH);
                    channel.setDescription(CHANNEL_DESC);
                    channel.enableVibration(true);
                    channel.setVibrationPattern(new long[]{0, 150, 100, 250});

                    Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                    AudioAttributes audioAttributes = new AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .build();
                    channel.setSound(defaultSoundUri, audioAttributes);

                    nm.createNotificationChannel(channel);
                }
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Hace vibrar el dispositivo con un patrón de éxito.
     */
    public void vibrarExito() {
        if (!hasVibrator || vibrator == null) {
            return;
        }
        try {
            long[] pattern = new long[]{0, 120, 80, 220};
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
            } else {
                vibrator.vibrate(pattern, -1);
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Hace vibrar el dispositivo con un patrón de advertencia / alerta (ej. chip no insertado).
     */
    public void vibrarAlerta() {
        if (!hasVibrator || vibrator == null) {
            return;
        }
        try {
            long[] pattern = new long[]{0, 200, 120, 200};
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
            } else {
                vibrator.vibrate(pattern, -1);
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Reproduce el sonido de notificación predeterminado del sistema.
     */
    public void reproducirSonidoNotificacion() {
        try {
            Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(context, soundUri);
            if (r != null) {
                r.play();
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Emite una notificación con sonido del sistema y vibración tras completar la grabación.
     */
    public void lanzarNotificacion(String titulo, String mensaje) {
        // Reproducir sonido inmediato
        reproducirSonidoNotificacion();

        try {
            Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(titulo)
                    .setContentText(mensaje)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(mensaje))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_EVENT)
                    .setSound(defaultSoundUri)
                    .setAutoCancel(true);

            if (hasVibrator) {
                builder.setVibrate(new long[]{0, 150, 100, 250});
            }

            NotificationManagerCompat nmc = NotificationManagerCompat.from(context);
            // Si el permiso POST_NOTIFICATIONS está otorgado o en Android < 13
            if (nmc.areNotificationsEnabled()) {
                nmc.notify(NOTIFICATION_ID_BASE, builder.build());
            }
        } catch (SecurityException se) {
            // Permiso denegado en Android 13+, sonido ya se reprodujo
        } catch (Exception ignored) {
        }
    }
}
