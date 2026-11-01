// Amethyst SDL shim: SDL3 API surface used by Minecraft 26.3, implemented over GLFW.
// See dev.amethyst.sdlshim.Sdl. Signatures match LWJGL 3.4.3 exactly.
package org.lwjgl.sdl;

import dev.amethyst.sdlshim.Sdl;
import java.nio.ByteBuffer;

public final class SDLKeyboard {
    private SDLKeyboard() {}
    public static boolean SDL_ClearComposition(long window) { return true; }
    public static int SDL_GetKeyFromScancode(int scancode, short modstate, boolean keyEvent) { return Sdl.keyFromScancode(scancode); }
    public static String SDL_GetKeyName(int key) { return Sdl.keyName(key); }
    public static ByteBuffer SDL_GetKeyboardState() { return Sdl.keyboardState(); }
    public static short SDL_GetModState() { return Sdl.modState(); }
    public static boolean SDL_SetTextInputArea(long window, SDL_Rect.Buffer rect, int cursor) { return true; }
    public static boolean SDL_StartTextInput(long window) { return Sdl.startTextInput(window); }
    public static boolean SDL_StopTextInput(long window) { return Sdl.stopTextInput(window); }
}
