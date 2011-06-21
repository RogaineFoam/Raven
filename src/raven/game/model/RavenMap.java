package raven.game.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.SwingUtilities;
import javax.swing.event.EventListenerList;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import raven.game.interfaces.IDrawable;
import raven.game.interfaces.IRavenBot;
import raven.game.interfaces.IRavenMap;
import raven.math.CellSpacePartition;
import raven.math.Vector2D;
import raven.math.Wall2D;
import raven.math.graph.GraphNode;
import raven.math.graph.SparseGraph;
import raven.messaging.RavenMessage;
import raven.navigation.model.NavGraphEdge;
import raven.navigation.model.NavGraphNode;
import raven.script.RavenScript;
import raven.systems.EntityManager;
import raven.systems.RavenObject;
import raven.triggers.Trigger;
import raven.triggers.TriggerHealthGiver;
import raven.triggers.TriggerOnButtonSendMsg;
import raven.triggers.TriggerSoundNotify;
import raven.triggers.TriggerSystem;
import raven.triggers.TriggerWeaponGiver;
import raven.ui.GameCanvas;
import raven.ui.RavenUserOptions;
import raven.utils.Log;
import raven.utils.Pair;

@XStreamAlias("RavenMap")
public class RavenMap implements IRavenMap {
	
	private EventListenerList listeners;

	
	/** the walls that comprise the current map's architecture. */
	private ArrayList<Wall2D> walls;
	
	/** trigger are objects that define a region of space. When a raven bot
	 * enters that area, it 'triggers' an event. That event may be anything
	 * from increasing a bot's health to opening a door or requesting a lift.
	 */
	private TriggerSystem<Trigger<IRavenBot>> triggerSystem;
	
	/** this holds a number of spawn positions. When a bot is instantiated it
	 * will appear at a randomly selected point chosen from this vector */
	private ArrayList<Vector2D> spawnPoints;
	
	/** a map may contain a number of sliding doors. */
	private ArrayList<RavenDoor> doors;
	
	/** this map's accompanying navigation graph */
	private SparseGraph<NavGraphNode<Trigger<IRavenBot>>, NavGraphEdge> navGraph;
	
	/** the graph nodes will be partitioned enabling fast lookup */
	transient private CellSpacePartition<NavGraphNode<Trigger<IRavenBot>>> spacePartition;
	
	/** the size of the search radius the cellspace partition uses when
	 * looking for neighbors */
	transient double cellSpaceNeighborhoodRange;
	
	int sizeX = 0;
	int sizeY = 0;
	
	/* this will hold a pre-calculated lookup table of the cost to travel
	 * from */
	transient private Map<Pair<Integer, Integer>, Double> pathCosts;

	/** the path this file was loaded from. null if unsaved. */
	transient private String path;

	/** the name of this map, as displayed to the user */
	private String name;
		
	private void partitionNavGraph() {
		spacePartition = new CellSpacePartition<NavGraphNode<Trigger<IRavenBot>>>(sizeX, sizeY,
				RavenScript.getInt("NumCellsX"), RavenScript.getInt("NumCellsY"),
				navGraph.numNodes());
		
		// add the graph nodes to the space partition
		for (int i = 0; i < navGraph.numNodes(); i++) {
			spacePartition.addEntity(navGraph.getNode(i));
		}
	}
	
	/* (non-Javadoc)
	 * @see raven.game.IRavenMap#addSpawnPoint(raven.math.Vector2D)
	 */
	public void addSpawnPoint(Vector2D point) {
		spawnPoints.add(point);
	}
	
	/* (non-Javadoc)
	 * @see raven.game.IRavenMap#addSpawnPoint(double, double)
	 */
	public void addSpawnPoint(double x, double y) {
		spawnPoints.add(new Vector2D(x, y));
	}
	
	/* (non-Javadoc)
	 * @see raven.game.IRavenMap#addHealthGiver(raven.math.Vector2D, int, int, int)
	 */
	public void addHealthGiver(Vector2D position, int radius, int healthPlus, int respawnDelay) {
		TriggerHealthGiver healthGiver = new TriggerHealthGiver(position, radius, healthPlus);
		
		triggerSystem.register(healthGiver);
		
		// Let the corresponding NavGraphNode point to this object
		NavGraphNode<Trigger<IRavenBot>> node = new NavGraphNode<Trigger<IRavenBot>>(navGraph.getNextFreeNodeIndex(), position);
		node.setExtraInfo(healthGiver);
		navGraph.addNode(node);
		
		healthGiver.setGraphNodeIndex(node.index());
		
		// register the entity
		EntityManager.registerEntity(healthGiver);
	}
	
