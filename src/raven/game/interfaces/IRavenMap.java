package raven.game.interfaces;

import java.util.List;

import raven.game.model.RavenDoor;
import raven.math.CellSpacePartition;
import raven.math.Vector2D;
import raven.math.Wall2D;
import raven.math.graph.SparseGraph;
import raven.messaging.RavenMessage;
import raven.navigation.model.NavGraphEdge;
import raven.navigation.model.NavGraphNode;
import raven.systems.RavenObject;
import raven.triggers.Trigger;
import raven.triggers.TriggerSystem;

public interface IRavenMap extends IRenderInvoker{

	public abstract void addSpawnPoint(Vector2D point);

	public abstract void addSpawnPoint(double x, double y);

	public abstract void addHealthGiver(Vector2D position, int radius,
			int healthPlus, int respawnDelay);

	public abstract void addWeaponGiver(RavenObject typeOfWeapon,
			Vector2D position, int radius);

	public abstract void addDoor(int id, Vector2D pos1, Vector2D pos2,
			int timeout);

	public abstract Trigger<IRavenBot> addDoorTrigger(Vector2D topLeft,
			Vector2D bottomRight, RavenMessage msg, int receiver);

	public abstract void clear();

	/**
	 * adds a wall and returns a pointer to that wall. (this method can be
	 * used by objects such as doors to add walls to the environment)
	 * @param from wall's starting point
	 * @param to wall's ending point
	 * @return the new wall created
	 */
	public abstract Wall2D addWall(Vector2D from, Vector2D to);

	public abstract void addSoundTrigger(IRavenBot soundSource, double range);

	public abstract double calculateCostToTravelBetweenNodes(int node1,
			int node2);

	/** returns the position of a graph node selected at random */
	public abstract Vector2D getRandomNodeLocation();

	public abstract void updateTriggerSystem(double delta, List<IRavenBot> bots);

	public abstract List<Trigger<IRavenBot>> getTriggers();

	public abstract List<Wall2D> getWalls();

	public abstract SparseGraph<NavGraphNode<Trigger<IRavenBot>>, NavGraphEdge> getNavGraph();

	public abstract List<RavenDoor> getDoors();

	public abstract List<Vector2D> getSpawnPoints();

	public abstract CellSpacePartition<NavGraphNode<Trigger<IRavenBot>>> getCellSpace();

	public abstract Vector2D getRandomSpawnPoint();

	public abstract int getSizeX();

	public abstract int getSizeY();

	public abstract int getMaxDimension();

	public abstract double getCellSpaceNeighborhoodRange();

	public abstract boolean equals(Object other);

	public abstract int hashCode();

	public abstract String getPath();

	public abstract void setPath(String path);

	public abstract String getName();

	public abstract void setName(String name);

	public abstract void setSize(int width, int height);

	public abstract TriggerSystem<Trigger<IRavenBot>> getTriggerSystem();

}