package raven.game.view;

import raven.game.interfaces.IDrawable;
import raven.game.model.RavenDoor;
import raven.game.model.RavenDoor.Status;
import raven.math.Vector2D;
import raven.ui.GameCanvas;

public class RavenDoorView implements IDrawable {

	private RavenDoor door;
	private double currentSize;
	private Vector2D vectorToP2Norm;
	
	public RavenDoorView(RavenDoor d){
		door = d;
		d.addDrawableListener(this);
		
		vectorToP2Norm = door.to().sub(door.from());
		vectorToP2Norm.normalize();
	}
	
	@Override
	public void render() {
		if(door.status() == Status.OPEN) return;
		
		GameCanvas.brownPen();
		//TODO: change length of drawn line pased on state(closing/opening/open/closed
		if(door.status() == Status.CLOSED) GameCanvas.line(door.from(), door.to());
	}
	
	
	public void changePosition(Vector2D newP1, Vector2D newP2) {
		// TODO
	}
	
	public void beginClose(){
		// TODO ask controller to change door state
		
		// reduce the current size
		currentSize += 1.0/60;
		
		currentSize = Math.min(Math.max(0, currentSize), door.getSize());
		
		changePosition(door.from(), door.from().add(vectorToP2Norm).mul(currentSize));
	}
	
	public void beginOpen(){
		// TODO ask controller to change door state
		
		
		// reduce the current size
		currentSize -= 1.0/60;
		
		currentSize = Math.min(Math.max(0, currentSize), door.getSize());
		
		changePosition(door.from(), door.from().add(vectorToP2Norm).mul(currentSize));
	}

	@Override
	public void run() {
		render();
	}

}
