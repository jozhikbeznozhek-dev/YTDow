# -*- mode: python ; coding: utf-8 -*-
# Onedir mode — без распаковки при запуске (мгновенный старт)
import sys
from pathlib import Path

base_path = Path(SPECPATH).parent

a = Analysis(
    ['main.py'],
    pathex=[str(base_path)],
    binaries=[],
    datas=[
        ('hermes_downloader/ui/styles.qss', 'hermes_downloader/ui'),
    ],
    hiddenimports=[
        'pydantic',
        'pydantic.deprecated.decorator',
        'yt_dlp',
        'yt_dlp.extractor',
        'yt_dlp.downloader',
        'yt_dlp.postprocessor',
        'PySide6.QtCore',
        'PySide6.QtGui',
        'PySide6.QtWidgets',
    ],
    hookspath=[],
    hooksconfig={},
    runtime_hooks=[],
    excludes=[],
    noarchive=False,
)

pyz = PYZ(a.pure)

exe = EXE(
    pyz,
    a.scripts,
    [],
    exclude_binaries=True,
    name='YTDow',
    debug=False,
    bootloader_ignore_signals=False,
    strip=False,
    upx=True,
    console=False,
    disable_windowed_traceback=False,
    target_arch=None,
    codesign_identity=None,
    entitlements_file=None,
    icon=['assets/icon.icns'],
)

coll = COLLECT(
    exe,
    a.binaries,
    a.datas,
    strip=False,
    upx=True,
    upx_exclude=[],
    name='Hermes Downloader',
)

app = BUNDLE(
    coll,
    name='YTDow.app',
    icon='assets/icon.icns',
    bundle_identifier='com.ytdow.app',
    info_plist={
        'NSPrincipalClass': 'NSApplication',
        'NSHighResolutionCapable': True,
        'LSMinimumSystemVersion': '11.0',
        'CFBundleShortVersionString': '1.1.0',
        'CFBundleName': 'YTDow',
    },
)
