package pubsher.talexsoultech.entity.attract;

import com.google.gson.JsonObject;
import lombok.Getter;

import java.io.Serializable;

public class CoolDown implements Serializable {

    @Getter
    private PlayerAttractData playerAttractData;
    @Getter
    private int level;
    @Getter
    private long time;

    public CoolDown(PlayerAttractData playerAttractData) {

        this.level = 0;
        this.time = 0;
        this.playerAttractData = playerAttractData;

    }

    private CoolDown(PlayerAttractData playerAttractData, int level, long time) {

        this.level = level;
        this.time = time;
        this.playerAttractData = playerAttractData;

    }

    public static CoolDown unSerialize(PlayerAttractData playerAttractData, JsonObject json) {

        if ( !json.has("level") ) {
            return null;
        }
        if ( !json.has("time") ) {
            return null;
        }

        return new CoolDown(playerAttractData, json.get("level").getAsInt(), json.get("time").getAsLong());

    }

    public String serialize() {

        JsonObject json = new JsonObject();

        json.addProperty("level", this.level);
        json.addProperty("time", this.time);

        return json.toString();

    }

    public CoolDown setLevel(int level) {

        this.level = level;
        return this;

    }

    public CoolDown setTime(long time) {

        this.time = time;
        return this;

    }

    public CoolDown addLevel(int level) {

        this.level += level;

        return this;

    }

    public CoolDown addTime(long time) {

        this.time += time;

        return this;

    }

    @Override
    public int hashCode() {

        return ( this.time + this.level + "" ).hashCode();

    }

    @Override
    public boolean equals(Object obj) {

        if ( !( obj instanceof CoolDown ) ) {
            return false;
        }

        CoolDown target = (CoolDown) obj;

        return target.level == this.level && target.time == this.time;

    }

}
