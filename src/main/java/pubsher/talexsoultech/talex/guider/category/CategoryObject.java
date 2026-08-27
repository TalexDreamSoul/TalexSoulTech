package pubsher.talexsoultech.talex.guider.category;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.inventory.ItemStack;
import pubsher.talexsoultech.entity.PlayerData;
import pubsher.talexsoultech.talex.BaseTalex;
import pubsher.talexsoultech.talex.guider.BaseGuider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * One node in the guide graph.  The legacy constructors remain available for
 * existing recipes, while manifest nodes carry the planning/runtime identities
 * separately so a planning id is never accidentally used as a PDC id.
 */
public class CategoryObject extends BaseGuider {

    @Getter
    private final int priority;
    @Getter
    private final String ID;
    @Getter
    private final ItemStack displayStack;
    private final Set<CategoryObject> children = new LinkedHashSet<>();
    @Setter
    @Getter
    private CategoryObject fatherCategory;
    private Set<CategoryObject> preposition;
    @Setter
    @Getter
    private CategoryType categoryType;
    @Getter
    @Setter
    private RecipeObject recipeObject;
    @Getter
    private int unlockLevelCost;

    @Getter
    private GuideNodeType guideNodeType = GuideNodeType.LEGACY;
    @Getter
    private String planningId;
    @Getter
    private String runtimeId;
    @Getter
    private String legacyRuntimeId;
    @Getter
    private String waveId;
    @Getter
    private String disciplineId;
    @Getter
    private String familyId;

    public enum GuideNodeType {
        ROOT, WAVE, DISCIPLINE, FAMILY, ITEM, LEGACY
    }

    public CategoryObject setUnlockLevelCost(int unlockLevelCost) {
        if (unlockLevelCost < 0) {
            throw new IllegalArgumentException("unlockLevelCost must not be negative");
        }
        this.unlockLevelCost = unlockLevelCost;
        return this;
    }

