package com.diamon.managers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

import com.diamon.chip.ChipPic;
import com.diamon.protocolo.ProtocoloP18A;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Pruebas unitarias para CalibracionManager:
 * - Detección de necesidad de calibración (PIC10F, PIC12F, PIC16F).
 * - Parseo y formateo de OSCCAL (RETLW y registros 10F).
 * - Validación y parseo de Vector de Depuración ICD (24 bits).
 * - Ciclo de preservación automática durante borrado masivo.
 */
public class CalibracionManagerTest {

    private Context context;
    private ProtocoloP18A protocolo;
    private PicProgrammingManager programmingManager;
    private CalibracionManager calibracionManager;

    private ChipPic chip10f200;
    private ChipPic chip12f675;
    private ChipPic chip16f628a;

    @Before
    public void setUp() throws Exception {
        context = mock(Context.class);
        protocolo = mock(ProtocoloP18A.class);
        programmingManager = mock(PicProgrammingManager.class);
        calibracionManager = new CalibracionManager(context, protocolo);

        Map<String, Object> fuses = new HashMap<>();

        // PIC10F200: 12-bit NewF12B con registros de calibración
        chip10f200 = new ChipPic(
                "10F200", "Y", "0pin", "6", "Y", "VccVpp1", "20", "1", "0",
                "NewF12B", "000100", "00000000", new String[]{"0FFF"}, "N", "Y", "N", "N", "0000", fuses);

        // PIC12F675: 14-bit con OSCCAL en última posición de ROM (CALword=Y)
        chip12f675 = new ChipPic(
                "12F675", "Y", "8pin", "2", "Y", "Vpp2Vcc", "50", "1", "0",
                "bit14_B", "000400", "00000080", new String[]{"3FFF"}, "N", "Y", "N", "N", "0FC0", fuses);

        // PIC16F628A: 14-bit estándar sin OSCCAL (CALword=N)
        chip16f628a = new ChipPic(
                "16F628A", "Y", "18pin", "2", "Y", "Vpp2Vcc", "50", "1", "0",
                "bit14_B", "000800", "00000080", new String[]{"3FFF"}, "N", "N", "N", "N", "1060", fuses);
    }

    @Test
    public void requiereCalibracion_clasificacionCorrecta() {
        assertTrue(CalibracionManager.requiereCalibracion(chip10f200));
        assertTrue(CalibracionManager.requiereCalibracion(chip12f675));
        assertFalse(CalibracionManager.requiereCalibracion(chip16f628a));
        assertFalse(CalibracionManager.requiereCalibracion(null));
    }

    @Test
    public void isFamilia10F_identificaFamilia() {
        assertTrue(CalibracionManager.isFamilia10F(chip10f200));
        assertFalse(CalibracionManager.isFamilia10F(chip12f675));
        assertFalse(CalibracionManager.isFamilia10F(chip16f628a));
        assertFalse(CalibracionManager.isFamilia10F(null));
    }

    @Test
    public void parsearCalibracionHex_reconstruyeValores() {
        // Formato Little-Endian con opcode RETLW (0x3448 desde "4834")
        assertEquals(0x3448, CalibracionManager.parsearCalibracionHex("4834"));

        // Formato directo RETLW (0x3450 desde "3450")
        assertEquals(0x3450, CalibracionManager.parsearCalibracionHex("3450"));

        // Formato PIC10F (12 bits: 0x0C2A desde "2A0C")
        assertEquals(0x0C2A, CalibracionManager.parsearCalibracionHex("2A0C"));

        // Casos inválidos
        assertEquals(-1, CalibracionManager.parsearCalibracionHex("ZZZZ"));
        assertEquals(-1, CalibracionManager.parsearCalibracionHex("12"));
        assertEquals(-1, CalibracionManager.parsearCalibracionHex(null));
    }

