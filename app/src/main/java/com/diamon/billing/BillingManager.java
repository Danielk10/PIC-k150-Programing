package com.diamon.billing;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;

import java.util.Collections;
import java.util.List;

/**
 * Gestor de compras integradas (In-App Billing) para PIC-k150-Programing
 * utilizando Google Play Billing Library.
 *
 * <p>Maneja la compra del producto no consumible "remove_ads_pro" para
 * eliminar publicidad permanentemente y activar las funciones Pro de la aplicación.</p>
 */
public class BillingManager implements PurchasesUpdatedListener {

    private static final String TAG = "BillingManager";

    /** ID del producto configurado en Google Play Console para eliminar anuncios. */
    public static final String PRODUCT_ID_REMOVE_ADS = "remove_ads_pro";

    public static final String PREFS_NAME = "billing_prefs";
    public static final String KEY_ADS_REMOVED = "is_ads_removed";
    public static final String KEY_PRO_USER = "is_pro_user";

    /**
     * Interfaz de escucha para notificaciones de estado y eventos de facturación.
     */
    public interface BillingListener {
        default void onAdsRemovedChanged(boolean adsRemoved) {}
        default void onProStatusChanged(boolean isProPurchased) { onAdsRemovedChanged(isProPurchased); }
        default void onPriceLoaded(String formattedPrice) {}
        default void onBillingError(String errorMessage) {}
        default void onPurchaseError(String errorMessage) { onBillingError(errorMessage); }
    }

    private final Context context;
    private final BillingClient billingClient;
    @Nullable
    private BillingListener listener;
    @Nullable
    private ProductDetails removeAdsDetails;
    private boolean isServiceConnected = false;
    private boolean isAdsRemovedCached = false;

    public BillingManager(@NonNull Context context, @Nullable BillingListener listener) {
        this(context, listener, null);
    }

    @VisibleForTesting
    BillingManager(@NonNull Context context, @Nullable BillingListener listener, @Nullable BillingClient billingClient) {
        this.context = context.getApplicationContext();
        this.listener = listener;

        if (billingClient != null) {
            this.billingClient = billingClient;
        } else {
            this.billingClient = BillingClient.newBuilder(this.context)
                    .setListener(this)
                    .enablePendingPurchases(
                            PendingPurchasesParams.newBuilder()
                                    .enableOneTimeProducts()
                                    .build()
                    )
                    .build();
        }

        // Cargar estado inicial desde preferencias
        isAdsRemovedCached = isAdsRemoved();
    }

    public void setBillingListener(@Nullable BillingListener listener) {
        this.listener = listener;
    }

    @Nullable
    public BillingListener getBillingListener() {
        return listener;
    }

