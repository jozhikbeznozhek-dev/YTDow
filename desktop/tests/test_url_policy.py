from hermes_downloader.core.url_policy import normalize_download_url


def test_accepts_web_url_and_removes_playlist_parameters():
    assert normalize_download_url(
        "https://example.test/watch?list=PL123&v=video&index=2"
    ) == "https://example.test/watch?v=video"


def test_rejects_non_web_and_credentialed_urls():
    assert normalize_download_url("file:///tmp/video") is None
    assert normalize_download_url("javascript:alert(1)") is None
    assert normalize_download_url("https://user:password@example.test/video") is None


def test_rejects_lookalike_scheme_and_missing_host():
    assert normalize_download_url("httpsx://example.test/video") is None
    assert normalize_download_url("https:///video") is None
