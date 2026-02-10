#!/bin/sh

# Xcode Cloud post-clone script
# Installs Java 25 for KMP Gradle builds

set -e

REQUIRED_JAVA_VERSION="25"

echo "=== Setting up Java $REQUIRED_JAVA_VERSION ==="

# Check if Java is already available
if [ -x "/usr/libexec/java_home" ]; then
    EXISTING="$(/usr/libexec/java_home 2>/dev/null || true)"
    if [ -n "$EXISTING" ] && [ -d "$EXISTING" ]; then
        echo "✓ Java already installed at: $EXISTING"
        "$EXISTING/bin/java" -version
        exit 0
    fi
fi

# Install via Homebrew (available on Xcode Cloud)
if command -v brew >/dev/null 2>&1; then
    echo "Installing openjdk@$REQUIRED_JAVA_VERSION via Homebrew..."
    brew install --quiet openjdk@$REQUIRED_JAVA_VERSION
    BREW_JAVA="$(brew --prefix openjdk@$REQUIRED_JAVA_VERSION)/libexec/openjdk.jdk/Contents/Home"
    if [ -d "$BREW_JAVA" ]; then
        echo "✓ Java $REQUIRED_JAVA_VERSION installed at: $BREW_JAVA"
        "$BREW_JAVA/bin/java" -version
        exit 0
    fi
fi

echo "❌ Failed to install Java $REQUIRED_JAVA_VERSION"
exit 1
