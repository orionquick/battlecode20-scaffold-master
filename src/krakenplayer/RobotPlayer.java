package krakenplayer;
import battlecode.common.*;

public strictfp class RobotPlayer {
    static RobotController rc;

    static Direction[] directions = Direction.allDirections();
    static Direction[] cardinalDirections = Direction.cardinalDirections();
    static Direction[] nonCardinalDirections = {Direction.NORTHEAST, Direction.SOUTHEAST, Direction.SOUTHWEST, Direction.NORTHWEST};
    
    static RobotType[] spawnedByMiner = {RobotType.REFINERY, RobotType.VAPORATOR, RobotType.DESIGN_SCHOOL,
            RobotType.FULFILLMENT_CENTER, RobotType.NET_GUN};

    static int turnCount;
    
    static int minerCount;
    
    static int moveCount;
    static Direction moveDir;
    static int errCount;
    
    static MapLocation hqLocation;
    static MapLocation fulfillmentCenterLocation;
    static MapLocation designSchoolLocation;
    static MapLocation miningLocation;
    
    static int mode;
    
    
    static final int MINERLIMIT = 3;
    
    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its current status.
        RobotPlayer.rc = rc;
        
        turnCount = 0;
        
        // for HQ
        minerCount = 0;
        
        // for movement
        moveCount = 0;
        moveDir = randomNonCardinalDirection();
        errCount = 0;
        
        // for doing tasks
        mode = 0;
        
        while (true) {
            turnCount += 1;
            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
                // Here, we've separated the controls into a different method for each RobotType.
                // You can add the missing ones or rewrite this into your own control structure.
                System.out.println("I'm a " + rc.getType() + "! Location " + rc.getLocation());
                switch (rc.getType()) {
                    case HQ:                 runHQ();                break;
                    case MINER:              runMiner();             break;
                    case REFINERY:           runRefinery();          break;
                    case VAPORATOR:          runVaporator();         break;
                    case DESIGN_SCHOOL:      runDesignSchool();      break;
                    case FULFILLMENT_CENTER: runFulfillmentCenter(); break;
                    case LANDSCAPER:         runLandscaper();        break;
                    case DELIVERY_DRONE:     runDeliveryDrone();     break;
                    case NET_GUN:            runNetGun();            break;
                    default:										 break;
                }

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            }
        }
    }

    static void runHQ() throws GameActionException {
    	if (minerCount < MINERLIMIT)
    		if (tryBuild(RobotType.MINER, Direction.NORTH))
    			minerCount++;
    }
    
    static void runMiner() throws GameActionException {
    	
    	if (turnCount == 1)													// on turn 1
    		hqLocation = rc.adjacentLocation(Direction.SOUTH);				// set HQ location
    	
        for (Direction dir : directions)									// in all directions
        	if(tryMine(dir))												// try to mine soup
        		miningLocation = rc.getLocation().add(dir);					// if mined, save location
        
        for (Direction dir : directions)									// in all directions
        	if (tryRefine(dir))												// try to refine soup
        		mode = 2;													// if refined, go back to mining location
        
        if (rc.getSoupCarrying() == RobotType.MINER.soupLimit)				// if soup is full
        	mode = 1;														// return to HQ
        
        if (mode == 0)														// explore randomly
        	if (!tryMove(soupDirection()))									// if soup can be sensed, go towards it
	    		if (!tryMove(moveDir))										// if not, try to move in a direction
	    			moveDir = randomNonCardinalDirection();					// if cannot, choose a new direction
        
        if (mode == 1)														// return to HQ
        	if (!tryMove(rc.getLocation().directionTo(hqLocation)))			// try to move toward HQ
        		tryMove(randomDirection());									// if cannot, try to move in a random direction
        
        if (mode == 2)														// return to mining location
        	if (!tryMove(rc.getLocation().directionTo(miningLocation)))		// try to move toward mining location
        		tryMove(randomDirection());									// if cannot, try to move in a random direction
    }
    
    static void runRefinery() throws GameActionException {

    }

    static void runVaporator() throws GameActionException {

    }

    static void runDesignSchool() throws GameActionException {

    }

    static void runFulfillmentCenter() throws GameActionException {
    	
    }

    static void runLandscaper() throws GameActionException {
    	
    }

    static void runDeliveryDrone() throws GameActionException {

    }

    static void runNetGun() throws GameActionException {

    }

    static Direction soupDirection() throws GameActionException {
    	for (Direction dir : directions)
    		if (rc.senseSoup(rc.getLocation().add(dir)) > 0 || rc.senseSoup(rc.getLocation().add(dir).add(dir)) > 0 ||  rc.senseSoup(rc.getLocation().add(dir).add(dir).add(dir)) > 0 )
    			return dir;
    	return Direction.CENTER;
    }
    
    /**
     * Returns a random Direction.
     *
     * @return a random Direction
     */
    static Direction randomDirection() {
        return directions[(int) (Math.random() * directions.length)];
    }

    /**
     * Returns a random Direction.
     *
     * @return a random Direction
     */
    static Direction randomCardinalDirection() {
        return cardinalDirections[(int) (Math.random() * cardinalDirections.length)];
    }
    
    /**
     * Returns a random Direction.
     *
     * @return a random Direction
     */
    static Direction randomNonCardinalDirection() {
        return nonCardinalDirections[(int) (Math.random() * nonCardinalDirections.length)];
    }
    
    /**
     * Returns a random RobotType spawned by miners.
     *
     * @return a random RobotType
     */
    static RobotType randomSpawnedByMiner() {
        return spawnedByMiner[(int) (Math.random() * spawnedByMiner.length)];
    }

    static boolean tryMove() throws GameActionException {
        for (Direction dir : directions)
            if (tryMove(dir))
                return true;
        return false;
    }

    /**
     * Attempts to move in a given direction.
     *
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMove(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canMove(dir)) {
        	moveCount++;
            rc.move(dir);
            return true;
        } else return false;
    }

    /**
     * Attempts to build a given robot in a given direction.
     *
     * @param type The type of the robot to build
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryBuild(RobotType type, Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canBuildRobot(type, dir)) {
            rc.buildRobot(type, dir);
            return true;
        } else return false;
    }
    
    /**
     * Attempts to build a given robot at a given location.
     *
     * @param type The type of the robot to build
     * @param dir The intended location
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryBuild(RobotType type, MapLocation loc) throws GameActionException {   
	    if (rc.isReady() && rc.getLocation().isAdjacentTo(loc) && rc.canBuildRobot(type, rc.getLocation().directionTo(loc))) {
            rc.buildRobot(type, rc.getLocation().directionTo(loc));
            return true;
	    } else return false;
    }       
    
    /**
     * Attempts to mine soup in a given direction.
     *
     * @param dir The intended direction of mining
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMine(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canMineSoup(dir)) {
            rc.mineSoup(dir);
            return true;
        } else return false;
    }

    /**
     * Attempts to refine soup in a given direction.
     *
     * @param dir The intended direction of refining
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryRefine(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canDepositSoup(dir)) {
            rc.depositSoup(dir, rc.getSoupCarrying());
            return true;
        } else return false;
    }


    static void tryBlockchain() throws GameActionException {
        if (turnCount < 3) {
            int[] message = new int[10];
            for (int i = 0; i < 10; i++) {
                message[i] = 123;
            }
            if (rc.canSubmitTransaction(message, 10))
                rc.submitTransaction(message, 10);
        }
    }
}
