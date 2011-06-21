/**
 * 
 */
package raven.armory.view;

import raven.armory.model.Slug;
import raven.game.interfaces.IDrawable;
import raven.ui.GameCanvas;

/**
 * @author chester
 *
 */
public class SlugView implements IDrawable {

	private Slug slug;
	
	public SlugView(Slug s){
		slug = s;
		slug.addDrawableListener(this);
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
	public void render()
	{
		if (slug.IsVisibleToPlayer() && slug.HasImpacted())
		{
			GameCanvas.greenPen();
			GameCanvas.line(slug.getOrigin(), slug.getImpactPoint());
		}
	}

}
