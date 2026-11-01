package dev.amethyst.sdlshim;

import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCharCallbackI;
import org.lwjgl.glfw.GLFWCursorEnterCallbackI;
import org.lwjgl.glfw.GLFWCursorPosCallbackI;
import org.lwjgl.glfw.GLFWFramebufferSizeCallbackI;
import org.lwjgl.glfw.GLFWKeyCallbackI;
import org.lwjgl.glfw.GLFWMouseButtonCallbackI;
import org.lwjgl.glfw.GLFWScrollCallbackI;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.glfw.GLFWWindowCloseCallbackI;
import org.lwjgl.glfw.GLFWWindowFocusCallbackI;
import org.lwjgl.glfw.GLFWWindowSizeCallbackI;
import org.lwjgl.opengl.GL;
import org.lwjgl.sdl.SDL_DisplayMode;
import org.lwjgl.sdl.SDL_Event;
import org.lwjgl.sdl.SDL_KeyboardEvent;
import org.lwjgl.sdl.SDL_MessageBoxButtonData;
import org.lwjgl.sdl.SDL_MessageBoxData;
import org.lwjgl.sdl.SDL_MouseButtonEvent;
import org.lwjgl.sdl.SDL_MouseMotionEvent;
import org.lwjgl.sdl.SDL_MouseWheelEvent;
import org.lwjgl.sdl.SDL_PixelFormatDetails;
import org.lwjgl.sdl.SDL_Rect;
import org.lwjgl.sdl.SDL_Surface;
import org.lwjgl.sdl.SDL_TextInputEvent;
import org.lwjgl.sdl.SDL_WindowEvent;
import org.lwjgl.system.FunctionProvider;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.glfw.GLFW.GLFW_CLIENT_API;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MAJOR;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MINOR;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR_DISABLED;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR_NORMAL;
import static org.lwjgl.glfw.GLFW.GLFW_FALSE;
import static org.lwjgl.glfw.GLFW.GLFW_MOD_CAPS_LOCK;
import static org.lwjgl.glfw.GLFW.GLFW_MOD_NUM_LOCK;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_API;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_CORE_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_FORWARD_COMPAT;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;
import static org.lwjgl.glfw.GLFW.GLFW_REPEAT;
import static org.lwjgl.glfw.GLFW.GLFW_RESIZABLE;
import static org.lwjgl.glfw.GLFW.GLFW_TRUE;
import static org.lwjgl.glfw.GLFW.GLFW_VISIBLE;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memAllocInt;
import static org.lwjgl.system.MemoryUtil.memAllocPointer;
import static org.lwjgl.system.MemoryUtil.memCalloc;
import static org.lwjgl.system.MemoryUtil.memCopy;
import static org.lwjgl.system.MemoryUtil.memFree;
import static org.lwjgl.system.MemoryUtil.memGetAddress;
import static org.lwjgl.system.MemoryUtil.memGetInt;
import static org.lwjgl.system.MemoryUtil.memPutAddress;
import static org.lwjgl.system.MemoryUtil.memPutByte;
import static org.lwjgl.system.MemoryUtil.memPutFloat;
import static org.lwjgl.system.MemoryUtil.memPutInt;
import static org.lwjgl.system.MemoryUtil.memPutLong;
import static org.lwjgl.system.MemoryUtil.memPutShort;
import static org.lwjgl.system.MemoryUtil.memSet;
import static org.lwjgl.system.MemoryUtil.memUTF8;
import static org.lwjgl.system.MemoryUtil.memUTF8Safe;
import static org.lwjgl.system.MemoryUtil.nmemCalloc;
import static org.lwjgl.system.MemoryUtil.nmemFree;

/**
 * SDL3 emulation on top of GLFW for Minecraft 26.3+ on Amethyst iOS.
 *
 * <p>Minecraft 26.3 replaced GLFW with SDL3 for windows, input and GL contexts. Amethyst only has
 * PojavLauncher's GLFW emulation, which is wired to the iPad's surface, keyboard and mouse. This class
 * implements the part of SDL3 that Minecraft actually calls (about 65 functions) by translating to
 * GLFW. Every SDL window maps onto Amethyst's single GLFW window and GL context.
 *
 * <p>All constants below are SDL3's ABI values (checked against LWJGL 3.4.3's SDL bindings).
 * Everything runs on the thread that calls SDL (Minecraft's render thread), like real SDL.
 */
public final class Sdl {
    private Sdl() {}

    public static final String VERSION = "0.1.0";

    /** True on Amethyst/PojavLauncher (GLFW emulated over the iOS surface), false on real GLFW. */
    public static final boolean POJAV = classExists("org.lwjgl.glfw.CallbackBridge");
    static final boolean DEBUG = Boolean.getBoolean("amethyst.sdlshim.debug");

    // ---------------------------------------------------------------- SDL3 constants (ABI values)
    static final int SDL_INIT_VIDEO = 0x20;

    static final long SDL_WINDOW_FULLSCREEN = 0x1L;
    static final long SDL_WINDOW_OPENGL = 0x2L;
    static final long SDL_WINDOW_HIDDEN = 0x8L;
    static final long SDL_WINDOW_RESIZABLE = 0x20L;
    static final long SDL_WINDOW_INPUT_FOCUS = 0x200L;
    static final long SDL_WINDOW_MOUSE_FOCUS = 0x400L;
    static final long SDL_WINDOW_HIGH_PIXEL_DENSITY = 0x2000L;
    static final long SDL_WINDOW_MOUSE_RELATIVE_MODE = 0x8000L;

