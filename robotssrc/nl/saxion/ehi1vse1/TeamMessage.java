package nl.saxion.ehi1vse1;

import java.io.Serializable;

/**
 * Created by youri on 29-3-2017.
 */
public class TeamMessage implements Serializable {
    private String name;
    private double x, y;

    public TeamMessage(String name, double x, double y) {
        this.name = name;
        this.x = x;
        this.y = y;
    }

    public String getName() {
        return name;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }
}
