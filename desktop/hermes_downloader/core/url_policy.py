"""Validation and normalization for URLs crossing the desktop UI boundary."""

from __future__ import annotations

from urllib.parse import parse_qsl, urlencode, urlsplit, urlunsplit


def normalize_download_url(raw_url: str) -> str | None:
    try:
        parsed = urlsplit(raw_url.strip())
        if parsed.scheme.lower() not in {"http", "https"}:
            return None
        if not parsed.hostname or parsed.username is not None or parsed.password is not None:
            return None
        query = urlencode(
            [(key, value) for key, value in parse_qsl(parsed.query, keep_blank_values=True)
             if key not in {"list", "index"}],
            doseq=True,
        )
        return urlunsplit((parsed.scheme.lower(), parsed.netloc, parsed.path, query, parsed.fragment))
    except (TypeError, ValueError):
        return None
