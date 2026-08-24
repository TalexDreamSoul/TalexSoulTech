package pubsher.talexsoultech.talex.items.machine.rooter;

import lombok.Getter;
import org.bukkit.Location;
import pubsher.talexsoultech.talex.electricity.BlockKey;
import pubsher.talexsoultech.talex.electricity.PowerCable;

/**
 * 导线规格工厂；普通导线不缓存能量。
 */
@Getter
public class BaseWire {

    private final long throughputPerCycle;
    private final int lossPermille;
    private final String symbol;

    public BaseWire(long throughputPerCycle, int lossPermille, String symbol) {
        if (throughputPerCycle <= 0) throw new IllegalArgumentException("throughput must be positive");
        if (lossPermille < 0 || lossPermille >= 1_000) {
            throw new IllegalArgumentException("lossPermille must be between 0 and 999");
        }
        if (symbol == null || symbol.isBlank()) throw new IllegalArgumentException("symbol must not be blank");
        this.throughputPerCycle = throughputPerCycle;
        this.lossPermille = lossPermille;
        this.symbol = symbol;
    }

    public PowerCable at(Location location) {
        return new PowerCable(BlockKey.from(location), throughputPerCycle, lossPermille, symbol);
    }
}
