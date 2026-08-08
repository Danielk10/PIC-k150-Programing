#!/usr/bin/env bash
# ==============================================================================
# Script para iniciar el emulador K150 y ejecutar las pruebas de integración Java
# ==============================================================================
set -euo pipefail

BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
EMULATOR_DIR="/home/danielpdiamon/emulador_picpro"
VTTY="${EMULATOR_DIR}/vtty"

echo "=== 1. Limpiando procesos previos y terminales virtuales ==="
# Matar instancias previas del emulador si existen
pkill -f "emulador_k150.py" || true
pkill -f "emulador_k150_cpp" || true
rm -f "$VTTY"

echo "=== 2. Iniciando el emulador K150 (Python) en segundo plano ==="
PYTHONUNBUFFERED=1 python3 "${EMULATOR_DIR}/emulador_k150.py" &
EMU_PID=$!

# Asegurar que el emulador se cierre cuando termine el script
cleanup() {
    echo "=== 4. Deteniendo el emulador K150 ($EMU_PID) ==="
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
sleep 1

echo "=== 3. Ejecutando pruebas de integración en Java ==="
chmod +x "${BASE_DIR}/gradlew"
"${BASE_DIR}/gradlew" testDebugUnitTest --tests "com.diamon.protocolo.ProtocoloP18AIntegrationTest"

echo "=========================================================="
# Si llega aquí, la ejecución fue exitosa
echo "¡PRUEBAS DE INTEGRACIÓN JAVA CON EL EMULADOR PASADAS EXITOSAMENTE!"
echo "=========================================================="
