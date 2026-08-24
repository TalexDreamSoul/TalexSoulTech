package pubsher.talexsoultech.platform;

import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Native Paper display-entity facade replacing legacy hologram-plugin APIs.
 */
public final class TextHologram {

    private static final List<TextHologram> ACTIVE = new ArrayList<>();

    private final Location origin;
    private final List<Display> lines = new ArrayList<>();
    private boolean deleted;

    private TextHologram(Location origin) {
        this.origin = origin.clone();
    }

    public static TextHologram create(Location origin) {
        TextHologram hologram = new TextHologram(origin);
        ACTIVE.add(hologram);
        return hologram;
    }

    public static void clearAll() {
        List.copyOf(ACTIVE).forEach(TextHologram::delete);
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void clearLines() {
        lines.forEach(Display::remove);
        lines.clear();
    }

    public void appendTextLine(String text) {
        if (deleted) {
            return;
        }
        TextDisplay display = origin.getWorld().spawn(nextLine(), TextDisplay.class, entity -> {
            entity.setText(text);
            entity.setBillboard(Display.Billboard.CENTER);
            entity.setSeeThrough(true);
            entity.setDefaultBackground(false);
        });
        lines.add(display);
    }

    public void appendItemLine(ItemStack item) {
        if (deleted) {
            return;
        }
        ItemDisplay display = origin.getWorld().spawn(nextLine(), ItemDisplay.class, entity -> {
            entity.setItemStack(item);
            entity.setBillboard(Display.Billboard.CENTER);
        });
        lines.add(display);
    }

    public void delete() {
        if (deleted) {
            return;
        }
        clearLines();
        deleted = true;
        ACTIVE.remove(this);
    }

    private Location nextLine() {
        return origin.clone().subtract(0, lines.size() * 0.28D, 0);
    }
}
