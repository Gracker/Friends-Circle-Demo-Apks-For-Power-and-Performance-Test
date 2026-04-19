#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PY_SCRIPT="${SCRIPT_DIR}/perfetto_release_scroll_benchmark.py"
RUNS_ROOT_DIR="${SCRIPT_DIR}/results/perfetto-scroll-runs"

if [[ ! -f "${PY_SCRIPT}" ]]; then
  echo "Error: script not found: ${PY_SCRIPT}" >&2
  exit 1
fi

serial_label="default"
forward_args=()

# --trace-dir / --result-dir / --log-dir are consumed locally and overridden
# with a structured per-run bundle directory below. Passing them on the CLI is
# silently ignored to keep the output layout predictable.
skip_next=0
for ((i=1; i<=$#; i++)); do
  if [[ ${skip_next} -eq 1 ]]; then
    skip_next=0
    continue
  fi

  arg="${!i}"
  if [[ "${arg}" == "--trace-dir" || "${arg}" == "--result-dir" || "${arg}" == "--log-dir" ]]; then
    echo "Warning: ${arg} ignored by wrapper (per-run bundle path is auto-generated)" >&2
    next=$((i + 1))
    if [[ ${next} -le $# ]]; then
      skip_next=1
    fi
    continue
  elif [[ "${arg}" == "--serial" ]]; then
    next=$((i + 1))
    if [[ ${next} -le $# ]]; then
      serial_label="${!next}"
      forward_args+=( "${arg}" "${!next}" )
      skip_next=1
    fi
  elif [[ "${arg}" == "--launch-flutter-build-heavy" ]]; then
    forward_args+=( "--prelaunch-flutter-variant" "all" )
  elif [[ "${arg}" == "--launch-flutter-build-heavy-v19" ]]; then
    forward_args+=( "--prelaunch-flutter-variant" "v19" )
  elif [[ "${arg}" == "--launch-flutter-build-heavy-v27" ]]; then
    forward_args+=( "--prelaunch-flutter-variant" "v27" )
  elif [[ "${arg}" == "--launch-flutter-build-heavy-v29" ]]; then
    forward_args+=( "--prelaunch-flutter-variant" "v29" )
  else
    forward_args+=( "${arg}" )
  fi
done

timestamp="$(date +%Y%m%d-%H%M%S)"

resolved_serial="${serial_label}"
if [[ "${serial_label}" == "default" ]]; then
  resolved_serial="$(adb get-serialno 2>/dev/null || true)"
fi
if [[ -z "${resolved_serial}" || "${resolved_serial}" == "unknown" || "${resolved_serial}" == "offline" ]]; then
  resolved_serial="${serial_label}"
fi

safe_serial="$(echo "${resolved_serial}" | sed 's/[^A-Za-z0-9._-]/-/g' | sed 's/-\{2,\}/-/g' | sed 's/^-*//;s/-*$//')"
if [[ -z "${safe_serial}" ]]; then
  safe_serial="default"
fi

adb_cmd=( "adb" )
if [[ -n "${resolved_serial}" && "${resolved_serial}" != "default" ]]; then
  adb_cmd+=( "-s" "${resolved_serial}" )
fi
model_raw="$("${adb_cmd[@]}" shell getprop ro.product.model 2>/dev/null | tr -d '\r' || true)"
if [[ -z "${model_raw}" || "${model_raw}" == adb:* || "${model_raw}" == *"not found"* ]]; then
  model_raw="unknown-device"
fi
safe_model="$(echo "${model_raw}" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//' | sed 's/[^A-Za-z0-9._-]/-/g' | sed 's/-\{2,\}/-/g' | sed 's/^-*//;s/-*$//')"
if [[ -z "${safe_model}" ]]; then
  safe_model="unknown-device"
fi

run_dir_name="${safe_model}-${safe_serial}-${timestamp}"
run_dir="${RUNS_ROOT_DIR}/${run_dir_name}"
trace_dir="${run_dir}/trace"
result_dir="${run_dir}/results"
log_dir="${run_dir}/logs"
log_file="${log_dir}/perfetto-release-scroll-${run_dir_name}.log"

mkdir -p "${trace_dir}" "${result_dir}" "${log_dir}"

cmd=( "${PY_SCRIPT}" "--trace-dir" "${trace_dir}" "--result-dir" "${result_dir}" )

if [[ ${#forward_args[@]} -gt 0 ]]; then
  python3 "${cmd[@]}" "${forward_args[@]}" 2>&1 | tee "${log_file}"
else
  python3 "${cmd[@]}" 2>&1 | tee "${log_file}"
fi
status=${PIPESTATUS[0]}

echo
echo "Log saved: ${log_file}"
echo "Run bundle dir: ${run_dir}"
echo "Trace dir: ${trace_dir}"
echo "Result dir: ${result_dir}"
exit "${status}"
