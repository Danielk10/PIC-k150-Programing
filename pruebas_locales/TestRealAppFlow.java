import android.content.Context;
import com.diamon.chip.ChipPic;
import com.diamon.managers.PicProgrammingManager;
import com.diamon.protocolo.ProtocoloP18A;
import com.diamon.protocolo.TipoProtocolo;
import com.hoho.android.usbserial.driver.PtyUsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialPort;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class TestRealAppFlow {

    private static final String VTTY_PATH = "/home/danielpdiamon/emulador_picpro/vtty";
    private static final String HEX_FILE_PATH = "/home/danielpdiamon/pwmc_main107_628A.HEX";

    public static void main(String[] args) {
        System.out.println("======================================================================");
        System.out.println("   VALIDACIÓN LOCAL: REPLICANDO EL FLUJO REAL DE LA APP CON EL EMULADOR");
        System.out.println("======================================================================");

        // 1. Validar puerto virtual
        File vttyFile = new File(VTTY_PATH);
        if (!vttyFile.exists()) {
            System.err.println("ERROR: El puerto virtual " + VTTY_PATH + " no existe.");
            System.exit(1);
        }

        // 2. Validar archivo HEX
        File hexFile = new File(HEX_FILE_PATH);
        if (!hexFile.exists()) {
            System.err.println("ERROR: El archivo HEX de prueba " + HEX_FILE_PATH + " no existe.");
            System.exit(1);
        }

        try {
            String hexContent = new String(Files.readAllBytes(Paths.get(HEX_FILE_PATH)), StandardCharsets.UTF_8);

            // 3. Crear Contexto Stub
            Context context = new Context() {
                @Override
                public String getString(int resId) {
                    switch (resId) {
                        case com.diamon.pic.R.string.protocolo_no_inicializado:
                            return "Protocolo no inicializado";
                        case com.diamon.pic.R.string.datos_invalidos_para_programac:
                            return "Datos inválidos para programación";
                        case com.diamon.pic.R.string.borrando_memorias:
                            return "Borrando memorias del PIC...";
                        case com.diamon.pic.R.string.error_borrando_memorias:
                            return "Error al borrar las memorias";
                        case com.diamon.pic.R.string.programando_memoria_rom:
                            return "Programando memoria ROM...";
                        case com.diamon.pic.R.string.error_programando_rom:
                            return "Error al programar la ROM";
                        case com.diamon.pic.R.string.programando_memoria_eeprom:
                            return "Programando memoria EEPROM...";
                        case com.diamon.pic.R.string.error_programando_eeprom:
                            return "Error al programar la EEPROM";
                        case com.diamon.pic.R.string.programando_fuses_id:
                            return "Programando fuses e ID...";
                        case com.diamon.pic.R.string.error_programando_fuses:
                            return "Error al programar fuses";
                        case com.diamon.pic.R.string.programando_fuses_18f:
                            return "Programando fuses adicionales PIC18...";
                        case com.diamon.pic.R.string.error_programando_fuses_18f:
                            return "Error al programar fuses de PIC18";
                        case com.diamon.pic.R.string.programacion_completada:
                            return "Programación completada exitosamente";
                        case com.diamon.pic.R.string.error_inesperado:
                            return "Error inesperado";
                        default:
                            return "Recurso_" + resId;
                    }
                }

                @Override
                public String getString(int resId, Object... formatArgs) {
                    return getString(resId);
                }
            };

            // 4. Abrir puerto virtual PTY como puerto serial USB
            PtyUsbSerialPort usbPort = new PtyUsbSerialPort(vttyFile);

            // 5. Instanciar Protocolo Real
            ProtocoloP18A protocolo = new ProtocoloP18A(context, usbPort, TipoProtocolo.P18A);

            // Realizar handshake inicial
            System.out.println("[Handshake] Inicializando Protocolo...");
            if (!protocolo.iniciarProtocolo()) {
                System.err.println("ERROR: Fallo al iniciar protocolo con el emulador.");
                System.exit(1);
            }
            System.out.println("[Handshake] Eco: " + formatEco(protocolo.hacerUnEco()));
            System.out.println("[Handshake] Programador: " + protocolo.obtenerVersionOModeloDelProgramador());
            System.out.println("[Handshake] Protocolo: " + protocolo.obtenerProtocoloDelProgramador());

            // 6. Instanciar PicProgrammingManager Real de la App
            PicProgrammingManager programmingManager = new PicProgrammingManager(context);
            programmingManager.setProtocolo(protocolo);

            // Configurar Listener de Progreso para capturar eventos reales
            programmingManager.setProgrammingListener(new PicProgrammingManager.ProgrammingListener() {
                @Override
                public void onProgrammingStarted() {
                    System.out.println("\n>>> [Listener] INICIANDO PROCESO DE PROGRAMACIÓN");
                }

                @Override
                public void onProgrammingProgress(String message, int progress) {
                    System.out.printf(">>> [Listener] Progreso [%d%%]: %s\n", progress, message);
                }

                @Override
                public void onProgrammingCompleted(boolean success) {
                    System.out.println(">>> [Listener] PROGRAMACIÓN FINALIZADA. Éxito: " + success);
                }

                @Override
                public void onProgrammingError(String errorMessage) {
                    System.err.println(">>> [Listener] ERROR DE PROGRAMACIÓN: " + errorMessage);
                }
            });

            // 7. Configurar ChipPic real para PIC16F628A
            Map<String, Object> fusesMap = new HashMap<>();
            ChipPic chip16f628a = new ChipPic(
                    "16F628A",      // CHIPname
                    "Y",            // INCLUDEr
                    "18pin",        // SocketImage
                    "2",            // EraseMode
                    "Y",            // FlashChip
                    "Vpp2Vcc",      // PowerSequence
                    "50",           // ProgramDelay
                    "1",            // ProgramTries
                    "0",            // OverProgram
                    "bit14_B",      // CoreType
                    "000800",       // ROMsize (2048 words / 4096 bytes)
                    "00000080",     // EEPROMsize (128 bytes)
                    new String[] { "3FFF" }, // FUSEblank
                    "N",            // CPwarn
                    "N",            // CALword
                    "N",            // BandGap
                    "N",            // ICSPonly
                    "1060",         // ChipID
                    fusesMap        // fuses map
            );

            // Cargar configuración de variables del chip
            if (!protocolo.iniciarVariablesDeProgramacion(chip16f628a)) {
                System.err.println("ERROR: No se pudieron cargar las variables de programación en el programador.");
                System.exit(1);
            }
            System.out.println("[Handshake] Variables del chip cargadas exitosamente.");

            // 8. Lanzar la programación completa usando PicProgrammingManager
            // (Borrado -> Grabar ROM -> Grabar EEPROM -> Grabar Fuses/ID)
            byte[] idPic = new byte[] { 0, 0, 0, 0 };
            boolean result = programmingManager.programChip(chip16f628a, hexContent, idPic, new ArrayList<>());

            usbPort.close();

            if (result) {
                System.out.println("\n======================================================================");
                System.out.println(" ¡EL FLUJO REAL DE PROGRAMACIÓN FUNCIONA PERFECTAMENTE CON EL EMULADOR!");
                System.out.println("======================================================================");
            } else {
                System.err.println("\n======================================================================");
                System.err.println(" ERROR: Falló la simulación del flujo real de programación.");
                System.err.println("======================================================================");
                System.exit(1);
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static String formatEco(String eco) {
        if (eco == null || eco.isEmpty()) return "vacío";
        return String.format("0x%02X", (int) eco.charAt(0));
    }
}
