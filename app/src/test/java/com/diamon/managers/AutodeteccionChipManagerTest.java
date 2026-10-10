package com.diamon.managers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.widget.Spinner;

import com.diamon.datos.ChipinfoReader;
import com.diamon.protocolo.ProtocoloP18A;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

/**
 * Pruebas unitarias para validar la interacción entre PicProgrammingManager,
 * ChipinfoReader y ChipSelectionManager durante la autodetección de chips.
 */
public class AutodeteccionChipManagerTest {

    private ChipinfoReader chipReader;

    @Before
    public void setUp() throws Exception {
        List<String> lineas = Arrays.asList(
                "CHIPname=16F628A",
                "INCLUDE=Y",
                "SocketImage=18pin",
                "EraseMode=2",
                "FlashChip=Y",
                "PowerSequence=Vpp2Vcc",
                "ProgramDelay=50",
                "ProgramTries=1",
                "OverProgram=0",
                "CoreType=bit14_B",
                "ROMsize=000800",
                "EEPROMsize=00000080",
                "FUSEblank=3FFF",
                "ChipID=1060"
        );
        chipReader = new ChipinfoReader(lineas);
    }

    @Test
    public void autodectarChip_sinProtocoloRetornaFalso() {
        Context context = mock(Context.class);
        PicProgrammingManager manager = new PicProgrammingManager(context);

        PicProgrammingManager.ResultadoAutodeteccion res = manager.autodectarChip(chipReader);
        assertNotNull(res);
        assertFalse(res.chipEnSocket);
        assertEquals(-1, res.rawDeviceId);
        assertNull(res.modeloIdentificado);
    }

    @Test
    public void autodectarChip_sinChipEnSocketRetornaFalso() throws Exception {
        Context context = mock(Context.class);
        PicProgrammingManager manager = new PicProgrammingManager(context);
        ProtocoloP18A mockProtocolo = mock(ProtocoloP18A.class);

        when(mockProtocolo.detectarPicEnElSocket()).thenReturn(false);

        // Inyectar protocolo via reflection
        Field protoField = PicProgrammingManager.class.getDeclaredField("protocolo");
        protoField.setAccessible(true);
        protoField.set(manager, mockProtocolo);

        PicProgrammingManager.ResultadoAutodeteccion res = manager.autodectarChip(chipReader);
        assertNotNull(res);
        assertFalse(res.chipEnSocket);
        assertEquals(-1, res.rawDeviceId);
        assertNull(res.modeloIdentificado);
    }

    @Test
    public void autodectarChip_conChipExitosoIdentifica16F628A() throws Exception {
        Context context = mock(Context.class);
        PicProgrammingManager manager = new PicProgrammingManager(context);
        ProtocoloP18A mockProtocolo = mock(ProtocoloP18A.class);

        when(mockProtocolo.detectarPicEnElSocket()).thenReturn(true);
        // Simular Device ID 0x6810 leído en hardware real
        when(mockProtocolo.leerDeviceIDDelSocket()).thenReturn(0x6810);

        Field protoField = PicProgrammingManager.class.getDeclaredField("protocolo");
        protoField.setAccessible(true);
        protoField.set(manager, mockProtocolo);

        PicProgrammingManager.ResultadoAutodeteccion res = manager.autodectarChip(chipReader);
        assertNotNull(res);
        assertTrue(res.chipEnSocket);
        assertEquals(0x6810, res.rawDeviceId);
        assertEquals("16F628A", res.modeloIdentificado);
    }

    @Test
    public void autodectarChip_chipPresentePeroDeviceIdDesconocido() throws Exception {
        Context context = mock(Context.class);
        PicProgrammingManager manager = new PicProgrammingManager(context);
        ProtocoloP18A mockProtocolo = mock(ProtocoloP18A.class);

        when(mockProtocolo.detectarPicEnElSocket()).thenReturn(true);
        when(mockProtocolo.leerDeviceIDDelSocket()).thenReturn(0x4321);

        Field protoField = PicProgrammingManager.class.getDeclaredField("protocolo");
        protoField.setAccessible(true);
        protoField.set(manager, mockProtocolo);

        PicProgrammingManager.ResultadoAutodeteccion res = manager.autodectarChip(chipReader);
        assertNotNull(res);
        assertTrue(res.chipEnSocket);
        assertEquals(0x4321, res.rawDeviceId);
        assertNull(res.modeloIdentificado);
    }

    @Test
    public void leerDeviceIDDelSocket_enProtocoloP18A() {
        ProtocoloP18A protocolo = mock(ProtocoloP18A.class);

        when(protocolo.leerDeviceIDDelSocket()).thenCallRealMethod();

        // 1. Caso exitoso: 0x6810
        when(protocolo.leerDatosDeConfiguracionDelPic()).thenReturn("6810FFFFFFFFFFFFFFFFFFFF");
        assertEquals(0x6810, protocolo.leerDeviceIDDelSocket());

        // 2. Caso error
        when(protocolo.leerDatosDeConfiguracionDelPic()).thenReturn("Error de lectura");
        assertEquals(-1, protocolo.leerDeviceIDDelSocket());

        // 3. Caso nulo
        when(protocolo.leerDatosDeConfiguracionDelPic()).thenReturn(null);
        assertEquals(-1, protocolo.leerDeviceIDDelSocket());

        // 4. Caso cadena corta
        when(protocolo.leerDatosDeConfiguracionDelPic()).thenReturn("12");
        assertEquals(-1, protocolo.leerDeviceIDDelSocket());

        // 5. Caso no hex
        when(protocolo.leerDatosDeConfiguracionDelPic()).thenReturn("ZZZZ");
        assertEquals(-1, protocolo.leerDeviceIDDelSocket());
    }
}
