// Amethyst SDL shim: SDL3 API surface used by Minecraft 26.3, implemented over GLFW.
// See dev.amethyst.sdlshim.Sdl. Signatures match LWJGL 3.4.3 exactly.
package org.lwjgl.sdl;

import dev.amethyst.sdlshim.Sdl;
import java.nio.ByteBuffer;
import org.lwjgl.system.SharedLibrary;

public final class SDL {
    private SDL() {}

    private static final SharedLibrary LIBRARY = new SharedLibrary() {
        @Override public String getName() { return "SDL3 (Amethyst GLFW shim " + Sdl.VERSION + ")"; }
        @Override public String getPath() { return null; }
        @Override public long getFunctionAddress(ByteBuffer name) { return 0L; }
        @Override public long address() { return 0x5D1L; }
        @Override public void free() {}
    };

    public static SharedLibrary getLibrary() { return LIBRARY; }
}
