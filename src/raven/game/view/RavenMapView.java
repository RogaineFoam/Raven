package raven.game.view;

import raven.game.interfaces.IDrawable;
import raven.game.interfaces.IRavenMap;
import raven.game.model.RavenDoor;
import raven.math.Vector2D;
import raven.math.Wall2D;
import raven.ui.GameCanvas;
import raven.ui.RavenUserOptions;

public class RavenMapView implements IDrawable {

	private IRavenMap map;
	
	public RavenMapView(IRavenMap map){
		this.map = map;
		map.addDrawableListener(this);
	}
	
	public void render() {
		//draw basic background
		GameCanvas.whiteBrush();
		int offset = 20;
		GameCanvas.filledRect(0, offset, map.getSizeX(), map.getSizeY()+offset);
		
		// render the navgraph
		if (RavenUserOptions.showGraph) {
			map.getNavGraph().render(RavenUserOptions.showNodeIndices);
		}
		
		// render any doors
		for (RavenDoor door : map.getDoors()) {
			// TODO render doors
		}
		
		// render all triggers
		map.getTriggerSystem().render();
		
		// render all walls
		for (Wall2D wall : map.getWalls()) {
			GameCanvas.thickBlackPen();
			wall.render();
		}
		
		// render spawn points
		for (Vector2D point : map.getSpawnPoints()) {
			GameCanvas.greyBrush();
			GameCanvas.greyPen();
			GameCanvas.filledCircle(point, 7);
		}
		
	}

	@Override
	public void run() {
		render();
	}
}
