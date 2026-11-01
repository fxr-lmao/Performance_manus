// Amethyst SDL shim: SDL3 API surface used by Minecraft 26.3, implemented over GLFW.
// See dev.amethyst.sdlshim.Sdl. Signatures match LWJGL 3.4.3 exactly.
package org.lwjgl.sdl;

import java.nio.IntBuffer;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;

public final class SDLStdinc {
    private SDLStdinc() {}
    public static void SDL_free(IntBuffer mem) { MemoryUtil.memFree((java.nio.Buffer) mem); }
    public static void SDL_free(PointerBuffer mem) { MemoryUtil.memFree((org.lwjgl.system.CustomBuffer<?>) mem); }
}
