import yt_dlp
import os
import threading
from datetime import datetime
from hermes_downloader.models.download_task import DownloadTask, TaskStatus
from hermes_downloader.core.history_store import append_history
from hermes_downloader.utils.ffmpeg_checker import get_ffmpeg_path
from PySide6.QtCore import QObject, Signal, QRunnable, Slot


class DownloadCancelledError(Exception):
    """Внутреннее исключение — загрузка отменена пользователем."""
    pass


class DownloadWorkerSignals(QObject):
    finished = Signal(str, str)      # task_id, file_path
    error = Signal(str, str)        # task_id, error_message
    progress = Signal(str, float, str, str, str)  # id, %, speed, eta, size
    cancelled = Signal(str)


class DownloadWorker(QRunnable):
    def __init__(self, task: DownloadTask, save_path: str, proxy: str = None):
        super().__init__()
        self.task = task
        self.save_path = save_path
        self.proxy = proxy
        self.signals = DownloadWorkerSignals()
        self._cancelled = threading.Event()
        self._temporary_paths: set[str] = set()

    def cancel(self):
        """Флаг отмены — хук прогресса выбросит исключение."""
        self._cancelled.set()

    def progress_hook(self, d):
        for key in ("filename", "tmpfilename"):
            if path := d.get(key):
                self._temporary_paths.add(path)
        if self._cancelled.is_set():
            raise DownloadCancelledError("Загрузка отменена пользователем")
        if d['status'] == 'downloading':
            total = d.get('total_bytes') or d.get('total_bytes_estimate')
            downloaded = d.get('downloaded_bytes', 0)
            if total:
                percent = downloaded / total
                self.signals.progress.emit(
                    self.task.id, percent,
                    d.get('_speed_str', '--'),
                    d.get('_eta_str', '--'),
                    d.get('_total_bytes_str', '--')
                )
        elif d['status'] == 'finished':
            self.signals.progress.emit(
                self.task.id, 1.0, 'Готово', '00:00', 'Конвертация...'
            )

    @Slot()
    def run(self):
        try:
            os.makedirs(self.save_path, exist_ok=True)
            ydl_opts = {
                'outtmpl': os.path.join(self.save_path, '%(title)s [%(id)s].%(ext)s'),
                'progress_hooks': [self.progress_hook],
                'quiet': True,
                'no_warnings': True,
                'noplaylist': True,              # не качать плейлисты
            }

            ffmpeg = get_ffmpeg_path()
            if ffmpeg:
                ydl_opts['ffmpeg_location'] = ffmpeg

            if self.task.format == 'mp3':
                ydl_opts['format'] = 'bestaudio/best'
                ydl_opts['postprocessors'] = [{
                    'key': 'FFmpegExtractAudio',
                    'preferredcodec': 'mp3',
                    'preferredquality': '192',
                }]
            else:
                # Всегда перекодируем в MP4 (ffmpeg склеит/перекодирует при необходимости)
                ydl_opts['merge_output_format'] = 'mp4'
                if self.task.quality == 'best':
                    ydl_opts['format'] = (
                        'bestvideo+bestaudio/best'
                    )
                else:
                    height = self.task.quality.replace('p', '')
                    ydl_opts['format'] = (
                        f'bestvideo[height<={height}]+bestaudio/best'
                    )

            if self.proxy:
                ydl_opts['proxy'] = self.proxy

            with yt_dlp.YoutubeDL(ydl_opts) as ydl:
                info = ydl.extract_info(self.task.url, download=True)
                file_path = self._resolve_output_path(ydl, info)
                title = str(info.get("title") or self.task.url)

            if self._cancelled.is_set():
                raise DownloadCancelledError("Загрузка отменена пользователем")
            if not file_path or not os.path.isfile(file_path):
                raise FileNotFoundError("yt-dlp не вернул итоговый файл")

            append_history({
                "url": self.task.url,
                "title": title,
                "format": self.task.format,
                "quality": self.task.quality,
                "filePath": file_path,
                "time": datetime.now().strftime("%d.%m.%Y %H:%M"),
            })
            self.signals.finished.emit(self.task.id, file_path)

        except DownloadCancelledError:
            self._cleanup_partials()
            self.signals.cancelled.emit(self.task.id)
        except Exception as e:
            self._cleanup_partials()
            self.signals.error.emit(self.task.id, str(e))

    def _resolve_output_path(self, ydl, info: dict) -> str:
        candidates: list[str] = []
        for download in info.get("requested_downloads") or []:
            if path := download.get("filepath"):
                candidates.append(path)
        for key in ("filepath", "_filename"):
            if path := info.get(key):
                candidates.append(path)
        candidates.append(ydl.prepare_filename(info))

        if self.task.format == "mp3":
            candidates.extend(f"{os.path.splitext(path)[0]}.mp3" for path in list(candidates))
        elif self.task.format == "mp4":
            candidates.extend(f"{os.path.splitext(path)[0]}.mp4" for path in list(candidates))

        save_root = os.path.realpath(self.save_path)
        for candidate in candidates:
            resolved = os.path.realpath(candidate)
            if os.path.commonpath((save_root, resolved)) == save_root and os.path.isfile(resolved):
                return resolved
        return ""

    def _cleanup_partials(self) -> None:
        save_root = os.path.realpath(self.save_path)
        for path in self._temporary_paths:
            resolved = os.path.realpath(path)
            if os.path.commonpath((save_root, resolved)) != save_root:
                continue
            if resolved.endswith((".part", ".ytdl")):
                try:
                    os.remove(resolved)
                except FileNotFoundError:
                    pass
