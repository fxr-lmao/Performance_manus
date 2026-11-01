// Amethyst SDL shim: SDL3 API surface used by Minecraft 26.3, implemented over GLFW.
// See dev.amethyst.sdlshim.Sdl. Signatures match LWJGL 3.4.3 exactly.
package org.lwjgl.sdl;

@FunctionalInterface
public interface SDL_LogOutputFunctionI {
    void invoke(long userdata, int category, int priority, long message);
}
