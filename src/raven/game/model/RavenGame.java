package raven.game.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.HashSet;
import java.util.List;

import javax.swing.SwingUtilities;
import javax.swing.event.EventListenerList;

import raven.armory.model.Bolt;
import raven.armory.model.Pellet;
import raven.armory.model.RavenProjectile;
import raven.armory.model.Rocket;
import raven.armory.model.Slug;
import raven.game.interfaces.IDrawable;
import raven.game.interfaces.IRavenBot;
import raven.game.interfaces.IRavenGame;
import raven.game.interfaces.IRavenMap;
import raven.math.Vector2D;
import raven.math.WallIntersectionTest;
import raven.messaging.Dispatcher;
import raven.messaging.RavenMessage;
import raven.navigation.model.PathManager;
import raven.script.RavenScript;
import raven.systems.EntityManager;
import raven.systems.RavenObject;
import raven.ui.RavenUI;
import raven.utils.Log;
import raven.utils.MapLoadedException;
import raven.utils.MapSerializer;

public class RavenGame implements IRavenGame {
	/** the current game map */
	private IRavenMap map;

	/** bots that inhabit the current map */
	private ArrayList<IRavenBot> bots = new ArrayList<IRavenBot>();

	/** A user may control a bot manually. This is that bot */
	private IRavenBot selectedBot;

	/** contains any active projectiles (slugs, rockets, shotgun pellets, etc) */
	private ArrayList<RavenProjectile> projectiles = new ArrayList<RavenProjectile>();

	/** manages all the path planning requests */
	PathManager pathManager = new PathManager(RavenScript.getInt("MaxSearchCyclesPerUpdateStep"));;

	/** true if the game is paused */
	boolean paused;

	/** true if a bot is removed from the game */
	boolean removeBot;

	/**
	 * When a bot is killed a "grave" is displayed for a few seconds. This class
	 * manages the graves.
	 */
	List<Grave> graveMarkers = new ArrayList<Grave>();

	/** Holds a request to load a new map. This is set from another thread */
	private String newMapPath;
	private volatile int botsToAdd;

	private EventListenerList listeners;

	private void clear() {
		Log.debug("game", "Clearing Map");
		// delete the bots
		bots.clear();
		// delete any active projectiles
		projectiles.clear();
	}

	private boolean attemptToAddBot(IRavenBot bot) {
		if (map.getSpawnPoints().size() <= 0) {
			Log.error("game", "Map has no spawn points, cannot add bot");
			return false;
		}

		// we'll make the same number of attempts to spawn a bot this update
		// as there are spawn points
		int attempts = map.getSpawnPoints().size();

		while (--attempts >= 0) {
			// select a random spawn point
			Vector2D pos = map.getRandomSpawnPoint();

			// check to see if it's occupied
			boolean available = true;
			for (IRavenBot other : bots) {
				if (pos.distance(other.pos()) < other.getBRadius()) {
					available = false;
				}
			}
			
			if (available) {
				bot.spawn(pos);
				
				return true;
			}
		}
		return false;
	}

	private void notifyAllBotsOfRemoval(IRavenBot bot) {
		for (IRavenBot other : bots) {
			Dispatcher.dispatchMsg(Dispatcher.SEND_MSG_IMMEDIATELY,
					Dispatcher.SENDER_ID_IRRELEVANT, other.ID(),
					RavenMessage.MSG_USER_HAS_REMOVED_BOT, bot);
		}
	}

	// /////////////////
	// Public methods

	public RavenGame() {
		EntityManager.reset();
		listeners = new EventListenerList();
		try {
			loadMap(RavenScript.getString("StartMap"));
		} catch (IOException e) {
			System.err.println("Failed to load default map: " + RavenScript.getString("StartMap") + ". Reason: \n" + e.getLocalizedMessage());
			System.exit(1);
		}
	}



