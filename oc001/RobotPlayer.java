package oc001;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameObject;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.Team;
import battlecode.common.Upgrade;

public class RobotPlayer {    	
	private final static double MIN_TEAM_POWER = 60.0;
	
	public static void run(RobotController rc) {
		while(true) {
			try {
				playOneTurn(rc);
				rc.yield();		
			} catch (Exception e) {
				debug_printf(e.toString());
			}
		}
	}

	// System.setProperty("debugMethodsEnabled", "true");
	// use bc.conf to turn debug mode on	
	private static void debug_printf(String format, Object ... objects) {
		System.out.printf(format, objects);
		System.out.printf("\n");
	}	
	
	private static void debug_printf(Exception e) {
		System.out.printf(e.getMessage());
		System.out.printf("\n");
	}	

	private static void playOneTurn(RobotController rc) {
		switch(rc.getType()) {
			case HQ:
				playOneTurn_hq(rc);
				break;		
			case SOLDIER:
				playOneTurn_soldier(rc);
				break;			
			case SUPPLIER:
				playOneTurn_supplier(rc);
				break;
			case ARTILLERY:
				playOneTurn_artillery(rc);
				break;
			case GENERATOR:
				playOneTurn_generator(rc);
				break;
			case MEDBAY:
				playOneTurn_medbay(rc);
				break;
			case SHIELDS:
				playOneTurn_shields(rc);
				break;
		}
	}

	private static void playOneTurn_hq(RobotController rc) {
		double power = rc.getTeamPower();
		// try to spawn a soldier
		
		if (power > MIN_TEAM_POWER) {
			Direction spawnDirection = findBestSpawnDirection(rc);
			if (spawnDirection != Direction.NONE && couldSpawnSoldier(rc, spawnDirection)) {
				return;
			}
		}
		
		// try to do research
		
		Upgrade bestUpgrade = findBestUpgrade(rc);
		try {
			rc.researchUpgrade(bestUpgrade);
		} catch (GameActionException e) {
			debug_printf(e);
		}
	}	
	
	private static Upgrade findBestUpgrade(RobotController rc) {
		Upgrade bestUpgrade = Upgrade.NUKE;
		
		Upgrade upgradePriorities[] = new Upgrade[]{
				Upgrade.FUSION, // assume FUSION is the best upgrade?
				Upgrade.VISION,
				Upgrade.PICKAXE,
				Upgrade.DEFUSION,
				Upgrade.NUKE
		};
		
		for (Upgrade upgrade: upgradePriorities) {
			int numRounds = 0;
			try {
				numRounds = rc.checkResearchProgress(upgrade);
			} catch (GameActionException e) {
				debug_printf(e);
			}
			if (numRounds < upgrade.numRounds) {
				bestUpgrade = upgrade;
				break;
			}
		}
		return bestUpgrade;
	}

	private static Direction findBestSpawnDirection(RobotController rc) {
		Direction bestDirection = Direction.NONE;
		MapLocation currentLocation = rc.getLocation();
		
		for (Direction direction : Direction.values()) {
			MapLocation newLocation = currentLocation.add(direction);
			GameObject gameObject = null;
			try {
				gameObject = rc.senseObjectAtLocation(newLocation);
			} catch (GameActionException e) {
				debug_printf(e);
			}
			if (gameObject == null) {
				bestDirection = direction;
				break;
			}
		}
		return bestDirection;
	}
	
	private static boolean couldSpawnSoldier(RobotController rc, Direction direction) {
		boolean spawned = false;
		try {
			rc.spawn(direction);
			spawned = true;
		} catch (GameActionException e) {
			debug_printf(e);
			spawned = false;
		} 	
		return spawned;
	}	

