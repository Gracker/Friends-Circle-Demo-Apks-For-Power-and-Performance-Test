from __future__ import annotations

import shlex
import subprocess
from typing import Dict, List, Optional, Sequence

from .utils import combined_output, log, run_host_command

class ADB:
    def __init__(self, serial: Optional[str], dry_run: bool = False):
        self.serial = serial
        self.dry_run = dry_run

    def base(self) -> List[str]:
        cmd = ["adb"]
        if self.serial:
            cmd += ["-s", self.serial]
        return cmd

    def run(self, args: Sequence[str], timeout: int = 60) -> subprocess.CompletedProcess[str]:
        cmd = self.base() + list(args)
        if self.dry_run:
            log("DRY", " ".join(shlex.quote(x) for x in cmd))
            return subprocess.CompletedProcess(cmd, 0, "", "")
        try:
            return subprocess.run(cmd, capture_output=True, text=True, timeout=timeout)
        except subprocess.TimeoutExpired as exc:
            return subprocess.CompletedProcess(cmd, 124, exc.stdout or "", exc.stderr or "timeout")

    def shell(self, command: str, timeout: int = 60) -> subprocess.CompletedProcess[str]:
        return self.run(["shell", command], timeout=timeout)

    def shell_out(self, command: str, timeout: int = 60) -> str:
        proc = self.shell(command, timeout=timeout)
        return combined_output(proc)

    def push(self, local: Path, remote: str, timeout: int = 60) -> subprocess.CompletedProcess[str]:
        return self.run(["push", str(local), remote], timeout=timeout)

    def get_serial(self) -> str:
        if self.serial:
            return self.serial
        proc = self.run(["get-serialno"], timeout=10)
        serial = combined_output(proc)
        return serial if serial else "unknown-serial"

    def get_model(self) -> str:
        out = self.shell_out("getprop ro.product.model", timeout=10)
        return out if out else "unknown-model"

    def check_device(self) -> bool:
        if self.dry_run:
            return True
        proc = self.run(["get-state"], timeout=10)
        return proc.returncode == 0 and "device" in combined_output(proc)


def discover_adb_devices() -> List[Dict[str, str]]:
    proc = run_host_command(["adb", "devices", "-l"], timeout=15)
    if proc.returncode != 0:
        raise RuntimeError(combined_output(proc) or "adb devices failed")
    devices: List[Dict[str, str]] = []
    for raw in (proc.stdout or "").splitlines()[1:]:
        line = raw.strip()
        if not line:
            continue
        parts = line.split()
        if len(parts) < 2 or parts[1] != "device":
            continue
        serial = parts[0]
        meta = {"serial": serial}
        for token in parts[2:]:
            if ":" in token:
                key, value = token.split(":", 1)
                meta[key] = value
        devices.append(meta)
    return devices
