package pubsher.talexsoultech.talex.electricity.achieve;

/**
 * 接收者 - 拥有此接口可以接收电量
 */
public interface IReceiver extends IPower {

    /**
     * 增加系统所储存的电量
     *
     * @param addCapacity 要增加的电量
     *
     * @return 返回系统所存储的现有的电量
     */
    Capacity saveStorageCapacity(Capacity addCapacity);

    /**
     * @return 返回设备标准电压
     */
    double getCapacityVoltage();

    /**
     * 检测是否能储存电量
     *
     * @param capacity 欲存储的电量
     *
     * @return 是否可以存储
     */
    boolean canStorageCapacity(Capacity capacity);

}
