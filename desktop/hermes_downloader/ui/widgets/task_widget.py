"""Виджет одного элемента в списке загрузок."""

from PySide6.QtWidgets import (
    QFrame, QVBoxLayout, QHBoxLayout, QLabel,
    QProgressBar, QPushButton
)
from PySide6.QtCore import Qt, Signal
from hermes_downloader.models.download_task import TaskStatus


class TaskWidget(QFrame):
    cancel_requested = Signal(str)
    retry_requested = Signal(str)
    open_requested = Signal(str)     # file_path
    delete_requested = Signal(str)   # file_path

    def __init__(self, task_id: str, title: str, parent=None):
        super().__init__(parent)
        self.task_id = task_id
        self.file_path = ""
        self.setObjectName("task_card")

        self._layout = QVBoxLayout(self); self._layout.setSpacing(6)

        header_layout = QHBoxLayout()
        self.icon_label = QLabel("🎬"); self.icon_label.setFixedWidth(28)
        self.title_label = QLabel(title)
        self.title_label.setObjectName("title_label"); self.title_label.setWordWrap(True)
        header_layout.addWidget(self.icon_label); header_layout.addWidget(self.title_label, 1)
        self._layout.addLayout(header_layout)

        self.progress_bar = QProgressBar()
        self.progress_bar.setRange(0, 100); self.progress_bar.setValue(0)
        self.progress_bar.setVisible(False)
        self._layout.addWidget(self.progress_bar)

        info_layout = QHBoxLayout()
        self.status_label = QLabel("В очереди...")
        self.status_label.setObjectName("status_label")
        info_layout.addWidget(self.status_label, 1)

        self.cancel_btn = QPushButton("❌ Отмена"); self.cancel_btn.setFixedWidth(100)
        self.cancel_btn.clicked.connect(lambda: self.cancel_requested.emit(self.task_id))
        info_layout.addWidget(self.cancel_btn)

        self.open_btn = QPushButton("▶ Открыть"); self.open_btn.setFixedWidth(100)
        self.open_btn.clicked.connect(lambda: self.open_requested.emit(self.file_path))
        self.open_btn.setVisible(False)
        info_layout.addWidget(self.open_btn)

        self.delete_btn = QPushButton("🗑 Удалить"); self.delete_btn.setFixedWidth(100)
        self.delete_btn.clicked.connect(lambda: self.delete_requested.emit(self.file_path))
        self.delete_btn.setVisible(False)
        info_layout.addWidget(self.delete_btn)

        self.retry_btn = QPushButton("🔄 Повтор"); self.retry_btn.setFixedWidth(100)
        self.retry_btn.clicked.connect(lambda: self.retry_requested.emit(self.task_id))
        self.retry_btn.setVisible(False)
        info_layout.addWidget(self.retry_btn)

        self._layout.addLayout(info_layout)
        self.setMinimumHeight(80)

    def update_progress(self, percent: float, speed: str, eta: str, size: str):
        pct = int(percent * 100)
        self.progress_bar.setVisible(True); self.progress_bar.setValue(pct)
        self.status_label.setText(f"{speed}  •  ETA: {eta}  •  {size}")

    def update_status(self, status: TaskStatus):
        self.cancel_btn.setVisible(False)
        self.open_btn.setVisible(False)
        self.delete_btn.setVisible(False)
        self.retry_btn.setVisible(False)
        self.progress_bar.setVisible(False)

        if status == TaskStatus.QUEUED:
            self.status_label.setText("В очереди..."); self.cancel_btn.setVisible(True)
        elif status == TaskStatus.PARSING:
            self.status_label.setText("Анализирую..."); self.cancel_btn.setVisible(True)
        elif status == TaskStatus.DOWNLOADING:
            self.progress_bar.setVisible(True); self.cancel_btn.setVisible(True)
        elif status == TaskStatus.COMPLETED:
            self.status_label.setText("✓ Готово")
            self.progress_bar.setVisible(True); self.progress_bar.setValue(100)
            self.open_btn.setVisible(True); self.delete_btn.setVisible(True)
        elif status == TaskStatus.CANCELLED:
            self.status_label.setText("✗ Отменено"); self.retry_btn.setVisible(True)
        elif status == TaskStatus.ERROR:
            self.status_label.setText("Ошибка"); self.retry_btn.setVisible(True)

    def show_error(self, error_msg: str):
        self.status_label.setText(f"⚠️ {error_msg}")
        self.status_label.setObjectName("error_label")
        self.progress_bar.setVisible(False); self.cancel_btn.setVisible(False)
        self.retry_btn.setVisible(True)
