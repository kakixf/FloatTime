#!/bin/bash
export ANDROID_HOME=~/android-sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools

# 下载并安装 Gradle
if [ ! -d ~/gradle-8.7 ]; then
    echo "Downloading Gradle..."
    wget -q https://services.gradle.org/distributions/gradle-8.7-bin.zip -O /tmp/gradle.zip
    unzip -q /tmp/gradle.zip -d ~/
    rm /tmp/gradle.zip
fi

export PATH=$HOME/gradle-8.7/bin:$PATH

# 构建项目
gradle assembleDebug
