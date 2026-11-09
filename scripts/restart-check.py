#!/usr/bin/env python3
"""Run only against an explicitly chosen test emulator; never reboots a physical device."""
import os, subprocess, time, pathlib, json
sdk=os.environ.get('ANDROID_HOME',str(pathlib.Path.home()/'Library/Android/sdk'))
serial=os.environ.get('NOVA_SERIAL','emulator-5554')
if not serial.startswith('emulator-'): raise SystemExit('Refusing to reboot a non-emulator target')
adb=[sdk+'/platform-tools/adb','-s',serial]
def run(*args):
    p=subprocess.run(adb+list(args),capture_output=True,text=True,check=True)
    return p.stdout.strip()
def test(method):
    output=run('shell','am','instrument','-w','-r','-e','class','dev.nova.launcher.RestartScenarioTest#'+method,'dev.nova.launcher.test/androidx.test.runner.AndroidJUnitRunner')
    if 'OK (1 test)' not in output: raise RuntimeError(output)
    return 'PASS'
result={'seed':test('aSeedPersistentLayout')}
run('shell','cmd','package','set-home-activity','dev.nova.launcher/.MainActivity')
run('shell','am','force-stop','dev.nova.launcher')
run('shell','am','start','-a','android.intent.action.MAIN','-c','android.intent.category.HOME')
result['process_death']=test('bVerifyPersistentLayout')
run('reboot')
for _ in range(90):
    try:
        if run('shell','getprop','sys.boot_completed')=='1': break
    except subprocess.CalledProcessError: pass
    time.sleep(1)
else: raise RuntimeError('Emulator did not boot within 90 seconds')
run('shell','input','keyevent','82')
result['reboot']=test('bVerifyPersistentLayout')
result['default_home']=run('shell','cmd','package','resolve-activity','--brief','-a','android.intent.action.MAIN','-c','android.intent.category.HOME')
assert 'dev.nova.launcher' in result['default_home'], result
pathlib.Path('artifacts/restart-results.json').write_text(json.dumps(result,indent=2))
print(json.dumps(result,indent=2))