    static final int SDL_EVENT_WINDOW_SHOWN = 0x202;
    static final int SDL_EVENT_WINDOW_EXPOSED = 0x204;
    static final int SDL_EVENT_WINDOW_RESIZED = 0x206;
    static final int SDL_EVENT_WINDOW_PIXEL_SIZE_CHANGED = 0x207;
    static final int SDL_EVENT_WINDOW_MOUSE_ENTER = 0x20C;
    static final int SDL_EVENT_WINDOW_MOUSE_LEAVE = 0x20D;
    static final int SDL_EVENT_WINDOW_FOCUS_GAINED = 0x20E;
    static final int SDL_EVENT_WINDOW_FOCUS_LOST = 0x20F;
    static final int SDL_EVENT_WINDOW_CLOSE_REQUESTED = 0x210;
    static final int SDL_EVENT_WINDOW_ENTER_FULLSCREEN = 0x217;
    static final int SDL_EVENT_WINDOW_LEAVE_FULLSCREEN = 0x218;
    static final int SDL_EVENT_KEY_DOWN = 0x300;
    static final int SDL_EVENT_KEY_UP = 0x301;
    static final int SDL_EVENT_TEXT_INPUT = 0x303;
    static final int SDL_EVENT_MOUSE_MOTION = 0x400;
    static final int SDL_EVENT_MOUSE_BUTTON_DOWN = 0x401;
    static final int SDL_EVENT_MOUSE_BUTTON_UP = 0x402;
    static final int SDL_EVENT_MOUSE_WHEEL = 0x403;

    static final int SDL_GL_CONTEXT_MAJOR_VERSION = 17;
    static final int SDL_GL_CONTEXT_MINOR_VERSION = 18;
    static final int SDL_GL_CONTEXT_FLAGS = 19;
    static final int SDL_GL_CONTEXT_PROFILE_MASK = 20;
    static final int SDL_GL_CONTEXT_PROFILE_CORE = 0x1;
    static final int SDL_GL_CONTEXT_FORWARD_COMPATIBLE_FLAG = 0x2;

    static final short SDL_KMOD_LSHIFT = 0x0001, SDL_KMOD_RSHIFT = 0x0002;
    static final short SDL_KMOD_LCTRL = 0x0040, SDL_KMOD_RCTRL = 0x0080;
    static final short SDL_KMOD_LALT = 0x0100, SDL_KMOD_RALT = 0x0200;
    static final short SDL_KMOD_LGUI = 0x0400, SDL_KMOD_RGUI = 0x0800;
    static final short SDL_KMOD_NUM = 0x1000, SDL_KMOD_CAPS = 0x2000;

    static final int SDL_SCANCODE_COUNT = 512;
    static final int SDL_PIXELFORMAT_XRGB8888 = 0x16161804;
    static final int SDL_MESSAGEBOX_BUTTON_RETURNKEY_DEFAULT = 0x1;

    // ---------------------------------------------------------------- state
    private static String lastError = "";
    private static final long START_NS = System.nanoTime();
    private static final Map<String, String> HINTS = new HashMap<>();
    private static final Map<Integer, Integer> GL_ATTRS = new HashMap<>();

    static final class Win {
        final long handle;
        final int id;
        final long flags;
        final int requestedW;
        final int requestedH;

        Win(long handle, int id, long flags, int w, int h) {
            this.handle = handle;
            this.id = id;
            this.flags = flags;
            this.requestedW = w;
            this.requestedH = h;
        }
    }

    private static final Map<Long, Win> WINDOWS = new LinkedHashMap<>();
    private static final Map<Integer, Win> WINDOWS_BY_ID = new HashMap<>();
    private static int nextWindowId = 1;
    private static Win mainWindow;
    private static long glfwWindow = NULL;
    private static boolean glfwReady;
    private static boolean contextCurrent;
    private static boolean relativeMode;
    private static boolean textInput;
    private static boolean fullscreen;
    private static boolean focused = true;

    // input state
    private static ByteBuffer keyboardState;
    private static short modState;
    private static int buttonMask;
    private static double mouseX, mouseY;
    private static boolean haveMousePos;

    // events
    static final class Ev {
        int type;
        int windowId;
        int i1, i2;             // scancode/key or data1/data2
        short mod;
        int raw;
        boolean down, repeat;
        float x, y, xrel, yrel;
        int button, state;
        String text;
    }

    private static final ArrayDeque<Ev> QUEUE = new ArrayDeque<>();
    private static final List<Long> PENDING_FREE = new ArrayList<>();

    // display mode / format details (stable native memory, like SDL's internal structs)
    private static long displayModePtr = NULL;
    private static long pixelFormatPtr = NULL;

    // GLFW callbacks must stay reachable
    private static final List<Object> CALLBACKS = new ArrayList<>();

    // ---------------------------------------------------------------- logging / errors
    public static void log(String msg) {
        System.out.println("[SDLShim] " + msg);
    }

    static void debug(String msg) {
        if (DEBUG) {
            log(msg);
        }
    }

    public static boolean setError(String msg) {
        lastError = msg == null ? "" : msg;
        debug("error: " + lastError);
        return false;
    }

    public static String getError() {
        return lastError;
    }

