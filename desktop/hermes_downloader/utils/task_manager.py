"""Менеджер задач загрузки — Singleton с очередью."""

import queue
import threading
from typing import Optional
from hermes_downloader.models.download_task import DownloadTask, TaskStatus
from hermes_downloader.core.downloader import DownloadWorker


class TaskManager:
    """Singleton-менеджер управления очередью загрузок."""

    _instance = None
    _lock = threading.Lock()

    MAX_CONCURRENT = 2  # макс. одновременных загрузок

    def __new__(cls, threadpool=None):
        with cls._lock:
            if cls._instance is None:
                cls._instance = super().__new__(cls)
                cls._instance._initialized = False
            return cls._instance

    def __init__(self, threadpool=None):
        if self._initialized:
            return
        self._initialized = True

        self._task_queue: queue.Queue[DownloadTask] = queue.Queue()
        self._active_tasks: dict[str, DownloadTask] = {}
        self._active_workers: dict[str, DownloadWorker] = {}  # для отмены
        self._task_widgets: dict[str, object] = {}  # TaskWidget refs
        self._threadpool = threadpool
        self._active_count = 0
        self._save_path = "~/Downloads/Скачанное Ютуб"
        self._proxy: Optional[str] = None

    @property
    def active_count(self) -> int:
        return self._active_count

    @property
    def save_path(self) -> str:
        return self._save_path

    @save_path.setter
    def save_path(self, path: str):
        self._save_path = path

    @property
    def proxy(self) -> Optional[str]:
        return self._proxy

    @proxy.setter
    def proxy(self, value: Optional[str]):
        self._proxy = value

    def add_task(self, task: DownloadTask):
        """Добавляет задачу в очередь."""
        self._task_queue.put(task)
        self._process_queue()

    def register_widget(self, task_id: str, widget):
        """Привязывает виджет к задаче для обновления UI."""
        self._task_widgets[task_id] = widget

    def cancel_task(self, task_id: str):
        """Отменяет задачу — останавливает загрузку."""
        task = self._active_tasks.get(task_id)
        worker = self._active_workers.get(task_id)
        if worker:
            worker.cancel()                        # реальная остановка
        if task:
            task.status = TaskStatus.CANCELLED
            widget = self._task_widgets.get(task_id)
            if widget:
                widget.update_status(TaskStatus.CANCELLED)
            self._active_tasks.pop(task_id, None)
            self._active_workers.pop(task_id, None)
            self._active_count = max(0, self._active_count - 1)
            self._process_queue()

    def _process_queue(self):
        """Запускает задачи из очереди, пока есть свободные слоты."""
        while self._active_count < self.MAX_CONCURRENT:
            try:
                task = self._task_queue.get_nowait()
            except queue.Empty:
                break

            if task.status == TaskStatus.CANCELLED:
                continue

            task.status = TaskStatus.DOWNLOADING
            self._active_tasks[task.id] = task
            self._active_count += 1

            worker = DownloadWorker(
                task=task,
                save_path=self._save_path,
                proxy=self._proxy,
            )

            worker.signals.progress.connect(self._on_progress)
            worker.signals.finished.connect(self._on_finished)
            worker.signals.error.connect(self._on_error)

            self._active_workers[task.id] = worker

            if self._threadpool:
                self._threadpool.start(worker)

    def _on_progress(self, task_id: str, percent: float,
                     speed: str, eta: str, size: str):
        task = self._active_tasks.get(task_id)
        if task:
            task.progress = percent
            task.speed = speed
            task.eta = eta
            task.filesize = size
        widget = self._task_widgets.get(task_id)
        if widget:
            widget.update_progress(percent, speed, eta, size)

    def _on_finished(self, task_id: str, file_path: str = ""):
        self._active_workers.pop(task_id, None)
        task = self._active_tasks.pop(task_id, None)
        if task:
            task.status = TaskStatus.COMPLETED
            task.file_path = file_path
        self._active_count = max(0, self._active_count - 1)
        widget = self._task_widgets.get(task_id)
        if widget:
            widget.file_path = file_path
            widget.update_status(TaskStatus.COMPLETED)
        self._process_queue()

    def _on_error(self, task_id: str, error_msg: str):
        self._active_workers.pop(task_id, None)
        task = self._active_tasks.pop(task_id, None)
        if task:
            task.status = TaskStatus.ERROR
        self._active_count = max(0, self._active_count - 1)
        widget = self._task_widgets.get(task_id)
        if widget:
            widget.show_error(str(error_msg))
        self._process_queue()
