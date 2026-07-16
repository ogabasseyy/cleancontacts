#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
verifier="$repo_root/scripts/verify-android-release-quality.sh"
fixture_root="$(mktemp -d)"
trap 'rm -rf "$fixture_root"' EXIT

create_aab() {
  local output="$1"
  local optimizations="$2"
  local shrinking="$3"
  local obfuscation="$4"
  local include_mapping="$5"

  python3 - "$output" "$optimizations" "$shrinking" "$obfuscation" "$include_mapping" <<'PY'
import json
import sys
import zipfile

output, optimizations, shrinking, obfuscation, include_mapping = sys.argv[1:]
metadata = {
    "options": {
        "isOptimizationsEnabled": optimizations == "true",
        "isShrinkingEnabled": shrinking == "true",
        "isObfuscationEnabled": obfuscation == "true",
    }
}

with zipfile.ZipFile(output, "w") as bundle:
    bundle.writestr(
        "BUNDLE-METADATA/com.android.tools/r8.json",
        json.dumps(metadata),
    )
    if include_mapping == "true":
        bundle.writestr(
            "BUNDLE-METADATA/com.android.tools.build.obfuscation/proguard.map",
            "com.example.Main -> a:\n",
        )
PY
}

expect_success() {
  local description="$1"
  local aab="$2"

  if ! "$verifier" "$aab"; then
    printf 'FAIL: expected success: %s\n' "$description" >&2
    exit 1
  fi
}

expect_failure() {
  local description="$1"
  local aab="$2"

  if "$verifier" "$aab"; then
    printf 'FAIL: expected failure: %s\n' "$description" >&2
    exit 1
  fi
}

optimized_aab="$fixture_root/optimized.aab"
unoptimized_aab="$fixture_root/unoptimized.aab"
unshrunk_aab="$fixture_root/unshrunk.aab"
unobfuscated_aab="$fixture_root/unobfuscated.aab"
missing_mapping_aab="$fixture_root/missing-mapping.aab"

create_aab "$optimized_aab" true true true true
create_aab "$unoptimized_aab" false true true true
create_aab "$unshrunk_aab" true false true true
create_aab "$unobfuscated_aab" true true false true
create_aab "$missing_mapping_aab" true true true false

expect_success "fully optimized release artifact" "$optimized_aab"
expect_failure "R8 optimization disabled" "$unoptimized_aab"
expect_failure "resource/code shrinking disabled" "$unshrunk_aab"
expect_failure "obfuscation disabled" "$unobfuscated_aab"
expect_failure "mapping metadata absent" "$missing_mapping_aab"
expect_failure "AAB absent" "$fixture_root/does-not-exist.aab"

printf 'All Android release-quality verifier tests passed.\n'
