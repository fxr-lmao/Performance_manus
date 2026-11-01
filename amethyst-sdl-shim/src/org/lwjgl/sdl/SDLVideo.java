// Amethyst SDL shim: SDL3 API surface used by Minecraft 26.3, implemented over GLFW.
// See dev.amethyst.sdlshim.Sdl. Signatures match LWJGL 3.4.3 exactly.
package org.lwjgl.sdl;

import dev.amethyst.sdlshim.Sdl;
import java.nio.IntBuffer;
import org.lwjgl.PointerBuffer;

public final class SDLVideo {
    private SDLVideo() {}

    public static long SDL_CreateWindow(CharSequence title, int w, int h, long flags) { return Sdl.createWindow(title, w, h, flags); }
    public static void SDL_DestroyWindow(long window) { Sdl.destroyWindow(window); }

    public static long SDL_GL_CreateContext(long window) { return Sdl.glCreateContext(window); }
    public static boolean SDL_GL_DestroyContext(long context) { return true; }
    public static boolean SDL_GL_GetAttribute(int attr, IntBuffer value) { return Sdl.glGetAttribute(attr, value); }
    public static long SDL_GL_GetProcAddress(CharSequence proc) { return Sdl.glGetProcAddress(proc); }
    public static boolean SDL_GL_LoadLibrary(CharSequence path) { return true; }
    public static boolean SDL_GL_MakeCurrent(long window, long context) { return Sdl.glMakeCurrent(window, context); }
    public static boolean SDL_GL_SetAttribute(int attr, int value) { return Sdl.glSetAttribute(attr, value); }
    public static boolean SDL_GL_SetSwapInterval(int interval) { return Sdl.glSetSwapInterval(interval); }
    public static boolean SDL_GL_SwapWindow(long window) { return Sdl.glSwapWindow(window); }
    public static void SDL_GL_UnloadLibrary() {}

    public static boolean SDL_GetClosestFullscreenDisplayMode(int displayID, int w, int h, float refreshRate, boolean includeHighDensityModes, SDL_DisplayMode closest) {
        return Sdl.closestFullscreenMode(closest);
    }
    public static SDL_DisplayMode SDL_GetCurrentDisplayMode(int displayID) { return SDL_DisplayMode.create(Sdl.displayMode()); }
    public static String SDL_GetCurrentVideoDriver() { return Sdl.videoDriver(); }
    public static SDL_DisplayMode SDL_GetDesktopDisplayMode(int displayID) { return SDL_DisplayMode.create(Sdl.displayMode()); }
    public static boolean SDL_GetDisplayBounds(int displayID, SDL_Rect rect) { return Sdl.displayBounds(rect); }
    public static int SDL_GetDisplayForWindow(long window) { return 1; }
    public static String SDL_GetDisplayName(int displayID) { return "Amethyst Display"; }
    public static IntBuffer SDL_GetDisplays() { return Sdl.displays(); }
    public static PointerBuffer SDL_GetFullscreenDisplayModes(int displayID) { return Sdl.fullscreenModes(); }
    public static int SDL_GetPrimaryDisplay() { return 1; }

    public static long SDL_GetWindowFlags(long window) { return Sdl.windowFlags(window); }
    public static SDL_DisplayMode SDL_GetWindowFullscreenMode(long window) { return null; }
    public static float SDL_GetWindowPixelDensity(long window) { return 1.0f; }
    public static boolean SDL_GetWindowPosition(long window, IntBuffer x, IntBuffer y) { return Sdl.windowPosition(window, x, y); }
    public static boolean SDL_GetWindowSizeInPixels(long window, IntBuffer w, IntBuffer h) { return Sdl.windowSizeInPixels(window, w, h); }
    public static boolean SDL_RestoreWindow(long window) { return true; }
    public static boolean SDL_SetWindowBordered(long window, boolean bordered) { return true; }
    public static boolean SDL_SetWindowFullscreen(long window, boolean fullscreen) { return Sdl.setFullscreen(window, fullscreen); }
    public static boolean SDL_SetWindowFullscreenMode(long window, SDL_DisplayMode mode) { return true; }
    public static boolean SDL_SetWindowIcon(long window, SDL_Surface icon) { return true; }
    public static boolean SDL_SetWindowMaximumSize(long window, int maxW, int maxH) { return true; }
    public static boolean SDL_SetWindowMinimumSize(long window, int minW, int minH) { return true; }
    public static boolean SDL_SetWindowMouseGrab(long window, boolean grabbed) { return true; }
    public static boolean SDL_SetWindowPosition(long window, int x, int y) { return true; }
    public static boolean SDL_SetWindowSize(long window, int w, int h) { return Sdl.setWindowSize(window, w, h); }
    public static boolean SDL_SetWindowTitle(long window, CharSequence title) { return Sdl.setWindowTitle(window, title); }
    public static boolean SDL_SyncWindow(long window) { return true; }
}
