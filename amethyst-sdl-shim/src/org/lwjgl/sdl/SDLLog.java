// Amethyst SDL shim: SDL3 API surface used by Minecraft 26.3, implemented over GLFW.
// See dev.amethyst.sdlshim.Sdl. Signatures match LWJGL 3.4.3 exactly.
package org.lwjgl.sdl;

public final class SDLLog {
    private SDLLog() {}
    public static void SDL_SetLogOutputFunction(SDL_LogOutputFunctionI callback, long userdata) {}
    public static void SDL_SetLogPriorities(int priority) {}
}
