#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
if [ -d .tooling/jdk-17.0.21+9/Contents/Home ]; then
    export JAVA_HOME="$PWD/.tooling/jdk-17.0.21+9/Contents/Home"
fi
export GRADLE_USER_HOME="$PWD/.tooling/gradle-home"
if [ -x .tooling/gradle-8.13/bin/gradle ]; then
    exec .tooling/gradle-8.13/bin/gradle "$@" --console=plain
fi
exec ./gradlew "$@" --console=plain
