package krakenplayer;

import java.util.ArrayList;

import battlecode.common.*;

public strictfp class RobotPlayer {
    static RobotController rc;

    static Direction[] directions = {Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST};
    static Direction[] cardinalDirections = Direction.cardinalDirections();
    static Direction[] nonCardinalDirections = {Direction.NORTHEAST, Direction.SOUTHEAST, Direction.SOUTHWEST, Direction.NORTHWEST};

    static Direction[] minerDirections = {Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST};
    static Direction[] camperDirections = {Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST};
    static Direction[] rusherDirections = {Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST};
    static Direction[] dammerDirections = {Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST};
    
    static RobotType[] spawnedByMiner = {RobotType.REFINERY, RobotType.VAPORATOR, RobotType.DESIGN_SCHOOL, RobotType.FULFILLMENT_CENTER, RobotType.NET_GUN};

    static int turnCount;
    static int minerCount;
    static int landscaperCount;
    
    static int moveCount;
    static Direction moveDir;
    static Direction prevDir;
    
    static MapLocation hqLocation;
    static MapLocation fcLocation;
    static MapLocation dsLocation;
    static MapLocation rfLocation;
    static MapLocation miningLocation;
    static MapLocation hqTest1;
    static MapLocation hqTest2;
    static MapLocation hqTest3;
    static MapLocation enemyHQLocation;
    static MapLocation enemyFCLocation;
	
    static int mode;
    static int subType;
    
    static final int MINERLIMIT = 6;
    static final int CAMPERLIMIT = 7;
    static final int RUSHERLIMIT = 2;
    static final int DAMMERLIMIT = 12;
    static final int MINER = 0;
    static final int CAMPER = 1;
    static final int RUSHER = 0;
    static final int DAMMER = 1;

    
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
        prevDir = moveDir;
        
        // for doing tasks
        mode = 0;
        
        while (true) {
            turnCount ++;
            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
                // Here, we've separated the controls into a different method for each RobotType.
                // You can add the missing ones or rewrite this into your own control structure.
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

    	for (RobotInfo robot : rc.senseNearbyRobots())
    		if (robot.team == rc.getTeam().opponent() && robot.type == RobotType.DELIVERY_DRONE)
    			if (rc.canShootUnit(robot.ID))
    				rc.shootUnit(robot.ID);
    	
		for (Direction dir : minerDirections)
			if (minerCount < MINERLIMIT)
    			tryBuild(RobotType.MINER, dir);
    	
    	for (Direction dir : camperDirections)
    		if (minerCount >= MINERLIMIT && minerCount < CAMPERLIMIT)
    			tryBuild(RobotType.MINER, dir);
    }

    static void runMiner() throws GameActionException {    	
    	if (turnCount == 1) {
    		hqLocation = rc.adjacentLocation(hqDirection());			
    		dsLocation = hqLocation.add(Direction.NORTH).add(Direction.NORTHEAST);
    		rfLocation = hqLocation.add(Direction.SOUTH).add(Direction.SOUTHWEST);
    		hqTest1 = new MapLocation(rc.getMapWidth() - hqLocation.x, hqLocation.y);						// does not actually cause an error
    		hqTest2 = new MapLocation(rc.getMapWidth() - hqLocation.x, rc.getMapHeight() - hqLocation.y);	// does not actually cause an error
    		hqTest3 = new MapLocation(hqLocation.x, rc.getMapHeight() - hqLocation.y);						// does not actually cause an error

    		for (Direction dir : minerDirections)
    			if (hqDirection() == dir.opposite())
    				subType = MINER;
    		for (Direction dir : camperDirections)
    			if (hqDirection() == dir.opposite())
    				subType = CAMPER;
    		
    	}
    	
    	if (dsDirection() == Direction.CENTER)
    		tryBuild(RobotType.DESIGN_SCHOOL, dsLocation);
    	
    	if (rfDirection() == Direction.CENTER)
    		tryBuild(RobotType.REFINERY, rfLocation);
    	
    	if (rc.canSenseLocation(dsLocation) && dsDirection() == Direction.CENTER && (!rc.isLocationOccupied(dsLocation.add(Direction.SOUTH)) || rc.getLocation().equals(dsLocation.add(Direction.SOUTH))) && rc.getRoundNum() > 50) {
    		if (!rc.getLocation().equals(dsLocation.add(Direction.SOUTH))) {
    			tryMove(rc.getLocation().directionTo(dsLocation.add(Direction.SOUTH)));
    		}
    	}
    	
    	else {
	    	if (subType == MINER) {
	    		
	        	Direction soupDirection = soupDirection(5);
	        	int soupAmount = soupAmount(2);
	        	Direction rfDirection = rfDirection();
	        	
	        	if (!rc.canSenseLocation(hqLocation) && rfDirection == Direction.CENTER && soupAmount > 1000)
	        		for (Direction dir : directions)
	        			tryBuild(RobotType.REFINERY, dir);
	        	
		    	
		        for (Direction dir : directions)											// in all directions
		        	if(tryMine(dir))														// try to mine soup
		        		miningLocation = rc.getLocation().add(dir);							// if mined, save location
		
		        for (Direction dir : directions)											// in all directions
		        	if (tryRefine(dir))														// try to refine soup
		        		mode = 2;															// if refined, go back to mining location
		        
		        if (rc.getSoupCarrying() == RobotType.MINER.soupLimit)						// if soup is full
		        	mode = 1;																// return to HQ
		        
		        switch (mode) {
		        	case 0 : 	tryMove(moveDir); if(moveCount % 5 == 0) moveDir = randomNonCardinalDirection();	break;
			        case 1 : 	if (rfDirection() == Direction.CENTER) 
			        				moveDir = rc.getLocation().directionTo(hqLocation);
			        			else 
			        				moveDir = rfDirection;	
			        			break;
			        case 2 : 	moveDir = rc.getLocation().directionTo(miningLocation); 							break;
		        }
		        
		        if (rc.getSoupCarrying() < RobotType.MINER.soupLimit && soupDirection != Direction.CENTER)
		        	moveDir = soupDirection;
		        
				for (int i = 0; i < 16; i++) { 
					if (moveDir == prevDir.opposite() || !tryMove(moveDir)) {
						if (i > 7)
							if(!tryMove(moveDir))
								moveDir = moveDir.rotateRight();
						moveDir = moveDir.rotateRight();
					}
				}
				
		    	if (mode == 2 && rc.getLocation().distanceSquaredTo(miningLocation) < 4)	// if going to mine and near mining location
		    		if (soupDirection == Direction.CENTER)									// and no soup nearby
		    			mode = rc.getSoupCarrying() == 0 ? 0 : 1;							// explore if soup empty, back to HQ if not
	    	}
	    	
	    	if (subType == CAMPER) {
	    		
	    		if (mode < 3 && enemyFCLocation() != null) {
	    			enemyFCLocation = enemyFCLocation();
	    			mode = 3;
	    		}
	    		
		    	switch (mode) {
		    		case 0 : 	moveDir = rc.getLocation().directionTo(hqTest1); 
		    					if (rc.getLocation().distanceSquaredTo(hqTest1) < 4) {
		    						if (enemyHQLocation() == null) {
		    							mode++;	
		    						}
		    					} break;
		    					
		    		case 1 : 	moveDir = rc.getLocation().directionTo(hqTest2); 
		    					if (rc.getLocation().distanceSquaredTo(hqTest2) < 4) {
		    						if (enemyHQLocation() == null) {
		    							mode++;		
		    						}
		    					} break;
		    					
		    		case 2 : 	moveDir = rc.getLocation().directionTo(hqTest3);
								if (rc.getLocation().distanceSquaredTo(hqTest3) < 4) {
									if (enemyHQLocation() == null) {
										mode = 0;		
									}
								} break;
		    					
		    		case 3 : 	moveDir = enemyFCDirection();
		    					if (rc.getLocation().distanceSquaredTo(enemyFCLocation()) < 16) {
	    							if(!tryBuild(RobotType.NET_GUN, enemyFCDirection()))
	    								for (RobotInfo robot : rc.senseNearbyRobots())
	    									if (robot.team == rc.getTeam().opponent() && robot.type == RobotType.DELIVERY_DRONE)
	    										moveDir = rc.getLocation().directionTo(robot.location).opposite();
		    					} break;
	
		    	}
	    	}
	    	
			for (int i = 0; i < 16; i++) {
				if (moveDir == prevDir.opposite() || !tryMove(moveDir)) {
					if (i > 7)
						if(!tryMove(moveDir))
							moveDir = moveDir.rotateRight();
					moveDir = moveDir.rotateRight();
				}
			}
    	}
    }

    static void runRefinery() throws GameActionException {

    }

    static void runVaporator() throws GameActionException {

    }

    static void runDesignSchool() throws GameActionException {
    	
		for (Direction dir : rusherDirections)
			if (landscaperCount < RUSHERLIMIT)
    			tryBuild(RobotType.LANDSCAPER, dir);
    	
		for (Direction dir : dammerDirections)
			if (landscaperCount >= RUSHERLIMIT && landscaperCount < DAMMERLIMIT && (landscaperCount < 6 || turnCount > 165))
    			tryBuild(RobotType.LANDSCAPER, dir);
    }

    static void runFulfillmentCenter() throws GameActionException {

    }

    static void runLandscaper() throws GameActionException {
    	if (turnCount == 1)	{															
        	dsLocation = rc.adjacentLocation(dsDirection());
        	hqLocation = dsLocation.add(Direction.SOUTH).add(Direction.SOUTHWEST);
    		hqTest1 = new MapLocation(rc.getMapWidth() - hqLocation.x, hqLocation.y);						// does not actually cause an error
    		hqTest2 = new MapLocation(rc.getMapWidth() - hqLocation.x, rc.getMapHeight() - hqLocation.y);	// does not actually cause an error
    		hqTest3 = new MapLocation(hqLocation.x, rc.getMapHeight() - hqLocation.y);						// does not actually cause an error
    		
    		for (Direction dir : rusherDirections)
    			if (dsDirection() == dir.opposite())
    				subType = RUSHER;
    		for (Direction dir : dammerDirections)
    			if (dsDirection() == dir.opposite())
    				subType = DAMMER;
    	}
    	
    	if (subType == RUSHER) {	    	
    		
    		if (mode < 3 && enemyHQLocation() != null) {
    			enemyHQLocation = enemyHQLocation();
    			mode = 3;
    		}
    		
    		if (mode > 3 && !adjacentToEnemyHQ()) {
    			mode = 3;
    		}
    		
	    	switch (mode) {
	    		case 0 : 	moveDir = rc.getLocation().directionTo(hqTest1); 
	    					if (rc.canSenseLocation(hqTest1)) {
	    						if (enemyHQLocation() == null) {
	    							mode++;	
	    						}
	    					} break;
	    					
	    		case 1 : 	moveDir = rc.getLocation().directionTo(hqTest2); 
	    					if (rc.canSenseLocation(hqTest2)) {
	    						if (enemyHQLocation() == null) {
	    							mode++;		
	    						}
	    					} break;
	    					
	    		case 2 : 	moveDir = rc.getLocation().directionTo(hqTest3);
							if (rc.canSenseLocation(hqTest3)) {
								if (enemyHQLocation() == null) {
									mode = 0;		
								}
							} break;
	    					
	    		case 3 : 	moveDir = enemyHQDirection();
	    					if (adjacentToEnemyHQ()) {
	    						mode++;
	    					} break;
	    					
	    		case 4 : 	tryDig(Direction.CENTER);
	    					mode++;																															
	    					break;

	    		case 5 : 	tryDeposit(rc.getLocation().directionTo(enemyHQLocation)); 
	    					mode--;																													
	    					break;
	    	}
	    	
	    	if (mode < 4) {
	    		for (int i = 0; i < 16; i++) { 	    			
	    			if (moveDir == prevDir.opposite() || !tryMove(moveDir)) {
	    				if (rc.senseElevation(rc.getLocation().add(moveDir)) > rc.senseElevation(rc.getLocation()) + 3 && !rc.isLocationOccupied(rc.getLocation().add(moveDir)) && rc.senseElevation(rc.getLocation().add(moveDir)) < rc.senseElevation(rc.getLocation()) + 100) {
	    					tryDig(moveDir);
	    					tryDeposit(moveDir.rotateLeft().rotateLeft());
	    				} 
	    				else if (rc.senseElevation(rc.getLocation().add(moveDir)) < rc.senseElevation(rc.getLocation()) - 3 && !rc.senseFlooding(rc.getLocation().add(moveDir)) && !rc.isLocationOccupied(rc.getLocation().add(moveDir)) && rc.senseElevation(rc.getLocation().add(moveDir)) > rc.senseElevation(rc.getLocation()) - 100) {
	    					tryDig(Direction.CENTER);
	    					tryDeposit(moveDir.rotateLeft().rotateLeft());
	    				}
	    				else if (i > 7) {
	    					if(!tryMove(moveDir))
	    						moveDir = moveDir.rotateRight();
	    				}
	    				else {
	    					moveDir = moveDir.rotateRight();
	    				}
	    			}
	    		}
	    	}
    	}
    	if (subType == DAMMER)
    	{
    		switch (mode) {
    			case 0 : 	moveDir = rc.getLocation().directionTo(hqLocation); 
    						if (adjacentToHQ()) {
    							mode++; 	
    						} break;
    						
    			case 1 : 	if (tryDig(rc.getLocation().directionTo(hqLocation))) {
    							mode++; 
    						} else if (tryDig(rc.getLocation().directionTo(hqLocation).opposite())) {
    							mode++; 							
    						} break;
    						
    			case 2 : 	if (tryDeposit(Direction.CENTER)) {
    							mode--;
    						} break;
    		}
    		
    		if (mode == 0) {
	    		for (int i = 0; i < 16; i++) { 
	    			if (moveDir == prevDir.opposite() || !tryMove(moveDir)) {
	    				if (i > 7)
	    					if(!tryMove(moveDir))
	    						moveDir = moveDir.rotateRight();
	    				moveDir = moveDir.rotateRight();
	    			}
	    		}
    		}
    	}
    }

    static void runDeliveryDrone() throws GameActionException {

    }

    static void runNetGun() throws GameActionException {
    	for (RobotInfo robot : rc.senseNearbyRobots())
    		if (robot.team == rc.getTeam().opponent() && robot.type == RobotType.DELIVERY_DRONE)
    			if (rc.canShootUnit(robot.ID))
    				rc.shootUnit(robot.ID);
    }

    static Direction soupDirection(int rad) throws GameActionException {
    	for (MapLocation soupLoc : rc.senseNearbySoup()) {
    		return rc.getLocation().directionTo(soupLoc);
    	}
    	return Direction.CENTER;
    }
    
    static int soupAmount(int rad) throws GameActionException {
    	MapLocation testLoc;
    	int amt = 0;
    	for (int x = -rad; x <= rad; x++)
    		for (int y = -rad; y <= rad; y++)
    			if (rc.canSenseLocation(testLoc = new MapLocation(rc.getLocation().x + x, rc.getLocation().y + y)))
    				if (rc.senseSoup(testLoc) > 0 && !rc.senseFlooding(testLoc))
    					amt += rc.senseSoup(testLoc);
    	return amt;
    }

    static MapLocation hqLocation() throws GameActionException {
    	for (RobotInfo robot : rc.senseNearbyRobots())
    		if (robot.type == RobotType.HQ && robot.team == rc.getTeam())
    			return robot.location;
    	return null;    	
    }
    
    static Direction hqDirection() throws GameActionException {
    	if (hqLocation() != null)
    		return rc.getLocation().directionTo(hqLocation());
    	else
    		return Direction.CENTER;
    }

    static boolean adjacentToHQ() throws GameActionException {
    	if (hqLocation() != null)
    		return rc.getLocation().isAdjacentTo(hqLocation());
    	else
    		return false;
    }  
    
    static MapLocation dsLocation() throws GameActionException {
    	for (RobotInfo robot : rc.senseNearbyRobots())
    		if (robot.type == RobotType.DESIGN_SCHOOL && robot.team == rc.getTeam())
    			return robot.location;
    	return null;    	
    }    
    
    static Direction dsDirection() throws GameActionException {
    	if (dsLocation() != null)
    		return rc.getLocation().directionTo(dsLocation());
    	else
    		return Direction.CENTER;
    }

    static MapLocation rfLocation() throws GameActionException {
    	for (RobotInfo robot : rc.senseNearbyRobots())
    		if (robot.type == RobotType.REFINERY && robot.team == rc.getTeam())
    			return robot.location;
    	return null;    	
    }        
    
    static Direction rfDirection() throws GameActionException {
    	if (rfLocation() != null)
    		return rc.getLocation().directionTo(rfLocation());
    	else
    		return Direction.CENTER;
    }
    
    static MapLocation enemyHQLocation() throws GameActionException {
    	for (RobotInfo robot : rc.senseNearbyRobots())
    		if (robot.type == RobotType.HQ && robot.team == rc.getTeam().opponent())
    			return robot.location;
    	return null;    	
    }
    
    static Direction enemyHQDirection() throws GameActionException {
    	if (enemyHQLocation() != null)
    		return rc.getLocation().directionTo(enemyHQLocation());
    	else
    		return Direction.CENTER;
    }  
    
    static boolean adjacentToEnemyHQ() throws GameActionException {
    	if (enemyHQLocation() != null)
    		return rc.getLocation().isAdjacentTo(enemyHQLocation());
    	else
    		return false;
    }
    
    static MapLocation enemyFCLocation() throws GameActionException {
    	for (RobotInfo robot : rc.senseNearbyRobots())
    		if (robot.type == RobotType.FULFILLMENT_CENTER && robot.team == rc.getTeam().opponent())
    			return robot.location;
    	return null;    	
    }
    
    static Direction enemyFCDirection() throws GameActionException {
    	if (enemyFCLocation() != null)
    		return rc.getLocation().directionTo(enemyFCLocation());
    	else
    		return Direction.CENTER;
    }  
    
    static boolean adjacentToEnemyFC() throws GameActionException {
    	if (enemyFCLocation() != null)
    		return rc.getLocation().isAdjacentTo(enemyFCLocation());
    	else
    		return false;
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
        if (rc.isReady() && rc.canMove(dir) && !rc.senseFlooding(rc.getLocation().add(dir))) {
        	moveCount++;
        	prevDir = dir;
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
	    	if (type == RobotType.MINER)
	    		minerCount++;
	    	if (type == RobotType.LANDSCAPER)
	    		landscaperCount++;
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
     * Attempts to dig dirt in a given direction.
     *
     * @param dir The intended direction of digging
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryDig(Direction dir) throws GameActionException {
    	MapLocation testLoc = rc.getLocation().add(dir);
    	RobotInfo testRobot;
    	if (rc.isLocationOccupied(testLoc))
    		testRobot = rc.senseRobotAtLocation(testLoc);
    	else
    		testRobot = null;
        if (rc.isReady() && rc.canDigDirt(dir) && (dir == Direction.CENTER || testRobot == null || testRobot.getTeam() != rc.getTeam() || testRobot.getType() == RobotType.HQ)) {
            rc.digDirt(dir);
            return true;
        } else return false;
    }
    
    /**
     * Attempts to deposit dirt in a given direction.
     *
     * @param dir The intended direction of depositing
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryDeposit(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canDepositDirt(dir)) {
            rc.depositDirt(dir);
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
