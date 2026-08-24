package pubsher.talexsoultech.entity;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import pubsher.talexsoultech.entity.attract.PlayerAttractData;
import pubsher.talexsoultech.inventory.MenuBasic;
import pubsher.talexsoultech.talex.BaseTalex;
import pubsher.talexsoultech.talex.items.GuideBookItem;
import pubsher.talexsoultech.utils.NBTsUtil;

import java.util.Objects;
import java.util.UUID;

@Accessors(chain = true)
@Getter
@Setter
public class PlayerData {

    private final BaseTalex talex;
    private final PlayerAttractData playerAttractData;
    private final Player player;
    private final String name;
    private final UUID uuid;
    private final boolean persistenceWritable;
    private JsonObject jsonData;
    private MenuBasic lastGuider;

    private PlayerData(
            BaseTalex talex,
            Player player,
            JsonObject jsonData,
            YamlConfiguration attractConfiguration,
            boolean persistenceWritable
    ) {

        if (player == null || !player.isOnline()) {
            throw new IllegalArgumentException("player must be online when PlayerData is published");
        }

        this.talex = Objects.requireNonNull(talex, "talex");
        this.player = player;
        this.name = player.getName();
        this.uuid = player.getUniqueId();
        this.persistenceWritable = persistenceWritable;
        this.jsonData = Objects.requireNonNull(jsonData, "jsonData");

        if (!this.jsonData.has("category_unlock") || this.jsonData.get("category_unlock").isJsonNull()) {
            this.jsonData.addProperty("category_unlock", "");
        }

        this.playerAttractData = new PlayerAttractData(this, attractConfiguration);

    }

    public static PlayerData createDefault(BaseTalex talex, Player player, boolean persistenceWritable) {

        JsonObject jsonData = new JsonObject();
        jsonData.addProperty("category_unlock", "");
        return new PlayerData(talex, player, jsonData, null, persistenceWritable);

    }

    public static PlayerData fromPersisted(BaseTalex talex, Player player, String encodedInfo) {

        String decodedInfo = NBTsUtil.Base64_Decode(encodedInfo);
        JsonObject jsonData = JsonParser.parseString(decodedInfo).getAsJsonObject();
        return new PlayerData(talex, player, jsonData, loadAttractConfiguration(jsonData), true);

    }

    private static YamlConfiguration loadAttractConfiguration(JsonObject jsonData) {

        JsonElement encodedAttractData = jsonData.get("attract_data");
        if (encodedAttractData == null || encodedAttractData.isJsonNull()) {
            return null;
        }

        String encoded = encodedAttractData.getAsString();
        if (encoded.isEmpty()) {
            return null;
        }

        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.loadFromString(NBTsUtil.Base64_Decode(encoded));
        } catch (InvalidConfigurationException exception) {
            throw new IllegalArgumentException("invalid persisted attraction data", exception);
        }

