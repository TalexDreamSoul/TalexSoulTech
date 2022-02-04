package pubsher.talexsoultech.entity.attract;

import com.google.gson.JsonParser;
import lombok.Getter;
import lombok.SneakyThrows;
import org.bukkit.configuration.file.YamlConfiguration;
import pubsher.talexsoultech.entity.PlayerData;

public class PlayerAttractData {

    @Getter
    private final PlayerData playerData;

    @Getter
    private Expansion expansion;

    @Getter
    private CoolDown coolDown;

    @Getter
    private Particles particles;

    @Getter
    private boolean enable;

    @SneakyThrows
    public PlayerAttractData(PlayerData playerData, YamlConfiguration yaml) {

        this.playerData = playerData;

        if ( yaml == null ) {

            this.expansion = new Expansion(this);
            this.coolDown = new CoolDown(this);
            this.particles = new Particles(this);
            this.enable = false;

        } else {

            this.enable = yaml.getBoolean("PlayerData.enable");

            String str = yaml.getString("PlayerData.Expansion.Area");

            Area area = Area.unSerialize(this, new JsonParser().parse(str).getAsJsonObject());

            this.expansion = new Expansion(this, yaml.getInt("PlayerData.Expansion.level"), area);

            this.coolDown = CoolDown.unSerialize(this, new JsonParser().parse(yaml.getString("PlayerData.CoolDown")).getAsJsonObject());

            this.particles = Particles.unSerialize(this, new JsonParser().parse(yaml.getString("PlayerData.Particles")).getAsJsonObject());

        }

    }

    @Override
    public String toString() {

        return toYaml().saveToString();

    }

    public YamlConfiguration toYaml() {

        YamlConfiguration yaml = new YamlConfiguration();

        yaml.set("PlayerData.enable", this.enable);
        yaml.set("PlayerData.Expansion.level", this.expansion.getLevel());
        yaml.set("PlayerData.Expansion.Area", this.expansion.getArea().serialize());
        yaml.set("PlayerData.CoolDown", this.coolDown.serialize());
        yaml.set("PlayerData.Particles", this.particles.serialize());

        return yaml;

    }

    @Override
    public int hashCode() {

        return playerData.getPlayer().getName().hashCode();

    }

    @Override
    public boolean equals(Object obj) {

        if ( !( obj instanceof PlayerAttractData ) ) {
            return false;
        }

        PlayerAttractData target = (PlayerAttractData) obj;

        return target.hashCode() == this.hashCode();

    }

}
