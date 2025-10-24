package com.diamon.pruebas;




import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PicFuseProcessor {

    public static class FuseProcessingException extends Exception {
        public FuseProcessingException(String message) {
            super(message);
        }

        public FuseProcessingException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    // Direcciones de memoria estándar para PICs
    private static final int ID_START_ADDRESS = 0x4000;
    private static final int ID_END_ADDRESS = 0x4008;
    private static final int FUSE_START_ADDRESS = 0x400E;
    private static final int FUSE_END_ADDRESS = 0x4010;

    /**
     * Extrae el ID del chip desde los registros de configuración
     *
     * ✅ CORREGIDO: Ahora pasa el baseAddress correcto a mergeRecords()
     */
    public static byte[] extractChipId(
            List<HexRecord> configRecords,
            ChipinfoEntry chipInfo,
            int pickByte,
            byte[] cachedId) {

        if (cachedId != null) {
            return cachedId;
        }

        List<HexRecord> idRecords = HexRecordProcessor.rangeFilterRecords(
                configRecords,
                ID_START_ADDRESS,
                ID_END_ADDRESS
        );

        byte[] defaultIdData = new byte[8];

        // ✅ CORRECCIÓN: Agregar ID_START_ADDRESS como baseAddress
        byte[] idData = HexRecordProcessor.mergeRecords(
                idRecords,
                defaultIdData,
                ID_START_ADDRESS  // ← CRÍTICO: baseAddress = 0x4000
        );

        Integer coreBits = chipInfo.getCoreBits();
        if (coreBits != null && coreBits == 16) {
            return idData;
        }

        byte[] result = new byte[4];
        for (int i = 0; i < 4; i++) {
            int sourceIndex = pickByte + (i * 2);
            if (sourceIndex < idData.length) {
                result[i] = idData[sourceIndex];
            }
        }

        return result;
    }

    /**
     * Extrae los valores actuales de los fusibles desde registros
     *
     * ✅ CORREGIDO: Ahora pasa el baseAddress correcto a mergeRecords()
     */
    @SuppressWarnings("unchecked")
    public static List<Integer> extractFuseValues(
            List<HexRecord> configRecords,
            ChipinfoEntry chipInfo) {

        List<Integer> fuseBlank = (List<Integer>) chipInfo.getVar("FUSEblank");

        ByteBuffer buffer = ByteBuffer.allocate(fuseBlank.size() * 2);
        buffer.order(ByteOrder.BIG_ENDIAN);
        for (Integer value : fuseBlank) {
            buffer.putShort(value.shortValue());
        }
        byte[] defaultFuseData = buffer.array();

        List<HexRecord> fuseRecords = HexRecordProcessor.rangeFilterRecords(
                configRecords,
                FUSE_START_ADDRESS,
                FUSE_END_ADDRESS
        );

        // ✅ CORRECCIÓN: Agregar FUSE_START_ADDRESS como baseAddress
        byte[] fuseData = HexRecordProcessor.mergeRecords(
                fuseRecords,
                defaultFuseData,
                FUSE_START_ADDRESS  // ← CRÍTICO: baseAddress = 0x400E
        );

        List<Integer> fuseValues = new ArrayList<>();
        ByteBuffer readBuffer = ByteBuffer.wrap(fuseData);
        readBuffer.order(ByteOrder.BIG_ENDIAN);

        while (readBuffer.remaining() >= 2) {
            int value = readBuffer.getShort() & 0xFFFF;
            fuseValues.add(value);
        }

        return fuseValues;
    }

    /**
     * Procesa y actualiza fusibles del chip
     *
     * Este es el método principal que:
     * 1. Extrae valores actuales de fusibles
     * 2. Decodifica a configuración legible
     * 3. Aplica nuevos valores de fusibles solicitados
     * 4. Codifica de vuelta a valores hexadecimales
     * 5. Maneja errores de fusibles inválidos
     *
     * @param configRecords Registros de configuración del chip
     * @param chipInfo Información del chip
     * @param newFuseSettings Nuevas configuraciones a aplicar (puede ser null)
     * @return Resultado del procesamiento de fusibles
     * @throws FuseProcessingException Si hay error en fusibles
     */
    public static FuseProcessingResult processFuses(
            List<HexRecord> configRecords,
            ChipinfoEntry chipInfo,
            Map<String, String> newFuseSettings)
            throws FuseProcessingException {

        try {
            // Paso 1: Extraer valores actuales de fusibles
            List<Integer> currentFuseValues = extractFuseValues(
                    configRecords,
                    chipInfo
            );

            // Paso 2: Decodificar a configuración legible
            Map<String, String> currentFuseSettings =
                    chipInfo.decodeFuseData(currentFuseValues);

            // Paso 3: Si hay nuevos valores, aplicarlos
            List<Integer> finalFuseValues;
            Map<String, String> finalFuseSettings;

            if (newFuseSettings != null && !newFuseSettings.isEmpty()) {
                // Combinar configuraciones actuales con nuevas
                finalFuseSettings = new HashMap<>(currentFuseSettings);
                finalFuseSettings.putAll(newFuseSettings);

                // Paso 4: Codificar de vuelta a valores hexadecimales
                try {
                    finalFuseValues = chipInfo.encodeFuseData(finalFuseSettings);
                } catch (ChipinfoEntry.FuseError e) {
                    // Error de fusible inválido
                    String errorMsg = "Configuración de fusible inválida.\n" +
                            "Fusibles válidos para este chip:\n" +
                            chipInfo.getFuseDoc();
                    throw new FuseProcessingException(errorMsg, e);
                }
            } else {
                // No hay cambios, usar valores actuales
                finalFuseValues = currentFuseValues;
                finalFuseSettings = currentFuseSettings;
            }

            // Crear y retornar resultado
            return new FuseProcessingResult(
                    currentFuseValues,
                    currentFuseSettings,
                    finalFuseValues,
                    finalFuseSettings,
                    true,
                    null
            );

        } catch (ChipinfoEntry.FuseError e) {
            // Error al decodificar fusibles actuales
            throw new FuseProcessingException(
                    "Error al decodificar fusibles actuales: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * Clase resultado del procesamiento de fusibles
     *
     * Contiene toda la información sobre fusibles antes y después
     * del procesamiento.
     */
    public static class FuseProcessingResult {
        private final List<Integer> currentValues;
        private final Map<String, String> currentSettings;
        private final List<Integer> finalValues;
        private final Map<String, String> finalSettings;
        private final boolean success;
        private final String errorMessage;

        public FuseProcessingResult(
                List<Integer> currentValues,
                Map<String, String> currentSettings,
                List<Integer> finalValues,
                Map<String, String> finalSettings,
                boolean success,
                String errorMessage) {
            this.currentValues = currentValues;
            this.currentSettings = currentSettings;
            this.finalValues = finalValues;
            this.finalSettings = finalSettings;
            this.success = success;
            this.errorMessage = errorMessage;
        }

        /**
         * Obtiene los valores actuales de fusibles (antes de cambios)
         */
        public List<Integer> getCurrentValues() {
            return currentValues;
        }

        /**
         * Obtiene la configuración actual de fusibles (antes de cambios)
         */
        public Map<String, String> getCurrentSettings() {
            return currentSettings;
        }

        /**
         * Obtiene los valores finales de fusibles (después de cambios)
         */
        public List<Integer> getFinalValues() {
            return finalValues;
        }

        /**
         * Obtiene la configuración final de fusibles (después de cambios)
         */
        public Map<String, String> getFinalSettings() {
            return finalSettings;
        }

        /**
         * Verifica si el procesamiento fue exitoso
         */
        public boolean isSuccess() {
            return success;
        }

        /**
         * Obtiene el mensaje de error (si hubo error)
         */
        public String getErrorMessage() {
            return errorMessage;
        }

        /**
         * Verifica si hubo cambios en los fusibles
         */
        public boolean hasChanges() {
            return !currentValues.equals(finalValues);
        }
    }
}