	/* (non-Javadoc)
	 * @see raven.game.IRavenGame#update(double)
	 */
	@Override
	public void update(double delta) throws MapLoadedException {
		Log.trace("game", "Beginning update");
		
		// Check if we need to switch maps
		if (newMapPath != null) {
			try {
				loadMap(newMapPath);
				throw new MapLoadedException();
			} catch (IOException e) {
				Log.warn("game", "Failed to laod map " + newMapPath + ".");
			}
		}
		
		// Check bot adding and removal
		while (botsToAdd != 0) {
			if (botsToAdd < 0) {
				removeBot();
				botsToAdd++;
			} else {
				addBots(botsToAdd);
				botsToAdd = 0;
			}
		}
		
		// don't update if the user has paused the game
		if (paused) {
			return;
		}
		
		for(Grave grave : graveMarkers){
			grave.update(delta);
		}
		
		// Update a player controlled bot
		getPlayerInput(delta);
		
		// update all the queued searches in the path manager
		pathManager.updateSearches();
		
		// update any doors
		for (RavenDoor door : map.getDoors()) {
			door.update(delta);
		}
		
		// update any current projectiles
		HashSet<RavenProjectile> toRemove = new HashSet<RavenProjectile>();
		for (RavenProjectile projectile : projectiles) {
			if (projectile.IsDead()) {
				toRemove.add(projectile);
			} else {
				projectile.update(delta);
			}
		}
		projectiles.removeAll(toRemove);
		
		// update the bots
		boolean spawnPossible = true;
		
		for (IRavenBot bot : bots) {
			// if this bot's status is 'respawning' attempt to resurrect it
			// from an unoccupied spawn point
			if (bot.isSpawning() && spawnPossible) {
				spawnPossible = attemptToAddBot(bot);
			}
			// if this bot's status is 'dead' add a grave at its current
			// location then change its status to 'respawning'
			else if (bot.isDead()) {
				graveMarkers.add(new Grave(bot.pos()));
				bot.setSpawning();
			}
		    // if this bot is alive update it.
			else if (bot.isAlive()) {
				bot.update(delta);
			}
		}
		
		// update the triggers
		map.updateTriggerSystem(delta, bots);
		
		// if the user has requested that the number of bots be decreased,
		// remove one
		if (removeBot) {
			Log.info("game", "Removing bot at user request");
			if (!bots.isEmpty()) {
				IRavenBot bot = bots.get(bots.size() - 1);
				if (bot.equals(selectedBot)) {
					selectedBot = null;
				}
				notifyAllBotsOfRemoval(bot);
				bot.removeDrawableListeners();
				bots.remove(bot);
			}
			
			removeBot = false;
		}
	}


	/* (non-Javadoc)
	 * @see raven.game.IRavenGame#switchToMap(java.lang.String)
	 */
	@Override
	public void switchToMap(String filename) {
		newMapPath = filename;
	}
	
	/* (non-Javadoc)
	 * @see raven.game.IRavenGame#changeBotCount(int)
	 */
	@Override
	public void changeBotCount(int count) {
		botsToAdd += count;
	}

	/* (non-Javadoc)
	 * @see raven.game.IRavenGame#loadMap(java.lang.String)
	 */
	@Override
	public boolean loadMap(String fileName) throws IOException {
		// clear any current bots and projectiles
		clear();

		// out with the old
		map = null;
		newMapPath = null;
		graveMarkers = null;
		pathManager = null;

		graveMarkers = new ArrayList<Grave>();
		pathManager = new PathManager(
				RavenScript.getInt("MaxSearchCyclesPerUpdateStep"));
		map = MapSerializer.deserializeMapFromPath(fileName);
		
		EntityManager.reset();
		addBots(RavenScript.getInt("NumBots"));
		
		Log.info("game", "Loaded map " + map);
		
		return true;
	}

	protected void addBots(int numBotsToAdd) {
		Log.info("game", "Adding " + numBotsToAdd + " bots to the map");
		while (numBotsToAdd-- > 0) {
			// create a bot. (its position is irrelevant at this point because
			// it will not be rendered until it is spawned)
			IRavenBot bot = new RavenBot(this, new Vector2D());
			
			// switch the default steering behaviors on
			bot.getSteering().wallAvoidanceOn();
			bot.getSteering().separationOn();
			bots.add(bot);
			Log.info("game", "Bot " + bot.ID() + " added");
			
			// register the bot with the entity manager
			EntityManager.registerEntity(bot);
			
			// Give this bot a default goal
			bot.getBrain().addGoal_explore();
//			bot.getBrain().activate();
		}
	}

