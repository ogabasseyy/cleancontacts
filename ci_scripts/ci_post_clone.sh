#!/bin/sh

# Xcode Cloud post-clone script
# Sets up Java for KMP Gradle builds

set -e

echo "Setting up Java for Xcode Cloud..."

# Xcode Cloud provides Java 21 at this path
export JAVA_HOME="/Applications/Xcode.app/Contents/Developer/usr/libexec/java/Home"

# Verify Java is available
if [ -d "$JAVA_HOME" ]; then
    echo "✓ JAVA_HOME set to: $JAVA_HOME"
    "$JAVA_HOME/bin/java" -version
else
    echo "⚠️  Java not found at $JAVA_HOME, trying alternate paths..."
    # Fallback: search for Java installations
    if [ -x "/usr/libexec/java_home" ]; then
        export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
        echo "✓ Found Java via java_home: $JAVA_HOME"
    else
        echo "❌ Java not found. Build may fail."
        exit 1
    fi
fi

echo "Java setup complete."
