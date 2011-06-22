package raven.armory.view;

import java.util.List;

import raven.armory.model.Shotgun;
import raven.game.interfaces.IDrawable;
import raven.math.Transformations;
import raven.math.Vector2D;
import raven.ui.GameCanvas;

public class ShotgunView extends RavenWeaponView implements IDrawable {

	private Shotgun shotgun;
	
	public ShotgunView(Shotgun s){
		shotgun = s;
		shotgun.addDrawableListener(this);
		
		Vector2D[] weapon = {
				new Vector2D(0, 0),
				new Vector2D(0, -2),
				new Vector2D(10, -2),
				new Vector2D(10, 0),
				new Vector2D(0, 0),
				new Vector2D(0, 2),
				new Vector2D(10, 2),
				new Vector2D(10, 0)};

		for(Vector2D v : weapon){
			// Dirty scaling hack
			getWeaponVectorBuffer().add(v.mul(1.0/10));
		}
	}
	
	@Override
	public void run() {
		render();
	}

	@Override
	public void render() {
		List<Vector2D> weaponTrans = Transformations.WorldTransform(getWeaponVectorBuffer(),
				shotgun.getOwner().pos(),
				shotgun.getOwner().facing(),
				shotgun.getOwner().facing().perp(),
				shotgun.getOwner().scale());

		GameCanvas.brownPen();
		GameCanvas.polyLine(weaponTrans);
	}

}
