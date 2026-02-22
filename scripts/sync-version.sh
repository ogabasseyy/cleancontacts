#!/bin/bash
# Syncs MARKETING_VERSION in iOS project files from the root VERSION file.
# Run this after bumping VERSION to keep iOS in sync.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
VERSION="$(tr -d '[:space:]' < "$REPO_ROOT/VERSION")"

echo "Syncing version to $VERSION"

# Update project.yml
sed -i '' "s/MARKETING_VERSION: .*/MARKETING_VERSION: $VERSION/" "$REPO_ROOT/iosApp/project.yml"

# Update project.pbxproj (all occurrences)
sed -i '' "s/MARKETING_VERSION = .*;/MARKETING_VERSION = $VERSION;/" "$REPO_ROOT/iosApp/CleanContactsAI.xcodeproj/project.pbxproj"

echo "Done. iOS version set to $VERSION"
