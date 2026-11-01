// Amethyst SDL shim: SDL3 API surface used by Minecraft 26.3, implemented over GLFW.
// See dev.amethyst.sdlshim.Sdl. Signatures match LWJGL 3.4.3 exactly.
package org.lwjgl.sdl;

import dev.amethyst.sdlshim.Sdl;

public final class SDLTimer {
    private SDLTimer() {}
    public static long SDL_GetTicksNS() { return Sdl.ticksNs(); }
}
