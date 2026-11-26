#!/bin/sh
# Local toolchain for this project: JDK and Gradle caches all live in this folder.
# Usage:  . ./env.sh          (then use ./gradlew as normal)
cd "$(dirname "$0")" || exit 1

if [ ! -x "$PWD/tools/jdk/bin/java" ]; then
	echo "tools/jdk missing. Re-download the Temurin JDK into tools/ first." >&2
	return 1 2>/dev/null || exit 1
fi

export JAVA_HOME="$PWD/tools/jdk"
export GRADLE_USER_HOME="$PWD/.gradle-home"
export PATH="$JAVA_HOME/bin:$PATH"
