/**
 * 
 */
package raven.armory.view;

import java.util.ArrayList;
import java.util.List;

import raven.game.interfaces.IDrawable;
import raven.math.Vector2D;

/**
 * @author chester
 *
 */
public class RavenWeaponView implements IDrawable {

	private List<Vector2D> WeaponVB, WeaponVBTrans;
	
	public RavenWeaponView(){
		WeaponVB = new ArrayList<Vector2D>();
		WeaponVBTrans = new ArrayList<Vector2D>();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see raven.game.interfaces.IDrawable#render()
	 */
	@Override
	public void render() {
		// TODO Auto-generated method stub

	}

	public List<Vector2D> getWeaponVectorBuffer() { return WeaponVB; }
	
	public List<Vector2D> getWeaponVectorTransBuffer() { return WeaponVBTrans; }
	
	public void setWeaponVectorTransBuffer(List<Vector2D> tempBuffer) { WeaponVBTrans = tempBuffer; }
	
}
