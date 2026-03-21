#!/usr/bin/env bash
set -euo pipefail

SRC_DIR="${1:-landing-page/dist}"
DEST_DIR="${WEB_DEPLOY_DIR:-/var/www/html/contactscleaner-tech}"

if [[ ! -d "$SRC_DIR" ]]; then
  echo "Source directory does not exist: $SRC_DIR" >&2
  exit 1
fi

if [[ -z "$DEST_DIR" || "$DEST_DIR" == "/" || "$DEST_DIR" == "." || "$DEST_DIR" == ".." ]]; then
  echo "Refusing to deploy to unsafe destination: $DEST_DIR" >&2
  exit 1
fi

if [[ "$DEST_DIR" != /* ]]; then
  echo "Deployment destination must be an absolute path: $DEST_DIR" >&2
  exit 1
fi

umask 022

mkdir -p "$DEST_DIR"
# Sync content without trying to preserve owner/group/perms on the VPS webroot.
rsync -rltz --delete \
  --chmod=Du=rwx,Dgo=rx,Fu=rw,Fgo=r \
  --no-perms \
  --no-owner \
  --no-group \
  "$SRC_DIR"/ "$DEST_DIR"/

echo "Published static site to $DEST_DIR"
