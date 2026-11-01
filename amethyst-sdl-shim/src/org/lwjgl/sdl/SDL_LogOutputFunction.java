// Amethyst SDL shim: SDL3 API surface used by Minecraft 26.3, implemented over GLFW.
// See dev.amethyst.sdlshim.Sdl. Signatures match LWJGL 3.4.3 exactly.
package org.lwjgl.sdl;

public final class SDL_LogOutputFunction implements SDL_LogOutputFunctionI {
    private final SDL_LogOutputFunctionI delegate;

    private SDL_LogOutputFunction(SDL_LogOutputFunctionI delegate) { this.delegate = delegate; }

    public static SDL_LogOutputFunction create(SDL_LogOutputFunctionI instance) {
        return instance instanceof SDL_LogOutputFunction ? (SDL_LogOutputFunction) instance : new SDL_LogOutputFunction(instance);
    }

    @Override public void invoke(long userdata, int category, int priority, long message) {
        delegate.invoke(userdata, category, priority, message);
    }

    public void free() {}
    public void close() {}
}