    /**
     * Configures the stable identities for a manifest-backed node.
     * The node's visible ID remains the graph/planning id.
     */
    public CategoryObject setGuideIdentity(
            GuideNodeType nodeType,
            String planningId,
            String runtimeId,
            String legacyRuntimeId,
            String waveId,
            String disciplineId,
            String familyId
    ) {
        this.guideNodeType = Objects.requireNonNull(nodeType, "nodeType");
        this.planningId = emptyToNull(planningId);
        this.runtimeId = emptyToNull(runtimeId);
        this.legacyRuntimeId = emptyToNull(legacyRuntimeId);
        this.waveId = emptyToNull(waveId);
        this.disciplineId = emptyToNull(disciplineId);
        this.familyId = emptyToNull(familyId);
        return this;
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    public boolean isUnlockedBy(PlayerData playerData) {
        Objects.requireNonNull(playerData, "playerData");
        return playerData.hasCategoryUnlock(this);
    }

    /**
     * Hard prerequisites only.  The manifest's family supports links are not
     * represented here and therefore can never become unlock requirements.
     */
    public boolean arePrepositionsUnlockedBy(PlayerData playerData) {
        return preposition == null
                || preposition.stream().allMatch(category -> category.isUnlockedBy(playerData));
    }

    public CategoryObject(int priority, String ID, CategoryObject fatherCategory, Set<CategoryObject> preposition, CategoryType categoryType, ItemStack displayStack, RecipeObject recipeObject) {
        this.priority = priority;
        this.ID = requireId(ID);
        this.fatherCategory = fatherCategory;
        this.preposition = copyPrepositions(preposition);
        this.categoryType = Objects.requireNonNull(categoryType, "categoryType");
        this.displayStack = displayStack;
        this.recipeObject = recipeObject;
        attachRecipeCategory();
    }

    public CategoryObject(int priority, String ID, CategoryObject fatherCategory, Set<CategoryObject> preposition, ItemStack displayStack) {
        this.priority = priority;
        this.ID = requireId(ID);
        this.fatherCategory = fatherCategory;
        this.preposition = copyPrepositions(preposition);
        this.categoryType = CategoryType.MENU;
        this.displayStack = displayStack;
    }

    public CategoryObject(int priority, String ID, CategoryObject fatherCategory, ItemStack displayStack) {
        this.priority = priority;
        this.ID = requireId(ID);
        this.fatherCategory = fatherCategory;
        this.categoryType = CategoryType.MENU;
        this.displayStack = displayStack;
    }

    public CategoryObject(int priority, String ID, ItemStack displayStack) {
        this.priority = priority;
        this.ID = requireId(ID);
        this.categoryType = CategoryType.MENU;
        this.displayStack = displayStack;
    }

    public CategoryObject(int priority, String ID, CategoryObject fatherCategory, Set<CategoryObject> preposition, RecipeObject recipeObject) {
        this.priority = priority;
        this.ID = requireId(ID);
        this.fatherCategory = fatherCategory;
        this.preposition = copyPrepositions(preposition);
        this.categoryType = CategoryType.OBJECT;
        this.recipeObject = Objects.requireNonNull(recipeObject, "recipeObject");
        this.displayStack = recipeObject.getDisplayItem().getItemBuilder().toItemStack();
        attachRecipeCategory();
    }

    public CategoryObject(int priority, String ID, CategoryObject fatherCategory, RecipeObject recipeObject) {
        this.priority = priority;
        this.ID = requireId(ID);
        this.fatherCategory = fatherCategory;
        this.categoryType = CategoryType.OBJECT;
        this.recipeObject = Objects.requireNonNull(recipeObject, "recipeObject");
        this.displayStack = recipeObject.getDisplayItem().getItemBuilder().toItemStack();
        attachRecipeCategory();
    }

    public CategoryObject(int priority, String ID, RecipeObject recipeObject) {
        this.priority = priority;
        this.ID = requireId(ID);
        this.categoryType = CategoryType.OBJECT;
        this.recipeObject = Objects.requireNonNull(recipeObject, "recipeObject");
        this.displayStack = recipeObject.getDisplayItem().getItemBuilder().toItemStack();
        attachRecipeCategory();
    }

    private static String requireId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("category id must not be blank");
        }
        return id;
    }

    private static Set<CategoryObject> copyPrepositions(Set<CategoryObject> source) {
        if (source == null || source.isEmpty()) {
            return source == null ? null : new LinkedHashSet<>();
        }
        LinkedHashSet<CategoryObject> copy = new LinkedHashSet<>();
        for (CategoryObject category : source) {
            copy.add(Objects.requireNonNull(category, "preposition"));
        }
        return copy;
    }

    private void attachRecipeCategory() {
        if (recipeObject != null && recipeObject.getDisplayItem() != null) {
            recipeObject.getDisplayItem().setOwnCategoryObject(this);
        }
    }

    public List<CategoryObject> getChildren() {
        List<CategoryObject> result = new ArrayList<>(children);
        result.sort(Comparator.comparingInt(CategoryObject::getPriority).thenComparing(CategoryObject::getID));
        return Collections.unmodifiableList(result);
    }

    public CategoryObject addPreposition(String categoryID) {
        CategoryObject categoryObject = BaseTalex.getInstance()
                .getCategoryManager()
                .getCategoryObject(categoryID);
        if (categoryObject == null) {
            throw new IllegalArgumentException("unknown guide prerequisite: " + categoryID);
        }
        return addPreposition(categoryObject);
    }

    public CategoryObject addPreposition(CategoryObject categoryObject) {
        Objects.requireNonNull(categoryObject, "categoryObject");
        if (categoryObject == this || categoryObject.reaches(this)) {
            throw new IllegalStateException("guide prerequisite cycle: " + ID + " -> " + categoryObject.ID);
        }
        if (preposition == null) {
            preposition = new LinkedHashSet<>();
        }
        preposition.add(categoryObject);
        return this;
    }

    public CategoryObject delPreposition(CategoryObject categoryObject) {
        if (preposition != null) {
            preposition.remove(categoryObject);
        }
        return this;
    }

    public CategoryObject addChild(CategoryObject categoryObject) {
        Objects.requireNonNull(categoryObject, "categoryObject");
        if (categoryObject == this || reaches(categoryObject)) {
            throw new IllegalStateException("guide child cycle: " + ID + " -> " + categoryObject.ID);
        }
        if (categoryObject.fatherCategory != null && categoryObject.fatherCategory != this) {
            throw new IllegalStateException("guide child already has a parent: " + categoryObject.ID);
        }
        if (!children.add(categoryObject)) {
            throw new IllegalStateException("duplicate guide child: " + categoryObject.ID);
        }
        categoryObject.setFatherCategory(this);
        BaseTalex base = BaseTalex.getInstance();
        if (base != null && base.getCategoryManager() != null) {
            base.getCategoryManager().addToCategoryMap(categoryObject);
        }
        return this;
    }

    private boolean reaches(CategoryObject target) {
        CategoryObject current = this;
        int steps = 0;
        while (current != null && steps++ < 10000) {
            if (current == target) {
                return true;
            }
            current = current.fatherCategory;
        }
        if (steps >= 10000) {
            throw new IllegalStateException("guide parent chain exceeds safety bound");
        }
        return false;
    }

    public CategoryObject delChild(CategoryObject categoryObject) {
        if (categoryObject != null && children.remove(categoryObject)) {
            categoryObject.setFatherCategory(BaseTalex.getInstance().getCategoryManager().getRootCategory());
        }
        return this;
    }

    public Set<CategoryObject> childrenSnapshot() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(children));
    }

    public CategoryObject clearChildren() {
        for (CategoryObject child : children) {
            child.setFatherCategory(null);
        }
        children.clear();
        return this;
    }

    public Set<CategoryObject> getPreposition() {
        return preposition == null
                ? Set.of()
                : Collections.unmodifiableSet(new LinkedHashSet<>(preposition));
    }

    @Override
    public int hashCode() {
        return ID.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof CategoryObject target && target.ID.equals(ID);
    }

    public boolean requiresLevelPayment() {
        return unlockLevelCost > 0;
    }

    public enum CategoryType {
        MENU, OBJECT
    }

}
