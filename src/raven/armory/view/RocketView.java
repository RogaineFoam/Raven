/**
 * 
 */
package raven.armory.view;

import raven.armory.model.Rocket;
import raven.game.interfaces.IDrawable;
import raven.ui.GameCanvas;

/**
 * @author chester
 *
 */
public class RocketView implements IDrawable {

	private Rocket rocket;
	
	public RocketView(Rocket r){
		rocket = r;
		rocket.addDrawableListener(this);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		render();
	}

	/* (non-Javadoc)
	 * @see raven.game.interfaces.IDrawable#render()
	 */
	@Override
	public void render() {
		GameCanvas.redPen();
		GameCanvas.orangeBrush();
		GameCanvas.circle(rocket.pos(), 2);

		if (rocket.HasImpacted())
		{
			GameCanvas.hollowBrush();
			GameCanvas.circle(rocket.pos(), rocket.getCurrentBlastRadius());
		}
	}

}
