#!/bin/sh
# Opens the iOS project in Xcode. You pick the phone or simulator there and press Run.
#   scripts/ios.sh device   open Xcode
#   scripts/ios.sh live     open Xcode, then Run again in Xcode every time a source file is saved
#   scripts/ios.sh deploy   build from the terminal and run on the phone or simulator picked in Xcode

cd "$(dirname "$0")/.." || exit 1

PROJECT=$PWD/iosApp/iosApp.xcodeproj
WATCH="shared/src/commonMain shared/src/iosMain iosApp/iosApp"
# Xcode saves the toolbar's scheme and run destination here.
STATE=$PROJECT/project.xcworkspace/xcuserdata/$USER.xcuserdatad/UserInterfaceState.xcuserstate
DERIVED=build/xcode

# Prints "<scheme> <device id> <platform>" for what's picked in Xcode's toolbar, e.g. "iosApp 00008110-… iphoneos".
picked() {
  python3 -I - "$STATE" <<'EOF'
import plistlib, sys
objs = plistlib.load(open(sys.argv[1], 'rb'))['$objects']
def get(x):
    x = objs[x.data] if isinstance(x, plistlib.UID) else x
    if isinstance(x, dict) and 'NS.keys' in x:
        return {get(k): get(v) for k, v in zip(x['NS.keys'], x['NS.objects'])}
    return x.get('NS.string', x) if isinstance(x, dict) else x
found = {}
for o in objs:
    if isinstance(o, dict) and 'NS.keys' in o:
        for k, v in zip(o['NS.keys'], o['NS.objects']):
            if objs[k.data] in ('ActiveScheme', 'IDERunContextRecentsLastUsedRunDestinationBySchemeKey'):
                found[objs[k.data]] = get(v)
scheme = found['ActiveScheme']['IDENameString']
device, platform, _ = found['IDERunContextRecentsLastUsedRunDestinationBySchemeKey'][scheme].rsplit('_', 2)
if device.startswith('dvtdevice'):
    sys.exit('Xcode has "Any iOS Device" picked. Pick a phone or a simulator at the top of Xcode.')
print(scheme, device, platform)
EOF
}

deploy() {
  [ -f "$STATE" ] || { echo "Open the project in Xcode once (make ios-device) and pick a phone or simulator."; exit 1; }
  # shellcheck disable=SC2046
  set -- $(picked) || exit 1
  [ $# -eq 3 ] || exit 1
  scheme=$1 id=$2 platform=$3
  name=$(xcrun xctrace list devices 2>/dev/null | grep -F "$id" | sed 's/ (.*//')
  echo "Building $scheme for ${name:-$id}…"
  xcodebuild -project "$PROJECT" -scheme "$scheme" -configuration Debug -destination "id=$id" \
    -derivedDataPath "$DERIVED" -allowProvisioningUpdates -quiet build || exit 1
  app=$(ls -d "$DERIVED/Build/Products/Debug-$platform"/*.app | head -1)
  bundle=$(/usr/libexec/PlistBuddy -c 'Print CFBundleIdentifier' "$app/Info.plist")
  echo "Installing $(basename "$app") on ${name:-$id}…"
  if [ "$platform" = iphonesimulator ]; then
    xcrun simctl boot "$id" 2>/dev/null
    open -a Simulator
    xcrun simctl install "$id" "$app" || exit 1
    xcrun simctl launch --terminate-running-process "$id" "$bundle" >/dev/null || exit 1
  else
    xcrun devicectl device install app --device "$id" "$app" >/dev/null || exit 1
    xcrun devicectl device process launch --device "$id" --terminate-existing "$bundle" >/dev/null \
      || { echo "Installed, but it didn't open. Unlock the phone and open it there."; exit 1; }
  fi
  echo "Running on ${name:-$id}."
}

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
  deploy) deploy ;;
  *) echo "Usage: $0 device|live|deploy"; exit 1 ;;
esac
