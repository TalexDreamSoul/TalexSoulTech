package pubsher.talexsoultech.talex.machine;

import lombok.Getter;
import org.bukkit.inventory.ItemStack;
import pubsher.talexsoultech.entity.PlayerData;
import pubsher.talexsoultech.talex.BaseTalex;
import pubsher.talexsoultech.talex.guider.category.RecipeObject;

import java.util.HashSet;
import java.util.Set;

public abstract class BaseMachine implements IMachine {

//    @Getter
//    protected static HashMap<String, MachineGUI> guis = new HashMap<>();
//
//    @SneakyThrows
//    public static void loadGuis(File file) {
//
//        YamlConfiguration yaml = new YamlConfiguration();
//
//        yaml.load(file);
//
//        Gson gson = new Gson();
//
//        Set<String> keys = yaml.getConfigurationSection("Inventories").getKeys(false);
//
//        for(String key : keys) {
//
//            gson.fromJson(NBTsUtil.Base64_Decode(yaml.getString("Inventories." + key)), )
//
//        }
//
//    }
//
//    public static void saveGuis(File file) {
//
//
//
//    }

//    public abstract boolean checkTypeTrue(String type);

    @Getter
    protected final MachineChecker machineChecker;

    @Getter
    protected final String machineName;

    @Getter
    protected final ItemStack displayItem;
    @Getter
    protected Set<RecipeObject> recipes = new HashSet<>();

    public BaseMachine(String machineName, ItemStack displayItem, MachineChecker checker) {

        this.machineChecker = checker;
        this.machineName = machineName;
        this.displayItem = displayItem;

        BaseTalex.getInstance().getMachineManager().registerMachine(this);

    }

    public abstract void onOpenMachineInfoViewer(PlayerData playerData);

    protected void addRecipe(RecipeObject recipe) {

        recipes.add(recipe);

    }

    protected void delRecipe(RecipeObject recipe) {

        recipes.remove(recipe);

    }

}
