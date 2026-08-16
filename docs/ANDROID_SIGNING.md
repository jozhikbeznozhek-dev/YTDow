# Android production signing

## v2.3.0 identity reset

YTDow 2.2.0–2.2.3 used an Android Debug certificate. The 2.3.0 release starts
a new production signing identity without a certificate lineage. This is an
intentional clean-install break: Android will reject 2.3.0 as an update over a
2.2.x installation.

The old Debug keystore is not a production credential. Do not upload it to
GitHub Actions, use it to sign 2.3.0, or weaken the release workflow to accept
`CN=Android Debug`.

## Approved v2.3.0 identity

The production identity generated on 2026-08-16 is RSA-4096 with this public
certificate SHA-256 fingerprint:

```text
5b88f4e377b1c6bd5e492886c442002b14f20c970bf9ea90d43076747a1c65c9
```

Local and GitHub release gates must match this exact value. A different
fingerprint is not an alternative release key and must stop publication.

## Create the production identity

Run `keytool` on a trusted offline workstation. Let `keytool` prompt for the
password instead of placing it in shell history:

```bash
umask 077
keytool -genkeypair \
  -keystore /secure/offline/ytdow-production.p12 \
  -storetype PKCS12 \
  -alias ytdow-production \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000
```

Record the certificate fingerprint without exposing private key material:

```bash
keytool -list -v \
  -keystore /secure/offline/ytdow-production.p12 \
  -alias ytdow-production
```

Keep at least two encrypted offline backups in separate locations and verify
that one backup can be opened before publishing. Losing this key prevents
future updates to every installation starting with 2.3.0.

For an automated local setup, the repository also includes a generator that
creates a strong random password, an encrypted PKCS12 file, and a mode-`600`
recovery environment file without printing the password:

```bash
scripts/create-android-production-key.sh \
  /protected/ytdow-production.p12 \
  /protected/ytdow-production-recovery.env
```

The two output files must not be committed. Keeping them together is suitable
only when the entire target folder and every synchronized endpoint are trusted.
For stronger separation, move the password fields from the recovery file into
a password manager after GitHub Secrets are configured.

## Local release gate

Provide credentials only through the process environment:

```bash
export YTDOW_KEYSTORE=/secure/path/ytdow-production.p12
export YTDOW_STORE_PASSWORD='...'
export YTDOW_KEY_ALIAS='ytdow-production'
export YTDOW_KEY_PASSWORD='...'
./gradlew --no-daemon --dependency-verification strict :app:productionRelease
```

Verify the resulting APK and compare its SHA-256 certificate digest with the
fingerprint recorded during key creation:

```bash
apksigner verify --verbose --print-certs \
  app/build/outputs/apk/release/app-release.apk
```

The output must report v2 and v3 verification, must not contain
`CN=Android Debug`, and must show the approved production fingerprint.

## GitHub Actions secrets

Configure these repository secrets from the protected release environment:

- `YTDOW_KEYSTORE_BASE64`
- `YTDOW_STORE_PASSWORD`
- `YTDOW_KEY_ALIAS`
- `YTDOW_KEY_PASSWORD`
- `YTDOW_EXPECTED_CERT_SHA256`

`YTDOW_KEYSTORE_BASE64` is the base64 representation of the production PKCS12
file. `YTDOW_EXPECTED_CERT_SHA256` is the certificate fingerprint, without any
private material. Restrict secret access to release maintainers and never paste
keystore contents or passwords into issues, logs, commits, or chat.

## Required device checks

Before tagging 2.3.0, use a physical arm64 Android device to verify:

1. Android rejects installation over 2.2.3 without removing the old app.
2. Files already present in `Downloads/YTDow` remain after uninstalling 2.2.3.
3. A clean 2.3.0 installation downloads, opens, retries, and deletes files.
4. A production-signed follow-up build with a higher `versionCode` updates
   2.3.0 in place and retains app data.
5. The APK fingerprint exactly matches the protected production identity.
