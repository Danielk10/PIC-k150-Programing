package com.diamon.managers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.diamon.chip.ChipPic;
import com.diamon.excepciones.ChipConfigurationException;
import com.diamon.managers.PicProgrammingManager.ResultadoVerificacionBorrado;
import com.diamon.managers.ProgramacionSelectivaManager.TipoProgramacion;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Pruebas unitarias para ProgramacionSelectivaManager.
 * Valida la delegación en los diferentes modos de programación
 * y el formateo detallado del reporte Blank Check Dual (ROM y EEPROM).
 */
public class ProgramacionSelectivaTest {

    private ProgramacionSelectivaManager manager;
    private PicProgrammingManager mockProgManager;
    private ChipPic mockChip;

    @Before
    public void setUp() {
        manager = new ProgramacionSelectivaManager(null);
        mockProgManager = mock(PicProgrammingManager.class);
        mockChip = mock(ChipPic.class);
    }

    @Test
    public void testTipoProgramacionEnum_valoresYDescripciones() {
        TipoProgramacion[] tipos = TipoProgramacion.values();
        assertEquals(4, tipos.length);

        for (TipoProgramacion tipo : tipos) {
            assertNotNull(tipo.getTitulo());
            assertFalse(tipo.getTitulo().isEmpty());
            assertNotNull(tipo.getDescripcion());
            assertFalse(tipo.getDescripcion().isEmpty());
        }

        assertEquals("🚀 Grabación Completa", TipoProgramacion.COMPLETA.getTitulo());
        assertEquals("💾 Grabar Solo Flash ROM", TipoProgramacion.SOLO_ROM.getTitulo());
        assertEquals("📦 Grabar Solo EEPROM", TipoProgramacion.SOLO_EEPROM.getTitulo());
        assertEquals("⚙️ Grabar Solo Fusibles de Configuración", TipoProgramacion.SOLO_CONFIG.getTitulo());
    }

    @Test
    public void testEjecutarProgramacion_conParametrosNulos_retornaFalse() {
        assertFalse(manager.ejecutarProgramacion(TipoProgramacion.COMPLETA, null, mockChip, ":00000001FF", new byte[]{0}, new ArrayList<>()));
        assertFalse(manager.ejecutarProgramacion(TipoProgramacion.COMPLETA, mockProgManager, null, ":00000001FF", new byte[]{0}, new ArrayList<>()));
        assertFalse(manager.ejecutarProgramacion(TipoProgramacion.COMPLETA, mockProgManager, mockChip, null, new byte[]{0}, new ArrayList<>()));
        assertFalse(manager.ejecutarProgramacion(TipoProgramacion.COMPLETA, mockProgManager, mockChip, "", new byte[]{0}, new ArrayList<>()));
    }

    @Test
    public void testEjecutarProgramacion_completa_delegaEnProgramChip() {
        String firmware = ":020000040000FA\n:00000001FF";
        byte[] idPic = new byte[]{0x12, 0x34};
        List<Integer> fuses = new ArrayList<>();
        fuses.add(0x3FFF);

        when(mockProgManager.programChip(mockChip, firmware, idPic, fuses)).thenReturn(true);

        boolean result = manager.ejecutarProgramacion(
                TipoProgramacion.COMPLETA,
                mockProgManager,
                mockChip,
                firmware,
                idPic,
                fuses
        );

        assertTrue(result);
        verify(mockProgManager).programChip(mockChip, firmware, idPic, fuses);
    }

    @Test
    public void testEjecutarProgramacion_soloRom_delegaEnProgramRomOnly() {
        String firmware = ":020000040000FA\n:00000001FF";
        byte[] idPic = new byte[]{0};
        List<Integer> fuses = new ArrayList<>();

        when(mockProgManager.programRomOnly(mockChip, firmware)).thenReturn(true);

        boolean result = manager.ejecutarProgramacion(
                TipoProgramacion.SOLO_ROM,
                mockProgManager,
                mockChip,
                firmware,
                idPic,
                fuses
        );

        assertTrue(result);
        verify(mockProgManager).programRomOnly(mockChip, firmware);
    }

    @Test
    public void testEjecutarProgramacion_soloEeprom_delegaEnProgramEepromOnly() {
        String firmware = ":020000040000FA\n:00000001FF";
        byte[] idPic = new byte[]{0};
        List<Integer> fuses = new ArrayList<>();

        when(mockProgManager.programEepromOnly(mockChip, firmware)).thenReturn(true);

        boolean result = manager.ejecutarProgramacion(
                TipoProgramacion.SOLO_EEPROM,
                mockProgManager,
                mockChip,
                firmware,
                idPic,
                fuses
        );

        assertTrue(result);
        verify(mockProgManager).programEepromOnly(mockChip, firmware);
    }

