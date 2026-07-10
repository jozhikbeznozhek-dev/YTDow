"""Парсинг информации о видео через yt-dlp."""

import yt_dlp
from dataclasses import dataclass
from typing import Optional, List


@dataclass
class VideoInfo:
    title: str
    duration: str
    thumbnail: Optional[str] = None
    formats: List[str] = None   # доступные разрешения
    filesize: Optional[str] = None     # примерный размер
    uploader: Optional[str] = None
    url: str = ""

    def __post_init__(self):
        if self.formats is None:
            self.formats = []


def parse_video(url: str, proxy: Optional[str] = None) -> Optional[VideoInfo]:
    """Извлекает информацию о видео без загрузки."""
    ydl_opts = {
        'quiet': True,
        'no_warnings': True,
        'skip_download': True,
        'noplaylist': True,
    }
    if proxy:
        ydl_opts['proxy'] = proxy

    try:
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            info = ydl.extract_info(url, download=False)

        formats = set()
        for f in info.get('formats', []):
            height = f.get('height')
            if height and height >= 360:
                formats.add(f"{height}p")

        duration = info.get('duration', 0) or 0
        minutes, seconds = divmod(duration, 60)
        hours, minutes = divmod(minutes, 60)
        if hours > 0:
            duration_str = f"{hours}:{minutes:02d}:{seconds:02d}"
        else:
            duration_str = f"{minutes}:{seconds:02d}"

        filesize = None
        if info.get('filesize_approx'):
            filesize = _format_bytes(info['filesize_approx'])
        elif info.get('filesize'):
            filesize = _format_bytes(info['filesize'])

        return VideoInfo(
            title=info.get('title', 'Без названия'),
            duration=duration_str,
            thumbnail=info.get('thumbnail'),
            formats=sorted(formats, key=lambda x: int(x[:-1]), reverse=True),
            filesize=filesize,
            uploader=info.get('uploader'),
            url=url,
        )

    except yt_dlp.utils.DownloadError as e:
        msg = str(e).lower()
        if 'private' in msg or 'unavailable' in msg:
            raise VideoUnavailableError("Видео недоступно (приватное или удалено)") from e
        raise ParseError(f"Не удалось получить данные: {e}") from e
    except Exception as e:
        raise ParseError(f"Ошибка парсинга: {e}") from e


def _format_bytes(bytes_val: int) -> str:
    for unit in ['B', 'KB', 'MB', 'GB']:
        if bytes_val < 1024:
            return f"{bytes_val:.1f} {unit}"
        bytes_val /= 1024
    return f"{bytes_val:.1f} TB"


class ParseError(Exception):
    pass


class VideoUnavailableError(ParseError):
    pass
