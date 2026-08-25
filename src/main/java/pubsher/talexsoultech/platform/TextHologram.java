package pubsher.talexsoultech.platform;

import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
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
    private static final String SCOREBOARD_TAG = "talexsoultech-hologram";

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
    public static int removeLegacyMachineDisplays(Location controller) {
        if (controller.getWorld() == null
                || !controller.getWorld().isChunkLoaded(controller.getBlockX() >> 4, controller.getBlockZ() >> 4)) {
            return 0;
        }

        int removed = 0;
        Location center = controller.clone().add(0.5, 1.25, 0.5);
        for (Entity entity : controller.getWorld().getNearbyEntities(center, 1.25, 1.75, 1.25)) {
            if (entity instanceof TextDisplay) {
                entity.remove();
                removed++;
            }
        }
        return removed;
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
            entity.setPersistent(false);
            entity.addScoreboardTag(SCOREBOARD_TAG);
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
            entity.setPersistent(false);
            entity.addScoreboardTag(SCOREBOARD_TAG);
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
