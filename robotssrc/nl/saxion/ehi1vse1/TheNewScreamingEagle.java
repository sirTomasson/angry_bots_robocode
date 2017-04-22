package nl.saxion.ehi1vse1;

import robocode.*;
import robocode.util.Utils;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by Matthijs on 29-3-2017.
 */
public class TheNewScreamingEagle extends TeamRobot {

    /**------------VARIABLES------------------------------------------------------------------------------------------------------------**/

    /**
     * Arrays to save teams
     */
    private String[] allMembers = new String[3];
    private String[] team1 = new String[2];
    private String[] team2 = new String[2];

    private int membersAlive = 4;

    /**
     * Used to save who your direct buddie is.
     */
    private String buddie;

    /**
     * Used to save the name of the enemy tank to track.
     */
    private String enemyTankName = null;

    /**
     * Used to save which tank the other team is attacking. This is used to prevent that both teams attack the same enemy tank.
     */
    private String enemyAttackedByOtherTeam = null;

    /**
     * Driving direction. 1 or -1. Used to detect if driving against a wall.
     */
    private int drivingDirection = 1;

    /**
     * The status of the robot
     */
    private RobotStatus robotStatus;

    /**
     * Buffer zone to prevent colliding with the wall
     */
    private final double BUFFER_ZONE = 0.15;

    /**
     * Buffers of the x axis and the y axis
     */
    private double xBuffer, yBuffer;

    /**
     * Wall buffers
     */
    private double northWallBuffer, eastWallBuffer, southWallBuffer, westWallBuffer;

    /**
     * Shooting system variables
     */
    private ArrayList<Double> shotArrayList = new ArrayList();
    private double shot = 370.0;
    private double gem = 370.0;

    private int enemyDeadCounter;

