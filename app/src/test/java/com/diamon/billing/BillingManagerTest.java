package com.diamon.billing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Pruebas unitarias para BillingManager:
 * - Persistencia y lectura de estado de compra en SharedPreferences.
 * - Ciclo de reconocimiento (Acknowledge) de compras.
 * - Manejo de estados de compra (Purchased, Pending, Item Already Owned).
 * - Lanzamiento del flujo de facturación (launchPurchaseFlow).
 * - Gestión de ciclo de vida (startConnection, destroy).
 */
public class BillingManagerTest {

    private Context mockContext;
    private SharedPreferences mockPrefs;
    private SharedPreferences.Editor mockEditor;
    private BillingClient mockBillingClient;
    private BillingManager.BillingListener mockListener;
    private BillingManager billingManager;
    private MockedStatic<TextUtils> mockedTextUtils;

    private Map<String, Boolean> prefsMap;

    @Before
    public void setUp() {
        mockedTextUtils = mockStatic(TextUtils.class);
        mockedTextUtils.when(() -> TextUtils.isEmpty(any())).thenAnswer(invocation -> {
            CharSequence cs = invocation.getArgument(0);
            return cs == null || cs.length() == 0;
        });

        mockContext = mock(Context.class);
        mockPrefs = mock(SharedPreferences.class);
        mockEditor = mock(SharedPreferences.Editor.class);
        mockBillingClient = mock(BillingClient.class);
        mockListener = mock(BillingManager.BillingListener.class);

        prefsMap = new HashMap<>();

        when(mockContext.getApplicationContext()).thenReturn(mockContext);
        when(mockContext.getSharedPreferences(eq(BillingManager.PREFS_NAME), anyInt())).thenReturn(mockPrefs);

        when(mockPrefs.getBoolean(anyString(), anyBoolean())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            boolean defaultValue = invocation.getArgument(1);
            return prefsMap.getOrDefault(key, defaultValue);
        });

