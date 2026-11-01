// Amethyst SDL shim: SDL3 API surface used by Minecraft 26.3, implemented over GLFW.
// See dev.amethyst.sdlshim.Sdl. Signatures match LWJGL 3.4.3 exactly.
package org.lwjgl.sdl;

import dev.amethyst.sdlshim.Sdl;
import java.nio.IntBuffer;

public final class SDLMessageBox {
    private SDLMessageBox() {}
    public static boolean SDL_ShowMessageBox(SDL_MessageBoxData data, IntBuffer buttonid) { return Sdl.showMessageBox(data, buttonid); }
    public static boolean SDL_ShowSimpleMessageBox(int flags, CharSequence title, CharSequence message, long window) {
        return Sdl.showSimpleMessageBox(flags, title, message);
    }
}
