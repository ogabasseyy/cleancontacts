#!/bin/sh

# Xcode Cloud post-clone script
# Installs Java for KMP Gradle builds

set -e

echo "=== Installing Java for Xcode Cloud ==="

# Install Java via Homebrew (Homebrew is pre-installed on Xcode Cloud)
if command -v brew >/dev/null 2>&1; then
    # Try Java 25, fall back to 21
    for VERSION in 25 21; do
        echo "Trying openjdk@$VERSION..."
        if brew install openjdk@$VERSION 2>/dev/null; then
            JAVA_PATH="$(brew --prefix openjdk@$VERSION)/libexec/openjdk.jdk/Contents/Home"
            if [ -d "$JAVA_PATH" ]; then
                echo "✓ Installed openjdk@$VERSION at: $JAVA_PATH"
                "$JAVA_PATH/bin/java" -version
                exit 0
            fi
        fi
    done
fi

echo "⚠️ Could not install Java via Homebrew. Build phase will attempt fallback."
exit 0
