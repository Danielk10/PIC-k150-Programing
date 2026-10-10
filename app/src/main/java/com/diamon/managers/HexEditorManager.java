package com.diamon.managers;

import android.app.AlertDialog;
import android.content.Context;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.diamon.chip.ChipPic;
import com.diamon.datos.DatosPicProcesados;

/**
 * Gestor del Editor Hexadecimal Interactivo en Vivo (Code Editor MicroPro).
 *
 * Permite examinar y editar palabras/bytes directamente al pulsar sobre las filas
 * de memoria (ROM o EEPROM).
 * - Abre un diálogo modal con validación hexadecimal estricta.
 * - Actualiza los búferes de memoria en tiempo real (DatosPicProcesados y firmware Intel HEX).
 * - Recalcula el checksum del registro Intel HEX modificado sin necesidad de recargar el archivo externo.
 * - Notifica la modificación y refresca la fila en pantalla.
 */
public class HexEditorManager {

    /**
     * Listener para recibir eventos tras la modificación de memoria.
     */
    public interface OnHexEditListener {
        void onMemoryEdited(
                boolean isRom,
                int address,
                int oldValue,
                int newValue,
                String updatedFirmware
        );
    }

    private final Context context;

    public HexEditorManager(Context context) {
        this.context = context;
    }