        when(mockPrefs.edit()).thenReturn(mockEditor);
        when(mockEditor.putBoolean(anyString(), anyBoolean())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            boolean value = invocation.getArgument(1);
            prefsMap.put(key, value);
            return mockEditor;
        });

        billingManager = new BillingManager(mockContext, mockListener, mockBillingClient);
    }

    @After
    public void tearDown() {
        if (mockedTextUtils != null) {
            mockedTextUtils.close();
        }
    }

    @Test
    public void initialState_noAdsRemovedByDefault() {
        assertFalse(billingManager.isAdsRemoved());
    }

    @Test
    public void isAdsRemoved_returnsTrueWhenKeyAdsRemovedSet() {
        prefsMap.put(BillingManager.KEY_ADS_REMOVED, true);
        assertTrue(billingManager.isAdsRemoved());
    }

    @Test
    public void isAdsRemoved_returnsTrueWhenKeyProUserSet() {
        prefsMap.put(BillingManager.KEY_PRO_USER, true);
        assertTrue(billingManager.isAdsRemoved());
    }

    @Test
    public void staticIsAdsRemoved_readsDirectlyFromPrefs() {
        assertFalse(BillingManager.isAdsRemoved(mockContext));
        prefsMap.put(BillingManager.KEY_ADS_REMOVED, true);
        assertTrue(BillingManager.isAdsRemoved(mockContext));
    }

    @Test
    public void setAdsRemoved_persistsInPrefsAndNotifiesListener() {
        billingManager.setAdsRemoved(true);

        verify(mockEditor).putBoolean(BillingManager.KEY_ADS_REMOVED, true);
        verify(mockEditor).putBoolean(BillingManager.KEY_PRO_USER, true);
        verify(mockEditor).apply();
        verify(mockListener).onAdsRemovedChanged(true);
        assertTrue(billingManager.isAdsRemoved());
    }

    @Test
    public void setAdsRemoved_doesNotNotifyWhenValueDoesNotChange() {
        billingManager.setAdsRemoved(false);
        verify(mockListener, never()).onAdsRemovedChanged(anyBoolean());
    }

    @Test
    public void handlePurchase_acknowledgedPurchase_activatesAdsRemovedDirectly() {
        Purchase purchase = mock(Purchase.class);
        when(purchase.getProducts()).thenReturn(Collections.singletonList(BillingManager.PRODUCT_ID_REMOVE_ADS));
        when(purchase.getPurchaseState()).thenReturn(Purchase.PurchaseState.PURCHASED);
        when(purchase.isAcknowledged()).thenReturn(true);

        billingManager.handlePurchase(purchase);

        assertTrue(billingManager.isAdsRemoved());
        verify(mockListener).onAdsRemovedChanged(true);
        verify(mockBillingClient, never()).acknowledgePurchase(any(), any());
    }

    @Test
    public void handlePurchase_unacknowledgedPurchase_triggersAcknowledgeFlow() {
        Purchase purchase = mock(Purchase.class);
        when(purchase.getProducts()).thenReturn(Collections.singletonList(BillingManager.PRODUCT_ID_REMOVE_ADS));
        when(purchase.getPurchaseState()).thenReturn(Purchase.PurchaseState.PURCHASED);
        when(purchase.isAcknowledged()).thenReturn(false);
        when(purchase.getPurchaseToken()).thenReturn("mock_token_12345");

        BillingResult okResult = mock(BillingResult.class);
        when(okResult.getResponseCode()).thenReturn(BillingClient.BillingResponseCode.OK);

        doAnswer(invocation -> {
            AcknowledgePurchaseResponseListener listener = invocation.getArgument(1);
            listener.onAcknowledgePurchaseResponse(okResult);
            return null;
        }).when(mockBillingClient).acknowledgePurchase(any(AcknowledgePurchaseParams.class), any());

        billingManager.handlePurchase(purchase);

        verify(mockBillingClient).acknowledgePurchase(any(AcknowledgePurchaseParams.class), any());
        assertTrue(billingManager.isAdsRemoved());
        verify(mockListener).onAdsRemovedChanged(true);
    }

    @Test
    public void handlePurchase_unacknowledgedPurchase_handlesAcknowledgeError() {
        Purchase purchase = mock(Purchase.class);
        when(purchase.getProducts()).thenReturn(Collections.singletonList(BillingManager.PRODUCT_ID_REMOVE_ADS));
        when(purchase.getPurchaseState()).thenReturn(Purchase.PurchaseState.PURCHASED);
        when(purchase.isAcknowledged()).thenReturn(false);
        when(purchase.getPurchaseToken()).thenReturn("mock_token_12345");

        BillingResult errorResult = mock(BillingResult.class);
        when(errorResult.getResponseCode()).thenReturn(BillingClient.BillingResponseCode.ERROR);
        when(errorResult.getDebugMessage()).thenReturn("Network timeout");

        doAnswer(invocation -> {
            AcknowledgePurchaseResponseListener listener = invocation.getArgument(1);
            listener.onAcknowledgePurchaseResponse(errorResult);
            return null;
        }).when(mockBillingClient).acknowledgePurchase(any(AcknowledgePurchaseParams.class), any());

        billingManager.handlePurchase(purchase);

        verify(mockBillingClient).acknowledgePurchase(any(AcknowledgePurchaseParams.class), any());
        assertFalse(billingManager.isAdsRemoved());
        verify(mockListener).onBillingError(eq("Failed to acknowledge purchase: Network timeout"));
    }

    @Test
    public void handlePurchase_pendingPurchase_doesNotEnableAdsRemoved() {
        Purchase purchase = mock(Purchase.class);
        when(purchase.getProducts()).thenReturn(Collections.singletonList(BillingManager.PRODUCT_ID_REMOVE_ADS));
        when(purchase.getPurchaseState()).thenReturn(Purchase.PurchaseState.PENDING);

        billingManager.handlePurchase(purchase);

        assertFalse(billingManager.isAdsRemoved());
        verify(mockListener, never()).onAdsRemovedChanged(anyBoolean());
    }

    @Test
    public void handlePurchase_otherProduct_ignored() {
        Purchase purchase = mock(Purchase.class);
        when(purchase.getProducts()).thenReturn(Collections.singletonList("other_product"));
        when(purchase.getPurchaseState()).thenReturn(Purchase.PurchaseState.PURCHASED);

        billingManager.handlePurchase(purchase);

        assertFalse(billingManager.isAdsRemoved());
        verify(mockBillingClient, never()).acknowledgePurchase(any(), any());
    }

    @Test
    public void onPurchasesUpdated_itemAlreadyOwned_restoresPurchase() {
        BillingResult result = mock(BillingResult.class);
        when(result.getResponseCode()).thenReturn(BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED);

        billingManager.onPurchasesUpdated(result, null);

        assertTrue(billingManager.isAdsRemoved());
        verify(mockListener).onAdsRemovedChanged(true);
    }

    @Test
    public void onPurchasesUpdated_userCanceled_doesNotNotifyError() {
        BillingResult result = mock(BillingResult.class);
        when(result.getResponseCode()).thenReturn(BillingClient.BillingResponseCode.USER_CANCELED);

        billingManager.onPurchasesUpdated(result, null);

        verify(mockListener, never()).onBillingError(anyString());
    }

    @Test
    public void onPurchasesUpdated_error_notifiesBillingError() {
        BillingResult result = mock(BillingResult.class);
        when(result.getResponseCode()).thenReturn(BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE);
        when(result.getDebugMessage()).thenReturn("Service Unavailable");

        billingManager.onPurchasesUpdated(result, null);

        verify(mockListener).onBillingError(eq("Purchase failed (2): Service Unavailable"));
    }

    @Test
    public void launchPurchaseFlow_whenProductDetailsNotLoaded_returnsFalseAndNotifiesError() {
        Activity mockActivity = mock(Activity.class);
        boolean launched = billingManager.launchPurchaseFlow(mockActivity);

        assertFalse(launched);
        verify(mockListener).onBillingError(eq("Product details not available. Cannot launch purchase flow."));
        verify(mockBillingClient, never()).launchBillingFlow(any(), any());
    }

    @Test
    public void launchPurchaseFlow_whenProductDetailsLoaded_launchesSuccessfully() {
        Activity mockActivity = mock(Activity.class);
        ProductDetails details = mock(ProductDetails.class);
        when(details.zza()).thenReturn("dummy_product_json");
        billingManager.setRemoveAdsDetails(details);

        BillingResult okResult = mock(BillingResult.class);
        when(okResult.getResponseCode()).thenReturn(BillingClient.BillingResponseCode.OK);
        when(mockBillingClient.launchBillingFlow(eq(mockActivity), any(BillingFlowParams.class))).thenReturn(okResult);

        boolean launched = billingManager.launchPurchaseFlow(mockActivity);

        assertTrue(launched);
        verify(mockBillingClient).launchBillingFlow(eq(mockActivity), any(BillingFlowParams.class));
        verify(mockListener, never()).onBillingError(anyString());
    }

    @Test
    public void startConnection_whenAlreadyReady_queriesDirectly() {
        when(mockBillingClient.isReady()).thenReturn(true);

        billingManager.startConnection();

        assertTrue(billingManager.isServiceConnected());
        verify(mockBillingClient).queryPurchasesAsync(any(QueryPurchasesParams.class), any());
        verify(mockBillingClient).queryProductDetailsAsync(any(QueryProductDetailsParams.class), any());
    }

    @Test
    public void destroy_endsConnectionAndClearsListener() {
        when(mockBillingClient.isReady()).thenReturn(true);

        billingManager.destroy();

        verify(mockBillingClient).endConnection();
        assertNull(billingManager.getBillingListener());
        assertFalse(billingManager.isServiceConnected());
    }
}
