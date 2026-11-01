// Amethyst SDL shim: Amethyst's LWJGL 3.3.3 lacks ALC_SOFT_system_events. Reporting it as unsupported
// makes Minecraft 26.3 use its polling audio-device tracker instead.
package org.lwjgl.openal;

public final class SOFTSystemEvents {
    public static final int ALC_EVENT_SUPPORTED_SOFT = 6617;
    public static final int ALC_EVENT_NOT_SUPPORTED_SOFT = 6618;

    private SOFTSystemEvents() {}

    public static int alcEventIsSupportedSOFT(int eventType, int deviceType) { return ALC_EVENT_NOT_SUPPORTED_SOFT; }
    public static boolean alcEventControlSOFT(int[] events, boolean enable) { return false; }
    public static void alcEventCallbackSOFT(SOFTSystemEventProcI callback, long userParam) {}
}
