package pubsher.talexsoultech.talex.machine;

import org.bukkit.event.player.PlayerEvent;
import pubsher.talexsoultech.entity.PlayerData;
import pubsher.talexsoultech.inventory.guider.GuiderBook;

public interface IMachine {

    /**
     * 当打开机器时
     *
     * @param playerData 玩家信息
     * @param event      事件
     */
    void onOpenMachine(PlayerData playerData, PlayerEvent event);

    /**
     * 当查阅配方时
     *
     * @param guiderBook GuiderBook
     *
     * @return 是否允许打开
     */
    boolean onOpenRecipeView(GuiderBook guiderBook);

    /**
     * 当机器保存时
     */
    String onSave();

    /**
     * 当机器加载时
     */
    void onLoad(String str);

}
