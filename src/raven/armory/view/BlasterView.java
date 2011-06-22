/**
 * 
 */
package raven.armory.view;

import java.util.List;

import raven.armory.model.Blaster;
import raven.math.Transformations;
import raven.math.Vector2D;
import raven.ui.GameCanvas;

/**
 * @author chester
 *
 */
public class BlasterView extends RavenWeaponView{

	private Blaster blaster;
	
	public BlasterView(Blaster b){
		super();
		blaster = b;
		blaster.addDrawableListener(this);
	
		final Vector2D[] weapon = {new Vector2D(0, -1),
				new Vector2D(10, -1),
				new Vector2D(10 , 1),
				new Vector2D(0, 1)};

		for(Vector2D v : weapon){
			// Dirty scaling hack
			getWeaponVectorBuffer().add(v.mul(1.0/10));
		}
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
		List<Vector2D> tempBuffer = Transformations.WorldTransform(getWeaponVectorBuffer(),
				blaster.getOwner().pos(),
				blaster.getOwner().facing(),
				blaster.getOwner().facing().perp(),
				blaster.getOwner().scale());

		setWeaponVectorTransBuffer(tempBuffer);

		GameCanvas.greenPen();
		GameCanvas.closedShape(tempBuffer);
	}

}