	/* (non-Javadoc)
	 * @see raven.game.IRavenMap#addWeaponGiver(raven.game.RavenObject, raven.math.Vector2D, int)
	 */
	public void addWeaponGiver(RavenObject typeOfWeapon, Vector2D position, int radius) {
		TriggerWeaponGiver weaponGiver = new TriggerWeaponGiver(position, radius);
		weaponGiver.setEntityType(typeOfWeapon);
		
		// add it to the appropriate vectors
		triggerSystem.register(weaponGiver);
		
		// Create a corresponding navGraph node
		NavGraphNode<Trigger<IRavenBot>> node = new NavGraphNode<Trigger<IRavenBot>>(navGraph.getNextFreeNodeIndex(), position);
		node.setExtraInfo(weaponGiver);
		navGraph.addNode(node);
		
		weaponGiver.setGraphNodeIndex(node.index());

	}
	
	/* (non-Javadoc)
	 * @see raven.game.IRavenMap#addDoor(int, raven.math.Vector2D, raven.math.Vector2D, int)
	 */
	public void addDoor(int id, Vector2D pos1, Vector2D pos2, int timeout) {
		RavenDoor door = new RavenDoor(id, pos1, pos2, timeout);
		
		doors.add(door);
		Trigger t = addDoorTrigger(pos1, pos2, RavenMessage.MSG_OPEN_SESAME, id);
		door.addSwitch(t.ID());
		// register the entity
		EntityManager.registerEntity(door);
	}
	
	/* (non-Javadoc)
	 * @see raven.game.IRavenMap#addDoorTrigger(raven.math.Vector2D, raven.math.Vector2D, raven.game.messaging.RavenMessage, int)
	 */
	public Trigger<IRavenBot> addDoorTrigger(Vector2D topLeft, Vector2D bottomRight, RavenMessage msg, int receiver) {
		TriggerOnButtonSendMsg<IRavenBot> trigger = new TriggerOnButtonSendMsg<IRavenBot>(topLeft, bottomRight, msg, receiver);
		triggerSystem.register(trigger);
		// register the entity
		EntityManager.registerEntity(trigger);
		return trigger;
	}
	
	/* (non-Javadoc)
	 * @see raven.game.IRavenMap#clear()
	 */
	public void clear() {
		// delete the triggers
		triggerSystem.clear();
		
		// delete the doors
		doors.clear();
		
		walls.clear();
		
		spawnPoints.clear();
		
		// delete the navgraph
		navGraph = null;
		
		// delete the partition info
		spacePartition = null;
	}
	
	public RavenMap() {
		triggerSystem = new TriggerSystem<Trigger<IRavenBot>>();
		doors = new ArrayList<RavenDoor>();
		walls = new ArrayList<Wall2D>();
		spawnPoints = new ArrayList<Vector2D>();
		sizeX = sizeY = 500;
		navGraph = new SparseGraph<NavGraphNode<Trigger<IRavenBot>>, NavGraphEdge>();
		spacePartition = new CellSpacePartition<NavGraphNode<Trigger<IRavenBot>>>(0.0, 0.0, 0, 0, 0);
		cellSpaceNeighborhoodRange = 0.0;
		listeners = new EventListenerList();
	}
	
	private Object readResolve() {
		cellSpaceNeighborhoodRange = navGraph.calculateAverageGraphEdgeLength() + 1;
		
		partitionNavGraph();
		
		pathCosts = navGraph.createAllPairsCostsTable();
		
		return this;
	}
	

	
	/* (non-Javadoc)
	 * @see raven.game.IRavenMap#addWall(raven.math.Vector2D, raven.math.Vector2D)
	 */
	public Wall2D addWall(Vector2D from, Vector2D to) {
		Wall2D wall = new Wall2D(from, to);
		walls.add(wall);
		return wall;
	}
	
	/* (non-Javadoc)
	 * @see raven.game.IRavenMap#addSoundTrigger(raven.game.interfaces.IRavenBot, double)
	 */
	public void addSoundTrigger(IRavenBot soundSource, double range) {
		triggerSystem.register(new TriggerSoundNotify(soundSource, range));
	}
	
	/* (non-Javadoc)
	 * @see raven.game.IRavenMap#calculateCostToTravelBetweenNodes(int, int)
	 */
	public double calculateCostToTravelBetweenNodes(int node1, int node2) {
		if (node1 < 0 || node2 < 0 || node1 >= navGraph.numNodes() || node2 >= navGraph.numNodes())
			throw new IndexOutOfBoundsException("Invalid node index: " + node1 + " to " + node2);
		
		double cost = 100.0;
		try{
			cost =  pathCosts.get(new Pair<Integer,Integer>(node1, node2));
		} catch (NullPointerException e) {
			Log.error("RavenMap", "pathCosts threw a NPE getting cost from " + node1 + " to " + node2);
		}
		return cost;
	}
	
	/* (non-Javadoc)
	 * @see raven.game.IRavenMap#getRandomNodeLocation()
	 */
	public Vector2D getRandomNodeLocation() {
		int randIndex = (int)(Math.random() * navGraph.numActiveNodes());
		
		GraphNode node = null;
		for (int i = 0; i < navGraph.numNodes(); i++) {
			node = navGraph.getNode(i);
			if (node.index() != GraphNode.INVALID_NODE_INDEX) {
				randIndex--;
			}
			if (randIndex < 0) {
				break;
			}
		}
		
		return node.pos();
		
	}
	
