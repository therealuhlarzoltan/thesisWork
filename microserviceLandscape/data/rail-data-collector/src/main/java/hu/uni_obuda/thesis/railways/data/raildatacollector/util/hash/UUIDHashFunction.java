package hu.uni_obuda.thesis.railways.data.raildatacollector.util.hash;

import java.util.UUID;

public final class UUIDHashFunction  {

    private UUIDHashFunction() {

    }

    public static Integer apply(UUID uuid) {
        long msb = uuid.getMostSignificantBits();
        long lsb = uuid.getLeastSignificantBits();

        long h1 = mix64(msb ^ 0x9E3779B97F4A7C15L);
        long h2 = mix64(lsb ^ 0xC2B2AE3D27D4EB4FL);
        long combined = h1 ^ (h2 + 0x9E3779B97F4A7C15L);

        int x = (int) (combined ^ (combined >>> 32)); // fold to 32-bit
        // Murmur3 fmix32 avalanche for uniformity
        x ^= x >>> 16;
        x *= 0x85ebca6b;
        x ^= x >>> 13;
        x *= 0xc2b2ae35;
        x ^= x >>> 16;

        int pos = x & 0x7fffffff;
        return (pos == 0) ? 1 : pos; // ensure >= 1 without overflow
    }

    private static long mix64(long z) {
        z ^= (z >>> 30);
        z *= 0xBF58476D1CE4E5B9L;
        z ^= (z >>> 27);
        z *= 0x94D049BB133111EBL;
        z ^= (z >>> 31);
        return z;
    }
}
