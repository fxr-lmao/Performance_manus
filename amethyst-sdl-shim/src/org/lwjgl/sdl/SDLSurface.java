// Amethyst SDL shim: SDL3 API surface used by Minecraft 26.3, implemented over GLFW.
// See dev.amethyst.sdlshim.Sdl. Signatures match LWJGL 3.4.3 exactly.
package org.lwjgl.sdl;

import dev.amethyst.sdlshim.Sdl;
import java.nio.ByteBuffer;

public final class SDLSurface {
    private SDLSurface() {}
    public static boolean SDL_AddSurfaceAlternateImage(SDL_Surface surface, SDL_Surface image) { return true; }
    public static SDL_Surface SDL_CreateSurfaceFrom(int width, int height, int format, ByteBuffer pixels, int pitch) {
        return Sdl.createSurfaceFrom(width, height, format, pixels, pitch);
    }
    public static void SDL_DestroySurface(SDL_Surface surface) { Sdl.destroySurface(surface); }
}