    @Override
    public void run() {

        setBodyColor(Color.RED);
        setGunColor(Color.WHITE);
        setRadarColor(Color.BLUE);

        xBuffer = this.getBattleFieldWidth() * BUFFER_ZONE;  //standard map height = 600 so BUFFER_ZONE = 90px
        yBuffer = this.getBattleFieldHeight() * BUFFER_ZONE; //standard map width = 800 so BUFFER-ZONE = 120px
        northWallBuffer = this.getBattleFieldHeight() - xBuffer;
        eastWallBuffer = this.getBattleFieldWidth() - xBuffer;
        southWallBuffer = xBuffer;
        westWallBuffer = xBuffer;

        this.setAdjustGunForRobotTurn(true);

        if(this.getEnergy() == 200) {
            allMembers = getTeammates();

            team1[0] = this.getName();
            team1[1] = allMembers[0];
            team2[0] = allMembers[1];
            team2[1] = allMembers[2];
            setBuddie();

            SetTeamsMessage setTeamsMessage = new SetTeamsMessage(team1, team2);
            try {
                broadcastMessage(setTeamsMessage);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        while (true) {
            turnRadarRightRadians(Double.POSITIVE_INFINITY);
        }
    }

    @Override
    public void onScannedRobot(ScannedRobotEvent e) {
        if(enemyTankName != null) {
            //Check if we have to correct tank to attack
            System.out.println("onScanned enemyDistance = " + e.getDistance());
            goXAndY(getEnemyX(e.getBearing(), e.getDistance()), getEnemyY(e.getBearing(), e.getDistance()), e.getBearingRadians(), e.getDistance());
            if (e.getName().equals(enemyTankName)) {
                callShootSystem(e.getBearingRadians(), e.getBearing(), e.getDistance(), e.getVelocity());
            }
        } else { //If the tank is not set to track.
            System.out.println(enemyDeadCounter);
            if(e.getName().contains("ScreamingEagle")) {
                membersAlive--;
            } else {
                if(!e.getName().equals(enemyAttackedByOtherTeam)) {
                    //Set it to track
                    trackEnemy(e.getName());
                } else if(enemyDeadCounter == 3) {
                    enemyTankName = enemyAttackedByOtherTeam;
                } else if (membersAlive < 2) {
                    trackEnemy(e.getName());
                }
            }
        }
    }

    @Override
    public void onMessageReceived(MessageEvent e) {
        if(e.getMessage() instanceof BuddyMessage) {
            BuddyMessage bm = (BuddyMessage) e.getMessage();
            enemyTankName = bm.getTankToTrack();
        } else if(e.getMessage() instanceof SetTeamsMessage) {
            SetTeamsMessage setTeamsMessage = (SetTeamsMessage) e.getMessage();
            team1 = setTeamsMessage.getTeam1();
            team2 = setTeamsMessage.getTeam2();
            setBuddie();
        } else if(e.getMessage() instanceof TeamToTeamMessage) {
            TeamToTeamMessage totm = (TeamToTeamMessage) e.getMessage();
            if(!e.getSender().equals(buddie)) {
                enemyAttackedByOtherTeam = totm.getEnemyWeAreAttackingName();
            }
        }
    }

    /**
     * When the enemy tank is dead. Empty the name value so an other tank can be tracked now.
     *
     * @param e The event checking if the enemy robot tank died.
     * @author Matthijs
     * @author Andy
     */
    public void onRobotDeath(RobotDeathEvent e) {
        if (e.getName().equals(enemyTankName)) {
            BuddyMessage bm = new BuddyMessage(null);
            try {
                sendMessage(buddie, bm);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            enemyTankName = null;
        }

        if(!e.getName().equals("ScreamingEagle")) {
            enemyDeadCounter++;
        }
    }

    /**
     * When driving against a wall. Change direction.
     *
     * @param e The event checking if a wall is hit.
     * @author Eric
     * @author Youri
     */
    public void onHitWall(HitWallEvent e) {
        drivingDirection = -drivingDirection;
    }

    /**
     * Set the status of the tank
     *
     * @param e Statusevent of onStatus
     * @author Matthijs
     */
    public void onStatus(StatusEvent e) {
        this.robotStatus = e.getStatus();
    }

    /**
     * System responsible for firing on enemy tanks
     *
     * @param bearingRadians
     * @param bearing
     * @param distance
     * @param velocity
     * @author Andy
     */
    private void callShootSystem(double bearingRadians, double bearing, double distance, double velocity) {
        //When an enemy is scanned. Lock the radar on the enemy tank by getting its position and adjusting the radar towards that posistion.
        double radarTurn = this.getHeadingRadians() + bearingRadians - this.getRadarHeadingRadians();
        setTurnRadarRightRadians(Utils.normalRelativeAngle(radarTurn));

        //Get the absolute bearing of the enemy tank.
        double absoluteBearing = this.getHeadingRadians() + bearingRadians;

        if(velocity > 1) {
            //If the enemy tank is driving in a positive direction. Adjust the gun with a positive value
            if (bearing > 0) {
                setTurnGunRightRadians(robocode.util.Utils.normalRelativeAngle(absoluteBearing - getGunHeadingRadians()) - getGunAdjustment(smartVelocity(distance)));
            } else { //If it does not drive in a positive direction, it drives in a negative direction. So adjust gun with a negative value
                setTurnGunRightRadians(robocode.util.Utils.normalRelativeAngle(absoluteBearing - getGunHeadingRadians()) + getGunAdjustment(smartVelocity(distance)));
            }
        }else{
            setTurnGunRightRadians(robocode.util.Utils.normalRelativeAngle(absoluteBearing - getGunHeadingRadians()));

        }


        this.betweenFireSystem();
        this.shotArrayList.add(this.getGunHeading());
        this.smartFireSystem(distance);

        //Execute all the set commands
        execute();


    }

    /**
     * Private method to return an adjust value for the gun based on the enemy velocity.
     *
     * @param enemyVelocity The speed of the enemy. Min = 1 and max = 8.
     * @return The adjustments
     * @author Matthijs
     * @author Andy
     */
    private double getGunAdjustment(double enemyVelocity) {

        //System.out.println(enemyVelocity);
        if (enemyVelocity == 8 || enemyVelocity == -8) {
            return 0.4;
        } else if (enemyVelocity == 7 || enemyVelocity == -7) {
            return 0.35;
        } else if (enemyVelocity == 6 || enemyVelocity == -6) {
            return 0.3;
        } else if (enemyVelocity == 5 || enemyVelocity == -5) {
            return 0.25;
        } else if (enemyVelocity == 4 || enemyVelocity == -4) {
            return 0.2;
        } else if (enemyVelocity == 3 || enemyVelocity == -3) {
            return 0.15;
        } else if (enemyVelocity == 2 || enemyVelocity == -2) {
            return 0.1;
        } else if (enemyVelocity == 1 || enemyVelocity == -1) {
            return 0.05;
        } else if (enemyVelocity == 0) {
            return 0.0;
        } else if (enemyVelocity > 8) {
            return 0.4;
        } else if (enemyVelocity < 0) {
            return 0.05;
        } else {
            return 0.4;
        }

    }

    /**
     * Private method to fire shells at enemy tanks. Firepower set by the distance of the enemy.
     *
     * @param enemyDistance The distance to the enemy.
     * @author Matthijs
     * @author Andy
     */
    private void smartFireSystem(double enemyDistance) {
        //Fire system using distance to decide which firepower is used.
        if (enemyDistance <= 150) {
            setFire(5);
        } else if (enemyDistance <= 300 && enemyDistance > 150) {
            setFire(4);
        } else if (enemyDistance <= 450 && enemyDistance > 600) {
            setFire(2);
        } else {
            setFire(1);
        }
    }

    /**
     * Private method to get the offset of the gunheading based on the distance of the enemy.
     *
     * @param enemyDistance The distance to the enemy.
     * @return
     * @author Andy
     */
    private int smartVelocity(double enemyDistance) {
        if (enemyDistance <= 150) {
            return 3;
        } else if (enemyDistance <= 300 && enemyDistance > 150) {
            return 2;
        } else if (enemyDistance <= 450 && enemyDistance > 600) {
            return 1;
        } else {
            return 1;
        }
    }

    /**
     * Method to compensate the gun turn for variable enemy movement
     * @author Andy
     */
    private void betweenFireSystem() {
        if (this.shotArrayList.size() > 3 &&
                (this.shotArrayList.get(0)) <= (this.shotArrayList.get(2)+ 5.0) &&
                (this.shotArrayList.get(0)) >= (this.shotArrayList.get(2))- 5.0 &&
                (this.shotArrayList.get(1)) <= (this.shotArrayList.get(3)) + 5.0 &&
                (this.shotArrayList.get(1)) >= (this.shotArrayList.get(3)) - 5.0) {

            this.gem = ((this.shotArrayList.get(0))+ (this.shotArrayList.get(1))+ (this.shotArrayList.get(2))+ (this.shotArrayList.get(3))) / (double) this.shotArrayList.size();
            this.shot = this.getGunHeading() - this.gem;
            this.shotArrayList.remove(0);
            if (this.shot > 0.0) {
                this.turnGunLeft(this.shot);
            } else if (this.shot < 0.0) {
                this.turnGunRight(this.shot);
            }
        }
    }

    /**
     * Method that sets buddie of tank. Get it out of array and save in variable.
     * @author Matthijs
     */
    private void setBuddie() {
        int[] location = getMyIndex(this.getName());
        if(location[1] == 1) {
            if(location[0] == 0) {
                buddie = team1[1];
            } else {
                buddie = team1[0];
            }
        } else {
            if(location[0] == 0) {
                buddie = team2[1];
            } else {
                buddie = team2[0];
            }
        }
        System.out.println("My buddie is: " + buddie);
    }

    /**
     * Method to find location in team arrays
     * @author Youri
     * @param name The name of the tank requesting the method
     * @return Location in array
     */
    private int[] getMyIndex(String name) {
        for(int i = 0; i < team1.length; i++) {
            if(team1[i].equals(name)) {
                int[] toReturn = {i ,1};
                return toReturn;
            }
        }

        for(int i = 0; i < team2.length; i++) {
            if(team2[i].equals(name)) {
                int[] toReturn = {i ,2};
                return toReturn;
            }
        }
        int[] toReturn = {-1,-1};
        return toReturn;
    }

    /**
     * Method to lock on enemy and send message to inform buddie who to track
     * @author Matthijs
     * @param enemyTank
     */
    private void trackEnemy(String enemyTank) {
        BuddyMessage bm = new BuddyMessage(enemyTank);
        TeamToTeamMessage totm = new TeamToTeamMessage(enemyTank);
        try {
            sendMessage(buddie, bm);
            broadcastMessage(totm);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        enemyTankName = enemyTank;
    }

    /**
     * Returns enemy x coordinate bases on his bearing and his distance
     * @author Matthijs
     * @param enemyBearing enemy bearing
     * @param enemyDistance enemy distance
     * @return the x coordinate
     */
    private double getEnemyX(double enemyBearing, double enemyDistance) {
        double angle = Math.toRadians((robotStatus.getHeading() + enemyBearing % 360));
        return (robotStatus.getX() + Math.sin(angle) * enemyDistance);
    }

    /**
     * Returns enemy y coordinate bases on his bearing and his distance
     * @param enemyBearing enemy bearing
     * @param enemyDistance enemy distance
     * @return the y coordinate
     */
    private double getEnemyY(double enemyBearing, double enemyDistance) {
        double angle = Math.toRadians((robotStatus.getHeading() + enemyBearing % 360));
        return (robotStatus.getY() + Math.cos(angle) * enemyDistance);
    }

    /**
     * When it may happen a wall will be hit. Turn around. This method calculates how much degrees to turn
     * @author Youri
     * @param ceil Max 45
     * @param floor Min 0
     * @return Where to turn to
     */
    private double generateRandom(double ceil, double floor) {
        return floor + Math.ceil(Math.random() * ceil);
    }

    /**
     * Move to a set of coördinates. Includes wall collision prevent system.
     * @author Eric
     * @author Youri
     * @param moveToX X coördinate
     * @param moveToY Y coördinate
     * @param enemyDistance Distance to enemy
     * @param enemyBearing Bearing of the enemy
     */
    private void goXAndY(double moveToX, double moveToY, double enemyBearing, double enemyDistance) {
        double yPos=getY();
        double xPos=getX();
        double turnAmount = 10;

        //get current heading in degrees
        double currentHeading = this.getHeading();

        this.setAhead(10);

        if (yPos >= northWallBuffer) { //the robot is in the north buffer zone
            //set number for turn amount
            //turnAmount = 45 + generateRandom(45, 0);

            //determine which turn will move the robot away the fastest from the wall
            if (currentHeading > 0 && currentHeading <= 180) {
                this.setTurnRight(turnAmount);
            } else if (currentHeading > 180 && currentHeading <= 360) {
                this.setTurnLeft(turnAmount);
            }
        } else if (xPos >= eastWallBuffer) { //the robot is in the east buffer zone
            //set number for turn amount
            //turnAmount = 45 + generateRandom(45, 0);

            //determine which turn wil move the robot away from the fastest from the wall
            if ((currentHeading > 270 && currentHeading <= 360) || (currentHeading > 0 && currentHeading <= 90)) {
                this.setTurnLeft(turnAmount);
            } else if (currentHeading > 90 && currentHeading <= 270) {
                this.setTurnRight(turnAmount);
            }
        } else if (yPos <= southWallBuffer) { //the robot is in the south buffer zone
            //set number for turn amount
            //turnAmount = 45 + generateRandom(45, 0);

            if (currentHeading > 0 && currentHeading <= 180) {
                this.setTurnLeft(turnAmount);
            } else if (currentHeading > 180 && currentHeading <= 360) {
                this.setTurnRight(turnAmount);
            }
        } else if (xPos <= westWallBuffer) { //the robot is in the west buffer zone
            //set number for turn amount
            //turnAmount = 45 + generateRandom(45, 0);

            if ((currentHeading > 270 && currentHeading <= 360) || (currentHeading > 0 && currentHeading <= 90)) {
                this.setTurnRight(turnAmount);
            } else if (currentHeading > 90 && currentHeading <= 270) {
                this.setTurnLeft(turnAmount);
            }
        } else {
            double absoluteBearing = getHeadingRadians() + enemyBearing;

            setTurnRightRadians(robocode.util.Utils.normalRelativeAngle(absoluteBearing - getHeadingRadians()));
            System.out.println(("goXAndY: enemyDistance: " + enemyDistance));
            setAhead(enemyDistance - 125);
//            moveToX = calculateX(-20, enemyHeading,enemyDistance);
//            moveToY = calculateY(0, enemyHeading,enemyDistance);
//            double targetAngle = Math.atan2(moveToX, moveToY);
//            double Angle = Utils.normalRelativeAngle(targetAngle - getHeadingRadians());
//            double distance = Math.hypot(moveToX, moveToY);
//            double turn = Math.atan(Math.tan(Angle));
//
//            setTurnRightRadians(turn);
//            if (Angle == turn) {
//                setAhead(distance);
//            } else {
//                setBack(distance);
//            }

        }
    }


    /**
     * Calculate route x pos
     * @author Eric
     * @param x Enemy x
     * @param enemyHeading enemy heading
     * @param enemyDistance enemy distance
     * @return x to go to
     */
    private double calculateX(double x, double enemyHeading, double enemyDistance){
        return x+Math.cos(enemyHeading)*enemyDistance;
    }

    /**
     * Calculate route y pos
     * @author Eric
     * @param y Enemy y
     * @param enemyHeading enemy heading
     * @param enemyDistance enemy distance
     * @return y to go to
     */
    private double calculateY(double y, double enemyHeading, double enemyDistance){
        return y+Math.cos(enemyHeading)*enemyDistance;
    }
}
