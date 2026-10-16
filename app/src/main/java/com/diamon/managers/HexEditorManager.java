package com.diamon.managers;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.diamon.chip.ChipPic;
import com.diamon.datos.DatosPicProcesados;
import com.diamon.pic.R;

import java.util.Locale;

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
        String tipoMem = isRom ? context.getString(R.string.hex_editor_type_rom_word) : context.getString(R.string.hex_editor_type_eeprom_byte);
        builder.setTitle(context.getString(R.string.hex_editor_title, tipoMem));

        final float density = context.getResources().getDisplayMetrics().density;
        final int dp4 = Math.round(4 * density);
        final int dp8 = Math.round(8 * density);
        final int dp12 = Math.round(12 * density);
        final int dp16 = Math.round(16 * density);

        ScrollView scrollView = new ScrollView(context);
        scrollView.setBackgroundColor(0xFF181824);

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp16, dp16, dp16, dp16);
        scrollView.addView(layout);

        String addrStr = String.format(Locale.US, "0x%04X", address);
        String currHex = isRom ? String.format(Locale.US, "%04X", valorActual & maxVal) : String.format(Locale.US, "%02X", valorActual & 0xFF);
        String maxHex = String.format(Locale.US, isRom ? "%04X" : "%02X", maxVal);

        // --- TARJETA 1: RESUMEN DE MEMORIA (Métricas limpias en 3 columnas) ---
        LinearLayout cardOverview = new LinearLayout(context);
        cardOverview.setOrientation(LinearLayout.VERTICAL);
        cardOverview.setPadding(dp12, dp12, dp12, dp12);
        GradientDrawable bgOverview = new GradientDrawable();
        bgOverview.setColor(0xFF222234);
        bgOverview.setCornerRadius(dp8);
        bgOverview.setStroke(Math.round(1 * density), 0xFF35354D);
        cardOverview.setBackground(bgOverview);

        LinearLayout rowStats = new LinearLayout(context);
        rowStats.setOrientation(LinearLayout.HORIZONTAL);
        rowStats.setWeightSum(3.0f);

        rowStats.addView(crearColumnaInfo(context, context.getString(R.string.hex_editor_label_address), addrStr, 0xFFFFFFFF, density));
        rowStats.addView(crearColumnaInfo(context, context.getString(R.string.hex_editor_label_current), "0x" + currHex, 0xFF00E676, density));
        rowStats.addView(crearColumnaInfo(context, context.getString(R.string.hex_editor_label_max), "0x" + maxHex, 0xFFFFB300, density));

        cardOverview.addView(rowStats);

        LinearLayout.LayoutParams cardOverviewParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardOverviewParams.bottomMargin = dp12;
        layout.addView(cardOverview, cardOverviewParams);

        // --- TARJETA 2: FORMATO TÉCNICO Y ORGANIZACIÓN ---
        LinearLayout infoCard = new LinearLayout(context);
        infoCard.setOrientation(LinearLayout.VERTICAL);
        infoCard.setPadding(dp12, dp12, dp12, dp12);
        GradientDrawable bgInfo = new GradientDrawable();
        bgInfo.setColor(0xFF1E1E2E);
        bgInfo.setCornerRadius(dp8);
        bgInfo.setStroke(Math.round(1 * density), 0xFF2E2E42);
        infoCard.setBackground(bgInfo);

        TextView tvCardTitle = new TextView(context);
        tvCardTitle.setText(isRom ? context.getString(R.string.hex_editor_card_title_rom) : context.getString(R.string.hex_editor_card_title_eeprom));
        tvCardTitle.setTextSize(12);
        tvCardTitle.setTextColor(0xFF00B0FF);
        tvCardTitle.setTypeface(null, Typeface.BOLD);
        infoCard.addView(tvCardTitle);

        if (isRom) {
            int lsb = valorActual & 0xFF;
            int msb = (valorActual >> 8) & 0xFF;

            LinearLayout rowBytes = new LinearLayout(context);
            rowBytes.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rbParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            rbParams.topMargin = dp8;
            rbParams.bottomMargin = dp8;
            rowBytes.setLayoutParams(rbParams);

            TextView tvLsb = crearBadgeByte(context, context.getString(R.string.hex_editor_byte_low, String.format(Locale.US, "%02X", lsb)), density);
            TextView tvMsb = crearBadgeByte(context, context.getString(R.string.hex_editor_byte_high, String.format(Locale.US, "%02X", msb)), density);

            LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
            p1.rightMargin = dp4;
            LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
            p2.leftMargin = dp4;

            rowBytes.addView(tvLsb, p1);
            rowBytes.addView(tvMsb, p2);
            infoCard.addView(rowBytes);
        }

        TextView tvCardDesc = new TextView(context);
        tvCardDesc.setText(isRom ? context.getString(R.string.hex_editor_desc_rom) : context.getString(R.string.hex_editor_desc_eeprom));
        tvCardDesc.setTextSize(11);
        tvCardDesc.setTextColor(0xFFB0BEC5);
        tvCardDesc.setLineSpacing(0, 1.15f);
        if (!isRom) {
            tvCardDesc.setPadding(0, dp8, 0, 0);
        }
        infoCard.addView(tvCardDesc);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.bottomMargin = dp12;
        layout.addView(infoCard, cardParams);

        // --- TARJETA 3: EDICIÓN DE VALOR (Input) ---
        TextView inputLabel = new TextView(context);
        inputLabel.setText(isRom ? context.getString(R.string.hex_editor_input_label_rom) : context.getString(R.string.hex_editor_input_label_eeprom));
        inputLabel.setTextSize(12);
        inputLabel.setTypeface(null, Typeface.BOLD);
        inputLabel.setTextColor(0xFFFFFFFF);
        inputLabel.setPadding(0, 0, 0, dp8);
        layout.addView(inputLabel);

        EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(currHex);
        input.setSelectAllOnFocus(true);
        input.setGravity(Gravity.CENTER);
        input.setTextSize(20);
        input.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        input.setTextColor(0xFF00E676);
        GradientDrawable inputBg = new GradientDrawable();
        inputBg.setColor(0xFF141420);
        inputBg.setCornerRadius(dp8);
        inputBg.setStroke(Math.round(1.5f * density), 0xFF00E676);
        input.setBackground(inputBg);
        input.setPadding(dp12, dp12, dp12, dp12);

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

        builder.setView(scrollView);

        builder.setPositiveButton(context.getString(R.string.guardar), (dialog, which) -> {
            String text = input.getText().toString().trim();
            if (text.isEmpty()) {
                Toast.makeText(context, context.getString(R.string.hex_editor_err_empty), Toast.LENGTH_SHORT).show();
                return;
            }

            int nuevoValor;
            try {
                nuevoValor = Integer.parseInt(text, 16);
            } catch (NumberFormatException e) {
                Toast.makeText(context, context.getString(R.string.hex_editor_err_invalid_hex), Toast.LENGTH_SHORT).show();
                return;
            }

            if (nuevoValor > maxVal) {
                Toast.makeText(context, context.getString(R.string.hex_editor_err_exceeds_max, String.format(isRom ? "%04X" : "%02X", maxVal)), Toast.LENGTH_SHORT).show();
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

        builder.setNegativeButton(context.getString(R.string.cancelar), null);
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
     * Obtiene el valor actual de una palabra ROM o byte EEPROM priorizando la cadena hexadecimal
     * leída del microcontrolador (lastReadRomData / lastReadEepromData), con respaldo en
     * DatosPicProcesados o el valor blank de la arquitectura.
     */
    public static int obtenerValorActualEnMemoria(
            String hexLeido,
            boolean isRom,
            int address,
            DatosPicProcesados datosPicProcesados,
            ChipPic chip) {

        int coreBits = 14;
        if (chip != null) {
            try {
                coreBits = chip.getTipoDeNucleoBit();
            } catch (Exception ignored) {
            }
        }
        int blankWord = (coreBits == 16) ? 0xFFFF : ((~(0xFFFF << coreBits)) & 0xFFFF);

        if (isRom) {
            int charOffset = address * 4;
            if (hexLeido != null && charOffset >= 0 && charOffset + 4 <= hexLeido.length()) {
                try {
                    return Integer.parseInt(hexLeido.substring(charOffset, charOffset + 4), 16) & blankWord;
                } catch (NumberFormatException ignored) {
                }
            }
            if (datosPicProcesados != null && datosPicProcesados.obtenerBytesHexROMProcesado() != null) {
                byte[] rom = datosPicProcesados.obtenerBytesHexROMProcesado();
                int byteOffset = address * 2;
                if (byteOffset >= 0 && byteOffset + 1 < rom.length) {
                    return (((rom[byteOffset] & 0xFF) << 8) | (rom[byteOffset + 1] & 0xFF)) & blankWord;
                }
            }
            return blankWord;
        } else {
            int charOffset = address * 2;
            if (hexLeido != null && charOffset >= 0 && charOffset + 2 <= hexLeido.length()) {
                try {
                    return Integer.parseInt(hexLeido.substring(charOffset, charOffset + 2), 16) & 0xFF;
                } catch (NumberFormatException ignored) {
                }
            }
            if (datosPicProcesados != null && datosPicProcesados.obtenerBytesHexEEPROMProcesado() != null) {
                byte[] eep = datosPicProcesados.obtenerBytesHexEEPROMProcesado();
                if (address >= 0 && address < eep.length) {
                    return eep[address] & 0xFF;
                }
            }
            return 0xFF;
        }
    }

    /**
     * Actualiza la cadena hexadecimal cruda (lastReadRomData o lastReadEepromData) en la
     * dirección editada para que tanto la exportación HEX/BIN como la reprogramación conserven el cambio.
     */
    public static String actualizarCeldaEnHexLeido(
            String hexActual,
            boolean isRom,
            int address,
            int nuevoValor,
            ChipPic chip) {

        if (address < 0) {
            return hexActual != null ? hexActual : "";
        }

        int coreBits = 14;
        int totalUnits = 0;
        if (chip != null) {
            try {
                coreBits = chip.getTipoDeNucleoBit();
            } catch (Exception ignored) {
            }
            try {
                totalUnits = isRom ? chip.getTamanoROM() : (chip.isTamanoValidoDeEEPROM() ? chip.getTamanoEEPROM() : 0);
            } catch (Exception ignored) {
            }
        }

        int maxWord = (coreBits == 16) ? 0xFFFF : ((~(0xFFFF << coreBits)) & 0xFFFF);
        int groupSize = isRom ? 4 : 2;
        String blankUnit = isRom ? String.format(Locale.US, "%04X", maxWord) : "FF";
        String newUnitHex = isRom
                ? String.format(Locale.US, "%04X", nuevoValor & maxWord)
                : String.format(Locale.US, "%02X", nuevoValor & 0xFF);

        String base = (hexActual != null && !hexActual.startsWith("Error")) ? hexActual : "";
        int currentUnits = base.length() / groupSize;
        int targetUnits = Math.max(address + 1, currentUnits);

        StringBuilder sb = new StringBuilder(Math.max(base.length(), targetUnits * groupSize));
        sb.append(base);
        while (currentUnits < targetUnits) {
            sb.append(blankUnit);
            currentUnits++;
        }

        int charOffset = address * groupSize;
        if (charOffset + groupSize <= sb.length()) {
            sb.replace(charOffset, charOffset + groupSize, newUnitHex);
        }
        return sb.toString();
    }

    /**
     * Construye un archivo Intel HEX completo y válido a partir de los datos leídos/editados
     * en memoria (ROM, EEPROM y Configuración), permitiendo reprogramar o verificar el chip directamente.
     */
    public static String construirFirmwareIntelHexDesdeLectura(
            String romHex,
            String eepromHex,
            String configHex,
            ChipPic chip) {

        int coreBits = 14;
        int fuseCount = 1;
        if (chip != null) {
            try {
                coreBits = chip.getTipoDeNucleoBit();
            } catch (Exception ignored) {
            }
            try {
                if (chip.getFuseBlank() != null) {
                    fuseCount = Math.max(1, chip.getFuseBlank().length);
                }
            } catch (Exception ignored) {
            }
        }

        StringBuilder fullHex = new StringBuilder();

        if (romHex != null && !romHex.isEmpty() && !romHex.startsWith("Error")) {
            byte[] romBytes = hexStringToByteArray(romHex);
            if (romBytes.length > 0) {
                byte[] romFormatted = HexExportManager.formatForHexExport(romBytes, coreBits, false);
                fullHex.append(HexExportManager.convertSegmentToIntelHex(romFormatted, 0));
            }
        }

        if (configHex != null && !configHex.isEmpty() && !configHex.startsWith("Error")) {
            byte[] rawConfig = hexStringToByteArray(configHex);
            int idLen = (coreBits == 16) ? 8 : 4;
            int idStart = 2;
            int fuseByteLen = fuseCount * 2;
            int fuseStart = 10;
            if (rawConfig.length >= (idStart + idLen) && rawConfig.length > fuseStart) {
                byte[] rawId = java.util.Arrays.copyOfRange(rawConfig, idStart, Math.min(idStart + idLen, rawConfig.length));
                byte[] fuseBytes = java.util.Arrays.copyOfRange(rawConfig, fuseStart, Math.min(fuseStart + fuseByteLen, rawConfig.length));

                int idAddr = (coreBits == 16) ? 0x200000 : 0x4000;
                int fuseAddr = (coreBits == 16) ? 0x300000 : 0x400E;

                byte[] idBytes;
                if (coreBits == 16) {
                    idBytes = rawId.clone();
                } else {
                    // Para núcleos de 12/14 bits, cada User ID ocupa una palabra de 16 bits en INHX32
                    // [LSB = rawId[i], MSB = 0x00].
                    // Al swabbear por endianness, el dato pasa al byte alto (impar: 1, 3, 5, 7)
                    // que es de donde DatosPicProcesados extrae el User ID final.
                    idBytes = new byte[8];
                    for (int i = 0; i < Math.min(rawId.length, 4); i++) {
                        idBytes[i * 2] = rawId[i]; // LSB
                        idBytes[i * 2 + 1] = 0x00; // MSB
                    }
                }

                fullHex.append(HexExportManager.convertSegmentToIntelHex(idBytes, idAddr));
                fullHex.append(HexExportManager.convertSegmentToIntelHex(fuseBytes, fuseAddr));
            }
        }

        if (eepromHex != null && !eepromHex.isEmpty() && !eepromHex.startsWith("Error")) {
            byte[] eepromBytes = hexStringToByteArray(eepromHex);
            if (eepromBytes.length > 0) {
                int eepromAddr = (coreBits == 16) ? 0xF000 : 0x4200;
                byte[] eepromFormatted = HexExportManager.formatForHexExport(eepromBytes, coreBits, true);
                fullHex.append(HexExportManager.convertSegmentToIntelHex(eepromFormatted, eepromAddr));
            }
        }

        if (fullHex.length() == 0) {
            return "";
        }

        fullHex.append(":00000001FF\r\n");
        return fullHex.toString();
    }

    private static byte[] hexStringToByteArray(String s) {
        if (s == null || s.isEmpty()) {
            return new byte[0];
        }
        try {
            String clean = s.replaceAll("\\s+", "");
            int len = clean.length() & ~1;
            byte[] data = new byte[len / 2];
            for (int i = 0; i < len; i += 2) {
                int hi = Character.digit(clean.charAt(i), 16);
                int lo = Character.digit(clean.charAt(i + 1), 16);
                if (hi == -1 || lo == -1) {
                    return new byte[0];
                }
                data[i / 2] = (byte) ((hi << 4) + lo);
            }
            return data;
        } catch (Exception e) {
            return new byte[0];
        }
    }

    /**
     * Refresca la vista TextView sustituyendo el valor anterior con el nuevo sin alterar el prefijo de dirección.
     */
    private static void refrescarFilaVisual(TextView filaView, String oldHex, String newHex) {
        if (filaView == null) return;
        CharSequence current = filaView.getText();
        if (current != null) {
            String text = current.toString();
            int colonIdx = text.indexOf(": ");
            if (colonIdx >= 0 && colonIdx + 2 < text.length()) {
                String prefix = text.substring(0, colonIdx + 2);
                String rest = text.substring(colonIdx + 2).replaceFirst(oldHex, newHex);
                filaView.setText(prefix + rest);
            } else {
                filaView.setText(text.replaceFirst(oldHex, newHex));
            }
        }
    }

    private static LinearLayout crearColumnaInfo(Context context, String label, String value, int valueColor, float density) {
        LinearLayout col = new LinearLayout(context);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        col.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView tvLabel = new TextView(context);
        tvLabel.setText(label.toUpperCase(Locale.ROOT));
        tvLabel.setTextSize(10);
        tvLabel.setTextColor(0xFF9E9EB4);
        tvLabel.setGravity(Gravity.CENTER_HORIZONTAL);
        col.addView(tvLabel);

        TextView tvValue = new TextView(context);
        tvValue.setText(value);
        tvValue.setTextSize(13);
        tvValue.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        tvValue.setTextColor(valueColor);
        tvValue.setGravity(Gravity.CENTER_HORIZONTAL);
        tvValue.setPadding(0, Math.round(2 * density), 0, 0);
        col.addView(tvValue);

        return col;
    }

    private static TextView crearBadgeByte(Context context, String text, float density) {
        TextView tv = new TextView(context);
        tv.setText(text);
        tv.setTextSize(11);
        tv.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        tv.setTextColor(0xFFE0E0E0);
        tv.setGravity(Gravity.CENTER);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF28283C);
        bg.setCornerRadius(Math.round(6 * density));
        tv.setBackground(bg);
        int padH = Math.round(8 * density);
        int padV = Math.round(6 * density);
        tv.setPadding(padH, padV, padH, padV);
        return tv;
    }

    private static int dpToPx(Context context, int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
