package pubsher.talexsoultech.data.paper_addon;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

/**
 * <br /> {@link pubsher.talexsoultech.data.paper_addon }
 *
 * @author TalexDreamSoul
 * @date 2021/8/16 13:40 <br /> Project: TalexSoulTech <br />
 */
public class TLocation extends Location {

    public TLocation(World world, double x, double y, double z) {

        super(world, x, y, z);
    }

    public TLocation(World world, double x, double y, double z, float yaw, float pitch) {

        super(world, x, y, z, yaw, pitch);

    }

    public TLocation(Block block) {

        super(block.getWorld(), block.getLocation().getBlockX(), block.getLocation().getBlockY(), block.getLocation().getBlockZ());

    }

    @Override
    public boolean equals(Object obj) {

        if ( obj instanceof TLocation ) {

            return ( (TLocation) obj ).distance(this) < 1;

        }

        return super.equals(obj);
    }

    @Override
    public int hashCode() {

        return getBlockX() * 31 + getBlockY() * 31 + getBlockZ() * 31;
    }

}
