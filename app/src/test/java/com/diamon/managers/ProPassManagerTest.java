package com.diamon.managers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.SharedPreferences;

import com.diamon.billing.BillingManager;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Pruebas unitarias para ProPassManager:
 * - Activación del pase de 12 horas.
 * - Cálculo y persistencia de tiempo de expiración.
 * - Formato del tiempo restante ("11h 45m", "Expirado").
 * - Integración de isProActive con BillingManager y estado del pase.
 */
public class ProPassManagerTest {

    private SharedPreferences mockPrefs;
    private SharedPreferences.Editor mockEditor;
    private final Map<String, Object> storage = new HashMap<>();
    private ProPassManager proPassManager;

    @Before
    public void setUp() {
        storage.clear();
        mockPrefs = mock(SharedPreferences.class);
        mockEditor = mock(SharedPreferences.Editor.class);

        when(mockPrefs.edit()).thenReturn(mockEditor);
        when(mockEditor.putLong(anyString(), anyLong())).thenAnswer(invocation -> {
            storage.put(invocation.getArgument(0), invocation.getArgument(1));
            return mockEditor;
        });
        when(mockEditor.remove(anyString())).thenAnswer(invocation -> {
            storage.remove(invocation.getArgument(0));
            return mockEditor;
        });
        when(mockPrefs.getLong(anyString(), anyLong())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            Long defVal = invocation.getArgument(1);
            Object val = storage.get(key);
            return val instanceof Long ? (Long) val : defVal;
        });

        proPassManager = new ProPassManager(mockPrefs);
    }

    @Test
    public void constructorConContext_obtieneSharedPreferencesCorrectas() {
        Context mockContext = mock(Context.class);
        when(mockContext.getApplicationContext()).thenReturn(mockContext);
        when(mockContext.getSharedPreferences(eq(ProPassManager.PREFS_NAME), eq(Context.MODE_PRIVATE)))
                .thenReturn(mockPrefs);

        ProPassManager manager = new ProPassManager(mockContext);
        assertFalse(manager.hasActiveProPass());
    }

    @Test
    public void activarPase12Horas_estableceTiempoFuturoCorrecto() {
        long antes = System.currentTimeMillis();
        proPassManager.activarPase12Horas();
        long despues = System.currentTimeMillis();

        long expireTime = proPassManager.getExpireTime();
        assertTrue("ExpireTime debe ser >= antes + 12h", expireTime >= antes + ProPassManager.DURACION_PASE_MS);
        assertTrue("ExpireTime debe ser <= despues + 12h", expireTime <= despues + ProPassManager.DURACION_PASE_MS);
        assertTrue("Pase debe estar activo", proPassManager.hasActiveProPass());
        assertTrue("isPaseActivo() debe ser true", proPassManager.isPaseActivo());
    }

    @Test
    public void hasActiveProPass_sinActivar_retornaFalse() {
        assertFalse(proPassManager.hasActiveProPass());
        assertFalse(proPassManager.isPaseActivo());
        assertEquals(0L, proPassManager.getTiempoRestanteMs());
    }

    @Test
    public void hasActiveProPass_cuandoExpirado_retornaFalse() {
        long expirado = System.currentTimeMillis() - 1000L;
        proPassManager.setExpireTime(expirado);

        assertFalse(proPassManager.hasActiveProPass());
        assertEquals(0L, proPassManager.getTiempoRestanteMs());
    }

    @Test
    public void getTiempoRestanteMs_calculaMilisegundosRestantes() {
        long ahora = System.currentTimeMillis();
        long dosHoras = 2 * 60 * 60 * 1000L;
        proPassManager.setExpireTime(ahora + dosHoras);

        long restante = proPassManager.getTiempoRestanteMs();
        assertTrue(restante > dosHoras - 2000L && restante <= dosHoras);
    }

    @Test
    public void getTiempoRestanteFormateado_conHorasYMinutos() {
        long ahora = System.currentTimeMillis();
        // 11 horas y 45 minutos y 10 segundos
        long tiempo = (11 * 3600 + 45 * 60 + 10) * 1000L;
        proPassManager.setExpireTime(ahora + tiempo);

        String formateado = proPassManager.getTiempoRestanteFormateado();
        assertEquals("11h 45m", formateado);
        assertEquals("11h 45m", proPassManager.formatearTiempoRestante());
    }

    @Test
    public void getTiempoRestanteFormateado_conSoloMinutos() {
        long ahora = System.currentTimeMillis();
        // 45 minutos y 10 segundos
        long tiempo = (45 * 60 + 10) * 1000L;
        proPassManager.setExpireTime(ahora + tiempo);

        String formateado = proPassManager.getTiempoRestanteFormateado();
        assertEquals("45m", formateado);
    }

    @Test
    public void getTiempoRestanteFormateado_cuandoExpirado_retornaExpirado() {
        proPassManager.setExpireTime(System.currentTimeMillis() - 5000L);
        assertEquals("Expirado", proPassManager.getTiempoRestanteFormateado());

        proPassManager.setExpireTime(0L);
        assertEquals("Expirado", proPassManager.getTiempoRestanteFormateado());
    }

    @Test
    public void isProActive_conPaseActivoYSinBilling_retornaTrue() {
        proPassManager.setExpireTime(System.currentTimeMillis() + 60000L);
        assertTrue(proPassManager.isProActive(null));
    }

    @Test
    public void isProActive_conPaseActivoYBillingSinCompra_retornaTrue() {
        proPassManager.setExpireTime(System.currentTimeMillis() + 60000L);
        BillingManager mockBilling = mock(BillingManager.class);
        when(mockBilling.isAdsRemoved()).thenReturn(false);

        assertTrue(proPassManager.isProActive(mockBilling));
    }

    @Test
    public void isProActive_conBillingCompraActivaYSinPase_retornaTrue() {
        proPassManager.setExpireTime(0L); // Pase no activo
        BillingManager mockBilling = mock(BillingManager.class);
        when(mockBilling.isAdsRemoved()).thenReturn(true);

        assertTrue(proPassManager.isProActive(mockBilling));
    }

    @Test
    public void isProActive_sinCompraYSinPase_retornaFalse() {
        proPassManager.setExpireTime(0L);
        assertFalse(proPassManager.isProActive(null));

        BillingManager mockBilling = mock(BillingManager.class);
        when(mockBilling.isAdsRemoved()).thenReturn(false);
        assertFalse(proPassManager.isProActive(mockBilling));
    }

    @Test
    public void limpiarPase_remueveClaveYDesactiva() {
        proPassManager.activarPase12Horas();
        assertTrue(proPassManager.hasActiveProPass());

        proPassManager.limpiarPase();
        assertFalse(proPassManager.hasActiveProPass());
        assertEquals(0L, proPassManager.getExpireTime());
        assertEquals("Expirado", proPassManager.getTiempoRestanteFormateado());
    }
}
