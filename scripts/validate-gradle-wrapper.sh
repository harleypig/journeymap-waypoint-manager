#!/usr/bin/env bash
set -euo pipefail

WRAPPER_JAR="gradle/wrapper/gradle-wrapper.jar"
WRAPPER_PROPS="gradle/wrapper/gradle-wrapper.properties"
VERSIONS_URL="https://services.gradle.org/versions/all"

version=$(grep -oP 'distributionUrl=.*gradle-\K[\d.]+(?=-)' "$WRAPPER_PROPS")
if [[ -z $version ]]; then
    echo "ERROR: Cannot parse Gradle version from $WRAPPER_PROPS" >&2
    exit 1
fi

local_hash=$(sha256sum "$WRAPPER_JAR" | cut -d' ' -f1)

if ! response=$(curl -fsL --max-time 10 "$VERSIONS_URL" 2>/dev/null); then
    echo "WARNING: Could not reach services.gradle.org; skipping wrapper validation." >&2
    exit 0
fi

expected=$(printf '%s' "$response" \
    | python3 -c "
import json, sys
versions = json.load(sys.stdin)
entry = next((v for v in versions if v.get('version') == '$version'), None)
print(entry['wrapperChecksum'] if entry else '')
")

if [[ -z $expected ]]; then
    echo "ERROR: Gradle $version not found in $VERSIONS_URL" >&2
    exit 1
fi

if [[ $local_hash != "$expected" ]]; then
    echo "FAIL: gradle-wrapper.jar checksum mismatch" >&2
    echo "  Gradle $version expected: $expected" >&2
    echo "  Local:                    $local_hash" >&2
    exit 1
fi

echo "OK: gradle-wrapper.jar matches Gradle $version (${local_hash:0:16}...)"
