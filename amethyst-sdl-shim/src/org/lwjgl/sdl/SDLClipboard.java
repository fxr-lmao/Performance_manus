// Amethyst SDL shim: SDL3 API surface used by Minecraft 26.3, implemented over GLFW.
// See dev.amethyst.sdlshim.Sdl. Signatures match LWJGL 3.4.3 exactly.
package org.lwjgl.sdl;

import dev.amethyst.sdlshim.Sdl;

public final class SDLClipboard {
    private SDLClipboard() {}
    public static String SDL_GetClipboardText() { return Sdl.clipboardText(); }
    public static boolean SDL_SetClipboardText(CharSequence text) { return Sdl.setClipboardText(text); }
}
