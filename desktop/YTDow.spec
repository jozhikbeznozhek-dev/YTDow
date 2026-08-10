# -*- mode: python ; coding: utf-8 -*-
import os
from PyInstaller.utils.hooks import collect_all

datas = [
    ('hermes_downloader/ui/styles.qss', 'hermes_downloader/ui'),
    ('assets/icon.icns', 'assets'),
]
binaries = []
hiddenimports = ['pydantic', 'yt_dlp', 'PySide6']
tmp_ret = collect_all('pydantic')
datas += tmp_ret[0]; binaries += tmp_ret[1]; hiddenimports += tmp_ret[2]
tmp_ret = collect_all('yt_dlp')
datas += tmp_ret[0]; binaries += tmp_ret[1]; hiddenimports += tmp_ret[2]


a = Analysis(
    ['main.py'],
    pathex=[],
    binaries=binaries,
    datas=datas,
    hiddenimports=hiddenimports,
    hookspath=[],
    hooksconfig={},
    runtime_hooks=[],
    excludes=[],
    noarchive=False,
    optimize=0,
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
    argv_emulation=False,
    target_arch=None,
    codesign_identity=os.environ.get('YTDOW_CODESIGN_IDENTITY'),
    entitlements_file=os.environ.get('YTDOW_ENTITLEMENTS'),
    icon=['assets/icon.icns'],
)
coll = COLLECT(
    exe,
    a.binaries,
    a.datas,
    strip=False,
    upx=True,
    upx_exclude=[],
    name='YTDow',
)
app = BUNDLE(
    coll,
    name='YTDow.app',
    icon='assets/icon.icns',
    bundle_identifier='com.jozhikbeznozhek.ytdow.desktop',
    info_plist={
        'CFBundleShortVersionString': '2.3.0',
        'CFBundleVersion': '9',
        'LSMinimumSystemVersion': '12.0',
        'NSHighResolutionCapable': True,
    },
)
