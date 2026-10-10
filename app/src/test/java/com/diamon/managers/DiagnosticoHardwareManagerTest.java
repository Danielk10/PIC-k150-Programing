package com.diamon.managers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.diamon.protocolo.ProtocoloP18A;

import org.junit.Before;
import org.junit.Test;

/**
 * Pruebas unitarias para validar DiagnosticoHardwareManager,
 * incluyendo recolección de diagnósticos, prueba de eco y control de voltajes VPP/VDD.
 */
public class DiagnosticoHardwareManagerTest {

    private DiagnosticoHardwareManager manager;
    private ProtocoloP18A mockProtocolo;

    @Before
    public void setUp() {
        manager = new DiagnosticoHardwareManager();
        mockProtocolo = mock(ProtocoloP18A.class);
    }

    @Test
    public void ejecutarDiagnostico_conProtocoloValido_recolectaDatosCorrectamente() {
        when(mockProtocolo.obtenerVersionOModeloDelProgramador()).thenReturn("K150");
        when(mockProtocolo.obtenerProtocoloDelProgramador()).thenReturn("P18A");
        when(mockProtocolo.hacerUnEco()).thenReturn("2");
        when(mockProtocolo.detectarPicEnElSocket()).thenReturn(true);
        when(mockProtocolo.detectarSiEstaFueraElPicDelSocket()).thenReturn(false);

        DiagnosticoHardwareManager.ResultadoDiagnostico res = manager.ejecutarDiagnostico(mockProtocolo);

        assertNotNull(res);
        assertEquals("K150", res.modeloProgramador);
        assertEquals("P18A", res.protocolo);
        assertTrue(res.ecoExitoso);
        assertTrue(res.latenciaEcoMs >= 0);
        assertTrue(res.picEnSocket);
        assertFalse(res.picFueraSocket);
        assertTrue(res.getEstadoSocketDescripcion().contains("PIC Detectado"));
    }

    @Test
    public void ejecutarDiagnostico_conSocketVacio_identificaCorrectamente() {
        when(mockProtocolo.obtenerVersionOModeloDelProgramador()).thenReturn("K149-B");
        when(mockProtocolo.obtenerProtocoloDelProgramador()).thenReturn("P18A");
        when(mockProtocolo.hacerUnEco()).thenReturn("2");
        when(mockProtocolo.detectarPicEnElSocket()).thenReturn(false);
        when(mockProtocolo.detectarSiEstaFueraElPicDelSocket()).thenReturn(true);

        DiagnosticoHardwareManager.ResultadoDiagnostico res = manager.ejecutarDiagnostico(mockProtocolo);

        assertNotNull(res);
        assertEquals("K149-B", res.modeloProgramador);
        assertFalse(res.picEnSocket);
        assertTrue(res.picFueraSocket);
        assertTrue(res.getEstadoSocketDescripcion().contains("Zócalo vacío"));
    }

    @Test
    public void ejecutarDiagnostico_conProtocoloNulo_retornaValoresSeguros() {
        DiagnosticoHardwareManager.ResultadoDiagnostico res = manager.ejecutarDiagnostico(null);

        assertNotNull(res);
        assertEquals("Desconectado", res.modeloProgramador);
        assertEquals("N/A", res.protocolo);
        assertFalse(res.ecoExitoso);
        assertEquals(-1, res.latenciaEcoMs);
        assertFalse(res.picEnSocket);
        assertFalse(res.picFueraSocket);
    }

    @Test
    public void probarEco_exitoso_retornaLatenciaPositiva() {
        when(mockProtocolo.hacerUnEco()).thenReturn("2");

        long latencia = manager.probarEco(mockProtocolo);

        assertTrue(latencia >= 0);
        verify(mockProtocolo).hacerUnEco();
    }

    @Test
    public void probarEco_fallido_retornaMenosUno() {
        when(mockProtocolo.hacerUnEco()).thenReturn("");

        long latencia = manager.probarEco(mockProtocolo);

        assertEquals(-1, latencia);
    }

    @Test
    public void probarEco_conProtocoloNulo_retornaMenosUno() {
        long latencia = manager.probarEco(null);
        assertEquals(-1, latencia);
    }

    @Test
    public void activarVoltajes_conProtocoloValido_invocaMetodoCorrecto() {
        when(mockProtocolo.activarVoltajesDeProgramacion()).thenReturn(true);

        boolean resultado = manager.activarVoltajes(mockProtocolo);

        assertTrue(resultado);
        verify(mockProtocolo).activarVoltajesDeProgramacion();
    }

    @Test
    public void desactivarVoltajes_conProtocoloValido_invocaMetodoCorrecto() {
        when(mockProtocolo.desactivarVoltajesDeProgramacion()).thenReturn(true);

        boolean resultado = manager.desactivarVoltajes(mockProtocolo);

        assertTrue(resultado);
        verify(mockProtocolo).desactivarVoltajesDeProgramacion();
    }

    @Test
    public void reiniciarVoltajes_conProtocoloValido_invocaMetodoCorrecto() {
        when(mockProtocolo.reiniciarVoltajesDeProgramacion()).thenReturn(true);

        boolean resultado = manager.reiniciarVoltajes(mockProtocolo);

        assertTrue(resultado);
        verify(mockProtocolo).reiniciarVoltajesDeProgramacion();
    }

    @Test
    public void metodosDeVoltaje_conProtocoloNulo_retornanFalso() {
        assertFalse(manager.activarVoltajes(null));
        assertFalse(manager.desactivarVoltajes(null));
        assertFalse(manager.reiniciarVoltajes(null));
    }
}
