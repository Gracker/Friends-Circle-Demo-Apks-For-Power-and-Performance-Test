#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PY_SCRIPT="${SCRIPT_DIR}/android_comprehensive_benchmark.py"

if [[ ! -f "${PY_SCRIPT}" ]]; then
  echo "Error: script not found: ${PY_SCRIPT}" >&2
  exit 1
fi

# Make sure the adb daemon is up before any stage runs — avoids racy first-stage failures.
if command -v adb >/dev/null 2>&1; then
  adb start-server >/dev/null 2>&1 || true
fi

python3 "${PY_SCRIPT}" "$@"
