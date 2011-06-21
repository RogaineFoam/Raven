/**
 * 
 */
package raven.game.model;

import javax.swing.SwingUtilities;
import javax.swing.event.EventListenerList;

import raven.game.interfaces.IDrawable;
import raven.game.interfaces.IRenderInvoker;
import raven.math.Vector2D;
import raven.script.RavenScript;

/**
 * @author chester
 *
 */
public class Grave extends BaseGameEntity implements IRenderInvoker {

	public Vector2D position;
	public double timeLeft;
	private EventListenerList listeners;
	public static double lifeTime = RavenScript.getDouble("GraveLifetime");
	
	public Grave(Vector2D position) {
		super(BaseGameEntity.getNextValidID());
		this.position = position;
		timeLeft = lifeTime;
	}
	
	@Override
	public void update(double delta) {
		timeLeft -= delta;
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