    private static boolean classExists(String name) {
        try {
            Class.forName(name, false, Sdl.class.getClassLoader());
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    // ---------------------------------------------------------------- init / misc
    public static boolean init(int flags) {
        debug("SDL_Init(0x" + Integer.toHexString(flags) + ")");
        if ((flags & SDL_INIT_VIDEO) != 0) {
            return ensureGlfw();
        }
        return true;
    }

    private static boolean ensureGlfw() {
        if (!glfwReady) {
            glfwReady = GLFW.glfwInit();
            if (!glfwReady) {
                return setError("glfwInit failed");
            }
            if (keyboardState == null) {
                keyboardState = memCalloc(SDL_SCANCODE_COUNT);
            }
        }
        return true;
    }

    public static void quit() {
        debug("SDL_Quit");
        if (!POJAV && glfwWindow != NULL) {
            GLFW.glfwDestroyWindow(glfwWindow);
            glfwWindow = NULL;
            GLFW.glfwTerminate();
            glfwReady = false;
        }
    }

    public static boolean setHint(CharSequence name, CharSequence value) {
        if (name != null) {
            HINTS.put(name.toString(), value == null ? null : value.toString());
        }
        return true;
    }

    public static long ticksNs() {
        return System.nanoTime() - START_NS;
    }

    public static String platform() {
        if (POJAV) {
            return "macOS"; // LWJGL on Amethyst reports MACOSX; keep Minecraft's view consistent
        }
        String os = System.getProperty("os.name", "");
        if (os.startsWith("Windows")) {
            return "Windows";
        }
        if (os.startsWith("Mac")) {
            return "macOS";
        }
        return "Linux";
    }

    public static String videoDriver() {
        return POJAV ? "amethyst" : "glfw";
    }

    // ---------------------------------------------------------------- windows
    public static long createWindow(CharSequence title, int w, int h, long flags) {
        if (!ensureGlfw()) {
            return NULL;
        }
        boolean hidden = (flags & SDL_WINDOW_HIDDEN) != 0;
        if (glfwWindow == NULL) {
            GLFW.glfwDefaultWindowHints();
            GLFW.glfwWindowHint(GLFW_CLIENT_API, GLFW_OPENGL_API);
            GLFW.glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, GL_ATTRS.getOrDefault(SDL_GL_CONTEXT_MAJOR_VERSION, 3));
            GLFW.glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, GL_ATTRS.getOrDefault(SDL_GL_CONTEXT_MINOR_VERSION, 3));
            if (GL_ATTRS.getOrDefault(SDL_GL_CONTEXT_PROFILE_MASK, 0) == SDL_GL_CONTEXT_PROFILE_CORE) {
                GLFW.glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
            }
            if ((GL_ATTRS.getOrDefault(SDL_GL_CONTEXT_FLAGS, 0) & SDL_GL_CONTEXT_FORWARD_COMPATIBLE_FLAG) != 0) {
                GLFW.glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
            }
            GLFW.glfwWindowHint(GLFW_VISIBLE, hidden ? GLFW_FALSE : GLFW_TRUE);
            GLFW.glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
            glfwWindow = GLFW.glfwCreateWindow(Math.max(w, 1), Math.max(h, 1), title == null ? "" : title, NULL, NULL);
            if (glfwWindow == NULL) {
                return setError("glfwCreateWindow failed") ? NULL : NULL;
            }
            installCallbacks();
            log("created GLFW window for SDL (" + w + "x" + h + ", hidden=" + hidden + ")");
        }
        int id = nextWindowId++;
        long handle = 0x5D1000L + ((long) id << 4);
        Win win = new Win(handle, id, flags, w, h);
        WINDOWS.put(handle, win);
        WINDOWS_BY_ID.put(id, win);
        debug("SDL_CreateWindow(" + title + ", " + w + "x" + h + ", flags=0x" + Long.toHexString(flags) + ") -> id " + id);
        if (!hidden && mainWindow == null) {
            mainWindow = win;
            showMainWindow(w, h);
        }
        return handle;
    }

    private static void showMainWindow(int w, int h) {
        if (!POJAV) {
            GLFW.glfwSetWindowSize(glfwWindow, w, h);
        }
        GLFW.glfwShowWindow(glfwWindow);
        int[] fb = framebufferSize();
        queueWindowEvent(SDL_EVENT_WINDOW_SHOWN, 0, 0);
        queueWindowEvent(SDL_EVENT_WINDOW_EXPOSED, 0, 0);
        queueWindowEvent(SDL_EVENT_WINDOW_PIXEL_SIZE_CHANGED, fb[0], fb[1]);
        queueWindowEvent(SDL_EVENT_WINDOW_FOCUS_GAINED, 0, 0);
        queueWindowEvent(SDL_EVENT_WINDOW_MOUSE_ENTER, 0, 0);
        log("main window shown, framebuffer " + fb[0] + "x" + fb[1]);
    }

    public static void destroyWindow(long handle) {
        Win win = WINDOWS.remove(handle);
        if (win != null) {
            WINDOWS_BY_ID.remove(win.id);
            if (win == mainWindow) {
                mainWindow = null;
            }
            debug("SDL_DestroyWindow(id " + win.id + ")");
        }
    }

    static Win win(long handle) {
        return WINDOWS.get(handle);
    }

    public static long windowFlags(long handle) {
        Win win = win(handle);
        if (win == null) {
            return 0L;
        }
        long f = win.flags | SDL_WINDOW_OPENGL;
        if (win == mainWindow) {
            if (focused) {
                f |= SDL_WINDOW_INPUT_FOCUS | SDL_WINDOW_MOUSE_FOCUS;
            }
            if (relativeMode) {
                f |= SDL_WINDOW_MOUSE_RELATIVE_MODE;
            }
            if (fullscreen) {
                f |= SDL_WINDOW_FULLSCREEN;
            }
        }
        return f;
    }

