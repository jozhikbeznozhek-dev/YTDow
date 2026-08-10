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

Copyrights remain with their respective owners. Component identities are
recorded in the SBOM; license texts and source archives must be obtained from
the corresponding upstream distributions. This notice does not replace those
licenses.

YTDow itself is licensed under GPL-3.0-only because the distributed Android
application links to the GPL-3.0 `youtubedl-android` library. If a release uses
a differently configured FFmpeg binary, the release owner must review and
publish any additional notices required by that binary before distribution.

## Corresponding Source status for v2.3.0

The Android dependency set resolves `youtubedl-android` `0.18.1`. Its source
tag resolves to commit `d725d5c9a18c3a99a13ee0308bf78275dc310760` at
<https://github.com/yausername/youtubedl-android/tree/0.18.1>.

That upstream tag documents building the bundled FFmpeg payload from the
Termux packages repository, but does not pin the exact Termux commit, all
source revisions, patches, or build inputs used for the distributed native
binaries. Until a matching, complete Corresponding Source bundle or a written
GPL-compliant source offer is available, the v2.3.0 APK must not be publicly
distributed.
