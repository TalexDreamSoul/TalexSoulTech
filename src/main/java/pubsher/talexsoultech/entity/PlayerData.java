package pubsher.talexsoultech.entity;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import pubsher.talexsoultech.builder.SqlAddBuilder;
import pubsher.talexsoultech.builder.SqlBuilder;
import pubsher.talexsoultech.builder.SqlUpdBuilder;
import pubsher.talexsoultech.entity.attract.PlayerAttractData;
import pubsher.talexsoultech.inventory.MenuBasic;
import pubsher.talexsoultech.talex.BaseTalex;
import pubsher.talexsoultech.talex.items.GuideBookItem;
import pubsher.talexsoultech.utils.NBTsUtil;

import java.sql.ResultSet;
import java.util.UUID;

/**
 * @author TalexDreamSoul
 */
@Accessors( chain = true )
@Getter
@Setter
public class PlayerData {

    private final BaseTalex talex;

    private final PlayerAttractData playerAttractData;

    private final Player player;
    private final String name;
    private final UUID uuid;
    private JsonObject jsonData;
    private MenuBasic lastGuider;

    @SneakyThrows
    public PlayerData(BaseTalex talex, Player player) {

        long a = System.currentTimeMillis();

        if ( player == null || !player.isOnline() ) {

            throw new RuntimeException("player error!");

        }

        this.talex = talex;

        this.player = player;
        this.name = player.getName();
        this.uuid = player.getUniqueId();

        talex.getPlayerManager().put(player.getName(), this);

        ResultSet rs = talex.getMysqlManager().readSearchData("soul_tech_player_data", "st_name", player.getName());

        if ( rs == null || !rs.next() ) {

            this.playerAttractData = new PlayerAttractData(this, null);

            this.jsonData = new JsonObject();

            this.jsonData.addProperty("category_unlock", "");

        } else {

            String data = rs.getString("st_info");

            data = NBTsUtil.Base64_Decode(data);

            JsonObject json = new JsonParser().parse(data).getAsJsonObject();

            String attractStr = json.get("attract_data").getAsString();

            attractStr = NBTsUtil.Base64_Decode(attractStr);

            YamlConfiguration yaml = new YamlConfiguration();

            yaml.loadFromString(attractStr);

            this.playerAttractData = new PlayerAttractData(this, yaml);
            this.jsonData = json;

        }

        talex.getPlugin().getLogger().info(" ---> " + this.player.getName() + " 数据加载完毕! (" + ( System.currentTimeMillis() - a ) + "ms)");

        PlayerData ins = this;

        new BukkitRunnable() {

            @Override
            public void run() {

                for ( Player player : Bukkit.getOnlinePlayers() ) {

                    player.sendActionBar("§8§l▸ §e" + PlayerData.this.player.getName() + " §a加入了游戏!");
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_FLUTE, 1.1F, 1.1F);

                }

                if ( !isFirstUse() ) {

                    new GuideBookItem(ins);

                    player.sendTitle("", "§e你获得了 §5灵魂科技 §e向导书!", 5, 15, 5);

                }

                jsonData.addProperty("installed", true);

            }
        }.runTask(talex.getPlugin());

    }

    public ItemStack reducePlayerHandItem(int amo) {

        ItemStack stack = player.getInventory().getItemInHand();

        if ( stack == null || stack.getType() == Material.AIR ) {
            return null;
        }

        ItemStack stack2 = stack.clone();

        stack.setAmount(stack.getAmount() - amo);

        if ( stack.getAmount() < 1 ) {

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

        if ( isCategoryUnLock(ID) ) {

            this.jsonData.addProperty("category_unlock", this.jsonData.get("category_unlock").getAsString().replaceFirst(ID + ", ", ""));

        }

        return this;

    }

    public PlayerData addCategoryUnlock(String ID) {

        if ( !isCategoryUnLock(ID) ) {

            this.jsonData.addProperty("category_unlock", this.jsonData.get("category_unlock").getAsString() + ID + ", ");

//            ParticleUtil.CircleJumpUp(player, Particle.FIREWORKS_SPARK, 5);

        }

        return this;

    }

    public boolean isCategoryUnLock(String ID) {

        String str = this.jsonData.get("category_unlock").getAsString();

        return str.contains(ID + ",");

    }

    public PlayerData dropItem(ItemStack stack) {

        if ( stack == null || stack.getType() == Material.AIR ) {
            return this;
        }

        player.getWorld().dropItem(player.getLocation().add(0, 0.5, 0), stack);

        return this;

    }

//    public PlayerData onRun(String ID) {
//
//        return this;
//
//    }

    public PlayerData delayRunTimerAsync(PlayerDataRunnable runnable, long delay, long timer) {

        new BukkitRunnable() {

            @Override
            public void run() {

                if ( runnable.isCancelled() ) {

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

                if ( runnable.isCancelled() ) {

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

                if ( runnable.isCancelled() ) {

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

                if ( runnable.isCancelled() ) {

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

                if ( runnable.isCancelled() ) {

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

                if ( runnable.isCancelled() ) {

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

    public boolean isFirstUse() {

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

        this.player.sendTitle(ChatColor.translateAlternateColorCodes('&', title), ChatColor.translateAlternateColorCodes('&', subTitle), fadeIn, stay, fadeOut);

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

    @SneakyThrows
    public void leave() {

        talex.getPlayerManager().remove(this.name);

        this.jsonData.addProperty("attract_data", NBTsUtil.Base64_Encode(this.playerAttractData.toString()));

        SqlBuilder sb;

        ResultSet rs = talex.getMysqlManager().readSearchData("soul_tech_player_data", "st_name", this.name);

        if ( rs == null || !rs.next() ) {

            sb = new SqlAddBuilder().setTableName("soul_tech_player_data")

                    .setType(SqlAddBuilder.AddType.IGNORE)
                    .addTableParam(new SqlAddBuilder.AddParam().setSubParamName("st_uuid").setSubParamValue(this.uuid.toString()))
                    .addTableParam(new SqlAddBuilder.AddParam().setSubParamName("st_name").setSubParamValue(this.name))
                    .addTableParam(new SqlAddBuilder.AddParam().setSubParamName("st_info").setSubParamValue(NBTsUtil.Base64_Encode(this.jsonData.toString()))

                    );

        } else {

            sb = new SqlUpdBuilder().setTableName("soul_tech_player_data")

                    .setWhereParam(new SqlUpdBuilder.UpdParam().setSubParamName("st_name").setSubParamValue(this.name))
                    .addTableParam(new SqlUpdBuilder.UpdParam().setSubParamName("st_info").setSubParamValue(NBTsUtil.Base64_Encode(this.jsonData.toString()))

                    );

        }

        talex.getMysqlManager().autoAccess(sb);

    }

    @Override
    public int hashCode() {

        return this.player.hashCode();

    }

    @Override
    public boolean equals(Object obj) {

        if ( !( obj instanceof PlayerData ) ) {
            return false;
        }

        PlayerData playerData = (PlayerData) obj;

        return playerData.hashCode() == this.hashCode();

    }

}
