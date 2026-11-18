#!/usr/bin/env bash
set -u
mkdir -p evidence
result=0
bash gradlew --no-daemon --console=plain connectedDebugAndroidTest || result=$?
adb pull /sdcard/Download/charbel-events-evidence evidence/ || true
adb logcat -d -s CampusExercise:I CampusEvents:W '*:S' > evidence/logcat.txt || true
exit "$result"
