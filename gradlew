#!/usr/bin/env sh
# Lightweight wrapper launcher. Downloads the pinned Gradle distribution when needed.
set -eu
GRADLE_VERSION="8.13"
GRADLE_HOME_DIR="${GRADLE_USER_HOME:-$HOME/.gradle}/murufloat-gradle/gradle-${GRADLE_VERSION}"
GRADLE_BIN="$GRADLE_HOME_DIR/bin/gradle"

if command -v gradle >/dev/null 2>&1; then
    exec gradle "$@"
fi

if [ ! -x "$GRADLE_BIN" ]; then
    BASE="${GRADLE_USER_HOME:-$HOME/.gradle}/murufloat-gradle"
    ZIP="$BASE/gradle-${GRADLE_VERSION}-bin.zip"
    mkdir -p "$BASE"
    echo "Downloading Gradle ${GRADLE_VERSION}..."
    if command -v curl >/dev/null 2>&1; then
        curl -fL --retry 3 --retry-delay 2 "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" -o "$ZIP"
    elif command -v wget >/dev/null 2>&1; then
        wget -O "$ZIP" "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"
    else
        echo "curl or wget is required to download Gradle." >&2
        exit 1
    fi
    command -v unzip >/dev/null 2>&1 || { echo "unzip is required." >&2; exit 1; }
    unzip -q -o "$ZIP" -d "$BASE"
    rm -f "$ZIP"
fi
exec "$GRADLE_BIN" "$@"
