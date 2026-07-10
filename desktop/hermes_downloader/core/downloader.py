import yt_dlp
import os
import threading
from hermes_downloader.models.download_task import DownloadTask, TaskStatus
from hermes_downloader.utils.ffmpeg_checker import get_ffmpeg_path
from PySide6.QtCore import QObject, Signal, QRunnable, Slot


class DownloadCancelledError(Exception):
    """Внутреннее исключение — загрузка отменена пользователем."""
    pass


class DownloadWorkerSignals(QObject):
    finished = Signal(str, str)      # task_id, file_path
    error = Signal(str, str)        # task_id, error_message
    progress = Signal(str, float, str, str, str)  # id, %, speed, eta, size


class DownloadWorker(QRunnable):
    def __init__(self, task: DownloadTask, save_path: str, proxy: str = None):
        super().__init__()
        self.task = task
        self.save_path = save_path
        self.proxy = proxy
        self.signals = DownloadWorkerSignals()
        self._cancelled = threading.Event()

    def cancel(self):
        """Флаг отмены — хук прогресса выбросит исключение."""
        self._cancelled.set()

    def progress_hook(self, d):
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
            ydl_opts = {
                'outtmpl': os.path.join(self.save_path, '%(title)s.%(ext)s'),
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

            file_path = ""
            title = ""

            def _capture_output(d):
                nonlocal file_path, title
                if d['status'] == 'finished':
                    file_path = d.get('filename', '')
                if d.get('info_dict'):
                    title = d['info_dict'].get('title', '')

            ydl_opts['progress_hooks'].append(_capture_output)

            with yt_dlp.YoutubeDL(ydl_opts) as ydl:
                ydl.download([self.task.url])

            # Сохраняем в историю
            try:
                import json
                history_path = os.path.expanduser("~/.ytdow_history.json")
                history = []
                if os.path.exists(history_path):
                    history = json.loads(open(history_path).read())
                entry = {
                    "url": self.task.url, "title": title or self.task.url,
                    "format": self.task.format, "quality": self.task.quality,
                    "filePath": file_path, "time": __import__('datetime').datetime.now().strftime("%d.%m.%Y %H:%M")
                }
                history.append(entry)
                history = history[-50:]
                with open(history_path, 'w') as f:
                    json.dump(history, f, indent=2, ensure_ascii=False)
            except: pass

            self.signals.finished.emit(self.task.id, file_path if file_path else self.task.id)

        except DownloadCancelledError:
            pass
        except Exception as e:
            self.signals.error.emit(self.task.id, str(e))
