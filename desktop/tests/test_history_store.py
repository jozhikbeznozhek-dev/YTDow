from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

from hermes_downloader.core.history_store import append_history, load_history, remove_history


def test_history_is_trimmed_and_valid(tmp_path: Path):
    path = tmp_path / "history.json"
    for index in range(60):
        append_history({"filePath": f"/{index}", "index": index}, path)

    history = load_history(path)
    assert len(history) == 50
    assert history[0]["index"] == 10
    assert history[-1]["index"] == 59


def test_concurrent_writes_do_not_lose_or_corrupt_entries(tmp_path: Path):
    path = tmp_path / "history.json"
    with ThreadPoolExecutor(max_workers=8) as pool:
        list(pool.map(lambda index: append_history({"filePath": f"/{index}"}, path), range(40)))

    history = load_history(path)
    assert len(history) == 40
    assert {entry["filePath"] for entry in history} == {f"/{index}" for index in range(40)}


def test_remove_history_removes_only_requested_path(tmp_path: Path):
    path = tmp_path / "history.json"
    append_history({"filePath": "/first"}, path)
    append_history({"filePath": "/second"}, path)

    remove_history("/first", path)

    assert load_history(path) == [{"filePath": "/second"}]
