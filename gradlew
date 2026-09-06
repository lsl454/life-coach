#!/usr/bin/env sh
set -eu
if command -v gradle >/dev/null 2>&1; then exec gradle "$@"; fi
GRADLE_VERSION=8.11.1
CACHE="${GRADLE_USER_HOME:-$HOME/.gradle}/wrapper/dists/gradle-$GRADLE_VERSION"
DIST="$CACHE/gradle-$GRADLE_VERSION-bin.zip"
if [ ! -f "$DIST" ]; then mkdir -p "$CACHE"; echo "Downloading Gradle $GRADLE_VERSION..."; curl -fsSL "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip" -o "$DIST"; fi
UNPACK="$CACHE/gradle-$GRADLE_VERSION"; [ -x "$UNPACK/bin/gradle" ] || unzip -q "$DIST" -d "$CACHE"; exec "$UNPACK/bin/gradle" "$@"