	/* (non-Javadoc)
	 * @see raven.game.IRavenMap#updateTriggerSystem(double, java.util.List)
	 */
	public void updateTriggerSystem(double delta, List<IRavenBot> bots) {
		triggerSystem.update(delta, bots);
	}
	
	// Accessors
	
	/* (non-Javadoc)
	 * @see raven.game.IRavenMap#getTriggers()
	 */
	public List<Trigger<IRavenBot>> getTriggers() {
		return triggerSystem.getTriggers();
	}
	
	/* (non-Javadoc)
	 * @see raven.game.IRavenMap#getWalls()
	 */
	public List<Wall2D> getWalls() {
		return walls;
	}
	
	/* (non-Javadoc)
	 * @see raven.game.IRavenMap#getNavGraph()
	 */
	public SparseGraph<NavGraphNode<Trigger<IRavenBot>>, NavGraphEdge> getNavGraph() {
		return navGraph;
	}
	
	/* (non-Javadoc)
	 * @see raven.game.IRavenMap#getDoors()
	 */
	public List<RavenDoor> getDoors() {
		return doors;
	}
	
	/* (non-Javadoc)
	 * @see raven.game.IRavenMap#getSpawnPoints()
	 */
	public List<Vector2D> getSpawnPoints() {
		return spawnPoints;
	}
	
	/* (non-Javadoc)
	 * @see raven.game.IRavenMap#getCellSpace()
	 */
	public CellSpacePartition<NavGraphNode<Trigger<IRavenBot>>> getCellSpace() {
		return spacePartition;
	}
	
	/* (non-Javadoc)
	 * @see raven.game.IRavenMap#getRandomSpawnPoint()
	 */
	public Vector2D getRandomSpawnPoint() {
		return spawnPoints.get((int)(Math.random() * spawnPoints.size()));
	}
	
	/* (non-Javadoc)
	 * @see raven.game.IRavenMap#getSizeX()
	 */
	public int getSizeX() { 
		return sizeX;
	}
	
	/* (non-Javadoc)
	 * @see raven.game.IRavenMap#getSizeY()
	 */
	public int getSizeY() {
		return sizeY;
	}
	
	/* (non-Javadoc)
	 * @see raven.game.IRavenMap#getMaxDimension()
	 */
	public int getMaxDimension() { 
		return Math.max(sizeX, sizeY);
	}
	
	/* (non-Javadoc)
	 * @see raven.game.IRavenMap#getCellSpaceNeighborhoodRange()
	 */
	public double getCellSpaceNeighborhoodRange() {
		return cellSpaceNeighborhoodRange;
	}

	/* (non-Javadoc)
	 * @see raven.game.IRavenMap#render()
	 */

	
	/* (non-Javadoc)
	 * @see raven.game.IRavenMap#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object other){
		if (this == other) return true;
		
		if(! (other instanceof RavenMap)) return false;
		
		RavenMap toCompare = (RavenMap) other;
		
		if( (walls.equals(toCompare.walls)) && (triggerSystem.equals(toCompare.triggerSystem)) 
				&& (spawnPoints.equals(toCompare.spawnPoints)) && (doors.equals(toCompare.doors)) && 
				(navGraph.equals(toCompare.navGraph)) && (spacePartition.equals(toCompare.spacePartition)) &&
				(cellSpaceNeighborhoodRange == toCompare.cellSpaceNeighborhoodRange) && 
				(sizeX == toCompare.sizeX) && (sizeY == toCompare.sizeY)) {
			return true;
		}
		else return false;		
	}
	
	/* (non-Javadoc)
	 * @see raven.game.IRavenMap#hashCode()
	 */
	@Override
	public int hashCode() {
		int result = 0;
		result += walls.hashCode();
		result += triggerSystem.hashCode();
		result += spawnPoints.hashCode();
		result += doors.hashCode();
		result += navGraph.hashCode();
		result += spacePartition.hashCode();
		result += cellSpaceNeighborhoodRange;
		result += sizeX;
		result += sizeY;
		
		// modded by a prime.  I chose 101 because I don;t think we'll have a lot more that that
		// many maps total.
		return result % 101;
	}

	/* (non-Javadoc)
	 * @see raven.game.IRavenMap#getPath()
	 */
	public String getPath() { return path; }
	/* (non-Javadoc)
	 * @see raven.game.IRavenMap#setPath(java.lang.String)
	 */
	public void setPath(String path) { this.path = path; }
	/* (non-Javadoc)
	 * @see raven.game.IRavenMap#getName()
	 */
	public String getName() { return name; }
	/* (non-Javadoc)
	 * @see raven.game.IRavenMap#setName(java.lang.String)
	 */
	public void setName(String name) { this.name = name; }

	/* (non-Javadoc)
	 * @see raven.game.IRavenMap#setSize(int, int)
	 */
	public void setSize(int width, int height) {
		this.sizeX = width;
		this.sizeY = height;		
	}

	@Override
	public TriggerSystem<Trigger<IRavenBot>> getTriggerSystem() {
		return triggerSystem;
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
		// maps update so often, may as well.
		return true;
	}
}
