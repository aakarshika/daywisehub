#!/bin/sh
# Wipes the debug app's data on the connected Android device and starts it with sample data.
# Usage: scripts/seed-sample.sh [badges|missions|metrics]   (default: badges)
set -e
PKG=com.ideeya.inbalance
SAMPLE=${1:-badges}
adb shell pm clear "$PKG"
adb shell am start -n "$PKG/com.example.todoapp.android.MainActivity" --es sample "$SAMPLE"
