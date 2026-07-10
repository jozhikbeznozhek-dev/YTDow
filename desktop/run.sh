#!/bin/bash
# Hermes Downloader — скрипт запуска
# Исправляет PYTHONPATH, чтобы не было конфликта с Hermes

cd "$(dirname "$0")"
PYTHONPATH="" ./venv/bin/python3 main.py
