// Amethyst SDL shim: SDL3 API surface used by Minecraft 26.3, implemented over GLFW.
// See dev.amethyst.sdlshim.Sdl. Signatures match LWJGL 3.4.3 exactly.
package org.lwjgl.sdl;

import dev.amethyst.sdlshim.Sdl;
import java.nio.LongBuffer;
import org.lwjgl.PointerBuffer;
import org.lwjgl.vulkan.VkAllocationCallbacks;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkPhysicalDevice;

/** Vulkan is not offered: Minecraft then stays on (or falls back to) its OpenGL backend. */
public final class SDLVulkan {
    private SDLVulkan() {}
    public static boolean SDL_Vulkan_CreateSurface(long window, VkInstance instance, VkAllocationCallbacks allocator, LongBuffer surface) {
        return Sdl.setError("Vulkan is not supported by the Amethyst SDL shim");
    }
    public static PointerBuffer SDL_Vulkan_GetInstanceExtensions() {
        Sdl.setError("Vulkan is not supported by the Amethyst SDL shim");
        return null;
    }
    public static boolean SDL_Vulkan_GetPresentationSupport(VkInstance instance, VkPhysicalDevice device, int queueFamily) { return false; }
    public static long SDL_Vulkan_GetVkGetInstanceProcAddr() { return 0L; }
    public static boolean SDL_Vulkan_LoadLibrary(CharSequence path) {
        return Sdl.setError("Vulkan is not supported by the Amethyst SDL shim");
    }
    public static void SDL_Vulkan_UnloadLibrary() {}
}
