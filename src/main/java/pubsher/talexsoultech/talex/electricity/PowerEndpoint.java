package pubsher.talexsoultech.talex.electricity;

/**
 * 电网端点契约。实现仅能在服务端主线程参与结算。
 */
public interface PowerEndpoint {

    BlockKey key();

    PowerEndpointType type();

    EnergyBuffer buffer();

    long maxReceivePerCycle();

    long maxExtractPerCycle();

    default int priority() {
        return 0;
    }

    default void beforePowerCycle() {
    }

    default void onPowerChanged() {
    }
}
