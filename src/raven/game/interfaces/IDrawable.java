/**
 * 
 */
package raven.game.interfaces;

import java.util.EventListener;

/**
 * @author chester
 *
 */
public interface IDrawable extends EventListener, Runnable {
	public void render();
}
