#!/bin/sh
# Opens the iOS project in Xcode. You pick the phone or simulator there and press Run.
#   scripts/ios.sh device   open Xcode
#   scripts/ios.sh live     open Xcode, then Run again in Xcode every time a source file is saved

cd "$(dirname "$0")/.." || exit 1

PROJECT=$PWD/iosApp/iosApp.xcodeproj
WATCH="shared/src/commonMain shared/src/iosMain iosApp/iosApp"

# Stop, then Run, on whatever scheme and target are picked in Xcode.
run_in_xcode() {
  osascript >/dev/null 2>&1 <<EOF
tell application "Xcode"
  set ws to first workspace document whose path contains "$PROJECT"
  stop ws
  delay 1
  run ws
end tell
EOF
}

live() {
  stamp=build/.ios-live-stamp
  mkdir -p build
  touch "$stamp"
  echo "Watching $WATCH"
  echo "Save a file and Xcode runs again on that target. Ctrl+C to stop."
  echo "First save only: macOS asks to let your terminal control Xcode. Allow it."
  while :; do
    sleep 2
    # shellcheck disable=SC2086
    if [ -n "$(find $WATCH -type f -newer "$stamp" -print -quit 2>/dev/null)" ]; then
      touch "$stamp"
      sleep 1 # let a multi-file save finish
      if run_in_xcode; then
        echo "$(date +%H:%M:%S) Saved. Xcode is running it again."
      else
        echo "$(date +%H:%M:%S) Couldn't reach Xcode. Is the project still open there?"
      fi
    fi
  done
}

case "${1:-}" in
  device)
    open "$PROJECT"
    echo "Xcode is open. Pick your iPhone or a simulator at the top and press Run (Cmd+R)."
    echo "First run on a phone: Settings -> General -> VPN & Device Management -> trust the cert."
    ;;
  live)
    open "$PROJECT"
    echo "Xcode is open. Pick your iPhone or a simulator and press Run (Cmd+R) once."
    echo ""
    live
    ;;
  *) echo "Usage: $0 device|live"; exit 1 ;;
esac