    static int[] framebufferSize() {
        if (glfwWindow == NULL) {
            int[] d = displaySize();
            return d;
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            GLFW.glfwGetFramebufferSize(glfwWindow, w, h);
            return new int[] {Math.max(w.get(0), 1), Math.max(h.get(0), 1)};
        }
    }

    public static boolean windowSizeInPixels(long handle, IntBuffer w, IntBuffer h) {
        int[] fb = framebufferSize();
        if (w != null) {
            w.put(w.position(), fb[0]);
        }
        if (h != null) {
            h.put(h.position(), fb[1]);
        }
        return true;
    }

    public static boolean windowPosition(long handle, IntBuffer x, IntBuffer y) {
        if (x != null) {
            x.put(x.position(), 0);
        }
        if (y != null) {
            y.put(y.position(), 0);
        }
        return true;
    }

    public static boolean setWindowSize(long handle, int w, int h) {
        if (!POJAV && glfwWindow != NULL && win(handle) == mainWindow) {
            GLFW.glfwSetWindowSize(glfwWindow, w, h);
        }
        return true;
    }

    public static boolean setWindowTitle(long handle, CharSequence title) {
        if (glfwWindow != NULL && win(handle) == mainWindow && title != null) {
            GLFW.glfwSetWindowTitle(glfwWindow, title);
        }
        return true;
    }

    public static boolean setFullscreen(long handle, boolean fs) {
        if (win(handle) == mainWindow && fs != fullscreen) {
            fullscreen = fs;
            queueWindowEvent(fs ? SDL_EVENT_WINDOW_ENTER_FULLSCREEN : SDL_EVENT_WINDOW_LEAVE_FULLSCREEN, 0, 0);
        }
        return true;
    }

    // ---------------------------------------------------------------- OpenGL
    public static boolean glSetAttribute(int attr, int value) {
        GL_ATTRS.put(attr, value);
        return true;
    }

    public static boolean glGetAttribute(int attr, IntBuffer value) {
        int v;
        switch (attr) {
            case SDL_GL_CONTEXT_MAJOR_VERSION: v = GL_ATTRS.getOrDefault(attr, 3); break;
            case SDL_GL_CONTEXT_MINOR_VERSION: v = GL_ATTRS.getOrDefault(attr, 3); break;
            case SDL_GL_CONTEXT_PROFILE_MASK: v = GL_ATTRS.getOrDefault(attr, SDL_GL_CONTEXT_PROFILE_CORE); break;
            case 0: case 1: case 2: v = 8; break;          // RED/GREEN/BLUE_SIZE
            case 3: v = 8; break;                          // ALPHA_SIZE
            case 5: v = 1; break;                          // DOUBLEBUFFER
            case 6: v = 24; break;                         // DEPTH_SIZE
            case 7: v = 8; break;                          // STENCIL_SIZE
            default: v = GL_ATTRS.getOrDefault(attr, 0);
        }
        if (value != null) {
            value.put(value.position(), v);
        }
        return true;
    }

    public static long glGetProcAddress(CharSequence name) {
        FunctionProvider provider = GL.getFunctionProvider();
        if (provider == null) {
            GL.create();
            provider = GL.getFunctionProvider();
        }
        return provider == null ? NULL : provider.getFunctionAddress(name);
    }

    public static long glCreateContext(long handle) {
        if (glfwWindow == NULL || win(handle) == null) {
            return setError("SDL_GL_CreateContext: invalid window") ? NULL : NULL;
        }
        makeContextCurrent();
        debug("SDL_GL_CreateContext(id " + win(handle).id + ")");
        return glfwWindow; // Amethyst: the GLFW "window" *is* the GL context
    }

    public static boolean glMakeCurrent(long handle, long context) {
        if (context == NULL) {
            return true; // releasing the context is not needed with a single surface
        }
        makeContextCurrent();
        return true;
    }

    private static void makeContextCurrent() {
        if (!contextCurrent && glfwWindow != NULL) {
            GLFW.glfwMakeContextCurrent(glfwWindow);
            contextCurrent = true;
        }
    }

    public static boolean glSwapWindow(long handle) {
        if (glfwWindow != NULL) {
            GLFW.glfwSwapBuffers(glfwWindow);
        }
        return true;
    }

    public static boolean glSetSwapInterval(int interval) {
        if (glfwWindow != NULL) {
            GLFW.glfwSwapInterval(interval < 0 ? 1 : interval);
        }
        return true;
    }

    // ---------------------------------------------------------------- displays
    static int[] displaySize() {
        try {
            long monitor = GLFW.glfwGetPrimaryMonitor();
            GLFWVidMode mode = monitor == NULL ? null : GLFW.glfwGetVideoMode(monitor);
            if (mode != null && mode.width() > 0 && mode.height() > 0) {
                return new int[] {mode.width(), mode.height(), mode.refreshRate()};
            }
        } catch (Throwable t) {
            debug("glfwGetVideoMode failed: " + t);
        }
        if (glfwWindow != NULL) {
            int[] fb = framebufferSize();
            return new int[] {fb[0], fb[1], 60};
        }
        return new int[] {1920, 1080, 60};
    }

