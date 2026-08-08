#!/usr/bin/env bash
# ==============================================================================
# Script para compilar la lógica real de la app y ejecutar pruebas locales
# ==============================================================================
set -euo pipefail

BASE_DIR="/home/danielpdiamon/PIC-k150-Programing"
EMULATOR_DIR="/home/danielpdiamon/emulador_picpro"
VTTY="${EMULATOR_DIR}/vtty"
MODE="${1:-cpp}" # 'python' o 'cpp'
PRUEBAS_DIR="${BASE_DIR}/pruebas_locales"

echo "=== 1. Limpiando procesos previos ==="
pkill -f "emulador_k150.py" || true
pkill -f "emulador_k150_cpp" || true
rm -f "$VTTY"

if [ "$MODE" = "cpp" ]; then
    echo "=== 2. Iniciando el emulador K150 (C++) en segundo plano ==="
    if [ ! -f "${EMULATOR_DIR}/emulador_k150_cpp" ]; then
        echo "Compilando emulador C++..."
        g++ -O2 "${EMULATOR_DIR}/emulador_k150.cpp" -o "${EMULATOR_DIR}/emulador_k150_cpp"
    fi
    "${EMULATOR_DIR}/emulador_k150_cpp" > /tmp/emu_real_flow.log 2>&1 &
    EMU_PID=$!
else
    echo "=== 2. Iniciando el emulador K150 (Python) en segundo plano ==="
    PYTHONUNBUFFERED=1 python3 "${EMULATOR_DIR}/emulador_k150.py" > /tmp/emu_real_flow.log 2>&1 &
    EMU_PID=$!
fi

# Asegurar limpieza al salir
cleanup() {
    echo ""
    echo "=== 5. Deteniendo el emulador K150 ($EMU_PID) ==="
    kill $EMU_PID 2>/dev/null || true
    rm -f "$VTTY"
}
trap cleanup EXIT

echo "Esperando a que se cree el puerto virtual vtty..."
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
sleep 0.5

echo "=== 3. Compilando el código real de la app y el runner ==="
rm -rf "${PRUEBAS_DIR}/build"
mkdir -p "${PRUEBAS_DIR}/build"

# Compilar mapeando el código fuente de la app y las clases stub locales
javac -d "${PRUEBAS_DIR}/build" \
      -sourcepath "${BASE_DIR}/app/src/main/java:${PRUEBAS_DIR}" \
      "${PRUEBAS_DIR}/TestRealAppFlow.java"

echo "=== 4. Ejecutando la simulación del flujo real de la app ==="
java -cp "${PRUEBAS_DIR}/build" TestRealAppFlow

echo "=========================================================="
echo "¡PRUEBA DEL FLUJO REAL DE LA APP FINALIZADA EN MODO: $MODE!"
echo "=========================================================="
