# Security Policy

## Supported versions

Security fixes are provided for the latest published release only.

## Reporting a vulnerability

Do not publish exploitable details in a public issue. Use GitHub's private
security advisory flow for this repository. Include the affected version,
reproduction steps, expected impact, and any relevant logs with secrets and
personal data removed.

The project does not request APKs, keystores, passwords, cookies, or account
credentials from reporters.

## Release integrity

Published Android artifacts are signed, checked with `apksigner`, accompanied
by SHA-256 checksums, CycloneDX SBOM files, and GitHub build provenance. The
in-app updater accepts HTTPS GitHub release URLs and installs only an APK whose
signer exactly matches the currently installed application.
