#!/usr/bin/env bash
set -euo pipefail

SRC_DIR="${1:-landing-page/dist}"
DEST_DIR="${WEB_DEPLOY_DIR:-/var/www/html/contactscleaner-tech}"

if [[ ! -d "$SRC_DIR" ]]; then
  echo "Source directory does not exist: $SRC_DIR" >&2
  exit 1
fi

mkdir -p "$DEST_DIR"
rsync -az --delete "$SRC_DIR"/ "$DEST_DIR"/

echo "Published static site to $DEST_DIR"
