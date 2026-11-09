#!/usr/bin/env python3
"""Repeatable smoke profile; debug/emulated numbers are not device benchmarks."""
import os, pathlib, subprocess, json, re, time, statistics, xml.etree.ElementTree as ET
serial=os.environ.get('NOVA_SERIAL','emulator-5554')
if not serial.startswith('emulator-'): raise SystemExit('Use a disposable test emulator')
sdk=os.environ.get('ANDROID_HOME',str(pathlib.Path.home()/'Library/Android/sdk'))
adb=[sdk+'/platform-tools/adb','-s',serial]
def run(*args): return subprocess.run(adb+list(args),capture_output=True,text=True,check=True).stdout
out=pathlib.Path('artifacts');out.mkdir(exist_ok=True)
times=[]
home=run('shell','cmd','package','resolve-activity','--brief','-a','android.intent.action.MAIN','-c','android.intent.category.HOME').strip().splitlines()[-1]
# HOME auto-restarts after force-stop; temporarily choose an actual alternate HOME.
choices=run('shell','cmd','package','query-activities','--brief','-a','android.intent.action.MAIN','-c','android.intent.category.HOME')
alternate=next(line.strip() for line in choices.splitlines() if '/' in line and 'dev.nova.launcher' not in line and 'FallbackHome' not in line)
try:
    run('shell','cmd','package','set-home-activity',alternate)
    for _ in range(5):
        run('shell','input','keyevent','3')
        run('shell','am','force-stop','dev.nova.launcher')
        result=run('shell','am','start','-W','-n','dev.nova.launcher/.MainActivity')
        assert 'LaunchState: COLD' in result, result
        elapsed=int(re.search(r'TotalTime: (\d+)',result)[1]); assert elapsed>0
        times.append(elapsed)
        time.sleep(1)
finally:
    run('shell','cmd','package','set-home-activity',home)
run('shell','dumpsys','gfxinfo','dev.nova.launcher','reset')
run('shell','uiautomator','dump','/sdcard/nova-window.xml')
root=ET.fromstring(run('shell','cat','/sdcard/nova-window.xml'))
node=next(n for n in root.iter('node') if n.get('text')=='All apps')
x1,y1,x2,y2=map(int,re.findall(r'\d+',node.get('bounds')))
run('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2))
time.sleep(1)
size=re.findall(r'(\d+)x(\d+)',run('shell','wm','size'))[-1];w,h=map(int,size)
for _ in range(8):
    run('shell','input','swipe',str(w//2),str(h*4//5),str(w//2),str(h//3),'250')
    run('shell','input','swipe',str(w//2),str(h//3),str(w//2),str(h*4//5),'250')
(out/'gfxinfo.txt').write_text(run('shell','dumpsys','gfxinfo','dev.nova.launcher'))
(out/'meminfo.txt').write_text(run('shell','dumpsys','meminfo','dev.nova.launcher'))
result={'environment':'API 36.1 arm64 emulator, SwiftShader, debug build','cold_start_total_ms':times,'median_ms':statistics.median(times),'scroll_swipes':16}
(out/'performance.json').write_text(json.dumps(result,indent=2));print(json.dumps(result,indent=2))
