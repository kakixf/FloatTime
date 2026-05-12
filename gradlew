#!/bin/bash

ANDROID_HOME="${ANDROID_HOME:-/Users/tyc/android-sdk}"
export ANDROID_HOME

GRADLE_HOME="/opt/homebrew/var/homebrew/tmp/.cellar/gradle/9.5.0/libexec"
exec "$GRADLE_HOME/bin/gradle" "$@"
