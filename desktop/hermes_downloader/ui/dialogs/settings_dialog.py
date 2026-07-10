"""Диалог настроек: папка сохранения, прокси."""

from PySide6.QtWidgets import (
    QDialog, QVBoxLayout, QHBoxLayout, QLabel,
    QLineEdit, QPushButton, QFileDialog, QFormLayout,
    QDialogButtonBox
)
from PySide6.QtCore import Qt


class SettingsDialog(QDialog):
    """Окно настроек приложения."""

    def __init__(self, current_path: str, current_proxy: str = "",
                 parent=None):
        super().__init__(parent)
        self.setWindowTitle("Настройки")
        self.setMinimumWidth(450)
        self.setModal(True)

        layout = QVBoxLayout(self)
        layout.setSpacing(16)
        layout.setContentsMargins(20, 20, 20, 20)

        # Форма
        form = QFormLayout()
        form.setSpacing(12)

        # Папка сохранения
        path_layout = QHBoxLayout()
        self.path_input = QLineEdit(current_path)
        self.path_input.setPlaceholderText("~/Downloads/Hermes")

        browse_btn = QPushButton("📁")
        browse_btn.setFixedWidth(40)
        browse_btn.clicked.connect(self._browse_folder)

        path_layout.addWidget(self.path_input, 1)
        path_layout.addWidget(browse_btn)
        form.addRow("Папка сохранения:", path_layout)

        # Прокси
        self.proxy_input = QLineEdit(current_proxy)
        self.proxy_input.setPlaceholderText("socks5://127.0.0.1:1080 (необязательно)")
        form.addRow("Прокси:", self.proxy_input)

        layout.addLayout(form)

        # Кнопки
        buttons = QDialogButtonBox(
            QDialogButtonBox.Ok | QDialogButtonBox.Cancel,
            Qt.Horizontal, self
        )
        buttons.accepted.connect(self.accept)
        buttons.rejected.connect(self.reject)
        layout.addWidget(buttons)

    def _browse_folder(self):
        folder = QFileDialog.getExistingDirectory(
            self, "Выберите папку для сохранения",
            self.path_input.text()
        )
        if folder:
            self.path_input.setText(folder)

    def get_save_path(self) -> str:
        return self.path_input.text().strip()

    def get_proxy(self) -> str:
        proxy = self.proxy_input.text().strip()
        return proxy if proxy else None
