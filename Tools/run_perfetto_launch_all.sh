#!/usr/bin/env bash
# Wrapper for Tools/auto_record_perfetto_launch.py.
#
# Usage:
#   bash Tools/run_perfetto_launch_all.sh [--serial SERIAL] [--apps ...] [--flavors ...]
#
# Any argument not listed below is forwarded verbatim to the Python script.
# Flags consumed locally: --log-dir, --trace-dir, --serial.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PY_SCRIPT="${SCRIPT_DIR}/auto_record_perfetto_launch.py"
DEFAULT_TRACE_DIR="${SCRIPT_DIR}/trace/launch"
DEFAULT_LOG_DIR="${SCRIPT_DIR}/logs"

if [[ ! -f "${PY_SCRIPT}" ]]; then
  echo "Error: script not found: ${PY_SCRIPT}" >&2
  exit 1
fi

trace_dir_provided=0
log_dir="${DEFAULT_LOG_DIR}"
serial_label="default"
forward_args=()

skip_next=0
for ((i=1; i<=$#; i++)); do
  if [[ ${skip_next} -eq 1 ]]; then
    skip_next=0
    continue
  fi

  arg="${!i}"
  if [[ "${arg}" == "--trace-dir" ]]; then
    trace_dir_provided=1
    forward_args+=( "${arg}" )
    next=$((i + 1))
    if [[ ${next} -le $# ]]; then
      forward_args+=( "${!next}" )
      skip_next=1
    fi
  elif [[ "${arg}" == "--log-dir" ]]; then
    next=$((i + 1))
    if [[ ${next} -le $# ]]; then
      log_dir="${!next}"
      skip_next=1
    fi
  elif [[ "${arg}" == "--serial" ]]; then
    next=$((i + 1))
    if [[ ${next} -le $# ]]; then
      serial_label="${!next}"
      forward_args+=( "${arg}" "${!next}" )
      skip_next=1
    fi
  else
    forward_args+=( "${arg}" )
  fi
done

mkdir -p "${log_dir}"
mkdir -p "${DEFAULT_TRACE_DIR}"

timestamp="$(date +%Y%m%d-%H%M%S)"
safe_serial="$(echo "${serial_label}" | sed 's/[^A-Za-z0-9._-]/-/g')"
log_file="${log_dir}/perfetto-launch-${safe_serial}-${timestamp}.log"

cmd=( "${PY_SCRIPT}" )
if [[ ${trace_dir_provided} -eq 0 ]]; then
  cmd+=( "--trace-dir" "${DEFAULT_TRACE_DIR}" )
fi

if [[ ${#forward_args[@]} -gt 0 ]]; then
  python3 "${cmd[@]}" "${forward_args[@]}" 2>&1 | tee "${log_file}"
else
  python3 "${cmd[@]}" 2>&1 | tee "${log_file}"
fi
status=${PIPESTATUS[0]}

echo
echo "Log saved: ${log_file}"
echo "Default trace dir: ${DEFAULT_TRACE_DIR}"
exit "${status}"
