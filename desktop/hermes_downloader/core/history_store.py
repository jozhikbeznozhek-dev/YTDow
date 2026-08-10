"""Thread-safe, atomic storage for the desktop download history."""

from __future__ import annotations

import json
import os
import tempfile
import threading
from pathlib import Path
from typing import Any


HISTORY_FILE = Path.home() / ".ytdow_history.json"
MAX_ENTRIES = 50
_LOCK = threading.RLock()


def load_history(path: Path = HISTORY_FILE) -> list[dict[str, Any]]:
    with _LOCK:
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
            return data if isinstance(data, list) else []
        except (FileNotFoundError, json.JSONDecodeError, OSError):
            return []


def append_history(entry: dict[str, Any], path: Path = HISTORY_FILE) -> None:
    with _LOCK:
        history = load_history(path)
        history.append(entry)
        _write_atomic(history[-MAX_ENTRIES:], path)


def remove_history(file_path: str, path: Path = HISTORY_FILE) -> None:
    with _LOCK:
        history = [entry for entry in load_history(path) if entry.get("filePath") != file_path]
        _write_atomic(history, path)


def _write_atomic(history: list[dict[str, Any]], path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    descriptor, temporary_name = tempfile.mkstemp(prefix=f".{path.name}.", dir=path.parent)
    try:
        with os.fdopen(descriptor, "w", encoding="utf-8") as handle:
            json.dump(history, handle, indent=2, ensure_ascii=False)
            handle.flush()
            os.fsync(handle.fileno())
        os.replace(temporary_name, path)
    except BaseException:
        try:
            os.unlink(temporary_name)
        except FileNotFoundError:
            pass
        raise
