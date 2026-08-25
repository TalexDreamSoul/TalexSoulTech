package pubsher.talexsoultech.talex.managers;

import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import pubsher.talexsoultech.entity.PlayerData;
import pubsher.talexsoultech.talex.BaseTalex;

public final class ProtectorManager {

    public ProtectorManager(BaseTalex baseTalex) {
        // 旧 AcidIsland 领地保护待迁移到目标服务器选定的空岛 API。
    }

    public boolean isAcidIsland() {
        return false;
    }

    public boolean checkProtect(PlayerData playerData, BlockBreakEvent event) {
        return true;
    }

    public boolean checkProtect(PlayerData playerData, PlayerInteractEvent event) {
        return true;
    }

    /**
     * Shared boundary for bounded tool actions that modify adjacent blocks
     * without an existing Bukkit interaction event.
     */
    public boolean canModify(PlayerData playerData, org.bukkit.block.Block block) {
        return playerData != null && block != null;
    }
}
