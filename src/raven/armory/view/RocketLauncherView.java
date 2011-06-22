/**
 * 
 */
package raven.armory.view;

import java.util.List;

import raven.armory.model.RocketLauncher;
import raven.math.Transformations;
import raven.math.Vector2D;
import raven.ui.GameCanvas;

/**
 * @author chester
 *
 */
public class RocketLauncherView extends RavenWeaponView {
	
	private RocketLauncher rocketLauncher;
	
	public RocketLauncherView(RocketLauncher rl){
		rocketLauncher = rl;
		rocketLauncher.addDrawableListener(this);
	
		final Vector2D[] weaponVectors = {
				new Vector2D(0, -3),
				new Vector2D(6, -3),
				new Vector2D(6, -1),
				new Vector2D(15, -1),
				new Vector2D(15, 1),
				new Vector2D(6, 1),
				new Vector2D(6, 3),
				new Vector2D(0, 3)
		};
		
		for (Vector2D v : weaponVectors)
		{
			// Dirty scaling hack
			getWeaponVectorBuffer().add(v.mul(1.0/10));
		}
	}
	
	
	
	
	@Override
	public void render(){
		List<Vector2D> weaponsTrans = Transformations.WorldTransform(getWeaponVectorBuffer(),
				rocketLauncher.getOwner().pos(),
				rocketLauncher.getOwner().facing(),
				rocketLauncher.getOwner().facing().perp(),
				rocketLauncher.getOwner().scale());

		GameCanvas.redPen();
		GameCanvas.closedShape(weaponsTrans);
	}
}
