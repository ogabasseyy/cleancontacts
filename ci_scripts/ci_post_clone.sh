#!/bin/sh

# Xcode Cloud post-clone script
# Sets up Java 25 for KMP Gradle builds

set -e

REQUIRED_JAVA_VERSION="25"

echo "Setting up Java $REQUIRED_JAVA_VERSION for Xcode Cloud..."

# Try system java_home first
if [ -x "/usr/libexec/java_home" ]; then
    JAVA_HOME_PATH="$(/usr/libexec/java_home -v $REQUIRED_JAVA_VERSION 2>/dev/null || true)"
    if [ -n "$JAVA_HOME_PATH" ] && [ -d "$JAVA_HOME_PATH" ]; then
        export JAVA_HOME="$JAVA_HOME_PATH"
        echo "✓ Found Java $REQUIRED_JAVA_VERSION at: $JAVA_HOME"
        "$JAVA_HOME/bin/java" -version
        echo "Java setup complete."
        exit 0
    fi
fi

# Fallback: Xcode bundled Java
XCODE_JAVA="/Applications/Xcode.app/Contents/Developer/usr/libexec/java/Home"
if [ -d "$XCODE_JAVA" ]; then
    export JAVA_HOME="$XCODE_JAVA"
    echo "⚠️  Java $REQUIRED_JAVA_VERSION not found, using Xcode bundled Java:"
    "$JAVA_HOME/bin/java" -version
    echo "Java setup complete (fallback)."
    exit 0
fi

# Install via Homebrew if available
if command -v brew >/dev/null 2>&1; then
    echo "Installing Java $REQUIRED_JAVA_VERSION via Homebrew..."
    brew install openjdk@$REQUIRED_JAVA_VERSION
    export JAVA_HOME="$(brew --prefix openjdk@$REQUIRED_JAVA_VERSION)/libexec/openjdk.jdk/Contents/Home"
    echo "✓ Installed Java $REQUIRED_JAVA_VERSION at: $JAVA_HOME"
    "$JAVA_HOME/bin/java" -version
    echo "Java setup complete."
    exit 0
fi

echo "❌ Java $REQUIRED_JAVA_VERSION not found and cannot be installed."
exit 1
