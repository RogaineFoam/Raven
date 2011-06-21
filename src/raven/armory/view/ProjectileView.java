/**
 * 
 */
package raven.armory.view;

import raven.armory.model.RavenProjectile;
import raven.game.interfaces.IDrawable;

/**
 * @author chester
 *
 */
public class ProjectileView implements IDrawable {

	private RavenProjectile model;
	
	/**
	 * 
	 */
	public ProjectileView(RavenProjectile model) {
		this.model = model;
	}

	/* (non-Javadoc)
	 * @see raven.game.interfaces.IDrawable#render()
	 */
	@Override
	public void render() {
		// TODO Get data from model and render
	}

	@Override
	public void run() {
		render();
	}

}
