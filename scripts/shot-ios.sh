#!/bin/sh
# Store screenshots on iOS. Each scene is a clean reinstall with the screenshots sample, on Thursday
# 29 Oct 2026 (ScreenshotSample.ANCHOR), the status bar at 9:41 and the dev clock hidden.
#   scripts/shot-ios.sh scene path/to/iosApp.app [launch args...]   reinstall and start a scene
#   scripts/shot-ios.sh snap path/to/file.png                        save the screen
# Scene arguments (ShotOptions): -shotStage pies|flags|cake  -shotToday morning|midday|evening
#   -shotOffer none|lighter|stepup|both  -shotWeekNotes YES  -shotMilestoneDue YES  -shotDate 2026-10-15
# SIM picks the simulator (a name or UDID); it defaults to the booted one.
set -e
SIM=${SIM:-booted}
BUNDLE=com.ideeya.inbalance

case "${1:-}" in
  scene)
    APP=$2
    shift 2
    xcrun simctl terminate "$SIM" "$BUNDLE" 2>/dev/null || true
    xcrun simctl uninstall "$SIM" "$BUNDLE" 2>/dev/null || true
    xcrun simctl install "$SIM" "$APP"
    xcrun simctl status_bar "$SIM" override --time 9:41 --dataNetwork wifi --wifiBars 3 --cellularMode active --cellularBars 4 --batteryState discharging --batteryLevel 100
    case " $* " in *" -shotDate "*) DATE= ;; *) DATE="-shotDate 2026-10-29" ;; esac
    # shellcheck disable=SC2086
    xcrun simctl launch "$SIM" "$BUNDLE" -sample screenshots -hideDevClock YES $DATE "$@" >/dev/null
    ;;
  snap)
    mkdir -p "$(dirname "$2")"
    xcrun simctl io "$SIM" screenshot --type=png "$2" >/dev/null 2>&1
    echo "$2"
    ;;
  *) echo "Usage: $0 scene app [args...] | snap file.png"; exit 1 ;;
esac
