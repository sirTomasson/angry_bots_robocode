package nl.saxion.ehi1vse1;

import java.io.Serializable;

/**
 * Created by Matthijs on 1-3-2017.
 * Message system between the tanks.
 */
public class BuddyMessage implements Serializable {

    private String tankToTrack;

    public BuddyMessage(String tankToTrack) {
        this.tankToTrack = tankToTrack;
    }

    public String getTankToTrack() {
        return tankToTrack;
    }

}