	/* (non-Javadoc)
	 * @see raven.game.IRavenGame#addRocket(raven.game.interfaces.IRavenBot, raven.math.Vector2D)
	 */
	@Override
	public void addRocket(IRavenBot shooter, Vector2D target) {
		Log.trace("game", "Added rocket");
		RavenProjectile rocket = new Rocket(shooter, target);	
		projectiles.add(rocket);
	}

	/* (non-Javadoc)
	 * @see raven.game.IRavenGame#addRailGunSlug(raven.game.interfaces.IRavenBot, raven.math.Vector2D)
	 */
	@Override
	public void addRailGunSlug(IRavenBot shooter, Vector2D target) {
		Log.trace("game", "Added slug");
		RavenProjectile slug = new Slug(shooter, target);
		projectiles.add(slug);
	}

	/* (non-Javadoc)
	 * @see raven.game.IRavenGame#addShotGunPellet(raven.game.interfaces.IRavenBot, raven.math.Vector2D)
	 */
	@Override
	public void addShotGunPellet(IRavenBot shooter, Vector2D target) {
		Log.trace("game", "Added pellet");
		RavenProjectile pellet = new Pellet(shooter, target);		
		projectiles.add(pellet);
	}

	/* (non-Javadoc)
	 * @see raven.game.IRavenGame#addBolt(raven.game.interfaces.IRavenBot, raven.math.Vector2D)
	 */
	@Override
	public void addBolt(IRavenBot shooter, Vector2D target) {
		Log.trace("game", "Added bolt");
		RavenProjectile bolt = new Bolt(shooter, target);
		projectiles.add(bolt);
	}

	/* (non-Javadoc)
	 * @see raven.game.IRavenGame#removeBot()
	 */
	@Override
	public void removeBot() {
		removeBot = true;
	}

	/* (non-Javadoc)
	 * @see raven.game.IRavenGame#isPathObstructed(raven.math.Vector2D, raven.math.Vector2D, double)
	 */
	@Override
	public boolean isPathObstructed(Vector2D a, Vector2D b, double boundingRadius) {
		Vector2D toB = b.sub(a);
		toB.normalize();
		
		Vector2D curPos = a;
		
		while (curPos.distanceSq(b) > boundingRadius * boundingRadius) {
			// advance curPos one step
			curPos = curPos.add(toB.mul(0.5).mul(boundingRadius));
			
			if (WallIntersectionTest.doWallsIntersectCircle(map.getWalls(), curPos, boundingRadius)) {
				return true;
			}
		}
		
		return false;
	}

	/* (non-Javadoc)
	 * @see raven.game.IRavenGame#getAllBotsInFOV(raven.game.interfaces.IRavenBot)
	 */
	@Override
	public List<IRavenBot> getAllBotsInFOV(final IRavenBot bot) {
		ArrayList<IRavenBot> visibleBots = new ArrayList<IRavenBot>();
		
		for (IRavenBot other : bots) {
			// make sure time is not wasted checking against the same bot or
			// against a bot that is dead or re-spawning
			if (bot.equals(other) || !other.isAlive())
				continue;
		    
			// first of all test to see if this bot is within the FOV
			if (Vector2D.isSecondInFOVOfFirst(bot.pos(), bot.facing(), other.pos(), bot.fieldOfView())) {
				// cast a ray from between the bots to test visibility. If the
				// bot is visible add it to the vector
				if (!WallIntersectionTest.doWallsObstructLineSegment(bot.pos(), other.pos(), map.getWalls())) {
					visibleBots.add(other);
				}	
			}
		}
		return visibleBots;
	}

