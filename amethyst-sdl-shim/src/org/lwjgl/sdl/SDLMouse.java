// Amethyst SDL shim: SDL3 API surface used by Minecraft 26.3, implemented over GLFW.
// See dev.amethyst.sdlshim.Sdl. Signatures match LWJGL 3.4.3 exactly.
package org.lwjgl.sdl;

import dev.amethyst.sdlshim.Sdl;
import java.nio.FloatBuffer;

public final class SDLMouse {
    private SDLMouse() {}
    public static long SDL_CreateSystemCursor(int id) { return 0xC0500L + id; }
    public static long SDL_GetDefaultCursor() { return 0xC0500L; }
    public static int SDL_GetGlobalMouseState(FloatBuffer x, FloatBuffer y) { return Sdl.mouseState(x, y); }
    public static int SDL_GetMouseState(FloatBuffer x, FloatBuffer y) { return Sdl.mouseState(x, y); }
    public static boolean SDL_SetCursor(long cursor) { return true; }
    public static boolean SDL_SetWindowRelativeMouseMode(long window, boolean enabled) { return Sdl.setRelativeMouseMode(window, enabled); }
    public static void SDL_WarpMouseInWindow(long window, float x, float y) { Sdl.warpMouse(window, x, y); }
}