        return yaml;

    }

    public void announceLoaded() {

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            onlinePlayer.sendActionBar("§8§l▸ §e" + this.name + " §a加入了游戏!");
            onlinePlayer.playSound(onlinePlayer.getLocation(), Sound.BLOCK_NOTE_BLOCK_FLUTE, 1.1F, 1.1F);
        }

        if (!isGuideInstalled()) {
            new GuideBookItem(this);
            player.sendTitle("", "§e你获得了 §5灵魂科技 §e向导书!", 5, 15, 5);
        }

    }

    public PersistenceSnapshot snapshotForPersistence() {

        if (!persistenceWritable) {
            throw new IllegalStateException("read-only player data must not be persisted");
        }

        JsonObject snapshot = JsonParser.parseString(this.jsonData.toString()).getAsJsonObject();
        snapshot.addProperty("attract_data", NBTsUtil.Base64_Encode(this.playerAttractData.toString()));

        return new PersistenceSnapshot(
                this.uuid,
                this.name,
                NBTsUtil.Base64_Encode(snapshot.toString())
        );

    }

    public ItemStack reducePlayerHandItem(int amo) {

        ItemStack stack = player.getInventory().getItemInHand();

        if (stack == null || stack.getType() == Material.AIR) {
            return null;
        }

        ItemStack stack2 = stack.clone();

        stack.setAmount(stack.getAmount() - amo);

        if (stack.getAmount() < 1) {
            stack.setType(Material.AIR);
        }

        return stack2;

    }

    public PlayerData addProperty(String key, String value) {

        this.jsonData.addProperty(key, value);
        return this;

    }

    public void addProperty(String key, boolean value) {

        this.jsonData.addProperty(key, value);

    }

    public PlayerData delCategoryUnlock(String ID) {

        if (isCategoryUnLock(ID)) {
            this.jsonData.addProperty("category_unlock", this.jsonData.get("category_unlock").getAsString().replaceFirst(ID + ", ", ""));
        }

        return this;

    }

    public PlayerData addCategoryUnlock(String ID) {

        if (!isCategoryUnLock(ID)) {
            this.jsonData.addProperty("category_unlock", this.jsonData.get("category_unlock").getAsString() + ID + ", ");
        }

        return this;

    }

    public boolean isCategoryUnLock(String ID) {

        String str = this.jsonData.get("category_unlock").getAsString();
        return str.contains(ID + ",");

    }

    public PlayerData dropItem(ItemStack stack) {

        if (stack == null || stack.getType() == Material.AIR) {
            return this;
        }

        player.getWorld().dropItem(player.getLocation().add(0, 0.5, 0), stack);
        return this;

    }

    public PlayerData delayRunTimerAsync(PlayerDataRunnable runnable, long delay, long timer) {

        new BukkitRunnable() {
            @Override
            public void run() {
                if (runnable.isCancelled()) {
                    cancel();
                    return;
                }

                runnable.run();
            }
        }.runTaskTimerAsynchronously(talex.getPlugin(), delay, timer);

        return this;

    }

    public PlayerData delayRunAsync(PlayerDataRunnable runnable, long delay) {

        new BukkitRunnable() {
            @Override
            public void run() {
                if (runnable.isCancelled()) {
                    cancel();
                    return;
                }

                runnable.run();
            }
        }.runTaskLaterAsynchronously(talex.getPlugin(), delay);

        return this;

    }

    public PlayerData delayRunTimer(PlayerDataRunnable runnable, long delay, long timer) {

        new BukkitRunnable() {
            @Override
            public void run() {
                if (runnable.isCancelled()) {
                    cancel();
                    return;
                }

                runnable.run();
            }
        }.runTaskTimer(talex.getPlugin(), delay, delay);

        return this;

    }

    public PlayerData delayRun(PlayerDataRunnable runnable, long delay) {

        new BukkitRunnable() {
            @Override
            public void run() {
                if (runnable.isCancelled()) {
                    cancel();
                    return;
                }

                runnable.run();
            }
        }.runTaskLater(talex.getPlugin(), delay);

        return this;

    }

    public PlayerData runTask(PlayerDataRunnable runnable) {

        new BukkitRunnable() {
            @Override
            public void run() {
                if (runnable.isCancelled()) {
                    cancel();
                    return;
                }

                runnable.run();
            }
        }.runTask(talex.getPlugin());

        return this;

    }

    public PlayerData runTaskAsync(PlayerDataRunnable runnable) {

        new BukkitRunnable() {
            @Override
            public void run() {
                if (runnable.isCancelled()) {
                    cancel();
                    return;
                }

                runnable.run();
            }
        }.runTaskAsynchronously(talex.getPlugin());

        return this;

    }

    public PlayerData closeInventory() {

        player.closeInventory();
        return this;

    }

    public boolean isGuideInstalled() {

        return this.jsonData.has("installed") && this.jsonData.get("installed").getAsBoolean();

    }

    public PlayerData title(String title, String subTitle, int fadeIn, int stay, int fadeOut, int delay) {

        new BukkitRunnable() {
            @Override
            public void run() {
                title(title, subTitle, fadeIn, stay, fadeOut);
            }
        }.runTaskLater(getTalex().getPlugin(), delay);

        return this;

    }

    public PlayerData title(String title, String subTitle, int fadeIn, int stay, int fadeOut) {

        this.player.sendTitle(
                ChatColor.translateAlternateColorCodes('&', title),
                ChatColor.translateAlternateColorCodes('&', subTitle),
                fadeIn,
                stay,
                fadeOut
        );

        return this;

    }

    public PlayerData playSound(Sound sound, float f, float v) {

        new BukkitRunnable() {
            @Override
            public void run() {
                player.playSound(player.getLocation(), sound, f, v);
            }
        }.runTask(talex.getPlugin());

        return this;

    }

    public PlayerData actionBar(String message) {

        new BukkitRunnable() {
            @Override
            public void run() {
                player.sendActionBar(ChatColor.translateAlternateColorCodes('&', message));
            }
        }.runTask(talex.getPlugin());

        return this;

    }

    @Override
    public int hashCode() {

        return this.player.hashCode();

    }

    @Override
    public boolean equals(Object obj) {

        if (!(obj instanceof PlayerData)) {
            return false;
        }

        PlayerData playerData = (PlayerData) obj;
        return playerData.hashCode() == this.hashCode();

    }

    public record PersistenceSnapshot(UUID uuid, String name, String encodedInfo) {

        public PersistenceSnapshot {
            Objects.requireNonNull(uuid, "uuid");
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(encodedInfo, "encodedInfo");
        }

    }

}
