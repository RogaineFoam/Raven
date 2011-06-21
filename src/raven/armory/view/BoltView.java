/**
 * 
 */
package raven.armory.view;

import raven.armory.model.Bolt;
import raven.game.interfaces.IDrawable;
import raven.ui.GameCanvas;


/**
 * @author chester
 *
 */
public class BoltView implements IDrawable {

	private Bolt bolt;
	
	public BoltView(Bolt b) {
		bolt = b;
		bolt.addDrawableListener(this);
	}

	@Override
	public void run() {
		render();
	}

	@Override
	public void render() {
		GameCanvas.thickGreenPen();
		GameCanvas.line(bolt.pos(), bolt.pos().sub(bolt.velocity()));
	}
}
