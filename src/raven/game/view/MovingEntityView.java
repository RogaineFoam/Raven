/**
 * 
 */
package raven.game.view;

import raven.game.interfaces.IDrawable;

/**
 * @author chester
 *
 */
public class MovingEntityView implements IDrawable {
	
	/**
	 * 
	 */
	public MovingEntityView() {
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see raven.game.interfaces.IDrawable#render()
	 */
	@Override
	public void render() {
		// TODO Auto-generated method stub

	}

	@Override
	public void run() {
		render();
	}

}
