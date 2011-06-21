package raven.game.interfaces;

import java.io.IOException;
import java.util.List;

import raven.armory.model.RavenProjectile;
import raven.game.model.Grave;
import raven.math.Vector2D;
import raven.navigation.model.PathManager;
import raven.systems.RavenObject;
import raven.utils.MapLoadedException;

public interface IRavenGame extends IRenderInvoker{

	/**
	 * Update the game state over the given timestep in seconds.
	 * 
	 * @param delta
	 *            amount of time to advance in seconds
	 * @throws MapLoadedException 
	 */
	public abstract void update(double delta) throws MapLoadedException;

	public abstract void switchToMap(String filename);

	public abstract void changeBotCount(int count);

	/** Loads an environment from a file 
	 * @throws IOException */
	public abstract boolean loadMap(String fileName) throws IOException;

	public abstract void addRocket(IRavenBot shooter, Vector2D target);

	public abstract void addRailGunSlug(IRavenBot shooter, Vector2D target);

	public abstract void addShotGunPellet(IRavenBot shooter, Vector2D target);

	public abstract void addBolt(IRavenBot shooter, Vector2D target);

	/** removes the last bot to be added */
	public abstract void removeBot();

	/**
	 * returns true if a bot of size BoundingRadius cannot move from A to B
	 * without bumping into world geometry. It achieves this by stepping from
	 * A to B in steps of size BoundingRadius and testing for intersection
	 * with world geometry at each point.
	 */
	public abstract boolean isPathObstructed(Vector2D a, Vector2D b,
			double boundingRadius);

	/** returns of bots in the FOV of the given bot */
	public abstract List<IRavenBot> getAllBotsInFOV(final IRavenBot bot);

	/**
	 * returns true if the second bot is unobstructed by walls and in the field
	 * of view of the first.
	 */
	public abstract boolean isSecondVisibleToFirst(final IRavenBot first,
			final IRavenBot second);

	/** returns true if the ray between A and B is unobstructed. */
	public abstract boolean isLOSOkay(final Vector2D A, final Vector2D B);

	/**
	 * starting from the given origin and moving in the direction Heading this
	 * method returns the distance to the closest wall
	 * 
	 * Note: This function is not implemented in the C++ version!
	 */
	public abstract double getDistanceToClosestWall(Vector2D origin,
			Vector2D heading);

	/**
	 * returns the position of the closest visible switch that triggers the door
	 * of the specified ID
	 */
	public abstract Vector2D getPosOfClosestSwitch(Vector2D botPos, int doorID);

	/**
	 * given a position on the map this method returns the bot found with its
	 * bounding radius of that position.If there is no bot at the position the
	 * method returns null
	 * 
	 * @param cursorPos
	 * @return
	 */
	public abstract IRavenBot getBotAtPosition(Vector2D cursorPos);

	public abstract boolean togglePause();

	/**
	 * this method is called when the user clicks the right mouse button. The
	 * method checks to see if a bot is beneath the cursor. If so, the bot is
	 * recorded as selected.If the cursor is not over a bot then any selected
	 * bot/s will attempt to move to that position.
	 * 
	 * @param p
	 *            the location clicked
	 */
	public abstract void clickRightMouseButton(Vector2D p,
			boolean shiftKeyPressed);

	/**
	 * this method is called when the user clicks the left mouse button. If
	 * there is a possessed bot, this fires the weapon, else does nothing
	 * 
	 * @param p
	 *            the location clicked
	 */
	public abstract void clickLeftMouseButton(Vector2D p);

	/** when called will release any possessed bot from user control */
	public abstract void exorciseAnyPossessedBot();

	/**
	 * if a bot is possessed the keyboard is polled for user input and any
	 * relevant bot methods are called appropriately
	 * @param delta 
	 */
	public abstract void getPlayerInput(double delta);

	/** Get the value of a selected bot. null if none is selected */
	public abstract IRavenBot possessedBot();

	/** Change to a new weapon for a possessed bot. */
	public abstract void changeWeaponOfPossessedBot(RavenObject weapon);

	public abstract IRavenMap getMap();

	public abstract List<IRavenBot> getBots();

	public abstract PathManager getPathManager();

	public abstract int getNumBots();

	/** Some weird helper method */
	public abstract void tagRavenBotsWithinViewRange(IRavenBot ravenBot,
			double viewDistance);
	
	public List<Grave> getGraves();

	public abstract IRavenBot selectedBot();

	public abstract List<RavenProjectile> getProjectiles();

}