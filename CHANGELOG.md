# Changelog

All notable changes to YTDow are documented in this file.

## [2.3.0] - Unreleased

### Breaking

- Android 2.3.0 starts a new production signing identity. It cannot be
  installed over debug-signed 2.2.0–2.2.3; users must keep their files in
  `Downloads/YTDow`, uninstall the old app, and perform a clean installation.
  App-private history and settings do not survive the uninstall.

### Added

- Reliable Android cancellation, retry, attempt history, and MediaStore-backed
  file removal.
- Desktop download history, cancellation, retry, URL validation, and packaged
  UI resources.
- Strict Gradle dependency verification, dependency locks, CycloneDX SBOMs,
  OSV and Python dependency auditing, and SHA-pinned CI actions.
- Privacy, security, GPL, and third-party notices in the application and
  repository.

### Fixed

- Reconcile active Android download cards with persisted native progress so
  progress and completion survive missed lifecycle broadcasts; completed cards
  now leave the active list automatically after a short success state.
- Keep cancellation terminal despite late progress events, play the error
  animation on cancel, and hide unloaded mascot frames instead of exposing a
  broken image placeholder.

### Security

- Restrict the Android JavaScript bridge to local WebView assets and validate
  download and updater URLs at native trust boundaries.
- Require signer equality for in-app APK updates and production-only release
  signing gates.
- Exclude application data from Android backup and device transfer.

### Known release blockers

- A physical Android device and a GPL Corresponding Source bundle for the
  bundled FFmpeg build are required before this version can be tagged or
  published. The new production signing identity is configured and verified.
