#!/bin/sh

# Xcode Cloud pre-xcodebuild script.
# Bridges Xcode Cloud environment variables into the Gradle-generated KMP
# Secrets.kt file by writing the ignored local.properties before xcodebuild
# invokes the Kotlin framework build phase.

set -eu

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
LOCAL_PROPERTIES="$REPO_ROOT/local.properties"

if [ -z "${WHATSAPP_DETECTOR_API_KEY:-}" ]; then
    echo "error: WHATSAPP_DETECTOR_API_KEY is missing from Xcode Cloud environment variables." >&2
    exit 1
fi

WHATSAPP_DETECTOR_BASE_URL="${WHATSAPP_DETECTOR_BASE_URL:-https://api.contactscleaner.tech}"

case "$WHATSAPP_DETECTOR_BASE_URL" in
    http://*|https://*) ;;
    *)
        echo "error: WHATSAPP_DETECTOR_BASE_URL must start with http:// or https://." >&2
        exit 1
        ;;
esac

upsert_local_property() {
    key="$1"
    value="$2"
    tmp_file="$(mktemp)"

    if [ -f "$LOCAL_PROPERTIES" ]; then
        awk -v key="$key" -v value="$value" '
            BEGIN { written = 0 }
            $0 ~ "^[[:space:]]*" key "[[:space:]]*=" {
                print key "=" value
                written = 1
                next
            }
            { print }
            END {
                if (!written) print key "=" value
            }
        ' "$LOCAL_PROPERTIES" > "$tmp_file"
    else
        printf '%s=%s\n' "$key" "$value" > "$tmp_file"
    fi

    mv "$tmp_file" "$LOCAL_PROPERTIES"
    chmod 600 "$LOCAL_PROPERTIES" || true
}

upsert_local_property "WHATSAPP_DETECTOR_API_KEY" "$WHATSAPP_DETECTOR_API_KEY"
upsert_local_property "WHATSAPP_DETECTOR_BASE_URL" "$WHATSAPP_DETECTOR_BASE_URL"

if ! curl --fail --silent --show-error --max-time 20 \
    -H "X-API-Key: $WHATSAPP_DETECTOR_API_KEY" \
    "$WHATSAPP_DETECTOR_BASE_URL/stats" >/dev/null; then
    echo "error: WhatsApp detector rejected the Xcode Cloud API key or the service is unreachable." >&2
    exit 1
fi

echo "WhatsApp detector config verified for iOS Gradle build."
