import subprocess
import shutil
import sys
import os


def is_ffmpeg_available() -> bool:
    """Проверяет, установлен ли ffmpeg в системе."""
    if shutil.which("ffmpeg"):
        return True

    # macOS: проверяем brew-пути
    brew_paths = ["/opt/homebrew/bin/ffmpeg", "/usr/local/bin/ffmpeg"]
    for path in brew_paths:
        if shutil.which(path) or os_path_exists(path):
            return True

    return False


def get_ffmpeg_path() -> str | None:
    """Возвращает путь к ffmpeg или None."""
    path = shutil.which("ffmpeg")
    if path:
        return path
    for p in ["/opt/homebrew/bin/ffmpeg", "/usr/local/bin/ffmpeg"]:
        if os.path.exists(p):
            return p
    return None


def os_path_exists(path: str) -> bool:
    import os
    return os.path.exists(path)


def get_ffmpeg_install_hint() -> str:
    """Подсказка по установке ffmpeg."""
    if sys.platform == "darwin":
        return "Установите ffmpeg: brew install ffmpeg"
    elif sys.platform == "win32":
        return "Скачайте ffmpeg с https://ffmpeg.org/download.html и добавьте в PATH"
    else:
        return "Установите ffmpeg: sudo apt install ffmpeg"
