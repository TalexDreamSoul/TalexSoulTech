package pubsher.talexsoultech.entity.attract;

import lombok.Getter;

public class Expansion {

    @Getter
    private final PlayerAttractData playerAttractData;
    @Getter
    private final Area area;
    @Getter
    private int level;

    public Expansion(PlayerAttractData playerAttractData, int level, Area area) {

        this.playerAttractData = playerAttractData;
        this.area = area;

    }

    public Expansion(PlayerAttractData playerAttractData) {

        this.playerAttractData = playerAttractData;
        this.level = 0;
        this.area = new Area(playerAttractData);

    }

    @Override
    public int hashCode() {

        return ( this.level + area.hashCode() + "" ).hashCode();

    }

    @Override
    public boolean equals(Object obj) {

        if ( !( obj instanceof Expansion ) ) {

            return false;

        }

        Expansion target = (Expansion) obj;

        return target.hashCode() == this.hashCode();

    }

}