    @Test
    public void testEjecutarProgramacion_soloConfig_delegaEnProgramConfigOnly() {
        String firmware = ":020000040000FA\n:00000001FF";
        byte[] idPic = new byte[]{0x01, 0x02};
        List<Integer> fuses = new ArrayList<>();
        fuses.add(0x2118);

        when(mockProgManager.programConfigOnly(mockChip, firmware, idPic, fuses)).thenReturn(true);

        boolean result = manager.ejecutarProgramacion(
                TipoProgramacion.SOLO_CONFIG,
                mockProgManager,
                mockChip,
                firmware,
                idPic,
                fuses
        );

        assertTrue(result);
        verify(mockProgManager).programConfigOnly(mockChip, firmware, idPic, fuses);
    }

    @Test
    public void testFormatearReporteBlankCheckDual_resultadoNulo_reportaError() {
        List<String> reporte = manager.formatearReporteBlankCheckDual(null, mockChip);
        assertNotNull(reporte);
        assertEquals(1, reporte.size());
        assertTrue(reporte.get(0).contains("resultado nulo"));
    }

    @Test
    public void testFormatearReporteBlankCheckDual_conError_reportaError() {
        ResultadoVerificacionBorrado res = new ResultadoVerificacionBorrado(
                false, false, false, "N/A", "Timeout de comunicación en socket"
        );

        List<String> reporte = manager.formatearReporteBlankCheckDual(res, mockChip);
        assertNotNull(reporte);
        assertEquals(1, reporte.size());
        assertTrue(reporte.get(0).contains("Timeout de comunicación en socket"));
    }

    @Test
    public void testFormatearReporteBlankCheckDual_pic14Bit_ambosEnBlanco() throws ChipConfigurationException {
        when(mockChip.getTipoDeNucleoBit()).thenReturn(14);
        when(mockChip.isTamanoValidoDeEEPROM()).thenReturn(true);

        ResultadoVerificacionBorrado res = new ResultadoVerificacionBorrado(
                true, true, false, "Hardware K150 (P18A)", null
        );

        List<String> reporte = manager.formatearReporteBlankCheckDual(res, mockChip);
        assertNotNull(reporte);

        String reporteCompleto = String.join("\n", reporte);
        assertTrue(reporteCompleto.contains("Blank Check Dual"));
        assertTrue(reporteCompleto.contains("ROM limpia al 100% (0x3FFF)"));
        assertTrue(reporteCompleto.contains("EEPROM limpia al 100% (0xFF)"));
        assertTrue(reporteCompleto.contains("Hardware K150 (P18A)"));
        assertTrue(reporteCompleto.contains("Microcontrolador listo para grabar (100% Borrado)"));
    }

    @Test
    public void testFormatearReporteBlankCheckDual_pic16Bit_ambosEnBlanco() throws ChipConfigurationException {
        when(mockChip.getTipoDeNucleoBit()).thenReturn(16);
        when(mockChip.isTamanoValidoDeEEPROM()).thenReturn(true);

        ResultadoVerificacionBorrado res = new ResultadoVerificacionBorrado(
                true, true, false, "Comando R 0x4200", null
        );

        List<String> reporte = manager.formatearReporteBlankCheckDual(res, mockChip);
        String reporteCompleto = String.join("\n", reporte);

        assertTrue(reporteCompleto.contains("ROM limpia al 100% (0xFFFF)"));
        assertTrue(reporteCompleto.contains("EEPROM limpia al 100% (0xFF)"));
        assertTrue(reporteCompleto.contains("Microcontrolador listo para grabar (100% Borrado)"));
    }

    @Test
    public void testFormatearReporteBlankCheckDual_memoriasConDatos_reportaAdvertencias() throws ChipConfigurationException {
        when(mockChip.getTipoDeNucleoBit()).thenReturn(14);
        when(mockChip.isTamanoValidoDeEEPROM()).thenReturn(true);

        ResultadoVerificacionBorrado res = new ResultadoVerificacionBorrado(
                false, false, false, "Hardware K150", null
        );

        List<String> reporte = manager.formatearReporteBlankCheckDual(res, mockChip);
        String reporteCompleto = String.join("\n", reporte);

        assertTrue(reporteCompleto.contains("ROM con datos previos detectados"));
        assertTrue(reporteCompleto.contains("EEPROM con datos previos detectados"));
        assertTrue(reporteCompleto.contains("Se requiere borrado previo antes de grabar"));
    }

    @Test
    public void testFormatearReporteBlankCheckDual_sinEeprom_noMuestraEeprom() throws ChipConfigurationException {
        when(mockChip.getTipoDeNucleoBit()).thenReturn(14);
        when(mockChip.isTamanoValidoDeEEPROM()).thenReturn(false);

        ResultadoVerificacionBorrado res = new ResultadoVerificacionBorrado(
                true, true, false, "Hardware K150", null
        );

        List<String> reporte = manager.formatearReporteBlankCheckDual(res, mockChip);
        String reporteCompleto = String.join("\n", reporte);

        assertTrue(reporteCompleto.contains("ROM limpia al 100% (0x3FFF)"));
        assertFalse(reporteCompleto.contains("EEPROM"));
        assertTrue(reporteCompleto.contains("Microcontrolador listo para grabar (100% Borrado)"));
    }
}
