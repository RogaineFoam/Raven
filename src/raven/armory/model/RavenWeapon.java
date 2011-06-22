/**
 * 
 */
package raven.armory.model;

import raven.game.interfaces.IRavenBot;
import raven.game.interfaces.IRenderInvoker;
import raven.goals.fuzzy.FuzzyModule;
import raven.math.Vector2D;
import raven.systems.RavenObject;

/**
 * @author chester
 *
 */
public abstract class RavenWeapon implements IRenderInvoker {

	private IRavenBot owner;
	private RavenObject itemType;
	private FuzzyModule fuzzyModule;
	private int roundsLeft, maxRoundCapacity; 
	private double rateOfFire, lastDesireabilityScore, idealRange, maxProjectileSpeed;
	public double timeUntilAvailable;
	
	public RavenWeapon(RavenObject weaponType, int defaultRoundsCount, int maxCapacity, 
					   double RoF, double iRange, double projectileSpd, IRavenBot holder)
	{
		itemType = weaponType;
		owner = holder;
		roundsLeft = defaultRoundsCount;
		maxRoundCapacity = maxCapacity;
		rateOfFire = RoF;
		timeUntilAvailable = 0;
		idealRange = iRange;
		maxProjectileSpeed = projectileSpd;
		lastDesireabilityScore = 0;
		
		fuzzyModule = new FuzzyModule();
	}
	
	
	/* OVERRIDES */
	
	/** Causes the weapon to shoot at the chosen position. Each weapons overrides this method.
	 * @param position */
	public abstract boolean ShootAt(Vector2D position);
	
	/**
	 * This overridden method uses fuzzy logic to assign a desireability value to this weapon, based on the distance and the logic
	 * currently assigned to the weapon.
	 * @param distanceToTarget
	 * @return
	 */
	public abstract double GetDesireability(double distanceToTarget);
	
	/* ACCESSORS */
	
//	public final boolean AimAt(Vector2D target) { return owner.rotateFacingTowardPosition(target); }
	
	public double getMaxProjectileSpeed() { return maxProjectileSpeed; }
	
	public int getRoundsRemaining() { return roundsLeft; }
	
	public void decrementRoundsLeft() { if(roundsLeft > 0) --roundsLeft; }
	
	public IRavenBot getOwner() { return owner; }
	
	public FuzzyModule getFuzzyModule() { return fuzzyModule; }
	
	public void setLastDesireability(double newDesireability) { lastDesireabilityScore = newDesireability; }
	/**
	 * Adds the specified number of rounds to this weapons' magazine, without exceeding the max capacity.
	 * @param numberToAdd
	 */
	public void incrementRounds(int numberToAdd){
		roundsLeft += numberToAdd; 
		if(roundsLeft > maxRoundCapacity) roundsLeft = maxRoundCapacity;
	}
	
	public RavenObject getWeaponType() {return itemType; }
	
	public double getIdealRange() { return idealRange; }
	
	
	public void update(double delta) {
		timeUntilAvailable -= delta;
	}
	
	public boolean isReadyForNextShot(double delta) { 
		return timeUntilAvailable < 0; 
	}
	
	protected void UpdateTimeWeaponIsNextAvailable() { timeUntilAvailable = 1.0/rateOfFire; }
	
	protected abstract void InitializeFuzzyModule();


	public double getLastDesirabilityScore() {
		return lastDesireabilityScore;
	}
	
	public int getMaxRounds()
	{
		return maxRoundCapacity;
	}
	
	public void setCurrentRounds(int rounds){
		this.roundsLeft = rounds;
	}
}
