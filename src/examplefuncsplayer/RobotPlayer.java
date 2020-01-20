package examplefuncsplayer;

import battlecode.common.*;

public strictfp class RobotPlayer {
    static RobotController rc;

    static Direction[] directions = {
        Direction.NORTH,
        Direction.NORTHEAST,
        Direction.EAST,
        Direction.SOUTHEAST,
        Direction.SOUTH,
        Direction.SOUTHWEST,
        Direction.WEST,
        Direction.NORTHWEST
    };
    static Direction[] diagonals = {
		Direction.NORTHWEST, 
		Direction.NORTHEAST, 
		Direction.SOUTHEAST, 
		Direction.SOUTHWEST
	};
    static Direction[] cardinals = Direction.cardinalDirections();
    
    static Direction[] minerDirections = {Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST};
    static Direction[] camperDirections = {Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST};
    static Direction[] rusherDirections = {Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST};
    static Direction[] dammerDirections = {Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST};
    
    static int turnCount;
    
    static int task;
    static int subtask;

    static int startingMiners;
    static int miners;
    
    // miner tasks
    static final int MINING = 0;
    static final int GUARDING = 1;
    static final int CAMPING = 2;
    
    // mining subtasks
    static final int FINDSOUP = 0;
    static final int TOREFINERY = 1;
    static final int TOSOUP = 2;
    
    // landscaper tasks
    static final int RUSHING = 0;
    static final int DAMMING = 1;
    
    // rushing subtasks
    static final int TOHQTEST1 = 0;
    static final int TOHQTEST2 = 1;
    static final int TOHQTEST3 = 2;
    static final int TOENEMYHQ = 3;
    static final int DIG = 4;
    static final int BURY = 5;
    
    // damming subtasks
    static final int TOHQ = 0;
    static final int DIGOUT = 1;
    static final int BUILDDAM = 2;
    
    
    // important locations
    static MapLocation hqLocation;
    static MapLocation rfLocation;
    static MapLocation dsLocation;
    static MapLocation soupLocation;
    static MapLocation hqTest1;
    static MapLocation hqTest2;
    static MapLocation hqTest3;
    static MapLocation enemyHQLocation;
    
    // for movement
    static Direction moveDir;
    static Direction prevDir;
    static int moveCount;
    static boolean pathRight;
    static MapLocation[] previousLocations;
    static final int PREVLOCMEM = 16;
    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        RobotPlayer.rc = rc;
        turnCount = 0;
        task = 0;
        subtask = 0;
        moveDir = randomDiagonal();
        prevDir = moveDir;
        pathRight = true;
        previousLocations = new MapLocation[PREVLOCMEM];
        miners = 0;
        
        while (true) {
            turnCount++;
            try {
                switch (rc.getType()) {
                    case HQ:                 runHQ();                	break;
                    case MINER:              runMiner();             	break;
                    case REFINERY:           runRefinery();          	break;
                    case VAPORATOR:          runVaporator();         	break;
                    case DESIGN_SCHOOL:      runDesignSchool();      	break;
                    case FULFILLMENT_CENTER: runFulfillmentCenter(); 	break;
                    case LANDSCAPER:         runLandscaper();        	break;
                    case DELIVERY_DRONE:     runDeliveryDrone();     	break;
                    case NET_GUN:            runNetGun();           	break;
                    default:											break;
                }
                Clock.yield();
            }
            catch (Exception e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            }
        }
    }

    static void runHQ() throws GameActionException {
    	
    	if (turnCount == 1) {
    		startingMiners = Math.max(soupAmount(36) / 250, 4);
    	}
    	
    	for (Direction dir : directions)
    		if (miners < startingMiners)
    			if (tryBuild(RobotType.MINER, dir))
    				miners++;
    }

    static void runMiner() throws GameActionException {
    	if (turnCount == 1) {
    		hqLocation = locationOf(RobotType.HQ);
    		rfLocation = hqLocation;
    		soupLocation = null;
    	}
    	
    	if (previousLocations[previousLocations.length - 1] == null) {
    		previousLocations[previousLocations.length - 1] = rc.getLocation();
    	}
    	
		if (previousLocations.length == PREVLOCMEM) {
			for (MapLocation loc1 : previousLocations) {
				int frequency = 0;
				for (MapLocation loc2 : previousLocations) {
					if (loc1 != null && loc2 != null && loc1.equals(loc2)) {
						frequency++;
					}
				}
				if (frequency > 2) {
					pathRight = !pathRight;
					previousLocations = new MapLocation[PREVLOCMEM];
					break;
				}
			}
		}
		
    	if (task == MINING) {
    		
    		if (rc.getLocation().isAdjacentTo(hqLocation) && locationOf(RobotType.DESIGN_SCHOOL) == null) {
    			tryBuild(RobotType.DESIGN_SCHOOL, rc.getLocation().directionTo(hqLocation).opposite());
    		}
    		
    		if (rc.getLocation().isAdjacentTo(hqLocation) && locationOf(RobotType.REFINERY) == null) {
    			tryBuild(RobotType.REFINERY, rc.getLocation().directionTo(hqLocation).opposite());
    		}
    		
    		if (!rc.getLocation().isAdjacentTo(hqLocation)) {
    			tryMine();
    		}
    		tryRefine();
    		
        	for (Direction dir : directions) {
    			if (!rc.canSenseLocation(hqLocation) && locationOf(RobotType.REFINERY) == null && soupAmount(16) > 1000) {
    				tryBuild(RobotType.REFINERY, dir);
    			}
			}
    		
    		if (rc.getSoupCarrying() == RobotType.MINER.soupLimit) {
    			subtask = TOREFINERY;
    		}
    		else {
    			subtask = FINDSOUP;
    		}
    		
    		if (subtask == FINDSOUP) {
    			MapLocation temp = soupLocation();
    			if (temp != null) {
    				soupLocation = temp;
    			}
    			if (soupLocation != null) {
    				moveDir = directionTo(soupLocation);
    				if (rc.getLocation().distanceSquaredTo(soupLocation) < 9 && temp == null) {
    					soupLocation = null;
    				}
    			}
    			else if (moveCount % 5 == 0) {
					moveDir = randomDiagonal();
				}
    		}
    		if (subtask == TOREFINERY) {
    			MapLocation temp = locationOf(RobotType.REFINERY);
    			System.out.println(temp);
    			if (temp != null) {
    				rfLocation = temp;
    			}
    			moveDir = directionTo(rfLocation);
    		}
    		
    	}

    	if (!rc.canMove(moveDir)) {
	    	MapLocation[] tempLocs = previousLocations;
			for (int j = 0; j < tempLocs.length - 1; j++) {
				previousLocations[j] = tempLocs[j + 1];
			}
			previousLocations[previousLocations.length - 1] = rc.getLocation();
    	}
    	
    	int i = 0;
		while(rc.isReady()) {
			if (moveDir == prevDir.opposite() || !tryMove(moveDir)) {
				if (i >= 8)
					if(!tryMove(moveDir))
						moveDir = pathRight ? moveDir.rotateRight() : moveDir.rotateLeft();
				moveDir = pathRight ? moveDir.rotateRight() : moveDir.rotateLeft();
			}
			i++;
		}
    }

    static void runRefinery() throws GameActionException {

    }

    static void runVaporator() throws GameActionException {

    }

    static void runDesignSchool() throws GameActionException {
    	tryBuild(RobotType.LANDSCAPER, randomDirection());
    }

    static void runFulfillmentCenter() throws GameActionException {

    }

    static void runLandscaper() throws GameActionException {
      	if (turnCount == 1)	{															
        	dsLocation = locationOf(RobotType.DESIGN_SCHOOL);
        	while (hqLocation == null)
        		hqLocation = locationOf(RobotType.HQ);
        	System.out.println(hqLocation);
    		hqTest1 = new MapLocation(rc.getMapWidth() - hqLocation.x, hqLocation.y);						
    		hqTest2 = new MapLocation(rc.getMapWidth() - hqLocation.x, rc.getMapHeight() - hqLocation.y);	
    		hqTest3 = new MapLocation(hqLocation.x, rc.getMapHeight() - hqLocation.y);
    		enemyHQLocation = null;
    		
    		for (Direction dir : rusherDirections)
    			if (rc.getLocation().directionTo(dsLocation) == dir.opposite())
    				task = RUSHING;
    		for (Direction dir : dammerDirections)
    			if (rc.getLocation().directionTo(dsLocation) == dir.opposite())
    				task = DAMMING;
    	}
    	
    	if (previousLocations[previousLocations.length - 1] == null) {
    		previousLocations[previousLocations.length - 1] = rc.getLocation();
    	}
    	
		if (previousLocations.length == PREVLOCMEM) {
			for (MapLocation loc1 : previousLocations) {
				int frequency = 0;
				for (MapLocation loc2 : previousLocations) {
					if (loc1 != null && loc2 != null && loc1.equals(loc2)) {
						frequency++;
					}
				}
				if (frequency > 2) {
					pathRight = !pathRight;
					previousLocations = new MapLocation[PREVLOCMEM];
					break;
				}
			}
		}
      	
    	if (task == RUSHING) {	    	
    		
    		if (enemyHQLocation == null) {
    			enemyHQLocation = locationOf(RobotType.HQ, rc.getTeam().opponent());
    		}
    		else if (subtask < TOENEMYHQ || (subtask > TOENEMYHQ && !rc.getLocation().isAdjacentTo(enemyHQLocation))) { 
    			subtask = TOENEMYHQ;
    		}
    		
	    	switch (subtask) {
	    		case TOHQTEST1 : 	moveDir = rc.getLocation().directionTo(hqTest1); 
			    					if (rc.canSenseLocation(hqTest1)) {
			    						if (locationOf(RobotType.HQ, rc.getTeam().opponent()) == null) {
			    							subtask++;	
			    						}
			    					} break;
	    					
	    		case TOHQTEST2 : 	moveDir = rc.getLocation().directionTo(hqTest2); 
			    					if (rc.canSenseLocation(hqTest2)) {
			    						if (locationOf(RobotType.HQ, rc.getTeam().opponent()) == null) {
			    							subtask++;		
			    						}
			    					} break;
	    					
	    		case TOHQTEST3 : 	moveDir = rc.getLocation().directionTo(hqTest3);
									if (rc.canSenseLocation(hqTest3)) {
										if (locationOf(RobotType.HQ, rc.getTeam().opponent()) == null) {
											subtask = 0;		
										}
									} break;
	    					
	    		case TOENEMYHQ : 	moveDir = rc.getLocation().directionTo(enemyHQLocation);
			    					if (rc.getLocation().isAdjacentTo(enemyHQLocation)) {
			    						subtask++;
			    					} break;
	    					
	    		case DIG : 			tryDig(Direction.CENTER);
			    					subtask++;																															
			    					break;

	    		case BURY : 		tryDeposit(rc.getLocation().directionTo(enemyHQLocation)); 
			    					subtask--;																													
			    					break;
	    	}
	    	
	    	if (subtask < DIG) {
	        	if (!rc.canMove(moveDir)) {
	    	    	MapLocation[] tempLocs = previousLocations;
	    			for (int j = 0; j < tempLocs.length - 1; j++) {
	    				previousLocations[j] = tempLocs[j + 1];
	    			}
	    			previousLocations[previousLocations.length - 1] = rc.getLocation();
	        	}
	        	
	        	int i = 0;
	    		while(rc.isReady()) {
	    			if (moveDir == prevDir.opposite() || !tryMove(moveDir)) {
	    				if (rc.senseElevation(rc.getLocation().add(moveDir)) > rc.senseElevation(rc.getLocation()) + 3 && !rc.isLocationOccupied(rc.getLocation().add(moveDir)) && rc.senseElevation(rc.getLocation().add(moveDir)) < rc.senseElevation(rc.getLocation()) + 100) {
	    					tryDig(moveDir);
	    					tryDeposit(moveDir.rotateLeft().rotateLeft());
	    				} 
	    				else if (rc.senseElevation(rc.getLocation().add(moveDir)) < rc.senseElevation(rc.getLocation()) - 3 && !rc.senseFlooding(rc.getLocation().add(moveDir)) && !rc.isLocationOccupied(rc.getLocation().add(moveDir)) && rc.senseElevation(rc.getLocation().add(moveDir)) > rc.senseElevation(rc.getLocation()) - 100) {
	    					tryDig(Direction.CENTER);
	    					tryDeposit(moveDir.rotateLeft().rotateLeft());
	    				}
	    				else if (i >= 8) {
	    					if(!tryMove(moveDir))
	    						moveDir = pathRight ? moveDir.rotateRight() : moveDir.rotateLeft();
	    				}
	    				else{
	    					moveDir = pathRight ? moveDir.rotateRight() : moveDir.rotateLeft();
	    				}
	    			}
	    			i++;
	    		}
	    	}
    	}
    	if (task == DAMMING)
    	{
    		switch (subtask) {
    			case TOHQ : 		moveDir = rc.getLocation().directionTo(hqLocation); 
		    						if (rc.getLocation().isAdjacentTo(hqLocation)) {
		    							subtask++; 	
		    						} break;
    						
    			case DIGOUT : 		if (tryDig(directionTo(hqLocation))) {
		    							subtask++; 
		    						} else if (tryDig(directionTo(hqLocation).opposite())) {
		    							subtask++; 							
		    						} else if (tryDig(directionTo(hqLocation).opposite().rotateLeft())) {
		    							subtask++; 							
		    						} else if (tryDig(directionTo(hqLocation).opposite().rotateRight())) {
		    							subtask++; 							
		    						} break;
    						
    			case BUILDDAM :		for (Direction dir : directions) {
	    								if (rc.senseElevation(hqLocation.add(dir)) <= rc.senseElevation(rc.getLocation())) {
	    									if (tryDeposit(rc.getLocation().directionTo(hqLocation.add(dir)))) {
	    										subtask--;
	    										break;
	    									}
	    								}
    								} subtask--; break;
    		}
    		
    		if (subtask == TOHQ) {
    	    	if (!rc.canMove(moveDir)) {
    		    	MapLocation[] tempLocs = previousLocations;
    				for (int j = 0; j < tempLocs.length - 1; j++) {
    					previousLocations[j] = tempLocs[j + 1];
    				}
    				previousLocations[previousLocations.length - 1] = rc.getLocation();
    	    	}
    	    	
    	    	int i = 0;
    			while(rc.isReady()) {
    				if (moveDir == prevDir.opposite() || !tryMove(moveDir)) {
    					if (i >= 8)
    						if(!tryMove(moveDir))
    							moveDir = pathRight ? moveDir.rotateRight() : moveDir.rotateLeft();
    					moveDir = pathRight ? moveDir.rotateRight() : moveDir.rotateLeft();
    				}
    				i++;
    			}
    		}
    	}
    }

    static void runDeliveryDrone() throws GameActionException {

    }

    static void runNetGun() throws GameActionException {

    }
    
    static Direction directionTo(MapLocation loc) {
    	if (loc != null)
    		return rc.getLocation().directionTo(loc);
    	else
    		return Direction.CENTER;
    }
    
    static MapLocation locationOf(RobotType type)  throws GameActionException {
    	for (RobotInfo robot : rc.senseNearbyRobots())
    		if (robot.type == type && robot.team == rc.getTeam())
    			return robot.location;
    	return null;    
    }
    
    static MapLocation locationOf(RobotType type, Team team)  throws GameActionException {
    	for (RobotInfo robot : rc.senseNearbyRobots())
    		if (robot.type == type && robot.team == team)
    			return robot.location;
    	return null;    
    }
    
    static MapLocation soupLocation() throws GameActionException {
    	for (MapLocation soup : rc.senseNearbySoup())
    		if (!rc.senseFlooding(soup) && Math.abs(rc.senseElevation(rc.getLocation()) - rc.senseElevation(soup)) <= 3)
    			return soup;
    	return null;
    }   
    
    static int soupAmount(int rad) throws GameActionException {
    	int amt = 0;
    	for (MapLocation soup : rc.senseNearbySoup(rad))
    		if (!rc.senseFlooding(soup) && Math.abs(rc.senseElevation(rc.getLocation()) - rc.senseElevation(soup)) <= 3)
    			amt+=rc.senseSoup(soup);
    	return amt;
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
    static Direction randomDiagonal() {
        return diagonals[(int) (Math.random() * diagonals.length)];
    }
    
    /**
     * Returns a random Direction.
     *
     * @return a random Direction
     */
    static Direction randomCardinal() {
        return cardinals[(int) (Math.random() * cardinals.length)];
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
        if (rc.isReady() && rc.canMove(dir) && !rc.senseFlooding(rc.adjacentLocation(dir))) {
        	moveCount++;
        	prevDir = dir;
            rc.move(dir);
            return true;
        } else return false;
    }

    static boolean tryBuild(RobotType type, MapLocation loc) throws GameActionException {
    	if (rc.getLocation().isAdjacentTo(loc) && rc.isReady() && rc.canBuildRobot(type, rc.getLocation().directionTo(loc))) {
	        rc.buildRobot(type, rc.getLocation().directionTo(loc));
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

    static boolean tryMine() throws GameActionException {
    	for (Direction dir : directions) {
	        if (rc.isReady() && rc.canMineSoup(dir)) {
	            rc.mineSoup(dir);
	            return true;
	        }
    	}
    	return false;
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
        if (rc.isReady() && rc.canDigDirt(dir)) {
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
    
    static boolean tryRefine() throws GameActionException {
    	for (Direction dir : directions) {
	        if (rc.isReady() && rc.canDepositSoup(dir)) {
	            rc.depositSoup(dir, rc.getSoupCarrying());
	            return true;
	        }
    	}
    	return false;
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
            int[] message = new int[7];
            for (int i = 0; i < 7; i++) {
                message[i] = 123;
            }
            if (rc.canSubmitTransaction(message, 10))
                rc.submitTransaction(message, 10);
        }
        // System.out.println(rc.getRoundMessages(turnCount-1));
    }
}
