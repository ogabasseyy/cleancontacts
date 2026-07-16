#!/usr/bin/env bash
set -euo pipefail

aab_path="${1:-androidApp/build/outputs/bundle/release/androidApp-release.aab}"
r8_metadata="BUNDLE-METADATA/com.android.tools/r8.json"
mapping_metadata="BUNDLE-METADATA/com.android.tools.build.obfuscation/proguard.map"

fail() {
  printf 'Android release-quality check failed: %s\n' "$1" >&2
  exit 1
}

[[ -f "$aab_path" ]] || fail "AAB not found at $aab_path"

command -v python3 >/dev/null 2>&1 || fail "python3 is required"

if ! python3 - "$aab_path" "$r8_metadata" "$mapping_metadata" <<'PY'
import json
import sys
import zipfile

aab_path, r8_metadata, mapping_metadata = sys.argv[1:]

try:
    with zipfile.ZipFile(aab_path) as bundle:
        entries = set(bundle.namelist())
        if r8_metadata not in entries:
            raise ValueError("R8 metadata is missing from the AAB")
        if mapping_metadata not in entries:
            raise ValueError("ProGuard/R8 mapping metadata is missing from the AAB")

        with bundle.open(r8_metadata) as metadata_file:
            metadata = json.load(metadata_file)

    options = metadata.get("options", {})
    required = (
        "isOptimizationsEnabled",
        "isShrinkingEnabled",
        "isObfuscationEnabled",
    )
    disabled = [name for name in required if options.get(name) is not True]
    if disabled:
        raise ValueError("R8 release options are not enabled: " + ", ".join(disabled))
except (OSError, ValueError, json.JSONDecodeError, zipfile.BadZipFile) as error:
    print(f"Android release-quality check failed: {error}", file=sys.stderr)
    raise SystemExit(1)
PY
then
  exit 1
fi

printf 'Android release-quality check passed: optimization, shrinking, and obfuscation are enabled.\n'
