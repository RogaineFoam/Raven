/**
 * 
 */
package raven.armory.view;

import java.util.List;

import raven.armory.model.Railgun;
import raven.math.Transformations;
import raven.math.Vector2D;
import raven.ui.GameCanvas;

/**
 * @author chester
 *
 */
public class RailgunView extends RavenWeaponView {

	private Railgun railgun;
	
	public RailgunView(Railgun r){
		railgun = r;
		railgun.addDrawableListener(this);
		
		final Vector2D[] weapon = {new Vector2D(0, -1),
				new Vector2D(10, -1),
				new Vector2D(10 , 1),
				new Vector2D(0, 1)};
		for(Vector2D v : weapon){
			// Dirty scaling hack
			getWeaponVectorBuffer().add(v.mul(1.0/10));
		}
	}
	
	public void run(){
		render();
	}
	
	public void render(){
		List<Vector2D> thisWeaponShape = Transformations.WorldTransform(getWeaponVectorBuffer(),
				railgun.getOwner().pos(),
				railgun.getOwner().facing(),
				railgun.getOwner().facing().perp(),
				railgun.getOwner().scale());
		setWeaponVectorTransBuffer(thisWeaponShape);
		GameCanvas.bluePen();
		GameCanvas.closedShape(thisWeaponShape);
	}
	
}
