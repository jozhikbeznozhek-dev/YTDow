"""YTDow — точка входа."""

import sys
import os

# Добавляем корень проекта в PYTHONPATH, чтобы работал импорт hermes_downloader
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from PySide6.QtWidgets import QApplication
from hermes_downloader.ui.main_window import MainWindow


def main():
    import sys, traceback

    def excepthook(exc_type, exc_value, exc_tb):
        """Показывает необработанные исключения в диалоге."""
        tb = ''.join(traceback.format_exception(exc_type, exc_value, exc_tb))
        from PySide6.QtWidgets import QMessageBox
        QMessageBox.critical(None, "YTDow — ошибка",
                             f"Необработанная ошибка:\n\n{tb[-1500:]}")
        sys.__excepthook__(exc_type, exc_value, exc_tb)

    sys.excepthook = excepthook

    app = QApplication(sys.argv)
    app.setApplicationName("YTDow")
    app.setOrganizationName("YTDow")

    window = MainWindow()
    window.show()

    sys.exit(app.exec())


if __name__ == "__main__":
    main()
