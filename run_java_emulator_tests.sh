#!/usr/bin/env bash
# ==============================================================================
# Script de compatibilidad: redirige a ejecutar_pruebas_con_emulador.sh
# ==============================================================================
set -euo pipefail

BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
exec "${BASE_DIR}/ejecutar_pruebas_con_emulador.sh" "$@"
