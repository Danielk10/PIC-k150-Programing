package com.diamon.publicidad;

import android.app.Activity;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.FormError;
import com.google.android.ump.UserMessagingPlatform;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Gestiona el consentimiento de privacidad con el SDK UMP de Google.
 *
 * Se pide una actualización en cada arranque, se muestra el formulario que
 * corresponda (normativas europeas / estatales de EE. UU.) y no se inicializa
 * MobileAds ni se pide ningún anuncio hasta que canRequestAds() sea true.
 *
 * El punto de entrada de "Opciones de privacidad" solo se muestra cuando UMP
 * indica que es obligatorio, tal como exige Google.
 */
public final class ConsentManager {

    private static final String TAG = "ConsentManager";

    private static ConsentInformation consentInformation;
    private static boolean requestInFlight = false;
    private static boolean resolved = false;
    private static boolean canRequestAds = false;
    private static boolean privacyOptionsRequired = false;
    private static WeakReference<Activity> activityRef = new WeakReference<>(null);
    private static final List<ConsentCallback> pendingCallbacks = new ArrayList<>();

    public interface ConsentCallback {
        void onConsentReady(boolean canRequestAds);
    }

    public interface PrivacyOptionsCallback {
        void onDismissed(@Nullable FormError error);
    }

    private ConsentManager() {
    }

    /**
     * Pide la información de consentimiento y, si hace falta, muestra el
     * formulario. El callback se ejecuta una sola vez por llamada y siempre en
     * el hilo principal.
     */
    public static void requestConsent(
            @NonNull Activity activity, @NonNull ConsentCallback callback) {
        if (resolved) {
            callback.onConsentReady(canRequestAds);
            return;
        }

        activityRef = new WeakReference<>(activity);
        pendingCallbacks.add(callback);

        if (requestInFlight) {
            return;
        }
        requestInFlight = true;

        consentInformation = UserMessagingPlatform.getConsentInformation(activity);
        ConsentRequestParameters params = new ConsentRequestParameters.Builder().build();

        consentInformation.requestConsentInfoUpdate(
                activity,
                params,
                () -> UserMessagingPlatform.loadAndShowConsentFormIfRequired(
                        activity,
                        formError -> {
                            if (formError != null) {
                                Log.w(TAG, "Error en el formulario de consentimiento: "
                                        + formError.getMessage());
                            }
                            finishConsentFlow();
                        }),
                requestError -> {
                    Log.w(TAG, "No se pudo actualizar el consentimiento: "
                            + requestError.getMessage());
                    finishConsentFlow();
                });
    }

    private static void finishConsentFlow() {
        if (consentInformation == null) {
            resolved = true;
            canRequestAds = false;
            privacyOptionsRequired = false;
        } else {
            canRequestAds = consentInformation.canRequestAds();
            privacyOptionsRequired =
                    consentInformation.getPrivacyOptionsRequirementStatus()
                            == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED;
            resolved = true;
        }

        requestInFlight = false;
        Log.d(TAG, "Consentimiento listo. canRequestAds=" + canRequestAds
                + ", privacyOptionsRequired=" + privacyOptionsRequired);

        List<ConsentCallback> callbacks = new ArrayList<>(pendingCallbacks);
        pendingCallbacks.clear();
        for (ConsentCallback callback : callbacks) {
            try {
                callback.onConsentReady(canRequestAds);
            } catch (Exception e) {
                Log.e(TAG, "Error en callback de consentimiento", e);
            }
        }

        Activity activity = activityRef.get();
        if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
            activity.invalidateOptionsMenu();
        }
    }

    public static boolean isResolved() {
        return resolved;
    }

    /**
     * Devuelve true si se pueden pedir anuncios. Antes de terminar el flujo de
     * consentimiento devuelve false para no adelantar peticiones.
     */
    public static boolean canRequestAds() {
        return resolved && canRequestAds;
    }

    /**
     * Indica si hay que mostrar el punto de entrada de opciones de privacidad.
     */
    public static boolean isPrivacyOptionsRequired() {
        return resolved && privacyOptionsRequired;
    }

    /**
     * Vuelve a abrir el formulario de privacidad para que el usuario cambie su
     * decisión. Es obligatorio ofrecer este punto de entrada cuando UMP lo pide.
     */
    public static void showPrivacyOptionsForm(
            @NonNull Activity activity, @Nullable PrivacyOptionsCallback callback) {
        if (!resolved || consentInformation == null || !privacyOptionsRequired) {
            if (callback != null) {
                callback.onDismissed(null);
            }
            return;
        }

        UserMessagingPlatform.showPrivacyOptionsForm(
                activity,
                formError -> {
                    if (formError != null) {
                        Log.w(TAG, "Error al mostrar opciones de privacidad: "
                                + formError.getMessage());
                    }
                    if (consentInformation != null) {
                        canRequestAds = consentInformation.canRequestAds();
                        privacyOptionsRequired =
                                consentInformation.getPrivacyOptionsRequirementStatus()
                                        == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED;
                        Log.d(TAG, "Opciones de privacidad actualizadas. canRequestAds="
                                + canRequestAds);
                    }
                    if (callback != null) {
                        callback.onDismissed(formError);
                    }
                });
    }
}
