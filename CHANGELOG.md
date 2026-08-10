# Changelog

All notable changes to YTDow are documented in this file.

## [2.3.0] - Unreleased

### Added

- Reliable Android cancellation, retry, attempt history, and MediaStore-backed
  file removal.
- Desktop download history, cancellation, retry, URL validation, and packaged
  UI resources.
- Strict Gradle dependency verification, dependency locks, CycloneDX SBOMs,
  OSV and Python dependency auditing, and SHA-pinned CI actions.
- Privacy, security, GPL, and third-party notices in the application and
  repository.

### Security

- Restrict the Android JavaScript bridge to local WebView assets and validate
  download and updater URLs at native trust boundaries.
- Require signer equality for in-app APK updates and production-only release
  signing gates.
- Exclude application data from Android backup and device transfer.

### Known release blockers

- A physical Android device, the approved production signing identity, and a
  GPL Corresponding Source bundle for the bundled FFmpeg build are required
  before this version can be tagged or published.
