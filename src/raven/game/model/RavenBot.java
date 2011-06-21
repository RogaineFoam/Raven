package raven.game.model;

import javax.swing.SwingUtilities;
import javax.swing.event.EventListenerList;

import raven.game.interfaces.IDrawable;
import raven.game.interfaces.IRavenBot;
import raven.game.interfaces.IRavenGame;
import raven.game.interfaces.IRavenTargetingSystem;
import raven.goals.model.GoalThink;
import raven.math.C2DMatrix;
import raven.math.Vector2D;
import raven.messaging.Dispatcher;
import raven.messaging.RavenMessage;
import raven.messaging.Telegram;
import raven.navigation.model.RavenPathPlanner;
import raven.script.RavenScript;
import raven.systems.RavenObject;
import raven.systems.RavenSensoryMemory;
import raven.systems.RavenSteering;
import raven.systems.RavenTargetingSystem;
import raven.systems.RavenWeaponSystem;
import raven.utils.Log;
import raven.utils.Regulator;

public class RavenBot extends MovingEntity implements IRavenBot {
	private enum Status {
		ALIVE, DEAD, SPAWNING
	}
	
	private EventListenerList listeners;

	/** alive, dead or spawning? */
	private Status status;

	/** a pointer to the world data */
	private IRavenGame world;

	/**
	 * this object handles the arbitration and processing of high level goals
	 */
	private GoalThink brain;

	/**
	 * this is a class that acts as the bots sensory memory. Whenever this bot
	 * sees or hears an opponent, a record of the event is updated in the
	 * memory.
	 */
	private RavenSensoryMemory sensoryMem;

	/** the bot uses this object to steer */
	private RavenSteering steering;

	/** the bot uses this object to plan paths */
	private RavenPathPlanner pathPlanner;

	/** this is responsible for choosing the bot's current target */
	private IRavenTargetingSystem targSys;

	/**
	 * this handles all the weapons. and has methods for aiming, selecting and
	 * shooting them
	 */
	private RavenWeaponSystem weaponSys;

	/**
	 * A regulator object limits the update frequency of a specific AI component
	 */
	private Regulator weaponSelectionRegulator;
	private Regulator goalArbitrationRegulator;
	private Regulator targetSelectionRegulator;
	private Regulator triggerTestRegulator;

	/**
	 * the bot's health. Every time the bot is shot this value is decreased. If
	 * it reaches zero then the bot dies (and respawns)
	 */
	private int health;

	/**
	 * the bot's maximum health value. It starts its life with health at this
	 * value
	 */
	private int maxHealth;

	/** each time this bot kills another this value is incremented */
	private int score;

	/**
	 * the direction the bot is facing (and therefore the direction of aim).
	 * Note that this may not be the same as the bot's heading, which always
	 * points in the direction of the bot's movement
	 */
	private Vector2D facing;

	/** a bot only perceives other bots within this field of view */
	private double fieldOfView;

	/**
	 * set to true when the bot is hit, and remains true until
	 * numSecondsHitPersistant becomes zero. (used by the render method to draw
	 * a thick red circle around a bot to indicate it's been hit)
	 */
	private boolean hit;

	/** set to true when a human player takes over control of the bot */
	private boolean possessed;

	/**
	 * this method is called from the update method. It calculates and applies
	 * the steering force for this time-step.
	 */
	private void updateMovement(double delta) {
		//calculate the combined steering force
		Vector2D force = steering.calculate();

		//if no steering force is produced decelerate the player by applying a
		//braking force
		if (steering.force().isZero())
		{
			final double BrakingRate = 0.8; 

			velocity = velocity.mul(BrakingRate);                                     
		}

		//calculate the acceleration
		Vector2D accel = force.div(mass);

		//update the velocity
		velocity = velocity.add(accel);

		//make sure vehicle does not exceed maximum velocity per second
		velocity.truncate(maxSpeed * delta);

		//update the position
		position = position.add(velocity);

		//if the vehicle has a non zero velocity the heading and side vectors must 
		//be updated
		if (!velocity.isZero())
		{    
			heading = new Vector2D(velocity);
			heading.normalize();

			side = heading.perp();
		}
	}

	/** initializes the bot's VB with its geometry */
	
	// ////////////////
	// Pulic methods

