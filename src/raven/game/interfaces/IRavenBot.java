package raven.game.interfaces;

import raven.goals.model.GoalThink;
import raven.math.Vector2D;
import raven.messaging.Telegram;
import raven.systems.RavenObject;
import raven.systems.RavenSensoryMemory;
import raven.systems.RavenSteering;
import raven.systems.RavenWeaponSystem;

public interface IRavenBot extends IUpdatable, IRenderInvoker {

	public boolean isAlive();
	public boolean isReadyForTriggerUpdate();
	public Vector2D pos();
	public double getBRadius();
	public void increaseHealth(int healthGiven);
	public int health();
	public RavenWeaponSystem getWeaponSys();
	public int ID();
	public IRavenGame getWorld();
	public boolean canWalkBetween(Vector2D pos, Vector2D pos2);
	public boolean canWalkTo(Vector2D targetPos);
	public void tag();
	public void unTag();
	public void changeWeapon(RavenObject weapon);
	public boolean rotateFacingTowardPosition(Vector2D clientCursorPosition,
			double delta);
	public boolean isPossessed();
	public void exorcise();
	public void fireWeapon(Vector2D p);
	public GoalThink getBrain();
	public void takePossession();
	public Vector2D facing();
	public double fieldOfView();
	public Vector2D scale();
	public RavenSensoryMemory getSensoryMem();
	public RavenSteering getSteering();
	public void update(double delta);
	public void setSpawning();
	public boolean isDead();
	public boolean isSpawning();
	public void spawn(Vector2D pos);
	public IRavenBot getTargetBot();
	public double getMaxSpeed();
	public Vector2D velocity();
	public RavenObject entityType();
	public IRavenTargetingSystem getTargetSys();
	public boolean hasLOSto(Vector2D aimingPos);
	public Vector2D heading();
	public boolean handleMessage(Telegram msg);
	public boolean isHit();
	public void setHit(boolean b);
	public int score();
	
}
