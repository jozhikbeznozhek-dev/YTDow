from pathlib import Path
from PySide6.QtCore import Qt

from hermes_downloader.core.parser import VideoInfo
from hermes_downloader.core.task_manager import TaskManager
from hermes_downloader.models.download_task import DownloadTask
from hermes_downloader.ui.main_window import MainWindow


def create_window(qtbot, monkeypatch):
    TaskManager._instance = None
    monkeypatch.setattr(MainWindow, "_check_ffmpeg", lambda self: None)
    window = MainWindow()
    qtbot.addWidget(window)
    return window


def test_parse_preview_does_not_replace_download_url(qtbot, monkeypatch):
    window = create_window(qtbot, monkeypatch)
    url = "https://example.test/watch?v=1"
    window.url_input.setText(url)

    window._on_parse_finished(VideoInfo(title="Untrusted title", duration="1:00"))

    assert window.url_input.text() == url
    assert "Untrusted title" in window.preview_label.text()


def test_retry_button_enqueues_a_new_task(qtbot, monkeypatch):
    window = create_window(qtbot, monkeypatch)
    monkeypatch.setattr(window.task_manager, "add_task", lambda task: None)
    original = DownloadTask(url="https://example.test/video")
    window._enqueue_task(original)
    widget = window.tasks_layout.itemAt(0).widget()

    qtbot.mouseClick(widget.retry_btn, Qt.LeftButton)

    assert len(window._task_specs) == 2
    assert {task.url for task in window._task_specs.values()} == {original.url}


def test_open_file_uses_argv_not_applescript(qtbot, monkeypatch, tmp_path: Path):
    window = create_window(qtbot, monkeypatch)
    media = tmp_path / 'title "quoted"\nline.mp4'
    media.write_bytes(b"media")
    calls = []
    monkeypatch.setattr("subprocess.Popen", lambda argv: calls.append(argv))

    window._open_file(str(media))

    assert calls == [["open", "-R", str(media)]]
