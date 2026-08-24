package pubsher.talexsoultech.talex.electricity;

/**
 * 灵魂电量单位。领域层使用毫 SE，界面层才转换成小数 SE。
 */
public final class EnergyUnits {

    public static final long MILLI_SE_PER_SE = 1_000L;

    private EnergyUnits() {
    }

    public static long fromSe(double se) {
        if (!Double.isFinite(se) || se < 0 || se > (double) Long.MAX_VALUE / MILLI_SE_PER_SE) {
            throw new IllegalArgumentException("SE must be a finite non-negative value");
        }
        return Math.round(se * MILLI_SE_PER_SE);
    }

    public static double toSe(long milliSe) {
        requireNonNegative(milliSe);
        return (double) milliSe / MILLI_SE_PER_SE;
    }

    public static String format(long milliSe, int decimals) {
        requireNonNegative(milliSe);
        if (decimals < 0 || decimals > 3) {
            throw new IllegalArgumentException("decimals must be between 0 and 3");
        }

        long whole = milliSe / MILLI_SE_PER_SE;
        if (decimals == 0) return Long.toString(whole);

        long remainder = milliSe % MILLI_SE_PER_SE;
        long divisor = switch (decimals) {
            case 1 -> 100L;
            case 2 -> 10L;
            default -> 1L;
        };
        String fraction = Long.toString(remainder / divisor);
        return whole + "." + "0".repeat(decimals - fraction.length()) + fraction;
    }

    public static void requireNonNegative(long amount) {
        if (amount < 0) throw new IllegalArgumentException("energy must be non-negative");
    }
}
