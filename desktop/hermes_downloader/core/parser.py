"""Парсинг информации о видео через yt-dlp."""

import yt_dlp
from dataclasses import dataclass
from typing import Optional, List


@dataclass
class VideoInfo:
    title: str
    duration: str
    thumbnail: Optional[str] = None
    formats: List[str] = None
    format_sizes: dict = None   # {"1080p": "450 MB", ...}
    filesize: Optional[str] = None
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
        format_sizes = {}  # "1080p" -> "450 MB"
        for f in info.get('formats', []):
            height = f.get('height')
            if height and height >= 144:
                label = f"{height}p"
                formats.add(label)
                # Сохраняем размер формата
                fs = f.get('filesize') or f.get('filesize_approx')
                if fs and label not in format_sizes:
                    format_sizes[label] = _format_bytes(fs)

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
            format_sizes=format_sizes,
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