    @Test
    public void parsearDireccionIcdHex_validaRango24Bits() {
        // Válidos con y sin prefijo 0x
        assertEquals(0x000020, CalibracionManager.parsearDireccionIcdHex("000020"));
        assertEquals(0x000020, CalibracionManager.parsearDireccionIcdHex("0x000020"));
        assertEquals(0x123456, CalibracionManager.parsearDireccionIcdHex("123456"));
        assertEquals(0xFFFFFF, CalibracionManager.parsearDireccionIcdHex("0xFFFFFF"));
        assertEquals(0x000000, CalibracionManager.parsearDireccionIcdHex("0"));

        // Inválidos (fuera de rango, no hex, nulos)
        assertEquals(-1, CalibracionManager.parsearDireccionIcdHex("1000000")); // más de 24 bits
        assertEquals(-1, CalibracionManager.parsearDireccionIcdHex("GHIJKL"));
        assertEquals(-1, CalibracionManager.parsearDireccionIcdHex(""));
        assertEquals(-1, CalibracionManager.parsearDireccionIcdHex(null));
    }

    @Test
    public void formatearCalibracionYDireccionIcd() {
        String f1 = CalibracionManager.formatearCalibracionHex(0x3448, false);
        assertTrue(f1.contains("0x3448") && f1.contains("RETLW 0x48"));

        String f2 = CalibracionManager.formatearCalibracionHex(0x0C2A, true);
        assertEquals("0x0C2A", f2);

        String f3 = CalibracionManager.formatearCalibracionHex(-1, false);
        assertEquals("No disponible", f3);

        String addr = CalibracionManager.formatearDireccionIcdHex(0x000020);
        assertEquals("0x000020", addr);
    }

    @Test
    public void borrarConPreservacion_flujoPIC12F675_preservaYRestaura() {
        // Simular lectura de OSCCAL previo: 0x3448 (hex LE "4834")
        when(protocolo.leerDatosDeCalibracionDelPic()).thenReturn("4834");
        when(programmingManager.eraseMemory()).thenReturn(true);
        when(protocolo.programarCalibracionDelPic(chip12f675, 0x3448)).thenReturn(true);

        CalibracionManager.ResultadoBorradoSeguro res =
                calibracionManager.borrarConPreservacion(chip12f675, programmingManager);

        assertNotNull(res);
        assertTrue(res.borradoExitoso);
        assertTrue(res.requiereCalibracion);
        assertEquals(Integer.valueOf(0x3448), res.osccalPreservado);
        assertTrue(res.restauracionExitosa);
        assertTrue(res.mensaje.contains("0x3448"));

        verify(protocolo).leerDatosDeCalibracionDelPic();
        verify(programmingManager).eraseMemory();
        verify(protocolo).programarCalibracionDelPic(chip12f675, 0x3448);
    }

    @Test
    public void borrarConPreservacion_flujoPIC10F_usaComandoDedicado10F() {
        // Simular lectura de OSCCAL en PIC10F: 0x0C2A (hex LE "2A0C")
        when(protocolo.leerDatosDeCalibracionDelPic()).thenReturn("2A0C");
        when(programmingManager.eraseMemory()).thenReturn(true);
        when(protocolo.programarDatosDeCalibracionDePics10F(0x0C2A, 0x0C2A)).thenReturn(true);

        CalibracionManager.ResultadoBorradoSeguro res =
                calibracionManager.borrarConPreservacion(chip10f200, programmingManager);

        assertNotNull(res);
        assertTrue(res.borradoExitoso);
        assertTrue(res.requiereCalibracion);
        assertEquals(Integer.valueOf(0x0C2A), res.osccalPreservado);
        assertTrue(res.restauracionExitosa);

        verify(protocolo).programarDatosDeCalibracionDePics10F(0x0C2A, 0x0C2A);
        verify(protocolo, never()).programarCalibracionDelPic(any(), anyInt());
    }

    @Test
    public void borrarConPreservacion_chipSinCalibracion_borradoDirecto() {
        when(programmingManager.eraseMemory()).thenReturn(true);

        CalibracionManager.ResultadoBorradoSeguro res =
                calibracionManager.borrarConPreservacion(chip16f628a, programmingManager);

        assertNotNull(res);
        assertTrue(res.borradoExitoso);
        assertFalse(res.requiereCalibracion);
        assertNull(res.osccalPreservado);
        assertFalse(res.restauracionExitosa);

        verify(programmingManager).eraseMemory();
        verify(protocolo, never()).leerDatosDeCalibracionDelPic();
        verify(protocolo, never()).programarCalibracionDelPic(any(), anyInt());
    }
}