    /**
     * Abre el diálogo modal para editar una palabra (ROM) o byte (EEPROM) en vivo.
     *
     * @param context            Contexto de la actividad.
     * @param isRom              true para memoria Flash ROM (14/16-bit words), false para EEPROM (8-bit bytes).
     * @param address            Dirección base de la palabra o byte.
     * @param valorActual        Valor hexadecimal actual.
     * @param chip               Información del chip PIC (núcleo, tamaño).
     * @param datosPicProcesados Búferes en memoria del firmware procesado.
     * @param firmwareActual     Cadena de texto del firmware en formato Intel HEX.
     * @param filaView           Vista TextView de la fila pulsada (opcional, para refresco visual).
     * @param listener           Callback de notificación.
     */
    public void mostrarDialogoEdicion(
            Context context,
            boolean isRom,
            int address,
            int valorActual,
            ChipPic chip,
            DatosPicProcesados datosPicProcesados,
            String firmwareActual,
            TextView filaView,
            OnHexEditListener listener) {

        int coreBits = 14;
        if (chip != null) {
            try {
                coreBits = chip.getTipoDeNucleoBit();
            } catch (com.diamon.excepciones.ChipConfigurationException ignored) {
            }
        }
        int maxWord = (coreBits == 16) ? 0xFFFF : ((~(0xFFFF << coreBits)) & 0xFFFF);
        int maxVal = isRom ? maxWord : 0xFF;
        int maxChars = isRom ? 4 : 2;

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        String tipoMem = isRom ? "ROM (Word)" : "EEPROM (Byte)";
        builder.setTitle("✏️ Editar " + tipoMem);

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = dpToPx(context, 16);
        layout.setPadding(padding, padding, padding, padding);

        TextView infoText = new TextView(context);
        String addrStr = String.format("0x%04X", address);
        String currHex = isRom ? String.format("%04X", valorActual & maxVal) : String.format("%02X", valorActual & 0xFF);
        infoText.setText("Dirección: " + addrStr + "\nValor actual: 0x" + currHex + " (Máx: 0x" + String.format(isRom ? "%04X" : "%02X", maxVal) + ")");
        infoText.setTextSize(14);
        infoText.setTextColor(0xFFCCCCCC);
        infoText.setPadding(0, 0, 0, dpToPx(context, 12));
        layout.addView(infoText);

        EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(currHex);
        input.setSelectAllOnFocus(true);
        input.setGravity(Gravity.CENTER);
        input.setTextSize(18);
        input.setTextColor(0xFF00E676); // Verde neón
        input.setBackgroundColor(0xFF1E1E2E);
        input.setPadding(dpToPx(context, 8), dpToPx(context, 10), dpToPx(context, 8), dpToPx(context, 10));

        // Filtro de caracteres hexadecimales y longitud máxima
        InputFilter hexFilter = (source, start, end, dest, dstart, dend) -> {
            for (int i = start; i < end; i++) {
                char c = source.charAt(i);
                if (Character.digit(c, 16) == -1) {
                    return "";
                }
            }
            return null;
        };
        input.setFilters(new InputFilter[]{hexFilter, new InputFilter.LengthFilter(maxChars)});
        layout.addView(input);

        builder.setView(layout);

        builder.setPositiveButton("Guardar", (dialog, which) -> {
            String text = input.getText().toString().trim();
            if (text.isEmpty()) {
                Toast.makeText(context, "El valor no puede estar vacío", Toast.LENGTH_SHORT).show();
                return;
            }

            int nuevoValor;
            try {
                nuevoValor = Integer.parseInt(text, 16);
            } catch (NumberFormatException e) {
                Toast.makeText(context, "Formato hexadecimal inválido", Toast.LENGTH_SHORT).show();
                return;
            }

            if (nuevoValor > maxVal) {
                Toast.makeText(context, "El valor excede el máximo permitido (0x" + String.format(isRom ? "%04X" : "%02X", maxVal) + ")", Toast.LENGTH_SHORT).show();
                return;
            }

            // 1. Actualizar búferes en memoria (DatosPicProcesados)
            aplicarCambioEnDatosProcesados(datosPicProcesados, isRom, address, nuevoValor);

            // 2. Parchar y recalcular firmware en formato Intel HEX
            String nuevoFirmware = parcharFirmwareIntelHex(firmwareActual, isRom, address, nuevoValor, chip);

            // 3. Refrescar texto de la fila si está disponible
            if (filaView != null) {
                refrescarFilaVisual(filaView, currHex, String.format(isRom ? "%04X" : "%02X", nuevoValor));
            }

            // 4. Notificar al listener
            if (listener != null) {
                listener.onMemoryEdited(isRom, address, valorActual, nuevoValor, nuevoFirmware);
            }
        });

        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    /**
     * Aplica la modificación en los arreglos de bytes de DatosPicProcesados.
     */
    public static void aplicarCambioEnDatosProcesados(
            DatosPicProcesados datosPicProcesados,
            boolean isRom,
            int address,
            int nuevoValor) {

        if (datosPicProcesados == null) return;

        if (isRom) {
            byte[] romBytes = datosPicProcesados.obtenerBytesHexROMProcesado();
            int byteOffset = address * 2;
            if (romBytes != null && byteOffset + 1 < romBytes.length) {
                // Little-Endian (Bus Microchip PIC)
                romBytes[byteOffset] = (byte) (nuevoValor & 0xFF);
                romBytes[byteOffset + 1] = (byte) ((nuevoValor >> 8) & 0xFF);
            }
        } else {
            byte[] eepromBytes = datosPicProcesados.obtenerBytesHexEEPROMProcesado();
            if (eepromBytes != null && address < eepromBytes.length) {
                eepromBytes[address] = (byte) (nuevoValor & 0xFF);
            }
        }
    }

    /**
     * Parcha el archivo Intel HEX actualizando los bytes modificados y recalculando su checksum.
     */
    public static String parcharFirmwareIntelHex(
            String firmwareHex,
            boolean isRom,
            int address,
            int nuevoValor,
            ChipPic chip) {

        if (firmwareHex == null || firmwareHex.isEmpty()) {
            return firmwareHex;
        }

        int byteAddress;
        byte[] nuevosBytes;

        if (isRom) {
            byteAddress = address * 2;
            nuevosBytes = new byte[]{
                    (byte) (nuevoValor & 0xFF),
                    (byte) ((nuevoValor >> 8) & 0xFF)
            };
        } else {
            // Mapeo de dirección base de EEPROM en Intel HEX según familia
            int core = 14;
            if (chip != null) {
                try {
                    core = chip.getTipoDeNucleoBit();
                } catch (com.diamon.excepciones.ChipConfigurationException ignored) {
                }
            }
            int eepromBase = (core == 16) ? 0xF00000 : 0x2100;
            byteAddress = eepromBase + address;
            nuevosBytes = new byte[]{(byte) (nuevoValor & 0xFF)};
        }

        return parcharIntelHex(firmwareHex, byteAddress, nuevosBytes);
    }

    /**
     * Algoritmo de parcheo y recálculo de checksum para registros Intel HEX.
     */
    public static String parcharIntelHex(String firmwareHex, int byteAddress, byte[] nuevosBytes) {
        if (firmwareHex == null || firmwareHex.isEmpty() || nuevosBytes == null || nuevosBytes.length == 0) {
            return firmwareHex;
        }

        String[] lineas = firmwareHex.split("\\r?\\n");
        int baseAddress = 0;
        boolean modificado = false;
        StringBuilder sb = new StringBuilder();

        for (String linea : lineas) {
            linea = linea.trim();
            if (!linea.startsWith(":") || linea.length() < 11) {
                sb.append(linea).append("\n");
                continue;
            }

            try {
                int byteCount = Integer.parseInt(linea.substring(1, 3), 16);
                int recordAddress = Integer.parseInt(linea.substring(3, 7), 16);
                int recordType = Integer.parseInt(linea.substring(7, 9), 16);

                if (recordType == 4) { // Extended Linear Address
                    int upper = Integer.parseInt(linea.substring(9, 13), 16);
                    baseAddress = upper << 16;
                } else if (recordType == 2) { // Extended Segment Address
                    int segment = Integer.parseInt(linea.substring(9, 13), 16);
                    baseAddress = segment << 4;
                } else if (recordType == 0) { // Data record
                    int absAddress = baseAddress + recordAddress;
                    int absEnd = absAddress + byteCount;

                    boolean lineaSolapa = false;
                    byte[] recordData = new byte[byteCount];
                    for (int k = 0; k < byteCount; k++) {
                        int pos = 9 + k * 2;
                        recordData[k] = (byte) Integer.parseInt(linea.substring(pos, pos + 2), 16);
                    }

                    for (int b = 0; b < nuevosBytes.length; b++) {
                        int targetAddr = byteAddress + b;
                        if (targetAddr >= absAddress && targetAddr < absEnd) {
                            int offsetInRecord = targetAddr - absAddress;
                            recordData[offsetInRecord] = nuevosBytes[b];
                            lineaSolapa = true;
                            modificado = true;
                        }
                    }

                    if (lineaSolapa) {
                        StringBuilder recBuilder = new StringBuilder();
                        recBuilder.append(String.format(":%02X%04X%02X", byteCount, recordAddress, recordType));
                        int checksumSum = byteCount + (recordAddress >> 8) + (recordAddress & 0xFF) + recordType;
                        for (int k = 0; k < byteCount; k++) {
                            int val = recordData[k] & 0xFF;
                            recBuilder.append(String.format("%02X", val));
                            checksumSum += val;
                        }
                        int checksum = (-checksumSum) & 0xFF;
                        recBuilder.append(String.format("%02X", checksum));
                        sb.append(recBuilder.toString()).append("\n");
                        continue;
                    }
                }
            } catch (Exception ignored) {
            }

            sb.append(linea).append("\n");
        }

        // Si la dirección no estaba presente en los registros del HEX original,
        // generar e insertar un registro antes del EOF :00000001FF
        if (!modificado) {
            StringBuilder nuevoRegistro = new StringBuilder();
            int count = nuevosBytes.length;
            int recAddr = byteAddress & 0xFFFF;
            int recType = 0;
            nuevoRegistro.append(String.format(":%02X%04X%02X", count, recAddr, recType));
            int sum = count + (recAddr >> 8) + (recAddr & 0xFF) + recType;
            for (byte b : nuevosBytes) {
                int val = b & 0xFF;
                nuevoRegistro.append(String.format("%02X", val));
                sum += val;
            }
            int chk = (-sum) & 0xFF;
            nuevoRegistro.append(String.format("%02X", chk));

            String res = sb.toString();
            int eofIdx = res.lastIndexOf(":00000001FF");
            if (eofIdx >= 0) {
                return res.substring(0, eofIdx) + nuevoRegistro.toString() + "\n" + res.substring(eofIdx);
            } else {
                return res + nuevoRegistro.toString() + "\n:00000001FF\n";
            }
        }

        return sb.toString();
    }

    /**
     * Refresca la vista TextView sustituyendo el valor anterior con el nuevo.
     */
    private static void refrescarFilaVisual(TextView filaView, String oldHex, String newHex) {
        if (filaView == null) return;
        CharSequence current = filaView.getText();
        if (current != null) {
            String updated = current.toString().replaceFirst(oldHex, newHex);
            filaView.setText(updated);
        }
    }

    private static int dpToPx(Context context, int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
