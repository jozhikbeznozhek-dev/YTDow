# Third-party notices

YTDow includes or depends on open-source software. The generated CycloneDX
SBOM distributed with each Android release is the authoritative machine-
readable component inventory for that build.

Key runtime components include:

| Component | License |
|---|---|
| youtubedl-android / FFmpeg integration | GPL-3.0 |
| yt-dlp | Unlicense |
| FFmpeg | LGPL-2.1-or-later / GPL components depending on build |
| AndroidX libraries | Apache-2.0 |
| Kotlin and kotlinx.coroutines | Apache-2.0 |
| Dagger / Hilt | Apache-2.0 |
| Jackson | Apache-2.0 |
| Apache Commons IO and Compress | Apache-2.0 |
| PySide6 / Qt for Python | LGPL-3.0 / GPL-3.0 / commercial |
| Pydantic | MIT |

Copyrights remain with their respective owners. License texts and source links
are available from each component's upstream distribution and from the SBOM
metadata. This notice does not replace those licenses.

YTDow itself is licensed under GPL-3.0-only because the distributed Android
application links to the GPL-3.0 `youtubedl-android` library. If a release uses
a differently configured FFmpeg binary, the release owner must review and
publish any additional notices required by that binary before distribution.
