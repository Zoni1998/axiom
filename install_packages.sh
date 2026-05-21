#!/bin/bash
# install_packages.sh
# This script installs JDK 17, Android SDK Command-line tools, and the necessary Android SDK packages
# to build OpenDroid on Debian/Ubuntu systems.

set -e

echo "================================================"
echo "  OpenDroid Build Dependency Installer for Debian"
echo "================================================"

# 1. Update and install JDK 17 & essential tools
echo "[1/4] Installing OpenJDK 17 and dependencies..."
sudo apt-get update
sudo apt-get install -y openjdk-17-jdk wget unzip curl git

# 2. Define Android SDK installation path
ANDROID_HOME="$HOME/Android/Sdk"
mkdir -p "$ANDROID_HOME/cmdline-tools"

echo "[2/4] Downloading Android Command Line Tools..."
TEMP_ZIP=$(mktemp)
# Stable download link for Android cmdline-tools (version 11076708)
wget -O "$TEMP_ZIP" "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"

echo "Extracting tools..."
unzip -q "$TEMP_ZIP" -d "$ANDROID_HOME/cmdline-tools"
rm -f "$TEMP_ZIP"

# Note: The zip extracts to 'cmdline-tools', but it needs to be inside 'latest' for sdkmanager to work
if [ -d "$ANDROID_HOME/cmdline-tools/latest" ]; then
    rm -rf "$ANDROID_HOME/cmdline-tools/latest"
fi
mv "$ANDROID_HOME/cmdline-tools/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest"

# Export variables for the current script execution session
export ANDROID_HOME
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"

# 3. Accept Licenses & install target SDK platforms
echo "[3/4] Installing Android SDK platforms & build-tools (API 34)..."
# Accept all SDK licenses automatically
yes | sdkmanager --licenses

# Install platforms, build-tools, and platform-tools
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"

# 4. Configure environment variables in .bashrc if not already present
echo "[4/4] Configuring environment variables..."
BASHRC="$HOME/.bashrc"

if ! grep -q "ANDROID_HOME" "$BASHRC"; then
    echo "" >> "$BASHRC"
    echo "# Android SDK environment variables" >> "$BASHRC"
    echo "export ANDROID_HOME=\$HOME/Android/Sdk" >> "$BASHRC"
    echo "export PATH=\$ANDROID_HOME/cmdline-tools/latest/bin:\$ANDROID_HOME/platform-tools:\$PATH" >> "$BASHRC"
    echo "Environment variables added to $BASHRC."
else
    echo "Android SDK environment variables already present in $BASHRC."
fi

echo "================================================"
echo "Installation complete!"
echo "Please restart your terminal or run: source ~/.bashrc"
echo "You can then build the app by running: ./gradlew assembleDebug"
echo "================================================"
