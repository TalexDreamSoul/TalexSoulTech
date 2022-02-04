package pubsher.talexsoultech.entity.attract;

import com.google.gson.JsonObject;
import lombok.Getter;

import java.io.Serializable;

public class Area implements Serializable {

    @Getter
    private double area_x;
    @Getter
    private double area_y;
    @Getter
    private double area_z;
    @Getter
    private PlayerAttractData playerAttractData;

    private Area(PlayerAttractData playerAttractData, double area_x, double area_y, double area_z) {

        this.playerAttractData = playerAttractData;

        this.area_x = area_x;
        this.area_y = area_y;
        this.area_z = area_z;

    }

    public Area(PlayerAttractData playerAttractData) {

        this.playerAttractData = playerAttractData;

        this.area_x = 0;
        this.area_y = 0;
        this.area_z = 0;

    }

    public static Area unSerialize(PlayerAttractData playerAttractData, JsonObject json) {

        if ( !json.has("area_x") ) {
            return null;
        }
        if ( !json.has("area_y") ) {
            return null;
        }
        if ( !json.has("area_z") ) {
            return null;
        }

        return new Area(playerAttractData, json.get("area_x").getAsDouble(), json.get("area_y").getAsDouble(), json.get("area_z").getAsDouble());

    }

    public String serialize() {

        JsonObject json = new JsonObject();

        json.addProperty("area_x", this.area_x);
        json.addProperty("area_y", this.area_y);
        json.addProperty("area_z", this.area_z);

        return json.toString();

    }

    @Override
    public int hashCode() {

        return ( this.area_x + this.area_y + this.area_z + "" ).hashCode();

    }

    @Override
    public boolean equals(Object obj) {

        if ( obj instanceof Area ) {

            Area target = (Area) obj;

            return target.area_y == this.area_y && target.area_x == this.area_x && target.area_z == this.area_z;

        }

        return false;

    }

    public Area setAreaX(double area_x) {

        this.area_x = area_x;

        return this;

    }

    public Area setAreaY(double area_y) {

        this.area_y = area_y;

        return this;

    }

    public Area setAreaZ(double area_z) {

        this.area_z = area_z;

        return this;

    }

    public Area addAreaX(double area_x) {

        this.area_x += area_x;

        return this;

    }

    public Area addAreaY(double area_y) {

        this.area_y += area_y;

        return this;

    }

    public Area addAreaZ(double area_z) {

        this.area_z += area_z;

        return this;

    }

}
