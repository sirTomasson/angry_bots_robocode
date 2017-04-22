package nl.saxion.ehi1vse1;

import java.io.Serializable;

/**
 * Created by Matthijs on 8-3-2017.
 */
public class SetTeamsMessage implements Serializable {

    private String[] team1;
    private String[] team2;

    public SetTeamsMessage(String[] team1, String[] team2) {
        this.team1 = team1;
        this.team2 = team2;
    }

    public String[] getTeam1() {
        return team1;
    }

    public String[] getTeam2() {
        return team2;
    }

}