	/* (non-Javadoc)
	 * @see raven.game.IRavenGame#isSecondVisibleToFirst(raven.game.interfaces.IRavenBot, raven.game.interfaces.IRavenBot)
	 */
	@Override
	public boolean isSecondVisibleToFirst(final IRavenBot first, final IRavenBot second) {
		// if the two bots are equal or if one of them is not alive return
		// false
		if (!first.equals(second) && second.isAlive()) {
			if (Vector2D.isSecondInFOVOfFirst(first.pos(), first.facing(), second.pos(), second.fieldOfView())) {
				if (!WallIntersectionTest.doWallsObstructLineSegment(first.pos(), second.pos(), map.getWalls())) {
					return true;
				}
			}
		}
		
		return false;
	}

	/* (non-Javadoc)
	 * @see raven.game.IRavenGame#isLOSOkay(raven.math.Vector2D, raven.math.Vector2D)
	 */
	@Override
	public boolean isLOSOkay(final Vector2D A, final Vector2D B) {
		return !WallIntersectionTest.doWallsObstructLineSegment(A, B, map.getWalls());
	}

	/* (non-Javadoc)
	 * @see raven.game.IRavenGame#getDistanceToClosestWall(raven.math.Vector2D, raven.math.Vector2D)
	 */
	@Override
	public double getDistanceToClosestWall(Vector2D origin, Vector2D heading) {
		return 0;
	}

	/* (non-Javadoc)
	 * @see raven.game.IRavenGame#getPosOfClosestSwitch(raven.math.Vector2D, int)
	 */
	@Override
	public Vector2D getPosOfClosestSwitch(Vector2D botPos, int doorID) {
		List<Integer> switchIDs = new ArrayList<Integer>();
		
		for (RavenDoor door : map.getDoors()) {
			if (door.ID() == doorID) {
				switchIDs = door.getSwitchIDs();
				break;
			}
		}
		
		Vector2D closest = null;
		double closestDist = Double.MAX_VALUE;
		
		for (Integer switchID : switchIDs) {
			BaseGameEntity trig = EntityManager.getEntityFromID(switchID);
			
			if (isLOSOkay(botPos, trig.pos())) {
				double dist = botPos.distanceSq(trig.pos());
				
				if (dist < closestDist) {
					closestDist = dist;
					closest = trig.pos();
				}
			}
		}
		
		return closest;
	}

	/* (non-Javadoc)
	 * @see raven.game.IRavenGame#getBotAtPosition(raven.math.Vector2D)
	 */
	@Override
	public IRavenBot getBotAtPosition(Vector2D cursorPos) {
		for (IRavenBot bot : bots) {
			if (bot.pos().distance(cursorPos) < bot.getBRadius()) {
				if (bot.isAlive()) {
					return bot;
				}
			}
		}
		
		return null;
	}

	/* (non-Javadoc)
	 * @see raven.game.IRavenGame#togglePause()
	 */
	@Override
	public boolean togglePause() {
		paused = !paused;
		Log.info("game", paused ? "Paused" : "Unpaused");
		
		return paused;
	}

	/* (non-Javadoc)
	 * @see raven.game.IRavenGame#clickRightMouseButton(raven.math.Vector2D, boolean)
	 */
	@Override
	public void clickRightMouseButton(Vector2D p, boolean shiftKeyPressed) {
		IRavenBot bot = getBotAtPosition(p);
		
		// if there is no selected bot just return
		if (bot == null && selectedBot == null)
			return;
		
		// if the user clicks on a selected bot twice it becomes possessed
		// (under the player's control)
		if (bot != null && bot.equals(selectedBot)) {
			selectedBot.takePossession();
			// clear any current goals
			selectedBot.getBrain().removeAllSubgoals();
		} 
 		
		// if the cursor is over a different bot to the existing selection,
		// change selection
		if (bot != null && !bot.equals(selectedBot)) {
			
			if (selectedBot != null) {
				selectedBot.exorcise();
			}
			selectedBot = bot;
		}
		

		
		if (selectedBot.isPossessed()) {
			// if the shift key is pressed down at the same time as clicking
			// then the movement command will be queued
			if (shiftKeyPressed) {
				selectedBot.getBrain().queueGoal_moveToPosition(selectedBot.pos(), p);
			} else {
				selectedBot.getBrain().removeAllSubgoals();
				selectedBot.getBrain().addGoal_moveToPosition(selectedBot.pos(), p);
			}
		}
	}

