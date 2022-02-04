package pubsher.talexsoultech.inventory;

import pubsher.talexsoultech.data.enums.LocationFloat;
import pubsher.talexsoultech.utils.inventory.InventoryUI;

/**
 * @Description: 为您的容器批量打印物品
 */
public interface IInventoryPainter {

    InventoryUI.ClickableItem onDrawBorder(int slot);

    InventoryPainter drawBorder();

    InventoryUI.ClickableItem onDrawLine(int slot);

    InventoryPainter drawLine();

    InventoryPainter drawLineRow(int row);

    InventoryPainter drawLine(int startSlot);

    /**
     * @Description: Full代表一行9个 不然一行7个
     */
    InventoryPainter drawLineFull();

    InventoryPainter drawLineRowFull(int row);

    InventoryPainter drawLineFull(int startSlot);

    /**
     * 填充整个容器
     *
     * @return 支持链式调用
     */
    InventoryPainter drawFull();

    /**
     * 填充屏幕的物品
     *
     * @param slot 当前要填充的格子
     *
     * @return 返回要填充的物品
     */
    InventoryUI.ClickableItem onDrawFull(int slot);

    /**
     * 基于一个坐标打印周围物品
     *
     * @param slot 槽位标准
     * @param item 答应的物品
     */
    InventoryPainter drawArena9(int slot, InventoryUI.ClickableItem item);

    /**
     * 打印水平位置的 进度条
     *
     * @param row      打印的行 行数从 0 开始
     * @param maxWidth 最多打印数量
     * @param percent  进度条百分比
     * @param location 进度条浮动位置 {@link LocationFloat}
     * @param finished 当进度条走过的物品
     * @param will     进度条没有到达的位置显示的物品
     */
    InventoryPainter drawProgressBarHorizontal(int row, int maxWidth, double percent, LocationFloat location, InventoryUI.ClickableItem finished, InventoryUI.ClickableItem will);

}
