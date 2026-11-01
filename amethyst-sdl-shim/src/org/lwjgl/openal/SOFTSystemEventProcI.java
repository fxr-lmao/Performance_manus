// Amethyst SDL shim: stand-in for LWJGL 3.4's ALC_SOFT_system_events callback type.
package org.lwjgl.openal;

@FunctionalInterface
public interface SOFTSystemEventProcI {
    void invoke(int eventType, int deviceType, long device, int length, long message, long userParam);
}
