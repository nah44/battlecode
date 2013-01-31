package bbnuke01;

import battlecode.common.Direction;
import battlecode.common.Upgrade;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

/** The example funcs player is a player meant to demonstrate basic usage of the most common commands.
 * Robots will move around randomly, occasionally mining and writing useless messages.
 * The HQ will spawn soldiers continuously. 
 */
public class RobotPlayer {
	public static void run(RobotController rc) {
		while (true) {
			try {
				if (rc.getType() == RobotType.HQ) {
					if (rc.isActive()) {
						// Spawn a soldier or research something.

						rc.researchUpgrade(Upgrade.valueOf("NUKE"));
						//Direction dir = rc.getLocation().directionTo(rc.senseEnemyHQLocation());
						//if (rc.canMove(dir)) {
						//    rc.spawn(dir);
						//}
					}
				} else if (rc.getType() == RobotType.SOLDIER) {
					if (rc.isActive()) {
						//if (Math.random()<0.000) {
						// Lay a mine 
						//    if(rc.senseMine(rc.getLocation())==null)
						//	rc.layMine();
						//} else { 
						// Choose a random direction, and move that way if possible
						Direction dir = Direction.values()[(int)(Math.random()*8)];

						// but, every so often, greedy to enemy HQ
						if (Math.random() < 0.25) {
							dir = rc.getLocation().directionTo(rc.senseEnemyHQLocation());
						}

						for (int i = 0; i < 8; i++) {
							MapLocation location_in_dir = rc.getLocation().add(dir);

							if (rc.senseMine(location_in_dir) == null) {
								break;
							}

							dir = dir.rotateLeft();
						}

						if(rc.canMove(dir)) {
							rc.move(dir);
							rc.setIndicatorString(0, "Last direction moved: "+dir.toString());
						}
						//}
					}

					if (Math.random()<0.01 && rc.getTeamPower()>5) {
						// Write the number 5 to a position on the message board corresponding to the robot's ID
						rc.broadcast(rc.getRobot().getID()%GameConstants.BROADCAST_MAX_CHANNELS, 5);
					}
				}

				// End turn
				rc.yield();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
