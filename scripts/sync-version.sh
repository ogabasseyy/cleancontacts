#!/bin/bash
# Syncs MARKETING_VERSION in iOS project files from the root VERSION file.
# Run this after bumping VERSION to keep iOS in sync.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
VERSION="$(tr -d '[:space:]' < "$REPO_ROOT/VERSION")"

if [ -z "$VERSION" ]; then
  echo "Error: VERSION file is empty or contains only whitespace" >&2
  exit 1
fi

# Escape sed special chars in version string
ESCAPED_VERSION="$(printf '%s' "$VERSION" | sed 's/[\\&/]/\\&/g')"

echo "Syncing version to $VERSION"

# Update project.yml
sed -i.bak "s/MARKETING_VERSION: .*/MARKETING_VERSION: $ESCAPED_VERSION/" "$REPO_ROOT/iosApp/project.yml"
rm -f "$REPO_ROOT/iosApp/project.yml.bak"

# Update project.pbxproj (all occurrences)
sed -i.bak "s/MARKETING_VERSION = .*;/MARKETING_VERSION = $ESCAPED_VERSION;/" "$REPO_ROOT/iosApp/CleanContactsAI.xcodeproj/project.pbxproj"
rm -f "$REPO_ROOT/iosApp/CleanContactsAI.xcodeproj/project.pbxproj.bak"

echo "Done. iOS version set to $VERSION"
