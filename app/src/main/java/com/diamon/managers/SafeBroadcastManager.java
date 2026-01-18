package com.diamon.managers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Build;
import android.util.Log;

import androidx.core.content.ContextCompat;

/**
 * Gestor seguro para el registro y desregistro de BroadcastReceivers.
 * 
 * Proporciona:
 * - Compatibilidad con Android 13+ (RECEIVER_EXPORTED/RECEIVER_NOT_EXPORTED)
 * - Manejo seguro de doble unregister
 * - Logging para debugging en produccion
 * 
 * Uso:
 * 
 * <pre>
 * SafeBroadcastManager manager = new SafeBroadcastManager(context);
 * manager.registerReceiver(receiver, filter, false); // false = not exported
 * // ...
 * manager.unregisterReceiver(receiver);
 * </pre>
 */
public class SafeBroadcastManager {

    private static final String TAG = "SafeBroadcastManager";

    private final Context context;
    private boolean isRegistered = false;
    private BroadcastReceiver currentReceiver = null;

    public SafeBroadcastManager(Context context) {
        this.context = context;
    }

    /**
     * Registra un BroadcastReceiver de forma segura y compatible con todas las
     * versiones de Android.
     * 
     * @param receiver El BroadcastReceiver a registrar
     * @param filter   El IntentFilter con las acciones a escuchar
     * @param exported true si el receiver debe recibir broadcasts de otras apps,
     *                 false si solo debe recibir broadcasts internos
     * @return true si el registro fue exitoso, false si hubo un error
     */
    public synchronized boolean registerReceiver(BroadcastReceiver receiver,
            IntentFilter filter,
            boolean exported) {
        if (receiver == null || filter == null) {
            Log.w(TAG, "Attempted to register null receiver or filter");
            return false;
        }

        // Si ya hay un receiver registrado, desregistrarlo primero
        if (isRegistered && currentReceiver != null) {
            unregisterReceiver(currentReceiver);
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13 (API 33) y superior requiere especificar el flag de exportacion
                int flag = exported ? ContextCompat.RECEIVER_EXPORTED : ContextCompat.RECEIVER_NOT_EXPORTED;

                ContextCompat.registerReceiver(context, receiver, filter, flag);
            } else {
                // Versiones anteriores a Android 13
                context.registerReceiver(receiver, filter);
            }

            isRegistered = true;
            currentReceiver = receiver;
            Log.d(TAG, "BroadcastReceiver registered successfully");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Error registering BroadcastReceiver: " + e.getMessage());
            isRegistered = false;
            currentReceiver = null;
            return false;
        }
    }

    /**
     * Desregistra un BroadcastReceiver de forma segura.
     * No lanza excepcion si el receiver ya fue desregistrado.
     * 
     * @param receiver El BroadcastReceiver a desregistrar
     * @return true si el desregistro fue exitoso, false si hubo un error o ya
     *         estaba desregistrado
     */
    public synchronized boolean unregisterReceiver(BroadcastReceiver receiver) {
        if (receiver == null) {
            Log.w(TAG, "Attempted to unregister null receiver");
            return false;
        }

        if (!isRegistered) {
            Log.d(TAG, "Receiver was not registered, skipping unregister");
            return false;
        }

        try {
            context.unregisterReceiver(receiver);
            Log.d(TAG, "BroadcastReceiver unregistered successfully");
            return true;

        } catch (IllegalArgumentException e) {
            // El receiver ya fue desregistrado - esto no es un error critico
            Log.d(TAG, "Receiver was already unregistered");
            return false;

        } catch (Exception e) {
            Log.e(TAG, "Error unregistering BroadcastReceiver: " + e.getMessage());
            return false;

        } finally {
            isRegistered = false;
            currentReceiver = null;
        }
    }

    /**
     * Verifica si hay un receiver actualmente registrado.
     * 
     * @return true si hay un receiver registrado
     */
    public synchronized boolean isReceiverRegistered() {
        return isRegistered && currentReceiver != null;
    }

    /**
     * Obtiene el receiver actualmente registrado.
     * 
     * @return El BroadcastReceiver registrado o null si no hay ninguno
     */
    public synchronized BroadcastReceiver getCurrentReceiver() {
        return currentReceiver;
    }
}
