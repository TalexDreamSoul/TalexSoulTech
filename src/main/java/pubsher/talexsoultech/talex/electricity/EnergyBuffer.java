package pubsher.talexsoultech.talex.electricity;

/**
 * 有界能量缓冲。所有修改都返回实际接收或提取量，不产生负数或溢出。
 */
public final class EnergyBuffer {

    private final long capacity;
    private long stored;

    public EnergyBuffer(long capacity) {
        this(capacity, 0L);
    }

    public EnergyBuffer(long capacity, long stored) {
        if (capacity <= 0) throw new IllegalArgumentException("capacity must be positive");
        if (stored < 0 || stored > capacity) {
            throw new IllegalArgumentException("stored energy must be within buffer capacity");
        }
        this.capacity = capacity;
        this.stored = stored;
    }

    public long receive(long requested, boolean simulate) {
        EnergyUnits.requireNonNegative(requested);
        long accepted = Math.min(requested, free());
        if (!simulate) stored += accepted;
        return accepted;
    }

    public long extract(long requested, boolean simulate) {
        EnergyUnits.requireNonNegative(requested);
        long extracted = Math.min(requested, stored);
        if (!simulate) stored -= extracted;
        return extracted;
    }

    public long capacity() {
        return capacity;
    }

    public long stored() {
        return stored;
    }

    public long free() {
        return capacity - stored;
    }
}
