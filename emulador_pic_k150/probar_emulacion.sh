#!/usr/bin/env bash
# ==============================================================================
# Script de Prueba del Emulador de K150 (picpro) con PIC16F628A
# ==============================================================================
set -euo pipefail

BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
HEX_FILE="/home/danielpdiamon/pwmc_main107_628A.HEX"
VTTY="${BASE_DIR}/vtty"
RUNNER="${BASE_DIR}/picpro_patched.py"

MODE="${1:-python}" # 'python' o 'cpp'

echo "=== 1. Validando existencia del archivo HEX ==="
if [ ! -f "$HEX_FILE" ]; then
    echo "ERROR: El archivo $HEX_FILE no existe en el home."
    exit 1
fi
echo "Archivo HEX encontrado: $HEX_FILE"

echo "=== 2. Iniciando el Emulador de K150 ($MODE) en segundo plano ==="
# Eliminar cualquier enlace simbólico previo para evitar condiciones de carrera
rm -f "$VTTY"

if [ "$MODE" = "cpp" ]; then
    # Iniciar emulador C++
    "${BASE_DIR}/emulador_k150_cpp" &
    EMU_PID=$!
else
    # Iniciar emulador Python
    PYTHONUNBUFFERED=1 python3 "${BASE_DIR}/emulador_k150.py" &
    EMU_PID=$!
fi

# Asegurar que el emulador se cierre cuando termine el script
cleanup() {
    echo "=== Terminando proceso del emulador ($EMU_PID) ==="
    kill $EMU_PID 2>/dev/null || true
    rm -f "$VTTY"
}
trap cleanup EXIT

echo "Esperando que se cree el puerto virtual vtty..."
for i in {1..20}; do
    if [ -L "$VTTY" ] && [ -e "$VTTY" ]; then
        break
    fi
    sleep 0.5
done

if [ ! -e "$VTTY" ]; then
    echo "ERROR: El puerto virtual $VTTY no se creó a tiempo."
    exit 1
fi
echo "Puerto virtual listo en: $(readlink -f "$VTTY")"
sleep 1

echo "=== 3. Probando lectura de la configuración en blanco ==="
python3 "$RUNNER" read_chip_config -p "$VTTY" -t 16F628A

echo "=== 4. Probando borrado del chip ==="
python3 "$RUNNER" erase -p "$VTTY" -t 16F628A

echo "=== 5. Probando programación con el archivo HEX ==="
python3 "$RUNNER" program -p "$VTTY" -i "$HEX_FILE" -t 16F628A

echo "=== 6. Probando verificación de la programación ==="
python3 "$RUNNER" verify -p "$VTTY" -i "$HEX_FILE" -t 16F628A

echo "=== 7. Probando volcado (dump) de la memoria ROM a archivo temporal ==="
python3 "$RUNNER" dump rom -p "$VTTY" -o "/tmp/dump_rom_test.hex" -t 16F628A
echo "Contenido del archivo volcado (primeras 5 líneas):"
head -n 5 "/tmp/dump_rom_test.hex"

echo "=========================================================="
echo "¡PRUEBAS DE EMULACIÓN DE PICPRO ($MODE) COMPLETADAS EXITOSAMENTE!"
echo "=========================================================="
