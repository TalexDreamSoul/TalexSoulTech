package pubsher.talexsoultech.talex.guider.category;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.inventory.ItemStack;
import pubsher.talexsoultech.talex.BaseTalex;
import pubsher.talexsoultech.talex.guider.BaseGuider;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author TalexDreamSoul
 */
public class CategoryObject extends BaseGuider {

    @Getter
    private final int priority;
    @Getter
    private final String ID;
    @Getter
    private final ItemStack displayStack;
    private final Set<CategoryObject> children = new HashSet<>();
    @Setter
    @Getter
    private CategoryObject fatherCategory;
    @Getter
    private Set<CategoryObject> preposition;
    @Setter
    @Getter
    private CategoryType categoryType;
    @Getter
    @Setter
    private RecipeObject recipeObject;

    public CategoryObject(int priority, String ID, CategoryObject fatherCategory, Set<CategoryObject> preposition, CategoryType categoryType, ItemStack displayStack, RecipeObject recipeObject) {

        this.priority = priority;
        this.ID = ID;
        this.fatherCategory = fatherCategory;
        this.preposition = preposition;
        this.categoryType = categoryType;
        this.displayStack = displayStack;
        this.recipeObject = recipeObject;

        if ( categoryType == CategoryType.OBJECT ) {
            this.recipeObject.getDisplayItem().setOwnCategoryObject(this);
        }

    }

    public CategoryObject(int priority, String ID, CategoryObject fatherCategory, Set<CategoryObject> preposition, ItemStack displayStack) {

        this.priority = priority;
        this.ID = ID;
        this.fatherCategory = fatherCategory;
        this.preposition = preposition;
        this.categoryType = CategoryType.MENU;
        this.displayStack = displayStack;

    }

    public CategoryObject(int priority, String ID, CategoryObject fatherCategory, ItemStack displayStack) {

        this.priority = priority;
        this.ID = ID;
        this.fatherCategory = fatherCategory;
        this.categoryType = CategoryType.MENU;
        this.displayStack = displayStack;

    }

    public CategoryObject(int priority, String ID, ItemStack displayStack) {

        this.priority = priority;
        this.ID = ID;
        this.categoryType = CategoryType.MENU;
        this.displayStack = displayStack;

    }

    public CategoryObject(int priority, String ID, CategoryObject fatherCategory, Set<CategoryObject> preposition, RecipeObject recipeObject) {

        this.priority = priority;
        this.ID = ID;
        this.fatherCategory = fatherCategory;
        this.preposition = preposition;
        this.categoryType = CategoryType.OBJECT;
        this.recipeObject = recipeObject;
        this.displayStack = recipeObject.getDisplayItem().getItemBuilder().toItemStack();

        this.recipeObject.getDisplayItem().setOwnCategoryObject(this);

    }

    public CategoryObject(int priority, String ID, CategoryObject fatherCategory, RecipeObject recipeObject) {

        this.priority = priority;
        this.ID = ID;
        this.fatherCategory = fatherCategory;
        this.categoryType = CategoryType.OBJECT;
        this.recipeObject = recipeObject;
        this.displayStack = recipeObject.getDisplayItem().getItemBuilder().toItemStack();

        this.recipeObject.getDisplayItem().setOwnCategoryObject(this);

    }

    public CategoryObject(int priority, String ID, RecipeObject recipeObject) {

        this.priority = priority;
        this.ID = ID;
        this.categoryType = CategoryType.OBJECT;
        this.recipeObject = recipeObject;
        this.displayStack = recipeObject.getDisplayItem().getItemBuilder().toItemStack();

        this.recipeObject.getDisplayItem().setOwnCategoryObject(this);

    }

    public List<CategoryObject> getChildren() {

        List<CategoryObject> list = new ArrayList<>(children);

        list.sort((o1, o2) -> {

            if ( o1.getPriority() == o2.getPriority() ) {

                return o1.getID().compareTo(o2.getID());

            }

            return Integer.compare(o1.getPriority(), o2.getPriority());

        });

        return list;

    }

    public CategoryObject addPreposition(String categoryID) {

        if ( preposition == null ) {
            preposition = new HashSet<>();
        }

        CategoryObject categoryObject = BaseTalex.getInstance().getCategoryManager().getCategoryObject(ID);

        if ( categoryObject == null ) {
            return this;
        }

        preposition.add(categoryObject);

        return this;

    }

    public CategoryObject addPreposition(CategoryObject categoryObject) {

        if ( preposition == null ) {
            preposition = new HashSet<>();
        }

        preposition.add(categoryObject);

        return this;

    }

    public CategoryObject delPreposition(CategoryObject categoryObject) {

        if ( preposition != null ) {
            preposition.remove(categoryObject);
        }

        return this;

    }

    public CategoryObject addChild(CategoryObject categoryObject) {

        categoryObject.setFatherCategory(this);

        children.add(categoryObject);

        BaseTalex.getInstance().getCategoryManager().addToCategoryMap(categoryObject);

        return this;

    }

    public CategoryObject delChild(CategoryObject categoryObject) {

        categoryObject.setFatherCategory(BaseTalex.getInstance().getCategoryManager().getRootCategory());

        children.remove(categoryObject);

        return this;

    }

    @Override
    public int hashCode() {

        return ID.hashCode();
    }

    @Override
    public boolean equals(Object obj) {

        if ( obj instanceof CategoryObject ) {

            CategoryObject target = (CategoryObject) obj;

            return target.hashCode() == hashCode();


        }

        return false;

    }

    public enum CategoryType {

        /**
         * @Description: MENU 代表旗下还有 Category 否则代表最底层
         */
        MENU(), OBJECT();

    }

}