    static float refreshRate(int fromGlfw) {
        String p = System.getProperty("UIScreen.maximumFramesPerSecond");
        if (p != null) {
            try {
                int fps = Integer.parseInt(p.trim());
                if (fps > 0) {
                    return fps;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return fromGlfw > 0 ? fromGlfw : 60;
    }

    /** Pointer to a stable SDL_DisplayMode describing the iPad screen. */
    public static long displayMode() {
        if (displayModePtr == NULL) {
            displayModePtr = nmemCalloc(1, SDL_DisplayMode.SIZEOF);
        }
        int[] d = displaySize();
        float hz = refreshRate(d[2]);
        long p = displayModePtr;
        memPutInt(p + SDL_DisplayMode.DISPLAYID, 1);
        memPutInt(p + SDL_DisplayMode.FORMAT, SDL_PIXELFORMAT_XRGB8888);
        memPutInt(p + SDL_DisplayMode.W, d[0]);
        memPutInt(p + SDL_DisplayMode.H, d[1]);
        memPutFloat(p + SDL_DisplayMode.PIXEL_DENSITY, 1.0f);
        memPutFloat(p + SDL_DisplayMode.REFRESH_RATE, hz);
        memPutInt(p + SDL_DisplayMode.REFRESH_RATE_NUMERATOR, Math.round(hz));
        memPutInt(p + SDL_DisplayMode.REFRESH_RATE_DENOMINATOR, 1);
        return p;
    }

    public static IntBuffer displays() {
        IntBuffer b = memAllocInt(1);
        b.put(0, 1);
        return b;
    }

    public static PointerBuffer fullscreenModes() {
        PointerBuffer b = memAllocPointer(1);
        b.put(0, displayMode());
        return b;
    }

    public static boolean closestFullscreenMode(SDL_DisplayMode closest) {
        if (closest != null) {
            memCopy(displayMode(), closest.address(), (long) SDL_DisplayMode.SIZEOF);
        }
        return true;
    }

    public static boolean displayBounds(SDL_Rect rect) {
        if (rect != null) {
            int[] d = displaySize();
            long p = rect.address();
            memPutInt(p + SDL_Rect.X, 0);
            memPutInt(p + SDL_Rect.Y, 0);
            memPutInt(p + SDL_Rect.W, d[0]);
            memPutInt(p + SDL_Rect.H, d[1]);
        }
        return true;
    }

    public static long pixelFormatDetails(int format) {
        if (pixelFormatPtr == NULL) {
            pixelFormatPtr = nmemCalloc(1, SDL_PixelFormatDetails.SIZEOF);
            long p = pixelFormatPtr;
            memPutInt(p + SDL_PixelFormatDetails.FORMAT, SDL_PIXELFORMAT_XRGB8888);
            memPutByte(p + SDL_PixelFormatDetails.BITS_PER_PIXEL, (byte) 24);
            memPutByte(p + SDL_PixelFormatDetails.BYTES_PER_PIXEL, (byte) 4);
            memPutInt(p + SDL_PixelFormatDetails.RMASK, 0x00FF0000);
            memPutInt(p + SDL_PixelFormatDetails.GMASK, 0x0000FF00);
            memPutInt(p + SDL_PixelFormatDetails.BMASK, 0x000000FF);
            memPutByte(p + SDL_PixelFormatDetails.RBITS, (byte) 8);
            memPutByte(p + SDL_PixelFormatDetails.GBITS, (byte) 8);
            memPutByte(p + SDL_PixelFormatDetails.BBITS, (byte) 8);
            memPutByte(p + SDL_PixelFormatDetails.RSHIFT, (byte) 16);
            memPutByte(p + SDL_PixelFormatDetails.GSHIFT, (byte) 8);
        }
        return pixelFormatPtr;
    }

    // ---------------------------------------------------------------- surfaces (window icon)
    public static SDL_Surface createSurfaceFrom(int w, int h, int format, ByteBuffer pixels, int pitch) {
        SDL_Surface s = SDL_Surface.calloc();
        long p = s.address();
        memPutInt(p + SDL_Surface.FORMAT, format);
        memPutInt(p + SDL_Surface.W, w);
        memPutInt(p + SDL_Surface.H, h);
        memPutInt(p + SDL_Surface.PITCH, pitch);
        memPutAddress(p + SDL_Surface.PIXELS, pixels == null ? NULL : org.lwjgl.system.MemoryUtil.memAddress(pixels));
        memPutInt(p + SDL_Surface.REFCOUNT, 1);
        return s;
    }

    public static void destroySurface(SDL_Surface s) {
        if (s != null) {
            s.free();
        }
    }

    // ---------------------------------------------------------------- GLFW -> SDL event translation
    private static void installCallbacks() {
        long w = glfwWindow;
        GLFWKeyCallbackI key = Sdl::onKey;
        GLFWCharCallbackI chr = Sdl::onChar;
        GLFWMouseButtonCallbackI btn = Sdl::onMouseButton;
        GLFWCursorPosCallbackI pos = Sdl::onCursorPos;
        GLFWScrollCallbackI scroll = Sdl::onScroll;
        GLFWFramebufferSizeCallbackI fb = (win, width, height) ->
            queueWindowEvent(SDL_EVENT_WINDOW_PIXEL_SIZE_CHANGED, width, height);
        GLFWWindowSizeCallbackI size = (win, width, height) ->
            queueWindowEvent(SDL_EVENT_WINDOW_RESIZED, width, height);
        GLFWWindowFocusCallbackI focus = (win, f) -> {
            focused = f;
            queueWindowEvent(f ? SDL_EVENT_WINDOW_FOCUS_GAINED : SDL_EVENT_WINDOW_FOCUS_LOST, 0, 0);
        };
        GLFWCursorEnterCallbackI enter = (win, entered) ->
            queueWindowEvent(entered ? SDL_EVENT_WINDOW_MOUSE_ENTER : SDL_EVENT_WINDOW_MOUSE_LEAVE, 0, 0);
        GLFWWindowCloseCallbackI close = win -> queueWindowEvent(SDL_EVENT_WINDOW_CLOSE_REQUESTED, 0, 0);
        CALLBACKS.add(GLFW.glfwSetKeyCallback(w, key));
        CALLBACKS.add(GLFW.glfwSetCharCallback(w, chr));
        CALLBACKS.add(GLFW.glfwSetMouseButtonCallback(w, btn));
        CALLBACKS.add(GLFW.glfwSetCursorPosCallback(w, pos));
        CALLBACKS.add(GLFW.glfwSetScrollCallback(w, scroll));
        CALLBACKS.add(GLFW.glfwSetFramebufferSizeCallback(w, fb));
        CALLBACKS.add(GLFW.glfwSetWindowSizeCallback(w, size));
        CALLBACKS.add(GLFW.glfwSetWindowFocusCallback(w, focus));
        CALLBACKS.add(GLFW.glfwSetCursorEnterCallback(w, enter));
        CALLBACKS.add(GLFW.glfwSetWindowCloseCallback(w, close));
        CALLBACKS.add(key);
        CALLBACKS.add(chr);
        CALLBACKS.add(btn);
        CALLBACKS.add(pos);
        CALLBACKS.add(scroll);
        CALLBACKS.add(fb);
        CALLBACKS.add(size);
        CALLBACKS.add(focus);
        CALLBACKS.add(enter);
        CALLBACKS.add(close);
    }

    private static int mainId() {
        return mainWindow != null ? mainWindow.id : 0;
    }

    private static void queueWindowEvent(int type, int data1, int data2) {
        Ev e = new Ev();
        e.type = type;
        e.windowId = mainId();
        e.i1 = data1;
        e.i2 = data2;
        QUEUE.add(e);
    }

    private static short modBit(int scancode) {
        switch (scancode) {
            case 225: return SDL_KMOD_LSHIFT;
            case 229: return SDL_KMOD_RSHIFT;
            case 224: return SDL_KMOD_LCTRL;
            case 228: return SDL_KMOD_RCTRL;
            case 226: return SDL_KMOD_LALT;
            case 230: return SDL_KMOD_RALT;
            case 227: return SDL_KMOD_LGUI;
            case 231: return SDL_KMOD_RGUI;
            default: return 0;
        }
    }

    static void onKey(long window, int glfwKey, int glfwScancode, int action, int mods) {
        int sc = glfwKey >= 0 && glfwKey < KeyTables.GLFW_TO_SCANCODE.length ? KeyTables.GLFW_TO_SCANCODE[glfwKey] : 0;
        boolean down = action != GLFW_RELEASE;
        if (sc > 0 && sc < SDL_SCANCODE_COUNT && keyboardState != null) {
            keyboardState.put(sc, (byte) (down ? 1 : 0));
        }
        short bit = modBit(sc);
        if (bit != 0) {
            modState = (short) (down ? (modState | bit) : (modState & ~bit));
        }
        modState = (short) ((modState & ~(SDL_KMOD_CAPS | SDL_KMOD_NUM))
            | ((mods & GLFW_MOD_CAPS_LOCK) != 0 ? SDL_KMOD_CAPS : 0)
            | ((mods & GLFW_MOD_NUM_LOCK) != 0 ? SDL_KMOD_NUM : 0));
        Ev e = new Ev();
        e.type = down ? SDL_EVENT_KEY_DOWN : SDL_EVENT_KEY_UP;
        e.windowId = mainId();
        e.i1 = sc;
        e.i2 = keyFromScancode(sc);
        e.mod = modState;
        e.raw = glfwScancode;
        e.down = down;
        e.repeat = action == GLFW_REPEAT;
        QUEUE.add(e);
        debug("key glfw=" + glfwKey + " sc=" + sc + " key=0x" + Integer.toHexString(e.i2) + " action=" + action);
    }

    static void onChar(long window, int codepoint) {
        if (!textInput || codepoint < 0x20 || codepoint == 0x7F) {
            return;
        }
        Ev e = new Ev();
        e.type = SDL_EVENT_TEXT_INPUT;
        e.windowId = mainId();
        e.text = new String(Character.toChars(codepoint));
        QUEUE.add(e);
    }

    static int sdlButton(int glfwButton) {
        switch (glfwButton) {
            case 0: return 1; // left
            case 1: return 3; // right
            case 2: return 2; // middle
            default: return glfwButton + 1; // 3 -> X1 (4), 4 -> X2 (5)
        }
    }

    static void onMouseButton(long window, int glfwButton, int action, int mods) {
        int b = sdlButton(glfwButton);
        boolean down = action == GLFW_PRESS;
        int bit = 1 << (b - 1);
        buttonMask = down ? (buttonMask | bit) : (buttonMask & ~bit);
        Ev e = new Ev();
        e.type = down ? SDL_EVENT_MOUSE_BUTTON_DOWN : SDL_EVENT_MOUSE_BUTTON_UP;
        e.windowId = mainId();
        e.button = b;
        e.down = down;
        e.x = (float) mouseX;
        e.y = (float) mouseY;
        QUEUE.add(e);
    }

    static void onCursorPos(long window, double x, double y) {
        double dx = haveMousePos ? x - mouseX : 0.0;
        double dy = haveMousePos ? y - mouseY : 0.0;
        mouseX = x;
        mouseY = y;
        haveMousePos = true;
        Ev e = new Ev();
        e.type = SDL_EVENT_MOUSE_MOTION;
        e.windowId = mainId();
        e.x = (float) x;
        e.y = (float) y;
        e.xrel = (float) dx;
        e.yrel = (float) dy;
        e.state = buttonMask;
        QUEUE.add(e);
    }

    static void onScroll(long window, double dx, double dy) {
        Ev e = new Ev();
        e.type = SDL_EVENT_MOUSE_WHEEL;
        e.windowId = mainId();
        e.x = (float) dx;
        e.y = (float) dy;
        e.xrel = (float) mouseX;
        e.yrel = (float) mouseY;
        QUEUE.add(e);
    }

    // ---------------------------------------------------------------- event queue API
    public static void pumpEvents() {
        if (glfwWindow != NULL) {
            GLFW.glfwPollEvents();
        }
    }

    public static boolean pollEvent(SDL_Event event) {
        freePending();
        if (QUEUE.isEmpty()) {
            pumpEvents();
        }
        Ev e = QUEUE.poll();
        if (e == null) {
            return false;
        }
        if (event != null) {
            write(e, event.address());
        }
        return true;
    }

    public static void flushEvents(int minType, int maxType) {
        Iterator<Ev> it = QUEUE.iterator();
        while (it.hasNext()) {
            Ev e = it.next();
            if (e.type >= minType && e.type <= maxType) {
                it.remove();
            }
        }
    }

    public static long windowFromEvent(SDL_Event event) {
        if (event == null) {
            return NULL;
        }
        int type = memGetInt(event.address());
        boolean hasWindow = (type >= 0x202 && type < 0x300)       // window events
            || type == SDL_EVENT_KEY_DOWN || type == SDL_EVENT_KEY_UP
            || type == 0x302 || type == SDL_EVENT_TEXT_INPUT           // text editing / input
            || (type >= SDL_EVENT_MOUSE_MOTION && type <= SDL_EVENT_MOUSE_WHEEL)
            || (type >= 0x1000 && type <= 0x1004);                     // drop events
        if (!hasWindow) {
            return NULL;
        }
        Win win = WINDOWS_BY_ID.get(memGetInt(event.address() + SDL_WindowEvent.WINDOWID));
        return win == null ? NULL : win.handle;
    }

    private static void freePending() {
        for (long p : PENDING_FREE) {
            nmemFree(p);
        }
        PENDING_FREE.clear();
    }

    private static void write(Ev e, long p) {
        memSet(p, 0, SDL_Event.SIZEOF);
        memPutInt(p, e.type);
        memPutLong(p + 8, ticksNs()); // timestamp, common to all events
        switch (e.type) {
            case SDL_EVENT_KEY_DOWN:
            case SDL_EVENT_KEY_UP:
                memPutInt(p + SDL_KeyboardEvent.WINDOWID, e.windowId);
                memPutInt(p + SDL_KeyboardEvent.WHICH, 1);
                memPutInt(p + SDL_KeyboardEvent.SCANCODE, e.i1);
                memPutInt(p + SDL_KeyboardEvent.KEY, e.i2);
                memPutShort(p + SDL_KeyboardEvent.MOD, e.mod);
                memPutShort(p + SDL_KeyboardEvent.RAW, (short) e.raw);
                memPutByte(p + SDL_KeyboardEvent.DOWN, (byte) (e.down ? 1 : 0));
                memPutByte(p + SDL_KeyboardEvent.REPEAT, (byte) (e.repeat ? 1 : 0));
                break;
            case SDL_EVENT_TEXT_INPUT: {
                ByteBuffer text = memUTF8(e.text, true);
                long addr = org.lwjgl.system.MemoryUtil.memAddress(text);
                PENDING_FREE.add(addr);
                memPutInt(p + SDL_TextInputEvent.WINDOWID, e.windowId);
                memPutAddress(p + SDL_TextInputEvent.TEXT, addr);
                break;
            }
            case SDL_EVENT_MOUSE_MOTION:
                memPutInt(p + SDL_MouseMotionEvent.WINDOWID, e.windowId);
                memPutInt(p + SDL_MouseMotionEvent.WHICH, 1);
                memPutInt(p + SDL_MouseMotionEvent.STATE, e.state);
                memPutFloat(p + SDL_MouseMotionEvent.X, e.x);
                memPutFloat(p + SDL_MouseMotionEvent.Y, e.y);
                memPutFloat(p + SDL_MouseMotionEvent.XREL, e.xrel);
                memPutFloat(p + SDL_MouseMotionEvent.YREL, e.yrel);
                break;
            case SDL_EVENT_MOUSE_BUTTON_DOWN:
            case SDL_EVENT_MOUSE_BUTTON_UP:
                memPutInt(p + SDL_MouseButtonEvent.WINDOWID, e.windowId);
                memPutInt(p + SDL_MouseButtonEvent.WHICH, 1);
                memPutByte(p + SDL_MouseButtonEvent.BUTTON, (byte) e.button);
                memPutByte(p + SDL_MouseButtonEvent.DOWN, (byte) (e.down ? 1 : 0));
                memPutByte(p + SDL_MouseButtonEvent.CLICKS, (byte) 1);
                memPutFloat(p + SDL_MouseButtonEvent.X, e.x);
                memPutFloat(p + SDL_MouseButtonEvent.Y, e.y);
                break;
            case SDL_EVENT_MOUSE_WHEEL:
                memPutInt(p + SDL_MouseWheelEvent.WINDOWID, e.windowId);
                memPutInt(p + SDL_MouseWheelEvent.WHICH, 1);
                memPutFloat(p + SDL_MouseWheelEvent.X, e.x);
                memPutFloat(p + SDL_MouseWheelEvent.Y, e.y);
                memPutInt(p + SDL_MouseWheelEvent.DIRECTION, 0); // SDL_MOUSEWHEEL_NORMAL
                memPutFloat(p + SDL_MouseWheelEvent.MOUSE_X, e.xrel);
                memPutFloat(p + SDL_MouseWheelEvent.MOUSE_Y, e.yrel);
                memPutInt(p + SDL_MouseWheelEvent.INTEGER_X, Math.round(e.x));
                memPutInt(p + SDL_MouseWheelEvent.INTEGER_Y, Math.round(e.y));
                break;
            default: // window events
                memPutInt(p + SDL_WindowEvent.WINDOWID, e.windowId);
                memPutInt(p + SDL_WindowEvent.DATA1, e.i1);
                memPutInt(p + SDL_WindowEvent.DATA2, e.i2);
                break;
        }
    }

    // ---------------------------------------------------------------- keyboard API
    public static ByteBuffer keyboardState() {
        ensureGlfw();
        return keyboardState;
    }

    public static short modState() {
        return modState;
    }

    public static int keyFromScancode(int scancode) {
        if (scancode <= 0 || scancode >= KeyTables.SCANCODE_TO_KEYCODE.length) {
            return 0;
        }
        return KeyTables.SCANCODE_TO_KEYCODE[scancode];
    }

    public static String keyName(int key) {
        if (key == 0) {
            return "";
        }
        if ((key & (1 << 30)) != 0) {
            int sc = key & ~(1 << 30);
            String n = sc < KeyTables.SCANCODE_NAME.length ? KeyTables.SCANCODE_NAME[sc] : null;
            return n == null ? "" : n;
        }
        switch (key) {
            case 0x0D: return "Return";
            case 0x1B: return "Escape";
            case 0x08: return "Backspace";
            case 0x09: return "Tab";
            case 0x20: return "Space";
            case 0x7F: return "Delete";
            default:
                if (key >= 'a' && key <= 'z') {
                    return String.valueOf((char) (key - 32));
                }
                return new String(Character.toChars(key));
        }
    }

    public static boolean startTextInput(long window) {
        textInput = true;
        return true;
    }

    public static boolean stopTextInput(long window) {
        textInput = false;
        return true;
    }

    // ---------------------------------------------------------------- mouse API
    public static int mouseState(FloatBuffer x, FloatBuffer y) {
        if (x != null) {
            x.put(x.position(), (float) mouseX);
        }
        if (y != null) {
            y.put(y.position(), (float) mouseY);
        }
        return buttonMask;
    }

    public static boolean setRelativeMouseMode(long window, boolean enabled) {
        if (glfwWindow == NULL) {
            return setError("no window");
        }
        if (enabled != relativeMode) {
            relativeMode = enabled;
            GLFW.glfwSetInputMode(glfwWindow, GLFW_CURSOR, enabled ? GLFW_CURSOR_DISABLED : GLFW_CURSOR_NORMAL);
            haveMousePos = false; // avoid a jump in xrel/yrel when the cursor gets re-centred
            debug("relative mouse mode " + enabled);
        }
        return true;
    }

    public static void warpMouse(long window, float x, float y) {
        if (glfwWindow != NULL) {
            GLFW.glfwSetCursorPos(glfwWindow, x, y);
        }
        mouseX = x;
        mouseY = y;
        haveMousePos = true;
    }

    // ---------------------------------------------------------------- clipboard
    public static String clipboardText() {
        if (glfwWindow == NULL) {
            return "";
        }
        String s = GLFW.glfwGetClipboardString(glfwWindow);
        return s == null ? "" : s;
    }

    public static boolean setClipboardText(CharSequence text) {
        if (glfwWindow != NULL) {
            GLFW.glfwSetClipboardString(glfwWindow, text == null ? "" : text);
        }
        return true;
    }

    // ---------------------------------------------------------------- message boxes
    public static boolean showSimpleMessageBox(int flags, CharSequence title, CharSequence message) {
        log("message box: " + title + ": " + message);
        return true;
    }

    public static boolean showMessageBox(SDL_MessageBoxData data, IntBuffer buttonId) {
        if (data == null) {
            return setError("no message box data");
        }
        long p = data.address();
        String title = memUTF8Safe(memGetAddress(p + SDL_MessageBoxData.TITLE));
        String message = memUTF8Safe(memGetAddress(p + SDL_MessageBoxData.MESSAGE));
        log("message box: " + title + ": " + message);
        int count = memGetInt(p + SDL_MessageBoxData.NUMBUTTONS);
        long buttons = memGetAddress(p + SDL_MessageBoxData.BUTTONS);
        int chosen = -1;
        for (int i = 0; i < count && buttons != NULL; i++) {
            long b = buttons + (long) i * SDL_MessageBoxButtonData.SIZEOF;
            int id = memGetInt(b + SDL_MessageBoxButtonData.BUTTONID);
            if (chosen == -1) {
                chosen = id;
            }
            if ((memGetInt(b + SDL_MessageBoxButtonData.FLAGS) & SDL_MESSAGEBOX_BUTTON_RETURNKEY_DEFAULT) != 0) {
                chosen = id;
                break;
            }
        }
        if (buttonId != null) {
            buttonId.put(buttonId.position(), chosen);
        }
        return true;
    }

    // ---------------------------------------------------------------- memory
    public static void free(long address) {
        if (address != NULL) {
            nmemFree(address);
        }
    }

    static void freeBuffer(java.nio.Buffer buffer) {
        if (buffer != null) {
            memFree(buffer);
        }
    }
}
