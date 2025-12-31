#!/usr/bin/env bash
set -euo pipefail

GRADLE_VERSION="${GRADLE_VERSION:-9.2.1}"
BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CACHE_DIR="${BASE_DIR}/.gradle-bootstrap"
DIST_DIR="${CACHE_DIR}/gradle-${GRADLE_VERSION}"
GRADLE_BIN="${DIST_DIR}/bin/gradle"

if [[ ! -x "${GRADLE_BIN}" ]]; then
  mkdir -p "${CACHE_DIR}"
  ZIP="${CACHE_DIR}/gradle-${GRADLE_VERSION}-bin.zip"
  URL="https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"
  echo "[gradlew] Bootstrapping Gradle ${GRADLE_VERSION}..."
  if command -v curl >/dev/null 2>&1; then
    curl -L -o "${ZIP}" "${URL}"
  elif command -v wget >/dev/null 2>&1; then
    wget -O "${ZIP}" "${URL}"
  else
    echo "ERROR: curl or wget is required to download Gradle." >&2
    exit 1
  fi
  rm -rf "${DIST_DIR}"
  unzip -q "${ZIP}" -d "${CACHE_DIR}"
  chmod +x "${GRADLE_BIN}"
fi

exec "${GRADLE_BIN}" "$@"
