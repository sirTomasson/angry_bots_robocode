package nl.saxion.ehi1vse1;

import java.io.Serializable;

/**
 * Created by Matthijs on 22-3-2017.
 */
public class TeamToTeamMessage implements Serializable {

    private String enemyWeAreAttackingName;

    public TeamToTeamMessage(String enemyWeAreAttackingName) {
        this.enemyWeAreAttackingName = enemyWeAreAttackingName;
    }

    public String getEnemyWeAreAttackingName() {
        return enemyWeAreAttackingName;
    }

}
