// Amethyst SDL shim: SDL3 API surface used by Minecraft 26.3, implemented over GLFW.
// See dev.amethyst.sdlshim.Sdl. Signatures match LWJGL 3.4.3 exactly.
package org.lwjgl.sdl;

import dev.amethyst.sdlshim.Sdl;

public final class SDLInit {
    private SDLInit() {}
    public static boolean SDL_Init(int flags) { return Sdl.init(flags); }
    public static void SDL_Quit() { Sdl.quit(); }
    public static boolean SDL_SetAppMetadataProperty(CharSequence name, CharSequence value) { return true; }
}
