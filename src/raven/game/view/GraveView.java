/**
 * 
 */
package raven.game.view;

import java.util.ArrayList;
import java.util.List;

import raven.game.interfaces.IDrawable;
import raven.game.model.Grave;
import raven.math.Transformations;
import raven.math.Vector2D;
import raven.ui.GameCanvas;
import raven.ui.RavenUI;

/**
 * @author chester
 *
 */
public class GraveView implements IDrawable {

	private Grave model;
	private List<Vector2D> vecRIPVB;
	private List<Vector2D> vecRIBVBTrans;
	
	public GraveView(Grave g){
		model = g;
		model.addDrawableListener(this);
		
		vecRIPVB = new ArrayList<Vector2D>();
		vecRIPVB.add(new Vector2D(-4, -5));
		vecRIPVB.add(new Vector2D(-4, 3));
		vecRIPVB.add(new Vector2D(-3, 5));
		vecRIPVB.add(new Vector2D(-1, 6));
		vecRIPVB.add(new Vector2D(1, 6));
		vecRIPVB.add(new Vector2D(3, 5));
		vecRIPVB.add(new Vector2D(4, 3));
		vecRIPVB.add(new Vector2D(4, -5));
		vecRIPVB.add(new Vector2D(-4, -5));
	}
	@Override
	public void render() {
		final Vector2D facing = new Vector2D(-1, 0);
		final Vector2D scale = new Vector2D(1, 1);
		
		GameCanvas.brownPen();
		vecRIBVBTrans = Transformations.WorldTransform(vecRIPVB, model.pos(), facing, facing.perp(), scale);
		GameCanvas.closedShape(vecRIBVBTrans);
	}
	@Override
	public void run() {
		render();
	}
}
