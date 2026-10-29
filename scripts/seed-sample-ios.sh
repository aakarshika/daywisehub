#!/bin/sh
# Reinstalls the debug app on the booted iOS simulator with a clean database and starts it with sample data.
# Build it first (Xcode, or xcodebuild into DerivedData). The status bar is set to 9:41, full battery, for screenshots.
# Usage: scripts/seed-sample-ios.sh path/to/iosApp.app [badges|missions|metrics|screenshots]   (default: screenshots)
set -e
APP=$1
SAMPLE=${2:-screenshots}
BUNDLE=com.ideeya.inbalance
xcrun simctl terminate booted "$BUNDLE" 2>/dev/null || true
xcrun simctl uninstall booted "$BUNDLE"
xcrun simctl install booted "$APP"
xcrun simctl status_bar booted override --time 9:41 --batteryState charged --batteryLevel 100 --cellularBars 4 --wifiBars 3
xcrun simctl launch booted "$BUNDLE" -sample "$SAMPLE" -hideDevClock YES
