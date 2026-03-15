#!/bin/bash
# Syncs iOS MARKETING_VERSION from the root VERSION file.
# In Xcode Cloud archive builds, derive a higher marketing version so uploads
# don't reuse a closed App Store Connect version train.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
VERSION="$(tr -d '[:space:]' < "$REPO_ROOT/VERSION")"
PROJECT_YML="$REPO_ROOT/iosApp/project.yml"
PBXPROJ="$REPO_ROOT/iosApp/CleanContactsAI.xcodeproj/project.pbxproj"

if [ -z "$VERSION" ]; then
  echo "Error: VERSION file is empty or contains only whitespace" >&2
  exit 1
fi

if [[ ! "$VERSION" =~ ^([0-9]+)\.([0-9]+)\.([0-9]+)$ ]]; then
  echo "Error: VERSION must use major.minor.patch format, got '$VERSION'" >&2
  exit 1
fi

MAJOR_VERSION="${BASH_REMATCH[1]}"
MINOR_VERSION="${BASH_REMATCH[2]}"
PATCH_VERSION="${BASH_REMATCH[3]}"

MARKETING_VERSION="$VERSION"
CURRENT_PROJECT_VERSION=""
VERSION_SOURCE="VERSION file"

if [[ "${CI_XCODE_CLOUD:-}" == "TRUE" && "${CI_XCODEBUILD_ACTION:-}" == "archive" ]]; then
  if [[ ! "${CI_BUILD_NUMBER:-}" =~ ^[0-9]+$ ]]; then
    echo "Error: CI_BUILD_NUMBER must be a positive integer for Xcode Cloud archive builds" >&2
    exit 1
  fi

  MARKETING_VERSION="${MAJOR_VERSION}.${MINOR_VERSION}.$((PATCH_VERSION + CI_BUILD_NUMBER))"
  CURRENT_PROJECT_VERSION="${CI_BUILD_NUMBER}"
  VERSION_SOURCE="Xcode Cloud archive build ${CI_BUILD_NUMBER}"
fi

escape_sed() {
  printf '%s' "$1" | sed 's/[\\&/]/\\&/g'
}

update_file() {
  local file="$1"
  local pattern="$2"
  local replacement="$3"

  sed -i.bak "s/${pattern}/${replacement}/" "$file"
  rm -f "${file}.bak"
}

ESCAPED_MARKETING_VERSION="$(escape_sed "$MARKETING_VERSION")"

echo "Syncing iOS MARKETING_VERSION to $MARKETING_VERSION from $VERSION_SOURCE"

update_file "$PROJECT_YML" "MARKETING_VERSION: .*" "MARKETING_VERSION: $ESCAPED_MARKETING_VERSION"
update_file "$PBXPROJ" "MARKETING_VERSION = .*;" "MARKETING_VERSION = $ESCAPED_MARKETING_VERSION;"

if [ -n "$CURRENT_PROJECT_VERSION" ]; then
  ESCAPED_CURRENT_PROJECT_VERSION="$(escape_sed "$CURRENT_PROJECT_VERSION")"
  echo "Syncing iOS CURRENT_PROJECT_VERSION to $CURRENT_PROJECT_VERSION"
  update_file "$PROJECT_YML" "CURRENT_PROJECT_VERSION: .*" "CURRENT_PROJECT_VERSION: $ESCAPED_CURRENT_PROJECT_VERSION"
  update_file "$PBXPROJ" "CURRENT_PROJECT_VERSION = .*;" "CURRENT_PROJECT_VERSION = $ESCAPED_CURRENT_PROJECT_VERSION;"
fi

echo "Done. iOS version set to $MARKETING_VERSION"
