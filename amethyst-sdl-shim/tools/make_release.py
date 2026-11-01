#!/usr/bin/env python3
"""Write release/<id>/<id>.json for a built shim jar.
Usage: make_release.py <26.3.json from Mojang> <release jar> <version>"""
import hashlib, json, os, sys
base = json.load(open(sys.argv[1]))
jar, ver = sys.argv[2], sys.argv[3]
data = open(jar, 'rb').read()
vid = '26.3-sdl-shim'
lib_path = f'dev/amethyst/sdlshim/{ver}/sdlshim-{ver}.jar'
doc = {
    'id': vid,
    'inheritsFrom': '26.3',
    'type': 'release',
    'mainClass': base['mainClass'],
    'releaseTime': base['releaseTime'],
    'time': base['time'],
    'javaVersion': base.get('javaVersion', {'component': 'java-runtime-epsilon', 'majorVersion': 25}),
    'arguments': {
        'game': [],
        'jvm': [
            '-javaagent:${library_directory}/' + lib_path,
            '-Dorg.lwjgl.vulkan.libname=amethyst-vulkan-disabled',
            '-Dorg.lwjgl.spvc.libname=libspirv-cross-c-shared.0.dylib',
            '-Damethyst.sdlshim.debug=true',
        ],
    },
    'libraries': [{
        'name': f'dev.amethyst:sdlshim:{ver}',
        'downloads': {'artifact': {
            'path': lib_path,
            'sha1': hashlib.sha1(data).hexdigest(),
            'size': len(data),
            'url': 'https://raw.githubusercontent.com/fxr-lmao/Performance_manus/refs/heads/claude/funny-newton-f07uqe/'
                   f'amethyst-sdl-shim/release/sdlshim-{ver}.jar',
        }},
    }],
}
out = os.path.join(os.path.dirname(jar), vid, vid + '.json')
os.makedirs(os.path.dirname(out), exist_ok=True)
open(out, 'w').write(json.dumps(doc, indent=2) + '\n')
print(out, doc['libraries'][0]['downloads']['artifact']['sha1'], len(data))
