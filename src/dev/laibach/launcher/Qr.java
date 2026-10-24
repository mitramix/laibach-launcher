package dev.laibach.launcher;

import java.nio.charset.StandardCharsets;

/**
 * A QR code of the one size the exchange needs: version 3, error correction
 * level M, byte mode, up to 42 bytes. Enough for a comrade's link and kept
 * small enough to write by hand, after Project Nayuki's reference encoder.
 * Plain Java, so it can be checked away from the phone.
 */
final class Qr {
    static final int SIZE = 29;
    private static final int DATA = 44, CHECK = 26;
    /** The level's two format bits: M is 00. */
    private static final int LEVEL_M = 0;

    private final boolean[][] dark = new boolean[SIZE][SIZE];
    private final boolean[][] fixed = new boolean[SIZE][SIZE];

    private Qr() {}

    /** The modules of the code for the text, row by row; true is dark. */
    static boolean[][] encode(String text) {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > DATA - 2) throw new IllegalArgumentException("too long for version 3-M");
        byte[] data = codewords(bytes);
        byte[] all = new byte[DATA + CHECK];
        System.arraycopy(data, 0, all, 0, DATA);
        System.arraycopy(remainder(data, divisor(CHECK)), 0, all, DATA, CHECK);

        Qr q = new Qr();
        q.patterns();
        q.place(all);
        int best = 0;
        long least = Long.MAX_VALUE;
        for (int m = 0; m < 8; m++) {
            q.mask(m);
            q.format(m);
            long p = q.penalty();
            if (p < least) {
                least = p;
                best = m;
            }
            q.mask(m);
        }
        q.mask(best);
        q.format(best);
        return q.dark;
    }

    /** Mode, count, the bytes, the terminator and the pad, as 44 codewords. */
    private static byte[] codewords(byte[] bytes) {
        int[] bits = new int[DATA * 8];
        int n = 0;
        n = put(bits, n, 0b0100, 4);
        n = put(bits, n, bytes.length, 8);
        for (byte b : bytes) n = put(bits, n, b & 0xFF, 8);
        n = Math.min(n + 4, bits.length);
        n = (n + 7) / 8 * 8;
        byte[] out = new byte[DATA];
        for (int i = 0; i < n; i++) out[i >>> 3] |= (byte) (bits[i] << (7 - (i & 7)));
        for (int i = n / 8, pad = 0xEC; i < DATA; i++, pad ^= 0xEC ^ 0x11) out[i] = (byte) pad;
        return out;
    }

    private static int put(int[] bits, int at, int value, int length) {
        for (int i = length - 1; i >= 0; i--) bits[at++] = (value >>> i) & 1;
        return at;
    }

    // ---- Reed-Solomon over GF(256), modulo x^8 + x^4 + x^3 + x^2 + 1 --------

    private static byte[] divisor(int degree) {
        byte[] result = new byte[degree];
        result[degree - 1] = 1;
        int root = 1;
        for (int i = 0; i < degree; i++) {
            for (int j = 0; j < degree; j++) {
                result[j] = (byte) multiply(result[j] & 0xFF, root);
                if (j + 1 < degree) result[j] ^= result[j + 1];
            }
            root = multiply(root, 0x02);
        }
        return result;
    }

    private static byte[] remainder(byte[] data, byte[] divisor) {
        byte[] result = new byte[divisor.length];
        for (byte b : data) {
            int factor = (b ^ result[0]) & 0xFF;
            System.arraycopy(result, 1, result, 0, result.length - 1);
            result[result.length - 1] = 0;
            for (int i = 0; i < result.length; i++) result[i] ^= (byte) multiply(divisor[i] & 0xFF, factor);
        }
        return result;
    }

    private static int multiply(int x, int y) {
        int z = 0;
        for (int i = 7; i >= 0; i--) {
            z = (z << 1) ^ ((z >>> 7) * 0x11D);
            z ^= ((y >>> i) & 1) * x;
        }
        return z;
    }

    // ---- The fixed patterns -------------------------------------------------

    private void set(int x, int y, boolean on) {
        dark[y][x] = on;
        fixed[y][x] = true;
    }

    private void patterns() {
        for (int i = 0; i < SIZE; i++) {
            set(6, i, i % 2 == 0);
            set(i, 6, i % 2 == 0);
        }
        finder(3, 3);
        finder(SIZE - 4, 3);
        finder(3, SIZE - 4);
        // Version 3 has one alignment pattern, clear of the finders.
        for (int dy = -2; dy <= 2; dy++) {
            for (int dx = -2; dx <= 2; dx++) set(22 + dx, 22 + dy, Math.max(Math.abs(dx), Math.abs(dy)) != 1);
        }
        format(0);
    }

    private void finder(int cx, int cy) {
        for (int dy = -4; dy <= 4; dy++) {
            for (int dx = -4; dx <= 4; dx++) {
                int x = cx + dx, y = cy + dy, d = Math.max(Math.abs(dx), Math.abs(dy));
                if (x >= 0 && x < SIZE && y >= 0 && y < SIZE) set(x, y, d != 2 && d != 4);
            }
        }
    }

    /** The level and the mask, twice over, with their BCH check. */
    private void format(int mask) {
        int data = LEVEL_M << 3 | mask, rem = data;
        for (int i = 0; i < 10; i++) rem = (rem << 1) ^ ((rem >>> 9) * 0x537);
        int bits = (data << 10 | rem) ^ 0x5412;
        for (int i = 0; i <= 5; i++) set(8, i, bit(bits, i));
        set(8, 7, bit(bits, 6));
        set(8, 8, bit(bits, 7));
        set(7, 8, bit(bits, 8));
        for (int i = 9; i < 15; i++) set(14 - i, 8, bit(bits, i));
        for (int i = 0; i < 8; i++) set(SIZE - 1 - i, 8, bit(bits, i));
        for (int i = 8; i < 15; i++) set(8, SIZE - 15 + i, bit(bits, i));
        set(8, SIZE - 8, true);
    }

    private static boolean bit(int value, int i) {
        return ((value >>> i) & 1) != 0;
    }

    // ---- The data, its mask and the choice of mask ----------------------------

    /** Up and down the columns in pairs from the right, stepping over the timing column. */
    private void place(byte[] words) {
        int i = 0;
        for (int right = SIZE - 1; right >= 1; right -= 2) {
            if (right == 6) right = 5;
            for (int vert = 0; vert < SIZE; vert++) {
                for (int j = 0; j < 2; j++) {
                    int x = right - j;
                    boolean upward = ((right + 1) & 2) == 0;
                    int y = upward ? SIZE - 1 - vert : vert;
                    if (!fixed[y][x] && i < words.length * 8) {
                        dark[y][x] = bit(words[i >>> 3], 7 - (i & 7));
                        i++;
                    }
                }
            }
        }
    }

    /** Flips the data modules the mask chooses; doing it twice undoes it. */
    private void mask(int m) {
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                if (fixed[y][x]) continue;
                boolean flip;
                switch (m) {
                    case 0: flip = (x + y) % 2 == 0; break;
                    case 1: flip = y % 2 == 0; break;
                    case 2: flip = x % 3 == 0; break;
                    case 3: flip = (x + y) % 3 == 0; break;
                    case 4: flip = (x / 3 + y / 2) % 2 == 0; break;
                    case 5: flip = x * y % 2 + x * y % 3 == 0; break;
                    case 6: flip = (x * y % 2 + x * y % 3) % 2 == 0; break;
                    default: flip = ((x + y) % 2 + x * y % 3) % 2 == 0; break;
                }
                dark[y][x] ^= flip;
            }
        }
    }

    /** The standard's four penalties: runs, blocks, finder look-alikes and imbalance. */
    private long penalty() {
        long p = 0;
        int black = 0;
        for (int pass = 0; pass < 2; pass++) {
            for (int a = 0; a < SIZE; a++) {
                int run = 0;
                boolean last = false;
                StringBuilder line = new StringBuilder("0000");
                for (int b = 0; b < SIZE; b++) {
                    boolean d = pass == 0 ? dark[a][b] : dark[b][a];
                    line.append(d ? '1' : '0');
                    if (b > 0 && d == last) {
                        run++;
                    } else {
                        if (run >= 5) p += run - 2;
                        run = 1;
                    }
                    last = d;
                }
                if (run >= 5) p += run - 2;
                String row = line.append("0000").toString();
                for (int at = row.indexOf("1011101"); at >= 0; at = row.indexOf("1011101", at + 1)) {
                    if (row.startsWith("0000", at - 4) || row.startsWith("0000", at + 7)) p += 40;
                }
            }
        }
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                if (dark[y][x]) black++;
                if (x + 1 < SIZE && y + 1 < SIZE) {
                    boolean d = dark[y][x];
                    if (dark[y][x + 1] == d && dark[y + 1][x] == d && dark[y + 1][x + 1] == d) p += 3;
                }
            }
        }
        int total = SIZE * SIZE;
        int k = (Math.abs(black * 20 - total * 10) + total - 1) / total - 1;
        return p + Math.max(0, k) * 10L;
    }
}
