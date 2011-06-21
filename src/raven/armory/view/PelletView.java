/**
 * 
 */
package raven.armory.view;

import raven.armory.model.Pellet;
import raven.game.interfaces.IDrawable;
import raven.ui.GameCanvas;

/**
 * @author chester
 *
 */
public class PelletView implements IDrawable {

	private Pellet pellet;
	
	public PelletView(Pellet p){
		pellet = p;
		pellet.addDrawableListener(this);
	}
	
	@Override
	public void run() {
		render();
	}

	@Override
	public void render()
	{
		if ((pellet.getPelletTimePersist() > 0) && pellet.HasImpacted()) {
			GameCanvas.yellowPen();
			GameCanvas.line(pellet.getOrigin(), pellet.getImpactPoint());

			GameCanvas.brownBrush();
			GameCanvas.circle(pellet.getImpactPoint(), 3);
		}
	}

}
