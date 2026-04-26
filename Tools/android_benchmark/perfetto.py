from __future__ import annotations

import math
import shlex
import signal
import subprocess
from pathlib import Path
from typing import Optional, Tuple

from .paths import SCRIPT_DIR
from .utils import log


class TraceRecorder:
    def __init__(
        self,
        serial: Optional[str],
        output_path: Path,
        perfetto_bin: Optional[Path] = None,
        perfetto_config: Optional[Path] = None,
        dry_run: bool = False,
        duration_sec: Optional[float] = None,
    ):
        self.serial = serial
        self.output_path = output_path
        self.perfetto_bin = perfetto_bin or (SCRIPT_DIR / "record_android_trace")
        self.perfetto_config = perfetto_config or (SCRIPT_DIR / "perfetto.config")
        self.dry_run = dry_run
        self.duration_sec = duration_sec
        self.proc: Optional[subprocess.Popen[str]] = None

    def start(self) -> Tuple[bool, str]:
        self.output_path.parent.mkdir(parents=True, exist_ok=True)
        cmd = [
            str(self.perfetto_bin),
            "-n",
            "--no-open-browser",
            "-c",
            str(self.perfetto_config),
            "-o",
            str(self.output_path),
        ]
        if self.duration_sec and self.duration_sec > 0:
            cmd += ["-t", f"{max(1, int(math.ceil(self.duration_sec)))}s"]
        if self.serial:
            cmd += ["-s", self.serial]
        if self.dry_run:
            log("DRY", " ".join(shlex.quote(x) for x in cmd))
            return True, ""
        try:
            self.proc = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
            return True, ""
        except OSError as exc:
            return False, str(exc)

    def stop(self, timeout_sec: int = 45) -> Tuple[bool, str]:
        if self.dry_run:
            return True, ""
        if self.proc is None:
            return False, "trace process not started"
        try:
            if self.proc.poll() is None:
                self.proc.send_signal(signal.SIGINT)
            stdout, stderr = self.proc.communicate(timeout=timeout_sec)
            out = ((stdout or "") + (stderr or "")).strip()
            return self.proc.returncode == 0, out
        except subprocess.TimeoutExpired:
            self.proc.kill()
            try:
                stdout, stderr = self.proc.communicate(timeout=5)
            except subprocess.TimeoutExpired:
                stdout, stderr = "", "perfetto wedged after kill"
            out = ((stdout or "") + (stderr or "")).strip()
            return False, f"trace stop timeout; output={out}"
