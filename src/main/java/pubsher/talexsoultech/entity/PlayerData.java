package pubsher.talexsoultech.entity;

import com.google.gson.JsonArray;
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
import pubsher.talexsoultech.talex.guider.category.CategoryObject;
import pubsher.talexsoultech.talex.items.GuideBookItem;
import pubsher.talexsoultech.utils.NBTsUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Accessors(chain = true)
@Getter
@Setter
public class PlayerData {
    private static final String CATEGORY_UNLOCKS = "category_unlock";
    private static final String PAID_CATEGORY_UNLOCKS = "paid_category_unlock";
    private static final String ADMIN_CATEGORY_UNLOCKS = "admin_category_unlock";
    private static final String GUIDE_SCHEMA = "guide_schema";
    private static final String GUIDE_UNLOCKS = "guide_unlocks";
    private static final String GUIDE_EVIDENCE = "guide_evidence";
    private static final String GUIDE_WAVES = "guide_waves";
    public static final int CURRENT_GUIDE_SCHEMA = 2;


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

        ensureUnlockProperty(CATEGORY_UNLOCKS);
        ensureUnlockProperty(PAID_CATEGORY_UNLOCKS);
        migrateGuideState();

        this.playerAttractData = new PlayerAttractData(this, attractConfiguration);
    }
    private void ensureUnlockProperty(String property) {
        if (!jsonData.has(property)) {
            jsonData.addProperty(property, "");
        }
    }

    /**
     * Idempotently upgrades legacy unlock strings to guide_schema=2. Unknown
     * properties and malformed existing canonical properties are left untouched;
     * readers then fall back to the exact-token legacy representation.
     */
    public PlayerData migrateGuideState() {
        migrateGuideState(jsonData);
        return this;
    }

    public static JsonObject migrateGuideState(JsonObject source) {
        Objects.requireNonNull(source, "source");
        JsonElement schema = source.get(GUIDE_SCHEMA);
        if (schema == null || schema.isJsonNull()) {
            source.addProperty(GUIDE_SCHEMA, CURRENT_GUIDE_SCHEMA);
        } else if (schema.isJsonPrimitive() && schema.getAsJsonPrimitive().isNumber()
                && schema.getAsInt() < CURRENT_GUIDE_SCHEMA) {
            source.addProperty(GUIDE_SCHEMA, CURRENT_GUIDE_SCHEMA);
        }

        JsonElement unlocks = source.get(GUIDE_UNLOCKS);
        if (unlocks == null || unlocks.isJsonNull()) {
            JsonObject canonical = new JsonObject();
            canonical.add("regular", exactTokens(source.get(CATEGORY_UNLOCKS)));
            canonical.add("paid", exactTokens(source.get(PAID_CATEGORY_UNLOCKS)));
            canonical.add("admin", exactTokens(source.get(ADMIN_CATEGORY_UNLOCKS)));
            source.add(GUIDE_UNLOCKS, canonical);
        }
        if (!source.has(GUIDE_EVIDENCE)) {
            source.add(GUIDE_EVIDENCE, new JsonObject());
        }
        if (!source.has(GUIDE_WAVES)) {
            source.add(GUIDE_WAVES, new JsonArray());
        }
        return source;
    }

    public static JsonArray exactTokens(JsonElement encoded) {
        JsonArray result = new JsonArray();
        if (encoded == null || encoded.isJsonNull() || !encoded.isJsonPrimitive()) {
            return result;
        }
        String raw = encoded.getAsString();
        for (String token : raw.split(",", -1)) {
            String trimmed = token.trim();
            if (!trimmed.isEmpty() && !containsExact(result, trimmed)) {
                result.add(trimmed);
            }
        }
        return result;
    }

    private static boolean containsExact(JsonArray values, String token) {
        for (JsonElement value : values) {
            if (value.isJsonPrimitive() && token.equals(value.getAsString())) return true;
        }
        return false;
    }


    public static PlayerData createDefault(BaseTalex talex, Player player, boolean persistenceWritable) {

        JsonObject jsonData = new JsonObject();
        jsonData.addProperty(CATEGORY_UNLOCKS, "");
        jsonData.addProperty(PAID_CATEGORY_UNLOCKS, "");
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
        removeCategoryUnlock(CATEGORY_UNLOCKS, ID);
        removeCanonicalUnlock("regular", ID);
        return this;
    }

    public PlayerData addCategoryUnlock(String ID) {
        addCategoryUnlock(CATEGORY_UNLOCKS, ID);
        addCanonicalUnlock("regular", ID);
        return this;
    }

    public boolean isCategoryUnLock(String ID) {
        return hasCategoryUnlock(CATEGORY_UNLOCKS, ID) || hasCanonicalUnlock("regular", ID);
    }

    public PlayerData addPaidCategoryUnlock(String ID) {
        addCategoryUnlock(PAID_CATEGORY_UNLOCKS, ID);
        addCanonicalUnlock("paid", ID);
        return this;
    }

    public boolean isPaidCategoryUnlock(String ID) {
        return hasCategoryUnlock(PAID_CATEGORY_UNLOCKS, ID) || hasCanonicalUnlock("paid", ID);
    }

    private void addCategoryUnlock(String property, String ID) {
        if (ID == null || ID.isBlank() || hasCategoryUnlock(property, ID)) {
            return;
        }
        JsonElement current = jsonData.get(property);
        String encoded = current != null && current.isJsonPrimitive() ? current.getAsString() : "";
        jsonData.addProperty(property, encoded + ID.trim() + ", ");
    }

    private void removeCategoryUnlock(String property, String ID) {
        if (ID == null || ID.isBlank()) return;
        JsonElement current = jsonData.get(property);
        if (current == null || !current.isJsonPrimitive()) return;
        List<String> tokens = new ArrayList<>(legacyTokens(current));
        if (tokens.remove(ID.trim())) {
            jsonData.addProperty(property, String.join(", ", tokens) + (tokens.isEmpty() ? "" : ", "));
        }
    }

    private boolean hasCategoryUnlock(String property, String ID) {
        if (ID == null || ID.isBlank()) return false;
        JsonElement current = jsonData.get(property);
        return current != null && legacyTokens(current).contains(ID.trim());
    }

    private static Set<String> legacyTokens(JsonElement encoded) {
        if (encoded == null || encoded.isJsonNull() || !encoded.isJsonPrimitive()) {
            return Set.of();
        }
        Set<String> tokens = new LinkedHashSet<>();
        for (String token : encoded.getAsString().split(",", -1)) {
            String trimmed = token.trim();
            if (!trimmed.isEmpty()) tokens.add(trimmed);
        }
        return tokens;
    }

    public boolean hasCategoryUnlock(CategoryObject category) {
        if (category == null) return false;
        return hasAnyCanonicalOrLegacy(category.getID(), category.getPlanningId(), category.getRuntimeId(),
                category.getLegacyRuntimeId());
    }

    private boolean hasAnyCanonicalOrLegacy(String... ids) {
        for (String id : ids) {
            if (id == null || id.isBlank()) continue;
            if (isCategoryUnLock(id) || isPaidCategoryUnlock(id) || hasCategoryUnlock(ADMIN_CATEGORY_UNLOCKS, id)
                    || hasCanonicalUnlock("admin", id)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasCanonicalUnlock(String bucket, String id) {
        JsonElement unlocks = jsonData.get(GUIDE_UNLOCKS);
        if (unlocks == null || !unlocks.isJsonObject()) return false;
        JsonElement values = unlocks.getAsJsonObject().get(bucket);
        if (values == null || !values.isJsonArray()) return false;
        return containsExact(values.getAsJsonArray(), id);
    }

    private void addCanonicalUnlock(String bucket, String id) {
        if (id == null || id.isBlank()) return;
        JsonElement unlocks = jsonData.get(GUIDE_UNLOCKS);
        if (unlocks == null || !unlocks.isJsonObject()) return;
        JsonObject object = unlocks.getAsJsonObject();
        JsonElement values = object.get(bucket);
        if (values == null || !values.isJsonArray()) {
            values = new JsonArray();
            object.add(bucket, values);
        }
        if (!containsExact(values.getAsJsonArray(), id.trim())) {
            values.getAsJsonArray().add(id.trim());
        }
    }

    private void removeCanonicalUnlock(String bucket, String id) {
        JsonElement unlocks = jsonData.get(GUIDE_UNLOCKS);
        if (unlocks == null || !unlocks.isJsonObject()) return;
        JsonElement values = unlocks.getAsJsonObject().get(bucket);
        if (values == null || !values.isJsonArray()) return;
        JsonArray array = values.getAsJsonArray();
        for (int index = array.size() - 1; index >= 0; index--) {
            if (id.equals(array.get(index).getAsString())) array.remove(index);
        }
    }

    /** One atomic unlock decision; callers must not mutate unlock fields directly. */
    public synchronized UnlockAttempt tryUnlock(String categoryId, UnlockEvidence evidence) {
        CategoryObject category = talex.getCategoryManager().getCategoryObject(categoryId);
        if (category == null) return UnlockAttempt.failed(UnlockFailure.UNKNOWN_CATEGORY);
        return tryUnlock(category, evidence);
    }

    public synchronized UnlockAttempt tryUnlock(CategoryObject category, UnlockEvidence evidence) {
        if (category == null) return UnlockAttempt.failed(UnlockFailure.UNKNOWN_CATEGORY);
        if (evidence == null || evidence.receipts().isEmpty()) {
            return UnlockAttempt.failed(UnlockFailure.MISSING_EVIDENCE);
        }
        if (hasCategoryUnlock(category)) {
            return UnlockAttempt.already();
        }
        if (!category.arePrepositionsUnlockedBy(this)) {
            return UnlockAttempt.failed(UnlockFailure.PREREQUISITES);
        }
        boolean admin = evidence.admin();
        boolean paid = evidence.paid() || admin;
        int levelCost = category.getUnlockLevelCost();
        if (levelCost > 0 && !paid) {
            return UnlockAttempt.failed(UnlockFailure.PAYMENT_REQUIRED);
        }
        if (levelCost > 0 && !admin && player.getLevel() < levelCost) {
            return UnlockAttempt.failed(UnlockFailure.INSUFFICIENT_LEVEL);
        }
        JsonObject canonical = canonicalUnlocksForWrite();
        if (canonical == null) {
            return UnlockAttempt.failed(UnlockFailure.INVALID_GUIDE_STATE);
        }

        String id = category.getID();
        String bucket = admin ? "admin" : paid ? "paid" : "regular";
        if (levelCost > 0 && !admin) {
            player.giveExpLevels(-levelCost);
        }
        addCanonicalUnlock(bucket, id);
        addCanonicalUnlock("regular", id);
        if (admin) addCategoryUnlock(ADMIN_CATEGORY_UNLOCKS, id);
        else if (paid) addCategoryUnlock(PAID_CATEGORY_UNLOCKS, id);
        else addCategoryUnlock(CATEGORY_UNLOCKS, id);
        recordEvidence(id, evidence);
        return UnlockAttempt.success();
    }

    private JsonObject canonicalUnlocksForWrite() {
        JsonElement value = jsonData.get(GUIDE_UNLOCKS);
        if (value == null || !value.isJsonObject()) return null;
        JsonObject object = value.getAsJsonObject();
        for (String bucket : List.of("regular", "paid", "admin")) {
            JsonElement values = object.get(bucket);
            if (values == null) object.add(bucket, new JsonArray());
            else if (!values.isJsonArray()) return null;
        }
        JsonElement evidence = jsonData.get(GUIDE_EVIDENCE);
        JsonElement waves = jsonData.get(GUIDE_WAVES);
        return evidence != null && evidence.isJsonObject() && waves != null && waves.isJsonArray() ? object : null;
    }

    private void recordEvidence(String id, UnlockEvidence evidence) {
        JsonObject records = jsonData.getAsJsonObject(GUIDE_EVIDENCE);
        JsonElement existing = records.get(id);
        JsonArray values = existing != null && existing.isJsonArray() ? existing.getAsJsonArray() : new JsonArray();
        for (String receipt : evidence.receipts()) {
            if (!containsExact(values, receipt)) values.add(receipt);
        }
        records.add(id, values);
    }

    public boolean isWaveComplete(String waveId) {
        JsonElement waves = jsonData.get(GUIDE_WAVES);
        return waves != null && waves.isJsonArray() && containsExact(waves.getAsJsonArray(), waveId);
    }

    public PlayerData completeWave(String waveId) {
        if (waveId == null || waveId.isBlank()) return this;
        JsonElement waves = jsonData.get(GUIDE_WAVES);
        if (waves != null && waves.isJsonArray() && !containsExact(waves.getAsJsonArray(), waveId)) {
            waves.getAsJsonArray().add(waveId.trim());
        }
        return this;
    }

    public enum UnlockFailure {
        UNKNOWN_CATEGORY, MISSING_EVIDENCE, PREREQUISITES, PAYMENT_REQUIRED, INSUFFICIENT_LEVEL, INVALID_GUIDE_STATE
    }

    public record UnlockAttempt(boolean unlocked, boolean alreadyUnlocked, UnlockFailure failure) {
        public static UnlockAttempt success() { return new UnlockAttempt(true, false, null); }
        public static UnlockAttempt already() { return new UnlockAttempt(true, true, null); }
        public static UnlockAttempt failed(UnlockFailure failure) { return new UnlockAttempt(false, false, failure); }
    }

    public record UnlockEvidence(Set<String> receipts, boolean paid, boolean admin) {
        public UnlockEvidence {
            Set<String> clean = new LinkedHashSet<>();
            if (receipts != null) {
                for (String receipt : receipts) {
                    if (receipt != null && !receipt.isBlank()) clean.add(receipt.trim());
                }
            }
            receipts = Collections.unmodifiableSet(clean);
        }

        public static UnlockEvidence regular(String receipt) {
            return new UnlockEvidence(Set.of(receipt), false, false);
        }

        public static UnlockEvidence paid(String receipt) {
            return new UnlockEvidence(Set.of(receipt), true, false);
        }

        public static UnlockEvidence admin(String receipt) {
            return new UnlockEvidence(Set.of(receipt), false, true);
        }
    }

    public record GuideView(String id, boolean unlocked, boolean prerequisitesUnlocked, boolean paymentRequired, int levelCost) {
    }

    public GuideView evaluateGuide(CategoryObject category) {
        Objects.requireNonNull(category, "category");
        return new GuideView(category.getID(), hasCategoryUnlock(category),
                category.arePrepositionsUnlockedBy(this), category.requiresLevelPayment(), category.getUnlockLevelCost());
    }

    /** Pure fixture evaluator; the supplied JSON is copied before migration. */
    public static GuideView evaluateGuide(JsonObject source, CategoryObject category) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(category, "category");
        JsonObject copy = JsonParser.parseString(source.toString()).getAsJsonObject();
        migrateGuideState(copy);
        boolean unlocked = hasCategoryUnlockIn(copy, category);
        boolean prerequisites = category.getPreposition().stream()
                .allMatch(prerequisite -> hasCategoryUnlockIn(copy, prerequisite));
        return new GuideView(category.getID(), unlocked, prerequisites,
                category.requiresLevelPayment(), category.getUnlockLevelCost());
    }

    private static boolean hasCategoryUnlockIn(JsonObject source, CategoryObject category) {
        return hasAnyUnlockIn(source, category.getID(), category.getPlanningId(),
                category.getRuntimeId(), category.getLegacyRuntimeId());
    }

    private static boolean hasAnyUnlockIn(JsonObject source, String... ids) {
        for (String id : ids) {
            if (id == null || id.isBlank()) continue;
            if (legacyTokens(source.get(CATEGORY_UNLOCKS)).contains(id)
                    || legacyTokens(source.get(PAID_CATEGORY_UNLOCKS)).contains(id)
                    || legacyTokens(source.get(ADMIN_CATEGORY_UNLOCKS)).contains(id)
                    || hasCanonicalUnlockIn(source, "regular", id)
                    || hasCanonicalUnlockIn(source, "paid", id)
                    || hasCanonicalUnlockIn(source, "admin", id)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasCanonicalUnlockIn(JsonObject source, String bucket, String id) {
        JsonElement unlocks = source.get(GUIDE_UNLOCKS);
        if (unlocks == null || !unlocks.isJsonObject()) return false;
        JsonElement values = unlocks.getAsJsonObject().get(bucket);
        return values != null && values.isJsonArray() && containsExact(values.getAsJsonArray(), id);
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
