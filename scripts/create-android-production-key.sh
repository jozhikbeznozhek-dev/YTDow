#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 2 ]]; then
  echo "Usage: $0 KEYSTORE_PATH CREDENTIALS_PATH" >&2
  exit 2
fi

keystore_path=$1
credentials_path=$2
key_alias=ytdow-production

if [[ -e "$keystore_path" || -e "$credentials_path" ]]; then
  echo "Refusing to overwrite an existing keystore or credentials file" >&2
  exit 1
fi

umask 077
mkdir -p "$(dirname "$keystore_path")" "$(dirname "$credentials_path")"

store_password=$(openssl rand -base64 48 | tr -d '\n')
export YTDOW_STORE_PASSWORD=$store_password
export YTDOW_KEY_PASSWORD=$store_password

keytool -genkeypair \
  -keystore "$keystore_path" \
  -storetype PKCS12 \
  -storepass:env YTDOW_STORE_PASSWORD \
  -keypass:env YTDOW_KEY_PASSWORD \
  -alias "$key_alias" \
  -keyalg RSA \
  -keysize 4096 \
  -sigalg SHA256withRSA \
  -validity 10000 \
  -dname "CN=YTDow Production, OU=Release, O=YTDow"

fingerprint=$(
  keytool -list -v \
    -keystore "$keystore_path" \
    -storepass:env YTDOW_STORE_PASSWORD \
    -alias "$key_alias" |
    sed -n 's/^[[:space:]]*SHA256: //p' |
    tr -d ':' |
    tr '[:upper:]' '[:lower:]'
)

if [[ ! "$fingerprint" =~ ^[0-9a-f]{64}$ ]]; then
  echo "Unable to read the generated certificate fingerprint" >&2
  exit 1
fi

{
  printf '# YTDow Android production signing recovery file\n'
  printf '# Keep this file private and move the passwords to a password manager.\n'
  printf 'export YTDOW_KEYSTORE=%q\n' "$keystore_path"
  printf 'export YTDOW_STORE_PASSWORD=%q\n' "$store_password"
  printf 'export YTDOW_KEY_ALIAS=%q\n' "$key_alias"
  printf 'export YTDOW_KEY_PASSWORD=%q\n' "$store_password"
  printf 'export YTDOW_EXPECTED_CERT_SHA256=%q\n' "$fingerprint"
} > "$credentials_path"

chmod 600 "$keystore_path" "$credentials_path"

echo "Created encrypted Android production identity"
echo "Keystore: $keystore_path"
echo "Recovery file: $credentials_path"
echo "Certificate SHA-256: $fingerprint"
