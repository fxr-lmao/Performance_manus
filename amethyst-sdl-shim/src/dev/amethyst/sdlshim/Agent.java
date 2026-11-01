package dev.amethyst.sdlshim;

import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.MethodModel;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.nio.charset.StandardCharsets;
import java.security.ProtectionDomain;

/**
 * Java agent loaded through the custom version JSON ({@code -javaagent:${library_directory}/...}).
 *
 * <p>It applies two tiny, targeted class patches that Minecraft 26.3 needs on Amethyst iOS:
 * <ol>
 *   <li>{@code MacosUtil.disableCloseWindowMenuItem()} talks to {@code NSApplication}, which does not
 *       exist on iOS. It is turned into a no-op.</li>
 *   <li>{@code NativeImage} calls {@code STBImageResize.nstbir_resize_uint8_linear}, which Amethyst's
 *       LWJGL 3.3.3 does not have. That call is redirected to {@link StbResize}.</li>
 * </ol>
 * Every patch is best-effort: if anything goes wrong the original class is used unchanged.
 */
public final class Agent {
    private Agent() {}

    static final String MACOS_UTIL = "com/mojang/blaze3d/platform/MacosUtil";
    static final String NATIVE_IMAGE = "com/mojang/blaze3d/platform/NativeImage";
    static final String STB_RESIZE = "org/lwjgl/stb/STBImageResize";
    static final String STB_RESIZE_SHIM = "dev/amethyst/sdlshim/StbResize";

    public static void premain(String args, Instrumentation inst) {
        Sdl.log("Amethyst SDL shim " + Sdl.VERSION + " loaded (pojav=" + Sdl.POJAV + ")");
        inst.addTransformer(new Patcher());
    }

    static final class Patcher implements ClassFileTransformer {
        @Override
        public byte[] transform(Module module, ClassLoader loader, String className, Class<?> redefined,
                                ProtectionDomain domain, byte[] bytes) {
            try {
                if (MACOS_UTIL.equals(className)) {
                    byte[] out = noOpMethod(bytes, "disableCloseWindowMenuItem");
                    Sdl.log("patched MacosUtil.disableCloseWindowMenuItem -> no-op");
                    return out;
                }
                if (NATIVE_IMAGE.equals(className) && !stbHasResize2(loader)) {
                    byte[] out = renameClassConstant(bytes, STB_RESIZE, STB_RESIZE_SHIM);
                    if (out != null) {
                        Sdl.log("patched NativeImage: stbir_resize_uint8_linear -> pure Java");
                    }
                    return out;
                }
            } catch (Throwable t) {
                Sdl.log("patch of " + className + " failed, using original: " + t);
            }
            return null;
        }
    }

    /** True if the LWJGL on the classpath already has the stb_image_resize2 binding. */
    static boolean stbHasResize2(ClassLoader loader) {
        try {
            Class<?> c = Class.forName("org.lwjgl.stb.STBImageResize", false,
                loader != null ? loader : ClassLoader.getSystemClassLoader());
            c.getMethod("nstbir_resize_uint8_linear", long.class, int.class, int.class, int.class,
                long.class, int.class, int.class, int.class, int.class);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /** Replaces the body of every method with the given name (must return void) by a plain return. */
    static byte[] noOpMethod(byte[] bytes, String methodName) {
        ClassFile cf = ClassFile.of();
        ClassModel model = cf.parse(bytes);
        return cf.transformClass(model, (builder, element) -> {
            if (element instanceof MethodModel method
                && method.methodName().equalsString(methodName)
                && method.methodTypeSymbol().returnType().descriptorString().equals("V")) {
                builder.withMethod(method.methodName(), method.methodType(), method.flags().flagsMask(),
                    mb -> mb.withCode(code -> code.return_()));
            } else {
                builder.with(element);
            }
        });
    }

    /**
     * Rewrites the constant-pool UTF-8 entry that holds {@code from} (a class's internal name) to
     * {@code to}. Constant-pool entries are addressed by index, not byte offset, so changing the
     * string's length is safe and no stack maps need to be regenerated. Returns null if not found.
     */
    static byte[] renameClassConstant(byte[] in, String from, String to) {
        if (in.length < 10 || (in[0] & 0xFF) != 0xCA || (in[1] & 0xFF) != 0xFE) {
            return null;
        }
        byte[] fromBytes = from.getBytes(StandardCharsets.UTF_8);
        byte[] toBytes = to.getBytes(StandardCharsets.UTF_8);
        int count = u2(in, 8);
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream(in.length + 64);
        out.write(in, 0, 10);
        int p = 10;
        boolean found = false;
        for (int i = 1; i < count; i++) {
            int tag = in[p] & 0xFF;
            int len;
            switch (tag) {
                case 1: { // CONSTANT_Utf8
                    int l = u2(in, p + 1);
                    if (!found && l == fromBytes.length && regionEquals(in, p + 3, fromBytes)) {
                        out.write(1);
                        out.write((toBytes.length >> 8) & 0xFF);
                        out.write(toBytes.length & 0xFF);
                        out.write(toBytes, 0, toBytes.length);
                        p += 3 + l;
                        found = true;
                        continue;
                    }
                    len = 3 + l;
                    break;
                }
                case 3: case 4: len = 5; break;                 // Integer, Float
                case 5: case 6: len = 9; break;                 // Long, Double (take two slots)
                case 7: case 8: case 16: case 19: case 20: len = 3; break;
                case 9: case 10: case 11: case 12: case 17: case 18: len = 5; break;
                case 15: len = 4; break;                        // MethodHandle
                default: throw new IllegalStateException("bad constant tag " + tag + " at " + p);
            }
            out.write(in, p, len);
            p += len;
            if (tag == 5 || tag == 6) {
                i++;
            }
        }
        if (!found) {
            return null;
        }
        out.write(in, p, in.length - p);
        return out.toByteArray();
    }

    private static int u2(byte[] b, int off) {
        return ((b[off] & 0xFF) << 8) | (b[off + 1] & 0xFF);
    }

    private static boolean regionEquals(byte[] b, int off, byte[] what) {
        for (int i = 0; i < what.length; i++) {
            if (b[off + i] != what[i]) {
                return false;
            }
        }
        return true;
    }
}
