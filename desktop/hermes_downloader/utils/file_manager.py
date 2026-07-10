import os
import shutil
import unicodedata
import re


def sanitize_filename(name: str) -> str:
    """Удаляет недопустимые символы из имени файла."""
    name = unicodedata.normalize('NFKD', name).encode('ascii', 'ignore').decode('ascii')
    name = re.sub(r'[<>:"/\\|?*]', '', name)
    name = name.strip('. ')
    return name or "download"


def get_free_space_gb(path: str) -> float:
    """Возвращает свободное место в гигабайтах."""
    try:
        stat = shutil.disk_usage(os.path.expanduser(path))
        return stat.free / (1024 ** 3)
    except Exception:
        return -1.0


def ensure_save_path(path: str) -> str:
    """Создаёт папку для сохранения, если её нет."""
    expanded = os.path.expanduser(path)
    os.makedirs(expanded, exist_ok=True)
    return expanded
