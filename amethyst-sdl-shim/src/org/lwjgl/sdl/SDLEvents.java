// Amethyst SDL shim: SDL3 API surface used by Minecraft 26.3, implemented over GLFW.
// See dev.amethyst.sdlshim.Sdl. Signatures match LWJGL 3.4.3 exactly.
package org.lwjgl.sdl;

import dev.amethyst.sdlshim.Sdl;

public final class SDLEvents {
    private SDLEvents() {}
    public static void SDL_FlushEvents(int minType, int maxType) { Sdl.flushEvents(minType, maxType); }
    public static long SDL_GetWindowFromEvent(SDL_Event event) { return Sdl.windowFromEvent(event); }
    public static boolean SDL_PollEvent(SDL_Event event) { return Sdl.pollEvent(event); }
    public static void SDL_PumpEvents() { Sdl.pumpEvents(); }
}
