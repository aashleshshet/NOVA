#!/usr/bin/env python3
"""Remove only NOVA's test provider; confirm persisted widget cleanup after real uninstall."""
import os, pathlib, subprocess, time, json
sdk=os.environ.get('ANDROID_HOME',str(pathlib.Path.home()/'Library/Android/sdk'))
serial=os.environ.get('NOVA_SERIAL','emulator-5554')
if not serial.startswith('emulator-'): raise SystemExit('Use a disposable test emulator')
adb=[sdk+'/platform-tools/adb','-s',serial]
def run(*args): return subprocess.run(adb+list(args),capture_output=True,check=True).stdout
output=run('shell','am','instrument','-w','-r','-e','class','dev.nova.launcher.RestartScenarioTest#aSeedPersistentLayout','dev.nova.launcher.test/androidx.test.runner.AndroidJUnitRunner')
assert b'OK (1 test)' in output,output
run('shell','am','start','-n','dev.nova.launcher/.MainActivity');time.sleep(2)
def state():
    data=run('shell','run-as','dev.nova.launcher','cat','files/datastore/nova.preferences_pb')
    start=data.index(b'{');return json.JSONDecoder().raw_decode(data[start:].decode())[0]
# StateStore's single JSON string is encoded in the Preferences protobuf.
before=state();assert before['widgets'],before
try:
    assert b'Success' in run('uninstall','dev.nova.launcher.test')
    for _ in range(30):
        after=state()
        if not after['widgets']: break
        time.sleep(.5)
    assert not after['widgets'],after
    result={'provider_uninstall':'PASS','persistent_widget_cleanup':'PASS'}
    pathlib.Path('artifacts/removal-results.json').write_text(json.dumps(result,indent=2));print(result)
finally:
    run('install','-r','app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk')
