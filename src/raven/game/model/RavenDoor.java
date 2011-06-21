package raven.game.model;

import java.util.LinkedList;
import java.util.List;

import javax.swing.SwingUtilities;
import javax.swing.event.EventListenerList;

import raven.game.interfaces.IDrawable;
import raven.game.interfaces.IRenderInvoker;
import raven.math.Vector2D;
import raven.math.Wall2D;

public class RavenDoor extends BaseGameEntity implements IRenderInvoker{
	
	public enum Status {
		OPEN,
		OPENING,
		CLOSED,
		CLOSING
	}
	
	protected Status status;
	
	/** a sliding door is created from two walls, back to back. These walls
	 * must be added to a map's geometry in order for an agent to detect them.
	 */
	protected Wall2D wall1;
	protected Wall2D wall2;
	
	/** a container of the id's of the triggers able to open this door */
	protected LinkedList<Integer> switches;
	
	protected float numSecondsStayOpen;
	
	protected float numSecondsCurrentlyOpen;
	
	protected Vector2D p1;
	protected Vector2D p2;
	protected double size;
	
	protected Vector2D vectorToP2Norm;
	
	protected double currentSize;

	private EventListenerList listeners;
	
	public void open() {
		if (status == Status.OPENING) {
			if (currentSize < 2) {
				status = Status.OPEN;
				
				numSecondsCurrentlyOpen = numSecondsStayOpen;
				
				return;
			}
		}
	}
	
	public void close() {
		if (status == Status.CLOSING) {
			status = Status.CLOSED;
			return;
		}
	}

	
	public RavenDoor(int id, Vector2D pos1, Vector2D pos2, int timeout) {
		super(id);

		status = Status.CLOSED;
		numSecondsStayOpen = timeout;
		
		vectorToP2Norm = pos2.sub(pos1);
		vectorToP2Norm.normalize();
		currentSize = size = pos2.distance(pos1);
	}
	
	public void addSwitch(int ID) {
		switches.add(ID);
	}
	
	public List<Integer> getSwitchIDs() {
		return switches;
	}

	public Status status() {
		return status;
	}
	
	public Vector2D from(){
		return p1;
	}
	
	public Vector2D to(){
		return p2;
	}
	
	public double getSize(){
		return size;
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
