#!/usr/bin/env bash
# Build the Amethyst SDL shim jar.
#
# Needs:
#   JDK 25+ (javac/jar)                       -> JAVA_HOME, or javac/jar on PATH
#   The lwjgl.jar bundled in the Amethyst IPA -> AMETHYST_LWJGL (Payload/AngelAuraAmethyst.app/libs/lwjgl.jar)
#   LWJGL 3.4.3 SDL bindings (struct classes) -> LWJGL_SDL (downloaded from Maven Central if unset)
#   LWJGL 3.3.3 SPIRV-Cross bindings          -> LWJGL_SPVC (downloaded from Maven Central if unset)
set -euo pipefail
cd "$(dirname "$0")"
VERSION=$(sed -n 's/.*String VERSION = "\(.*\)";.*/\1/p' src/dev/amethyst/sdlshim/Sdl.java)
JAVAC=${JAVA_HOME:+$JAVA_HOME/bin/}javac
JAR=${JAVA_HOME:+$JAVA_HOME/bin/}jar
if [ -z "${AMETHYST_LWJGL:-}" ]; then
  echo "set AMETHYST_LWJGL to the lwjgl.jar from the Amethyst IPA (libs/lwjgl.jar)" >&2
  exit 1
fi
BUILD=$PWD/build
rm -rf "$BUILD" && mkdir -p "$BUILD/classes" "$BUILD/deps"

if [ -z "${LWJGL_SDL:-}" ]; then
  LWJGL_SDL=$BUILD/deps/lwjgl-sdl-3.4.3.jar
  curl -sSfL -o "$LWJGL_SDL" https://repo1.maven.org/maven2/org/lwjgl/lwjgl-sdl/3.4.3/lwjgl-sdl-3.4.3.jar
fi
LWJGL_SDL=$(cd "$(dirname "$LWJGL_SDL")" && pwd)/$(basename "$LWJGL_SDL")
if [ -z "${LWJGL_SPVC:-}" ]; then
  LWJGL_SPVC=$BUILD/deps/lwjgl-spvc-3.3.3.jar
  curl -sSfL -o "$LWJGL_SPVC" https://repo1.maven.org/maven2/org/lwjgl/lwjgl-spvc/3.3.3/lwjgl-spvc-3.3.3.jar
fi
LWJGL_SPVC=$(cd "$(dirname "$LWJGL_SPVC")" && pwd)/$(basename "$LWJGL_SPVC")

# 1) Compile. The real SDL jar supplies the struct classes at compile time; our sources replace the
#    SDL function classes, and compiling against Amethyst's own lwjgl.jar catches any API mismatch.
"$JAVAC" --release 25 -nowarn -encoding UTF-8 -d "$BUILD/classes" \
  -cp "$LWJGL_SDL:$AMETHYST_LWJGL" $(find src -name '*.java')

# 2) Add the LWJGL 3.4.3 struct/constant classes Minecraft needs (checked to link against LWJGL 3.3.3).
while read -r cls || [ -n "$cls" ]; do
  [ -z "$cls" ] && continue
  [ -f "$BUILD/classes/$cls.class" ] && continue   # never overwrite our own replacements
  unzip -q -o "$LWJGL_SDL" "$cls.class" -d "$BUILD/classes"
done < keep-classes.txt

# 2b) Minecraft 26.3 compiles every shader GLSL -> SPIR-V (shaderc) -> GLSL (SPIRV-Cross), even on OpenGL.
#     Amethyst ships libspirv-cross-c-shared but not LWJGL's Java bindings for it; the 3.3.3 bindings link
#     against Amethyst's LWJGL core and every function they need is exported by Amethyst's iOS dylib.
unzip -q -o "$LWJGL_SPVC" 'org/lwjgl/util/spvc/*' -d "$BUILD/classes"

# 3) Jar with the agent manifest.
printf 'Premain-Class: dev.amethyst.sdlshim.Agent\nImplementation-Title: Amethyst SDL shim\nImplementation-Version: %s\n' \
  "$VERSION" > "$BUILD/MANIFEST.MF"
cp LICENSE-LWJGL.txt "$BUILD/classes/" 2>/dev/null || true
mkdir -p dist
"$JAR" --create --file "dist/amethyst-sdlshim-$VERSION.jar" --manifest "$BUILD/MANIFEST.MF" -C "$BUILD/classes" .
echo "built dist/amethyst-sdlshim-$VERSION.jar"
