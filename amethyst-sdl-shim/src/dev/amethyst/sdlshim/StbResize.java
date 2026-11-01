package dev.amethyst.sdlshim;

import static org.lwjgl.system.MemoryUtil.memGetByte;
import static org.lwjgl.system.MemoryUtil.memPutByte;
import static org.lwjgl.system.MemoryUtil.nmemAlloc;

/**
 * Pure-Java stand-in for {@code stbir_resize_uint8_linear} (stb_image_resize2), which Minecraft 26.3
 * calls from {@code NativeImage.resizeSubRectTo}. Amethyst bundles LWJGL 3.3.3, whose native STB only
 * has the older stb_image_resize API, so the agent redirects that one call here.
 *
 * <p>Down-scaling uses an area average (box filter); up-scaling uses bilinear sampling. The output is
 * not bit-identical to stb's Mitchell/Catmull-Rom filters, but it is used for world thumbnails and
 * similar images where that does not matter.
 */
public final class StbResize {
    private StbResize() {}

    /** Matches {@code STBImageResize.nstbir_resize_uint8_linear(JIIIJIIII)J} from LWJGL 3.4.x. */
    public static long nstbir_resize_uint8_linear(long input, int inW, int inH, int inStride,
                                                  long output, int outW, int outH, int outStride,
                                                  int pixelLayout) {
        if (input == 0L || inW <= 0 || inH <= 0 || outW <= 0 || outH <= 0) {
            return 0L;
        }
        int channels = channels(pixelLayout);
        if (inStride == 0) {
            inStride = inW * channels;
        }
        if (outStride == 0) {
            outStride = outW * channels;
        }
        if (output == 0L) {
            output = nmemAlloc((long) outStride * outH);
            if (output == 0L) {
                return 0L;
            }
        }

        double sx = (double) inW / outW;
        double sy = (double) inH / outH;
        double[] acc = new double[channels];

        for (int oy = 0; oy < outH; oy++) {
            for (int ox = 0; ox < outW; ox++) {
                long dst = output + (long) oy * outStride + (long) ox * channels;
                if (sx > 1.0 || sy > 1.0) {
                    // Area average over the source rectangle covered by this output pixel.
                    int x0 = (int) Math.floor(ox * sx);
                    int x1 = Math.max(x0 + 1, Math.min(inW, (int) Math.ceil((ox + 1) * sx)));
                    int y0 = (int) Math.floor(oy * sy);
                    int y1 = Math.max(y0 + 1, Math.min(inH, (int) Math.ceil((oy + 1) * sy)));
                    java.util.Arrays.fill(acc, 0.0);
                    int count = 0;
                    for (int y = y0; y < y1; y++) {
                        long row = input + (long) y * inStride;
                        for (int x = x0; x < x1; x++) {
                            long src = row + (long) x * channels;
                            for (int c = 0; c < channels; c++) {
                                acc[c] += memGetByte(src + c) & 0xFF;
                            }
                            count++;
                        }
                    }
                    for (int c = 0; c < channels; c++) {
                        memPutByte(dst + c, (byte) Math.round(acc[c] / count));
                    }
                } else {
                    // Bilinear sample at the pixel centre.
                    double fx = Math.max(0.0, (ox + 0.5) * sx - 0.5);
                    double fy = Math.max(0.0, (oy + 0.5) * sy - 0.5);
                    int x0 = Math.min((int) fx, inW - 1);
                    int y0 = Math.min((int) fy, inH - 1);
                    int x1 = Math.min(x0 + 1, inW - 1);
                    int y1 = Math.min(y0 + 1, inH - 1);
                    double ax = fx - x0;
                    double ay = fy - y0;
                    long r0 = input + (long) y0 * inStride;
                    long r1 = input + (long) y1 * inStride;
                    for (int c = 0; c < channels; c++) {
                        double p00 = memGetByte(r0 + (long) x0 * channels + c) & 0xFF;
                        double p10 = memGetByte(r0 + (long) x1 * channels + c) & 0xFF;
                        double p01 = memGetByte(r1 + (long) x0 * channels + c) & 0xFF;
                        double p11 = memGetByte(r1 + (long) x1 * channels + c) & 0xFF;
                        double top = p00 + (p10 - p00) * ax;
                        double bottom = p01 + (p11 - p01) * ax;
                        memPutByte(dst + c, (byte) Math.round(top + (bottom - top) * ay));
                    }
                }
            }
        }
        return output;
    }

    /** Channel count for an {@code stbir_pixel_layout} value. */
    static int channels(int layout) {
        switch (layout) {
            case 1:                 // STBIR_1CHANNEL
                return 1;
            case 2: case 9: case 10: case 15: case 16: // 2CHANNEL, RA, AR, RA_PM, AR_PM
                return 2;
            case 0: case 3:         // BGR, RGB
                return 3;
            default:                // RGBA, 4CHANNEL, BGRA, ARGB, ABGR and the _PM variants
                return 4;
        }
    }
}