	public RavenBot(IRavenGame world, Vector2D position) {
		super(position,
				RavenScript.getDouble("Bot_Scale"),
				new Vector2D(0, 0),
				RavenScript.getDouble("Bot_MaxSpeed"),
				new Vector2D(1, 0),
				RavenScript.getDouble("Bot_Mass"),
				new Vector2D(RavenScript.getDouble("Bot_Scale"), RavenScript.getDouble("Bot_Scale")),
				RavenScript.getDouble("Bot_MaxHeadTurnRate"),
				RavenScript.getDouble("Bot_MaxForce"));
		maxHealth = RavenScript.getInt("Bot_MaxHealth");
		health = RavenScript.getInt("Bot_MaxHealth");
		this.world = world;
		hit = false;
		score = 0;
		status = Status.SPAWNING;
		possessed = false;
		fieldOfView = Math.toRadians(RavenScript.getDouble("Bot_FOV"));

		setEntityType(RavenObject.BOT);

		listeners = new EventListenerList();
		

		// a bot starts off facing in the direction it is heading
		facing = heading;

		// create the navigation module
		pathPlanner = new RavenPathPlanner(this);

		// create the steering behavior class
		steering = new RavenSteering(world, this);

		// create the regulators
		weaponSelectionRegulator = new Regulator(
				RavenScript.getDouble("Bot_WeaponSelectionFrequency"));
		goalArbitrationRegulator = new Regulator(
				RavenScript.getDouble("Bot_GoalAppraisalUpdateFreq"));
		targetSelectionRegulator = new Regulator(
				RavenScript.getDouble("Bot_TargetingUpdateFreq"));
		triggerTestRegulator = new Regulator(
				RavenScript.getDouble("Bot_TriggerUpdateFreq"));

		// create the goal queue
		brain = new GoalThink(this);

		// create the targeting system
		targSys = new RavenTargetingSystem(this);

		weaponSys = new RavenWeaponSystem(this,
				RavenScript.getDouble("Bot_ReactionTime"),
				RavenScript.getDouble("Bot_AimAccuracy"),
				RavenScript.getDouble("Bot_AimPersistance"));

		sensoryMem = new RavenSensoryMemory(this,
				RavenScript.getDouble("Bot_MemorySpan"));
	}

	/**
	 * Testing constructor that only establishes a position.  This is a dummy, and has no logic.  To be precise, it lacks:
	 * 		Facing - Vector2D
	 * 		pathPlanner - RavenPathPlanner
	 * 		steering - RavenSteering
	 * 		regulators - Regulator (one for weapon selector, goal arbitration, target selection, and trigger testing
	 * 		brain - GoalThink
	 * 		targSys - RavenTargetingSystem
	 * 		weaponSys - RavenWeaponSystem
	 * 		sensoryMem - RavenSensoryMemory
	 * @param position The center point of the RavenBot
	 * @param health The health to start the RavenBot off with. This is a range from 0 to 100.
	 */
	public RavenBot(Vector2D position, int health){
		super(position,
				RavenScript.getDouble("Bot_Scale"),
				new Vector2D(0, 0),
				RavenScript.getDouble("Bot_MaxSpeed"),
				new Vector2D(1, 0),
				RavenScript.getDouble("Bot_Mass"),
				new Vector2D(RavenScript.getDouble("Bot_Scale"), RavenScript.getDouble("Bot_Scale")),
				RavenScript.getDouble("Bot_MaxHeadTurnRate"),
				RavenScript.getDouble("Bot_MaxForce"));
		
		maxHealth = RavenScript.getInt("Bot_MaxHealth");
		this.health = health;
		this.world = world;
		hit = false;
		score = 0;
		status = Status.SPAWNING;
		possessed = false;
		fieldOfView = Math.toRadians(RavenScript.getDouble("Bot_FOV"));

		setEntityType(RavenObject.BOT);

		listeners = new EventListenerList();
	}
	
	// The usual suspects


	@Override
	public void update(double delta) {
		// Moved from render() since this is time dependent!
		
		// process the currently active goal. Note this is required even if
		// the bot is under user control. This is because a goal is created
		// whenever a user clicks on an area of the map that necessitates a
		// path planning request.
		brain.process(delta);

		// Calculate the steering force and update the bot's velocity and
		// position
		updateMovement(delta);

		// if the bot is under AI control but not scripted
		if (!isPossessed()) {
			weaponSelectionRegulator.update(delta);
			goalArbitrationRegulator.update(delta);
			targetSelectionRegulator.update(delta);
			triggerTestRegulator.update(delta);

			// examine all the opponents in the bots sensory memory and select
			// one to be the current target
			if (targetSelectionRegulator.isReady()) {
				targSys.update();
			}

			// appraise and arbitrate between all possible high level goals
			if (goalArbitrationRegulator.isReady()) {
				brain.Arbitrate();
			}

			// update the sensory memory with any visual stimulus
			sensoryMem.updateVision(delta);

			// select the appropriate weapon to use from the weapons currently
			// in the inventory
			if (weaponSelectionRegulator.isReady()) {
				weaponSys.selectWeapon();
			}

			// this method aims the bot's current weapon at the current target
			// and takes a shot if a shot is possible
			weaponSys.takeAimAndShoot(delta);
		}

	}

