// Amethyst SDL shim: SDL3 API surface used by Minecraft 26.3, implemented over GLFW.
// See dev.amethyst.sdlshim.Sdl. Signatures match LWJGL 3.4.3 exactly.
package org.lwjgl.sdl;

import dev.amethyst.sdlshim.Sdl;

public final class SDLMisc {
    private SDLMisc() {}
    public static boolean SDL_OpenURL(CharSequence url) {
        Sdl.log("SDL_OpenURL not supported on Amethyst: " + url);
        return Sdl.setError("Opening URLs is not supported");
    }
}