    /**
     * Inicia la conexión con el servicio de Google Play Billing.
     */
    public void startConnection() {
        if (billingClient.isReady()) {
            isServiceConnected = true;
            queryActivePurchases();
            queryProducts();
            return;
        }

        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    isServiceConnected = true;
                    Log.d(TAG, "Billing client connected successfully.");
                    queryActivePurchases();
                    queryProducts();
                } else {
                    isServiceConnected = false;
                    String errorMsg = "Billing setup failed: " + billingResult.getDebugMessage();
                    Log.w(TAG, errorMsg);
                    if (listener != null) {
                        listener.onBillingError(errorMsg);
                    }
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                isServiceConnected = false;
                Log.w(TAG, "Billing service disconnected. Will retry on next request.");
            }
        });
    }

    /**
     * Consulta los detalles del producto in-app remove_ads_pro en Google Play.
     */
    public void queryProducts() {
        QueryProductDetailsParams.Product product = QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_ID_REMOVE_ADS)
                .setProductType(BillingClient.ProductType.INAPP)
                .build();

        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(Collections.singletonList(product))
                .build();

        billingClient.queryProductDetailsAsync(params, (billingResult, queryResult) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                    && queryResult != null
                    && queryResult.getProductDetailsList() != null
                    && !queryResult.getProductDetailsList().isEmpty()) {
                removeAdsDetails = queryResult.getProductDetailsList().get(0);
                Log.d(TAG, "Product details loaded: " + removeAdsDetails.getName());

                ProductDetails.OneTimePurchaseOfferDetails offerDetails =
                        removeAdsDetails.getOneTimePurchaseOfferDetails();
                if (offerDetails != null && listener != null) {
                    listener.onPriceLoaded(offerDetails.getFormattedPrice());
                }
            } else {
                removeAdsDetails = null;
                String errorMsg = "Failed to load product details: " + billingResult.getDebugMessage();
                Log.w(TAG, errorMsg);
                if (listener != null) {
                    listener.onBillingError(errorMsg);
                }
            }
        });
    }

    /**
     * Lanza la hoja modal de compra de Google Play para el producto "remove_ads_pro".
     *
     * @param activity Actividad desde donde se inicia la compra.
     * @return true si el flujo se lanzó exitosamente; false de lo contrario.
     */
    public boolean launchPurchaseFlow(@NonNull Activity activity) {
        if (removeAdsDetails == null) {
            String errorMsg = "Product details not available. Cannot launch purchase flow.";
            Log.w(TAG, errorMsg);
            if (listener != null) {
                listener.onBillingError(errorMsg);
            }
            return false;
        }

        BillingFlowParams.ProductDetailsParams productDetailsParams =
                BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(removeAdsDetails)
                        .build();

        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(Collections.singletonList(productDetailsParams))
                .build();

        BillingResult result = billingClient.launchBillingFlow(activity, billingFlowParams);
        boolean success = result.getResponseCode() == BillingClient.BillingResponseCode.OK;
        if (!success && listener != null) {
            listener.onBillingError("Launch billing flow failed: " + result.getDebugMessage());
        }
        return success;
    }

    @Override
    public void onPurchasesUpdated(@NonNull BillingResult billingResult,
                                   @Nullable List<Purchase> purchases) {
        int responseCode = billingResult.getResponseCode();

        if (responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (Purchase purchase : purchases) {
                handlePurchase(purchase);
            }
        } else if (responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.d(TAG, "User canceled the purchase.");
        } else if (responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
            Log.d(TAG, "Item already owned. Restoring purchase...");
            setAdsRemoved(true);
        } else {
            String errorMsg = "Purchase failed (" + responseCode + "): " + billingResult.getDebugMessage();
            Log.w(TAG, errorMsg);
            if (listener != null) {
                listener.onBillingError(errorMsg);
            }
        }
    }

    /**
     * Procesa una compra individual: reconoce (acknowledge) compras no reconocidas
     * y actualiza el estado local de anuncios eliminados.
     *
     * @param purchase Compra devuelta por Google Play.
     */
    public void handlePurchase(@NonNull Purchase purchase) {
        if (purchase.getProducts().contains(PRODUCT_ID_REMOVE_ADS)) {
            if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                if (!purchase.isAcknowledged()) {
                    acknowledgePurchase(purchase);
                } else {
                    setAdsRemoved(true);
                }
            } else if (purchase.getPurchaseState() == Purchase.PurchaseState.PENDING) {
                Log.d(TAG, "Purchase is pending. Ads will be removed once confirmed.");
            }
        }
    }

    /**
     * Reconoce una compra para evitar que Google Play la reembolse automáticamente
     * transcurridos 3 días.
     */
    private void acknowledgePurchase(@NonNull Purchase purchase) {
        AcknowledgePurchaseParams params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.getPurchaseToken())
                .build();

        billingClient.acknowledgePurchase(params, billingResult -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "Purchase acknowledged successfully.");
                setAdsRemoved(true);
            } else {
                String errorMsg = "Failed to acknowledge purchase: " + billingResult.getDebugMessage();
                Log.w(TAG, errorMsg);
                if (listener != null) {
                    listener.onBillingError(errorMsg);
                }
            }
        });
    }

    /**
     * Consulta las compras activas de la cuenta para restaurar compras previas.
     */
    public void queryActivePurchases() {
        QueryPurchasesParams params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build();

        billingClient.queryPurchasesAsync(params, (billingResult, purchases) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                boolean hasActivePurchase = false;
                if (purchases != null) {
                    for (Purchase purchase : purchases) {
                        if (purchase.getProducts().contains(PRODUCT_ID_REMOVE_ADS)
                                && purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                            hasActivePurchase = true;
                            handlePurchase(purchase);
                            break;
                        }
                    }
                }
                if (!hasActivePurchase && !isAdsRemoved()) {
                    setAdsRemoved(false);
                }
            } else {
                String errorMsg = "Failed to query purchases: " + billingResult.getDebugMessage();
                Log.w(TAG, errorMsg);
                if (listener != null) {
                    listener.onBillingError(errorMsg);
                }
            }
        });
    }

    /**
     * Actualiza el estado de anuncios eliminados en SharedPreferences y notifica al listener.
     */
    @VisibleForTesting
    void setAdsRemoved(boolean value) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean previousValue = isAdsRemoved();

        prefs.edit()
                .putBoolean(KEY_ADS_REMOVED, value)
                .putBoolean(KEY_PRO_USER, value)
                .apply();

        isAdsRemovedCached = value;

        if (value != previousValue && listener != null) {
            listener.onAdsRemovedChanged(value);
        }
    }

    /**
     * Retorna si los anuncios han sido eliminados (por compra activa o persistida).
     */
    public boolean isAdsRemoved() {
        if (isAdsRemovedCached) {
            return true;
        }
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean adsRemoved = prefs.getBoolean(KEY_ADS_REMOVED, false) || prefs.getBoolean(KEY_PRO_USER, false);
        if (adsRemoved) {
            isAdsRemovedCached = true;
        }
        return adsRemoved;
    }

    /**
     * Método estático para verificar rápidamente si los anuncios están desactivados.
     */
    public static boolean isAdsRemoved(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_ADS_REMOVED, false) || prefs.getBoolean(KEY_PRO_USER, false);
    }

    public boolean isProPurchased() {
        return isAdsRemoved();
    }

    public boolean isProductAvailable() {
        return removeAdsDetails != null;
    }

    @Nullable
    public ProductDetails getRemoveAdsDetails() {
        return removeAdsDetails;
    }

    @Nullable
    public String getFormattedPrice() {
        if (removeAdsDetails != null && removeAdsDetails.getOneTimePurchaseOfferDetails() != null) {
            return removeAdsDetails.getOneTimePurchaseOfferDetails().getFormattedPrice();
        }
        return null;
    }

    public boolean isServiceConnected() {
        return isServiceConnected;
    }

    @VisibleForTesting
    void setRemoveAdsDetails(@Nullable ProductDetails details) {
        this.removeAdsDetails = details;
    }

    @VisibleForTesting
    void setServiceConnected(boolean connected) {
        this.isServiceConnected = connected;
    }

    /**
     * Desconecta de forma segura el cliente de facturación.
     */
    public void destroy() {
        if (billingClient != null && billingClient.isReady()) {
            billingClient.endConnection();
        }
        isServiceConnected = false;
        listener = null;
    }
}