	@Override
	public boolean handleMessage(Telegram msg) {
		// first see if the current goal accepts the message
		if (getBrain().handleMessage(msg)) {
			return true;
		}

		// handle any messages not handles by the goals
		switch (msg.msg) {
		case MSG_TAKE_THAT_MF:
			// just return if already dead or spawning
			if (isDead() || isSpawning()) {
				return true;
			}

			// the extra info field of the telegram carries the amount of dmg
			reduceHealth((Integer) msg.extraInfo);

			// if this bot is now dead let the shooter know
			if (isDead()) {
				Dispatcher.dispatchMsg(Dispatcher.SEND_MSG_IMMEDIATELY, ID(),
						msg.senderID, RavenMessage.MSG_YOU_GOT_ME_YOU_SOB,
						Dispatcher.NO_ADDITIONAL_INFO);
			}

			return true;
			
		case MSG_YOU_GOT_ME_YOU_SOB:
			incrementScore();
			
			targSys.clearTarget();
			
			return true;
			
		case MSG_GUNSHOT_SOUND:
			getSensoryMem().updateWithSoundSource((RavenBot)msg.extraInfo);
			
			return true;
			
		case MSG_USER_HAS_REMOVED_BOT:
			RavenBot removedBot = (RavenBot)msg.extraInfo;
			
			getSensoryMem().removeBotFromMemory(removedBot);
			
			if (removedBot.equals(targSys.getTarget())) {
				targSys.clearTarget();
			}
			
			return true;
		
		default:
			return false;	
		}
	}

	/**
	 * this rotates the bot's heading until it is facing directly at the target
	 * position. Returns false if not facing at the target.
	 * 
	 * @param target the target to face
	 * @param delta the amount of time this rotation should take
	 * @return
	 */
	public boolean rotateFacingTowardPosition(Vector2D target, double delta) {
		Vector2D toTarget = target.sub(position);
		toTarget.normalize();

		double dot = facing.dot(toTarget);

		// Clamp to rectify any rounding errors
		dot = Math.min(1, Math.max(-1, dot));
		
		// determine the angle between the heading vector and the target
		double angle = Math.acos(dot);
		
		// return true if the bot's facing is within WeaponAimTolerance degs
		// of facing the target
		final double weaponAimTolerance = 0.034906585; // 2 degrees in radians
		
		if (angle < weaponAimTolerance) {
			facing = toTarget;
			return true;
		}
		
		// clamp the amount to turn to the max turn rate
		if (angle > maxTurnRate * delta) {
			angle = maxTurnRate * delta;
		}
		
		// The next few lines use a rotation matrix to rotate the player's
		// facing vector accordingly
		C2DMatrix rotationMatrix = new C2DMatrix();
		
		// notice how the direction of rotation has to be determined when creating
		// the rotation matrix
		rotationMatrix.rotate(angle * facing.sign(toTarget));
		rotationMatrix.transformVector2Ds(facing);
		
		return false;
	}

	// Attribute access

	public int health() {
		return health;
	}

	public int maxHealth() {
		return maxHealth;
	}

	public void reduceHealth(int amount) {
		health -= amount;
		
		if (health <= 0) {
			setDead();
		}
		
		hit = true;
		
	}

	public void increaseHealth(int amount) {
		health += amount;
		health = Math.min(Math.max(0, health), maxHealth);
	}

	public void restoreHealthToMaximum() { health = maxHealth; }

	public int score() {
		return score;
	}

	public void incrementScore() {
		++score;
	}

	public Vector2D facing() {
		return facing;
	}

	public double fieldOfView() {
		return fieldOfView;
	}

	public boolean isPossessed() {
		return possessed;
	}

	public boolean isDead() {
		return status == Status.DEAD;
	}

	public boolean isAlive() {
		return status == Status.ALIVE;
	}

	public boolean isSpawning() {
		return status == Status.SPAWNING;
	}

	public void setSpawning() {
		status = Status.SPAWNING;
	}

	public void setDead() {
		status = Status.DEAD;
	}

	public void setAlive() {
		status = Status.ALIVE;
	}

	/**
	 * returns a value indicating the time in seconds it will take the bot to
	 * reach the given position at its current speed.
	 * 
	 * @param pos
	 *            position to reach
	 * @return seconds until arrival
	 */
	public double calculateTimeToReachPosition(Vector2D pos) {
		return position.distance(pos) / maxSpeed;
	}