	private static void playOneTurn_soldier(RobotController rc) {
		Direction direction = findBestDirectionToMove(rc);

		// try to move
		
		if (direction != Direction.NONE) {
			if (couldMove(rc, direction)) {
				return;
			}
		}
		
		// try to plant a mine
		
		MapLocation myLocation = rc.getLocation();
		if (rc.senseMine(myLocation) == null) {
			try {
				rc.layMine();
			} catch (GameActionException e) {
				debug_printf(e);
			}
		}
		
		// TODO: should try to defuse, or try to build encampments, etc.
	}	

	private static boolean couldMove(RobotController rc, Direction direction) {
		boolean moved = false;
		try {
			rc.move(direction);
			moved = true;
		} catch (GameActionException e) {
			debug_printf(e);
			moved = false;
		}
		return moved;
	}
	
	private static Direction findBestDirectionToMove(RobotController rc) {
		Direction bestDirection = Direction.NONE;
		Team myTeam = rc.getTeam();
		MapLocation myLocation = rc.getLocation();
		
		Robot enemyRobots[] = getNearbyEnemies(rc);
		Robot friendlyRobots[] = getNearbyFriendlies(rc);
		
		if (enemyRobots.length > 1 || friendlyRobots.length > 1) {
			List<Direction> prioritisedDirections = getPrioritisedDirections(rc);
			
			for (Direction direction: prioritisedDirections) {
				MapLocation newLocation = myLocation.add(direction);
				Team mineTeam = rc.senseMine(newLocation);
				boolean safeDirection = (mineTeam == null || mineTeam == myTeam);
				if (safeDirection && rc.canMove(direction)) {
					bestDirection = direction;
					break;
				}
			}			
		}
		return bestDirection;
	}
	
	private static List<Direction> getPrioritisedDirections(final RobotController rc) {
		
		List<Direction> directions = Arrays.asList(Direction.values());
		
		final Robot nearbyEnemies[] = getNearbyEnemies(rc);
		
		// if there are nearby enemy, go towards them
		if (nearbyEnemies.length > 0) {
			debug_printf("NEARBY ENEMIES");
			final MapLocation myLocation = rc.getLocation();
			
			// sort based on distance to enemy
			Collections.sort(directions, new Comparator<Direction>() {
				@Override
				public int compare(Direction o1, Direction o2) {
					Double distance1 = distanceToEnemy(o1);
					Double distance2 = distanceToEnemy(o2);
					return distance1.compareTo(distance2);
				}
				private double distanceToEnemy(Direction direction) {
					MapLocation newLocation = myLocation.add(direction);
					double shortestDistance = -1;
					
					for (Robot robot: nearbyEnemies) {
						double distance = -1;
						try {
							distance = newLocation.distanceSquaredTo(rc.senseLocationOf(robot));
						} catch (GameActionException e) {
							debug_printf(e);
						}
						if (shortestDistance == -1 || distance < shortestDistance) {
							shortestDistance = distance;
						}
					}
					
					return shortestDistance;
				}
			});
			debug_printf(directions.toString());
			
		} else {			
			// otherwise move randomly
			Collections.shuffle(directions);
		}
	
		return directions;
	}
	
	private static Robot[] getNearbyEnemies(RobotController rc) {
		Team otherTeam = rc.getTeam().opponent();
		return rc.senseNearbyGameObjects(Robot.class, 30, otherTeam);
	}
	
	private static Robot[] getNearbyFriendlies(RobotController rc) {
		Team myTeam = rc.getTeam();
		return rc.senseNearbyGameObjects(Robot.class, 1, myTeam);
	}	

	private static void playOneTurn_shields(RobotController rc) {
		throw new RuntimeException("TODO: implement shields logic");
	}

	private static void playOneTurn_medbay(RobotController rc) {
		throw new RuntimeException("TODO: implement medbay logic");
	}

	private static void playOneTurn_generator(RobotController rc) {
		throw new RuntimeException("TODO: implement generator logic");		
	}

	private static void playOneTurn_artillery(RobotController rc) {
		throw new RuntimeException("TODO: implement artillery logic");		
	}

	private static void playOneTurn_supplier(RobotController rc) {
		throw new RuntimeException("TODO: implement supplier logic");		
	}	
}
