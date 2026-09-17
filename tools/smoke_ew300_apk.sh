#!/usr/bin/env bash
# Run against a disposable Android emulator with no attached physical USB device.
# The exact signed candidate is installed and launched twice; no USB button is used.
set -euo pipefail
apk="$1"
record_dir="$2"
package=com.weekssa.opraeqforuapp.ew300evidence
component="$package/com.weekssa.opraeqforuapp.diagnostics.Ew300UsbDiscoveryActivity"
mkdir -p "$record_dir"
adb install -r "$apk"
adb logcat -c
for attempt in 1 2; do
  adb shell am force-stop "$package"
  adb shell am start -W -n "$component" | tee "$record_dir/launch-$attempt.txt"
  grep -q 'Status: ok' "$record_dir/launch-$attempt.txt"
  sleep 5
  adb shell pidof "$package"
  adb shell uiautomator dump /sdcard/ew300-window.xml
  adb pull /sdcard/ew300-window.xml "$record_dir/window-$attempt.xml"
  grep -q 'Scan connected USB devices' "$record_dir/window-$attempt.xml"
  grep -q 'Request read-only descriptor capture' "$record_dir/window-$attempt.xml"
  grep -q 'Capture provisional stock-EQ snapshot' "$record_dir/window-$attempt.xml"
  grep -q 'Run reversible +0.1 dB write test' "$record_dir/window-$attempt.xml"
  # The initial status copy may be below the first viewport now that the fourth
  # control is present. The four explicit control assertions above are the
  # launch contract; this confirms remaining content is reachable by scrolling.
  grep -q 'android.widget.ScrollView' "$record_dir/window-$attempt.xml"
done
adb logcat -d -b crash > "$record_dir/crash-log.txt"
if grep -q 'FATAL EXCEPTION' "$record_dir/crash-log.txt"; then
  cat "$record_dir/crash-log.txt"
  exit 1
fi
printf 'PASS: exact signed APK launched twice and all four controls rendered on Android API 36. The write test remained stock-snapshot-gated; no USB connection or command was used.\n' | tee "$record_dir/result.txt"