	/* (non-Javadoc)
	 * @see raven.game.IRavenGame#clickLeftMouseButton(raven.math.Vector2D)
	 */
	@Override
	public void clickLeftMouseButton(Vector2D p) {
		if (selectedBot != null && selectedBot.isPossessed()) {
			selectedBot.fireWeapon(p);
			Log.debug("game", "Fired possessed bot weapon");
		}
	}

	/* (non-Javadoc)
	 * @see raven.game.IRavenGame#exorciseAnyPossessedBot()
	 */
	@Override
	public void exorciseAnyPossessedBot() {
		if (selectedBot != null) {
			selectedBot.exorcise();
		}
	}

	/* (non-Javadoc)
	 * @see raven.game.IRavenGame#getPlayerInput(double)
	 */
	@Override
	public void getPlayerInput(double delta) {
		if (selectedBot != null && selectedBot.isPossessed()) {
			selectedBot.rotateFacingTowardPosition(RavenUI.getClientCursorPosition(), delta);
		}
	}

	/* (non-Javadoc)
	 * @see raven.game.IRavenGame#possessedBot()
	 */
	@Override
	public IRavenBot possessedBot() {
		return selectedBot;
	}

	/* (non-Javadoc)
	 * @see raven.game.IRavenGame#changeWeaponOfPossessedBot(raven.game.RavenObject)
	 */
	@Override
	public void changeWeaponOfPossessedBot(RavenObject weapon) {
		if (selectedBot != null) {
			switch (weapon) {
			case BLASTER:
				Log.info("game", "Switching to blaster");
				possessedBot().changeWeapon(RavenObject.BLASTER);
				break;
			case SHOTGUN:
				Log.info("game", "Switching to shotgun");
				possessedBot().changeWeapon(RavenObject.SHOTGUN);
				break;
			case ROCKET_LAUNCHER:
				Log.info("game", "Switching to rocket launcher");
				possessedBot().changeWeapon(RavenObject.ROCKET_LAUNCHER);
				break;
			case RAIL_GUN:
				Log.info("game", "Switching to rail gun");
				possessedBot().changeWeapon(RavenObject.ROCKET_LAUNCHER);
				break;
			}
		}
	}

	//////////////
	// Accessors

	/* (non-Javadoc)
	 * @see raven.game.IRavenGame#getMap()
	 */
	@Override
	public IRavenMap getMap() {
		return map;
	}

	/* (non-Javadoc)
	 * @see raven.game.IRavenGame#getBots()
	 */
	@Override
	public ArrayList<IRavenBot> getBots() {
		return bots;
	}

	/* (non-Javadoc)
	 * @see raven.game.IRavenGame#getPathManager()
	 */
	@Override
	public PathManager getPathManager() {
		return pathManager;
	}

	/* (non-Javadoc)
	 * @see raven.game.IRavenGame#getNumBots()
	 */
	@Override
	public int getNumBots() {
		return bots.size();
	}
	
	/* (non-Javadoc)
	 * @see raven.game.IRavenGame#tagRavenBotsWithinViewRange(raven.game.interfaces.IRavenBot, double)
	 */
	@Override
	public void tagRavenBotsWithinViewRange(IRavenBot ravenBot,
			double viewDistance) {
		//iterate through all entities checking for range
		for (IRavenBot bot : bots) {
			bot.unTag();

			//work in distance squared to avoid sqrts
			Vector2D to = bot.pos().sub(ravenBot.pos());
			
			//the bounding radius of the other is taken into account by adding it 
		    //to the range
			double range = viewDistance + bot.getBRadius();
			
			//if entity within range, tag for further consideration
			if (!bot.equals(ravenBot) && to.lengthSq() < range * range)
				bot.tag();
		}//next entity
	}

	@Override
	public List<Grave> getGraves() {
		return graveMarkers;
	}

	@Override
	public IRavenBot selectedBot() {
		return selectedBot;
	}

	@Override
	public List<RavenProjectile> getProjectiles() {
		return projectiles;
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
