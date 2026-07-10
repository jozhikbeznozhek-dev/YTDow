"""Основное окно YTDow."""

import os, json, re, threading
from PySide6.QtWidgets import (
    QMainWindow, QVBoxLayout, QHBoxLayout,
    QWidget, QComboBox, QPushButton, QScrollArea, QLabel, QMessageBox,
    QFileDialog, QTabWidget, QFrame
)
from PySide6.QtCore import Qt, QThreadPool
from PySide6.QtGui import QIcon, QDesktopServices
from PySide6.QtCore import QUrl

from hermes_downloader.models.download_task import DownloadTask, TaskStatus
from hermes_downloader.core.task_manager import TaskManager
from hermes_downloader.core.parser import parse_video, VideoUnavailableError, ParseError
from hermes_downloader.ui.widgets.url_bar import UrlBar
from hermes_downloader.ui.widgets.task_widget import TaskWidget
from hermes_downloader.ui.dialogs.settings_dialog import SettingsDialog
from hermes_downloader.utils.ffmpeg_checker import is_ffmpeg_available, get_ffmpeg_install_hint

HISTORY_FILE = os.path.expanduser("~/.ytdow_history.json")


class MainWindow(QMainWindow):
    def __init__(self):
        super().__init__()
        self.setWindowTitle("YTDow")
        self.setMinimumSize(750, 550)
        self.resize(850, 650)

        icon_path = os.path.join(os.path.dirname(__file__), "..", "..", "assets", "icon.icns")
        if os.path.exists(icon_path):
            self.setWindowIcon(QIcon(icon_path))

        self.threadpool = QThreadPool()
        self.threadpool.setMaxThreadCount(4)

        self.task_manager = TaskManager(self.threadpool)
        self.task_manager.save_path = os.path.expanduser("~/Downloads/Скачанное Ютуб")

        self._setup_ui()
        self._load_styles()
        self._check_ffmpeg()

    def _setup_ui(self):
        self.central_widget = QWidget()
        self.setCentralWidget(self.central_widget)
        self.layout = QVBoxLayout(self.central_widget)
        self.layout.setSpacing(12)
        self.layout.setContentsMargins(20, 16, 20, 20)

        # --- Вкладки ---
        self.tabs = QTabWidget()
        self.layout.addWidget(self.tabs)

        # === Вкладка 1: Загрузка ===
        self.download_tab = QWidget()
        dl_layout = QVBoxLayout(self.download_tab)
        dl_layout.setSpacing(12)

        self.url_input = UrlBar()
        self.url_input.url_submitted.connect(self._on_url_submitted)
        dl_layout.addWidget(self.url_input)

        settings_layout = QHBoxLayout(); settings_layout.setSpacing(8)
        settings_layout.addWidget(QLabel("Формат:"))
        self.format_combo = QComboBox()
        self.format_combo.addItems(["mp4", "mp3"])
        self.format_combo.currentTextChanged.connect(self._on_format_changed)
        settings_layout.addWidget(self.format_combo)
        settings_layout.addWidget(QLabel("Качество:"))
        self.quality_combo = QComboBox()
        self.quality_combo.addItems(["best", "2160p", "1440p", "1080p", "720p", "480p", "360p"])
        settings_layout.addWidget(self.quality_combo)
        settings_layout.addStretch()

        self.size_btn = QPushButton("📊 Размер")
        self.size_btn.clicked.connect(self._calc_size)
        settings_layout.addWidget(self.size_btn)
        self.folder_btn = QPushButton("📁 Папка")
        self.folder_btn.clicked.connect(self._choose_folder)
        settings_layout.addWidget(self.folder_btn)
        self.settings_btn = QPushButton("⚙️")
        self.settings_btn.setFixedWidth(40)
        self.settings_btn.clicked.connect(self._open_settings)
        settings_layout.addWidget(self.settings_btn)
        self.download_btn = QPushButton("⬇ Скачать")
        self.download_btn.setObjectName("download_btn")
        self.download_btn.clicked.connect(self._start_download)
        settings_layout.addWidget(self.download_btn)
        dl_layout.addLayout(settings_layout)

        # Превью
        self.preview_label = QLabel("")
        self.preview_label.setStyleSheet("background:#2d2d2d;border-radius:10px;padding:12px;font-size:12px;color:#b0b0b0")
        self.preview_label.setVisible(False)
        dl_layout.addWidget(self.preview_label)

        dl_layout.addWidget(QLabel("Загрузки"))
        self.scroll_area = QScrollArea()
        self.scroll_widget = QWidget()
        self.tasks_layout = QVBoxLayout(self.scroll_widget)
        self.tasks_layout.setAlignment(Qt.AlignTop); self.tasks_layout.setSpacing(8)
        self.scroll_area.setWidget(self.scroll_widget); self.scroll_area.setWidgetResizable(True)
        dl_layout.addWidget(self.scroll_area, 1)
        self.tabs.addTab(self.download_tab, "🏠 Главная")

        # === Вкладка 2: Библиотека ===
        self.lib_tab = QWidget()
        lib_layout = QVBoxLayout(self.lib_tab); lib_layout.setSpacing(12)

        lib_layout.addWidget(QLabel("📥 Скачанное"))
        self.lib_scroll = QScrollArea()
        self.lib_widget = QWidget()
        self.lib_layout = QVBoxLayout(self.lib_widget)
        self.lib_layout.setAlignment(Qt.AlignTop); self.lib_layout.setSpacing(8)
        self.lib_scroll.setWidget(self.lib_widget); self.lib_scroll.setWidgetResizable(True)
        lib_layout.addWidget(self.lib_scroll, 1)

        lib_layout.addWidget(QLabel("⚙️ Настройки"))
        settings_frame = QFrame()
        settings_frame.setStyleSheet("QFrame{background:#2d2d2d;border-radius:10px;padding:12px}")
        sf_layout = QVBoxLayout(settings_frame)
        self.update_btn = QPushButton("🔄 Проверить обновление")
        self.update_btn.setObjectName("download_btn")
        self.update_btn.clicked.connect(self._check_update)
        sf_layout.addWidget(self.update_btn)
        self.update_status = QLabel("")
        self.update_status.setStyleSheet("font-size:12px;color:#6d6d6d")
        sf_layout.addWidget(self.update_status)
        sf_layout.addWidget(QLabel("YTDow v1.1.0 · macOS"))
        lib_layout.addWidget(settings_frame)
        self.tabs.addTab(self.lib_tab, "📚 Библиотека")

        self.tabs.currentChanged.connect(self._on_tab_changed)

    # === URL submitted: preview ===
    def _on_url_submitted(self, url: str):
        self.url_input.setEnabled(False)
        self.url_input.setPlaceholderText("Анализирую...")
        def _parse():
            try:
                info = parse_video(url, proxy=self.task_manager.proxy)
                self.url_input.setEnabled(True)
                self.url_input.setPlaceholderText("Вставьте ссылки через запятую...")
                if info:
                    self.url_input.setText(info.title)
                    self.quality_combo.clear()
                    if info.formats:
                        self.quality_combo.addItems(info.formats)
                    else:
                        self.quality_combo.addItems(["best","2160p","1440p","1080p","720p","480p","360p"])
                    self.preview_label.setText(
                        f"🎬 {info.title}\n⏱ {info.duration} · 📦 {info.filesize or '?'}"
                    )
                    self.preview_label.setVisible(True)
            except VideoUnavailableError:
                self.url_input.setEnabled(True); self.url_input.setPlaceholderText("Вставьте ссылки через запятую...")
                QMessageBox.warning(self, "Ошибка", "Видео недоступно.")
            except ParseError as e:
                self.url_input.setEnabled(True); self.url_input.setPlaceholderText("Вставьте ссылки через запятую...")
                QMessageBox.warning(self, "Ошибка парсинга", str(e))
        threading.Thread(target=_parse, daemon=True).start()

    def _calc_size(self):
        url = self.url_input.text().strip()
        if not url or not url.startswith("http"):
            QMessageBox.warning(self, "Ошибка", "Вставьте ссылку"); return
        self.preview_label.setText("Расчёт..."); self.preview_label.setVisible(True)
        def _calc():
            try:
                info = parse_video(url, proxy=self.task_manager.proxy)
                self.preview_label.setText(
                    f"🎬 {info.title}\n⏱ {info.duration} · 📦 {info.filesize or '?'}"
                )
                if info.formats:
                    self.quality_combo.clear(); self.quality_combo.addItems(info.formats)
            except Exception as e:
                self.preview_label.setText(f"⚠ Ошибка: {e}")
        threading.Thread(target=_calc, daemon=True).start()

    def _on_format_changed(self, fmt: str):
        self.quality_combo.setEnabled(fmt != "mp3")
        if fmt == "mp3": self.quality_combo.setCurrentText("best")

    def _start_download(self):
        raw = self.url_input.text().strip()
        if not raw: return
        urls = [u.strip() for u in raw.split(",") if u.strip().startswith("http")]
        if not urls:
            QMessageBox.warning(self, "Ошибка", "Нет корректных ссылок"); return
        fmt = self.format_combo.currentText()
        qual = self.quality_combo.currentText()

        for url in urls:
            url = re.sub(r'[&?]list=[^&]+', '', url)
            url = re.sub(r'[&?]index=\d+', '', url).rstrip('?')
            task = DownloadTask(url=url, quality=qual, format=fmt)
            widget = TaskWidget(task.id, task.title)
            widget.cancel_requested.connect(self.task_manager.cancel_task)
            widget.open_requested.connect(self._open_file)
            widget.delete_requested.connect(self._delete_file)
            self.tasks_layout.addWidget(widget)
            self.task_manager.register_widget(task.id, widget)
            self.task_manager.add_task(task)

        self.url_input.clear()
        self.url_input.setPlaceholderText("Вставьте ссылки через запятую...")
        self.preview_label.setVisible(False)

    def _choose_folder(self):
        folder = QFileDialog.getExistingDirectory(self, "Папка", self.task_manager.save_path)
        if folder: self.task_manager.save_path = folder

    def _open_settings(self):
        dialog = SettingsDialog(current_path=self.task_manager.save_path,
                                current_proxy=self.task_manager.proxy or "", parent=self)
        if dialog.exec():
            path = dialog.get_save_path()
            if path: self.task_manager.save_path = path
            self.task_manager.proxy = dialog.get_proxy()

    def _check_ffmpeg(self):
        if not is_ffmpeg_available():
            QMessageBox.warning(self, "ffmpeg", get_ffmpeg_install_hint())

    def _open_file(self, file_path: str):
        if file_path and os.path.exists(file_path):
            QDesktopServices.openUrl(QUrl.fromLocalFile(file_path))

    def _delete_file(self, file_path: str):
        if not file_path or not os.path.exists(file_path): return
        reply = QMessageBox.question(self, "Удалить", f"Удалить {os.path.basename(file_path)}?",
                                      QMessageBox.Yes | QMessageBox.No)
        if reply == QMessageBox.Yes:
            os.remove(file_path)
            self._load_history()

    def _on_tab_changed(self, idx):
        if idx == 1: self._load_history()

    def _load_history(self):
        # Очищаем
        for i in reversed(range(self.lib_layout.count())):
            w = self.lib_layout.itemAt(i).widget()
            if w: w.setParent(None)

        history = []
        if os.path.exists(HISTORY_FILE):
            try: history = json.loads(open(HISTORY_FILE).read())
            except: pass

        if not history:
            self.lib_layout.addWidget(QLabel("Пусто"))
            return

        for entry in history[-50:]:
            card = QFrame()
            card.setStyleSheet("QFrame{background:#2d2d2d;border-radius:10px;padding:12px;margin-bottom:4px}")
            cl = QVBoxLayout(card)
            cl.addWidget(QLabel(f"🎬 {entry.get('title', entry.get('url', '?'))}"))
            meta = f"{entry.get('format','mp4')} · {entry.get('quality','best')} · {entry.get('time','')}"
            cl.addWidget(QLabel(meta))
            btn_row = QHBoxLayout()
            if entry.get('filePath') and os.path.exists(entry['filePath']):
                open_btn = QPushButton("▶ Открыть")
                open_btn.clicked.connect(lambda _, fp=entry['filePath']: self._open_file(fp))
                btn_row.addWidget(open_btn)
            folder_btn = QPushButton("📁 Папка")
            folder_btn.clicked.connect(lambda _, fp=self.task_manager.save_path: self._open_file(fp))
            btn_row.addWidget(folder_btn)
            del_btn = QPushButton("🗑 Удалить")
            del_btn.clicked.connect(lambda _, fp=entry.get('filePath',''): self._delete_file(fp))
            btn_row.addWidget(del_btn)
            cl.addLayout(btn_row)
            self.lib_layout.addWidget(card)
        self.lib_layout.addStretch()

    def _check_update(self):
        self.update_status.setText("Проверка...")
        def _upd():
            try:
                import urllib.request
                req = urllib.request.Request("https://api.github.com/repos/jozhikbeznozhek-dev/YTDow/releases/latest",
                                              headers={"Accept": "application/json"})
                data = json.loads(urllib.request.urlopen(req, timeout=5).read())
                latest = data.get("tag_name", "").lstrip("v")
                if latest and latest != "1.1.0":
                    self.update_status.setText(f"🆕 Доступна версия {latest}")
                else:
                    self.update_status.setText("✓ У вас последняя версия")
            except Exception as e:
                self.update_status.setText(f"⚠ Ошибка: {e}")
        threading.Thread(target=_upd, daemon=True).start()

    def _load_styles(self):
        import sys
        for path in [
            os.path.join(getattr(sys, '_MEIPASS', ''), 'hermes_downloader', 'ui', 'styles.qss'),
            os.path.join(os.path.dirname(__file__), 'styles.qss'),
            os.path.join(os.getcwd(), 'hermes_downloader', 'ui', 'styles.qss'),
        ]:
            if os.path.exists(path):
                with open(path, 'r', encoding='utf-8') as f:
                    self.setStyleSheet(f.read())
                return
