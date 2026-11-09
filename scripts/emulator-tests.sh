#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/.."
NOVA_SDK="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
NOVA_SERIAL="${NOVA_SERIAL:-emulator-5554}"
ADB="$NOVA_SDK/platform-tools/adb"
mkdir -p artifacts/screens
"$ADB" -s "$NOVA_SERIAL" install -r app/build/outputs/apk/debug/app-debug.apk
"$ADB" -s "$NOVA_SERIAL" install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
if [ "$#" -gt 0 ]; then
    "$ADB" -s "$NOVA_SERIAL" shell am instrument -w -r -e class "$1" dev.nova.launcher.test/androidx.test.runner.AndroidJUnitRunner | tee artifacts/instrumentation.log
else
    "$ADB" -s "$NOVA_SERIAL" shell am instrument -w -r dev.nova.launcher.test/androidx.test.runner.AndroidJUnitRunner | tee artifacts/instrumentation.log
fi
python3 - <<'PY'
from pathlib import Path
import re
log=Path('artifacts/instrumentation.log').read_text()
if re.search(r'FAILURES!!!|INSTRUMENTATION_FAILED|shortMsg=',log) or not re.search(r'OK \(\d+ tests?\)',log):
    raise SystemExit('Instrumentation failed; inspect artifacts/instrumentation.log')
PY
"$ADB" -s "$NOVA_SERIAL" pull /sdcard/Android/data/dev.nova.launcher/files/ artifacts/screens/ || true
