"""Виджет поля ввода ссылки с плейсхолдером и авто-парсингом."""

from PySide6.QtWidgets import QLineEdit
from PySide6.QtCore import Signal


class UrlBar(QLineEdit):
    """Поле ввода URL с поддержкой отправки по Enter."""

    url_submitted = Signal(str)  # эмитится при нажатии Enter

    def __init__(self, parent=None):
        super().__init__(parent)
        self.setPlaceholderText("Вставьте ссылку YouTube, VK или другого сайта...")
        self.returnPressed.connect(self._on_submit)

    def _on_submit(self):
        url = self.text().strip()
        if url:
            self.url_submitted.emit(url)
