/**
 *  Model classes will implement this interface to have a uniform add/remove/invoke listener.  
 *  These methods will be used to register a view to a specific instance of a model class, and then be invoked when the model decides.
 *  
 */
package raven.game.interfaces;

/**
 * @author chester
 *
 */
public interface IRenderInvoker {

	public void addDrawableListener(IDrawable drawable);
	public void removeDrawableListeners();
	abstract void notifyDrawables();
	abstract boolean shouldDraw();
}
