package droneplayer;
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
    
    static Direction moveDir;
    static Direction prevDir;
    static int moveCount;
    
    static MapLocation hqLocation;
    static MapLocation refineryLocation;
    static MapLocation miningLocation;
    static MapLocation fulfullmentLocation;
    static MapLocation fcLocation;
    static MapLocation dsLocation;
    
    
    
    static int mode; // explore / to refinery / to mine
    
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
        
        // for moving units
        moveCount = 0;

        mode = 0;
        
        System.out.println("I'm a " + rc.getType() + " and I just got created!");
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
    	if (minerCount < 3)
    		if (tryBuild(RobotType.MINER, Direction.NORTH))
    			minerCount++;
    }
    
    static void runMiner() throws GameActionException {
    	
    	if (turnCount == 1) {
    		hqLocation = rc.adjacentLocation(hqDirection());			
    		fcLocation = hqLocation.add(Direction.EAST).add(Direction.EAST);
    	}
    	
    	tryBuild(RobotType.FULFILLMENT_CENTER, fcLocation);
    
    	
        for (Direction dir : directions)											// in all directions
        	if(tryMine(dir))														// try to mine soup
        		miningLocation = rc.getLocation().add(dir);							// if mined, save location

        for (Direction dir : directions)											// in all directions
        	if (tryRefine(dir))														// try to refine soup
        		mode = 2;															// if refined, go back to mining location
        
        if (rc.getSoupCarrying() == RobotType.MINER.soupLimit)						// if soup is full
        	mode = 1;																// return to HQ
        
        
        switch (mode) {
        	case 0 : tryMove(moveDir); if(moveCount % 5 == 0) {moveDir = randomNonCardinalDirection();}	break;
	        case 1 : moveDir = rc.getLocation().directionTo(hqLocation);				break;
	        case 2 : moveDir = rc.getLocation().directionTo(miningLocation); 			break;
        }
        
        if (rc.getSoupCarrying() < RobotType.MINER.soupLimit && soupDirection() != Direction.CENTER)
        	moveDir = soupDirection();
        
		for (int i = 0; i < 8; i++)
			if (!tryMove(moveDir))
				moveDir = moveDir.rotateRight();
		
    	if (mode == 2 && rc.getLocation().distanceSquaredTo(miningLocation) < 4)	// if going to mine and near mining location
    		if (soupDirection() == Direction.CENTER)								// and no soup nearby
    			mode = rc.getSoupCarrying() == 0 ? 0 : 1;							// explore if soup empty, back to HQ if not
    	
    	
    } 
    static void runRefinery() throws GameActionException {
        // System.out.println("Pollution: " + rc.sensePollution(rc.getLocation()));
    }

    static void runVaporator() throws GameActionException {

    }

    static void runDesignSchool() throws GameActionException {

    }

    static void runFulfillmentCenter() throws GameActionException {
        for (Direction dir : cardinalDirections)
            tryBuild(RobotType.DELIVERY_DRONE, dir);
    }

    static void runLandscaper() throws GameActionException {

    }

    static void runDeliveryDrone() throws GameActionException {
        Team enemy = rc.getTeam().opponent();
        if (!rc.isCurrentlyHoldingUnit()) {
            // See if there are any enemy robots within striking range (distance 1 from lumberjack's radius)
            RobotInfo[] robots = rc.senseNearbyRobots(GameConstants.DELIVERY_DRONE_PICKUP_RADIUS_SQUARED, enemy);

            if (robots.length > 0) {
                // Pick up a first robot within range
                rc.pickUpUnit(robots[0].getID());
                System.out.println("I picked up " + robots[0].getID() + "!");
            }
        } else {
            // No close robots, so search for robots within sight radius
            tryMove(randomNonCardinalDirection());
        }
    }

    static void runNetGun() throws GameActionException {

    }

    /**
     * Returns a random Direction.
     *
     * @return a random Direction
     */
    static Direction randomDirection() {
        return directions[(int) (Math.random() * directions.length)];
    }
    
    static Direction soupDirection() throws GameActionException {
    	MapLocation testLoc;
    	for (int x = -5; x <= 5; x++)
    		for (int y = -5; y <= 5; y++)
    			if (rc.canSenseLocation(testLoc = new MapLocation(rc.getLocation().x + x, rc.getLocation().y + y)))
    				if (rc.senseSoup(testLoc) > 0 && !rc.senseFlooding(testLoc))
    					return rc.getLocation().directionTo(testLoc);
    					
    	return Direction.CENTER;
    }
    
    static Direction hqDirection() throws GameActionException {
    	for (RobotInfo robot : rc.senseNearbyRobots())
    		if (robot.type == RobotType.HQ && robot.team == rc.getTeam())
    			return rc.getLocation().directionTo(robot.location);
    	return Direction.CENTER;
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
        // MapLocation loc = rc.getLocation();
        // if (loc.x < 10 && loc.x < loc.y)
        //     return tryMove(Direction.EAST);
        // else if (loc.x < 10)
        //     return tryMove(Direction.SOUTH);
        // else if (loc.x > loc.y)
        //     return tryMove(Direction.WEST);
        // else
        //     return tryMove(Direction.NORTH);
    }

    /**
     * Attempts to move in a given direction.
     *
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMove(Direction dir) throws GameActionException {
        // System.out.println("I am trying to move " + dir + "; " + rc.isReady() + " " + rc.getCooldownTurns() + " " + rc.canMove(dir));
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
        // System.out.println(rc.getRoundMessages(turnCount-1));
    }
}
