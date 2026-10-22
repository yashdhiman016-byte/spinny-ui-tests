#!/bin/sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
TOOLS_DIR="$ROOT_DIR/.tools"
MAVEN_HOME="$TOOLS_DIR/apache-maven-3.10.0"

if [ -d "$TOOLS_DIR/jdk-17.0.20.1+1/Contents/Home" ]; then
    JAVA_HOME="$TOOLS_DIR/jdk-17.0.20.1+1/Contents/Home"
elif [ -d "$TOOLS_DIR/jdk-17.0.20.1+1" ]; then
    JAVA_HOME="$TOOLS_DIR/jdk-17.0.20.1+1"
fi

if [ -n "${JAVA_HOME:-}" ]; then
    export JAVA_HOME
    PATH="$JAVA_HOME/bin:$PATH"
    export PATH
fi

if [ -x "$MAVEN_HOME/bin/mvn" ]; then
    exec "$MAVEN_HOME/bin/mvn" -Dmaven.repo.local="$TOOLS_DIR/m2" "$@"
fi

if command -v mvn >/dev/null 2>&1; then
    exec mvn "$@"
fi

printf '%s\n' "Maven was not found. Install Maven 3.9+ or provide .tools/apache-maven-3.10.0/bin/mvn." >&2
exit 127