	/**
	 * determines if the bot is close to the given location
	 * 
	 * @param pos
	 *            the position to check
	 * @return true if the bot is close
	 */
	public boolean isAtPosition(Vector2D pos) {
		final double tolerance = getBRadius();
		
		return position.distanceSq(pos) < tolerance * tolerance;
	}

	// Interface for human player

	public void fireWeapon(Vector2D pos) {
		weaponSys.shootAt(pos);
	}

	public void changeWeapon(RavenObject type) {
		weaponSys.changeWeapon(type);
	}

	public void takePossession() {
		if (!(isSpawning() || isDead())) {
			Log.info("bot", "Possesed bot " + ID());
			possessed = true;
		}
	}

	public void exorcise() {
		possessed = false;
		Log.info("bot", "Unpossesed bot " + ID());
		
		brain.addGoal_explore();
	}

	/** spawns the bot at the given position */
	public void spawn(Vector2D pos) {
		setAlive();
		brain.removeAllSubgoals();
		targSys.clearTarget();
		setPos(pos);
		weaponSys.initialize();
		restoreHealthToMaximum();
	}

	/** returns true if this bot is ready to test against all triggers */
	public boolean isReadyForTriggerUpdate() {
		return triggerTestRegulator.isReady();
	}

	/** returns true if the bot has line of sight to the given position. */
	public boolean hasLOSto(Vector2D pos) {
		return world.isLOSOkay(pos(), pos);
	}

	/**
	 * returns true if this bot can move directly to the given position without
	 * bumping into any walls
	 */
	public boolean canWalkTo(Vector2D pos) {
		return !world.isPathObstructed(pos(), pos, getBRadius());
	}

	/**
	 * similar to above. Returns true if the bot can move between the two given
	 * positions without bumping into any walls
	 */
	public boolean canWalkBetween(Vector2D from, Vector2D to) {
		return !world.isPathObstructed(from, to, getBRadius());
	}

	// returns true if there is space enough to step in the indicated direction
	// If true PositionOfStep will be assigned the offset position
	// TODO: check to see if result vector would cross a wall, this needs to be done for all steps.
	public Vector2D canStepLeft() {
		final double stepDistance = getBRadius() * 2;
		
		Vector2D positionOfStep = pos().sub(facing().perp().mul(stepDistance)).sub(facing().perp().mul(getBRadius()));
		
		return canWalkTo(positionOfStep) ? positionOfStep : null;
	}

	public Vector2D canStepRight() {
		final double stepDistance = getBRadius() * 2;
		
		Vector2D positionOfStep = pos().add(facing().perp().mul(stepDistance)).add(facing().perp().mul(getBRadius()));
		
		return canWalkTo(positionOfStep) ? positionOfStep : null;
	}

	public Vector2D canStepForward() {
		final double stepDistance = getBRadius() * 2;
		
		Vector2D positionOfStep = pos().add(facing().mul(stepDistance)).add(facing().mul(getBRadius()));
		
		return canWalkTo(positionOfStep) ? positionOfStep : null;
	}

	public Vector2D canStepBackward() {
		final double stepDistance = getBRadius() * 2;
		
		Vector2D positionOfStep = pos().sub(facing().mul(stepDistance)).sub(facing().mul(getBRadius()));
		
		return canWalkTo(positionOfStep) ? positionOfStep : null;
	}

	// Generic accessors

	public IRavenGame getWorld() {
		return world;
	}

	public RavenSteering getSteering() {
		return steering;
	}

	public RavenPathPlanner getPathPlanner() {
		return pathPlanner;
	}

	public GoalThink getBrain() {
		return brain;
	}

	public IRavenTargetingSystem getTargetSys() {
		return targSys;
	}

	public IRavenBot getTargetBot() {
		return targSys.getTarget();
	}

	public RavenWeaponSystem getWeaponSys() {
		return weaponSys;
	}

	public boolean isHit(){
		return hit;
	}
	
	public void setHit(boolean value){
		hit = value;
	}
	
	public RavenSensoryMemory getSensoryMem() {
		return sensoryMem;
	}

	@Override
	public void addDrawableListener(IDrawable drawable) {
		listeners.add(IDrawable.class, drawable);
	}

	@Override
	public void removeDrawableListeners() {
		IDrawable[] draws = listeners.getListeners(IDrawable.class);
		for(IDrawable d : draws){
			listeners.remove(IDrawable.class, d);
		}
	}

	@Override
	public void notifyDrawables() {
		if(!shouldDraw()) return;
		for(IDrawable drawable : listeners.getListeners(IDrawable.class)){
			SwingUtilities.invokeLater(drawable);
		}
	}

	@Override
	public boolean shouldDraw() {
		// bots update so often, may as well.
		return true;
	}
}
