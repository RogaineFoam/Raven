/**
 * 
 */
package raven.game.view;

import java.util.ArrayList;

import raven.game.interfaces.IDrawable;
import raven.game.interfaces.IRavenBot;
import raven.math.Transformations;
import raven.math.Vector2D;
import raven.script.RavenScript;
import raven.ui.GameCanvas;
import raven.ui.RavenUserOptions;

/**
 * @author chester
 *
 */
public class RavenBotView implements IDrawable {

	private IRavenBot bot;
	
	/** a vertex buffer containing the bot's geometry */
	transient private ArrayList<Vector2D> vecBotVB;

	/** the buffer for the transformed vertices */
	transient private ArrayList<Vector2D> vecBotVBTrans;
	
	private double numSecondsHitPersistant;
	
	public RavenBotView(IRavenBot data){
		bot = data;
		setUpVertexBuffer();
		bot.addDrawableListener(this);
		numSecondsHitPersistant = RavenScript.getDouble("HitFlashTime");
	}
	
	/* (non-Javadoc)
	 * @see raven.game.interfaces.IDrawable#render()
	 */
	@Override
	public void render() {
		// when a bot is hit by a projectile this value is set to a constant
		// user defined value which dictates how long the bot should have a
		// thick red circle drawn around it (to indicate it's been hit) The
		// circle is drawn as long as this value is positive. (see Render)

		if (bot.isDead() || bot.isSpawning()) {
			return;
		}

		GameCanvas.bluePen();

		vecBotVBTrans = new ArrayList<Vector2D>(Transformations.WorldTransform(
				vecBotVB, bot.pos(), bot.facing(), bot.facing().perp(), bot.scale()));

		GameCanvas.closedShape(vecBotVBTrans);

		// draw the head
		GameCanvas.brownPen();
		GameCanvas.brownBrush();
		GameCanvas.circle(bot.pos(), bot.getBRadius() * 0.5);

		// render the bot's weapon
		bot.getWeaponSys().renderCurrentWeapon();
		
		// render a thick red circle if the bot gets hit by a weapon
		if (bot.isHit()) {
			GameCanvas.thickRedPen();
			GameCanvas.hollowBrush();
			GameCanvas.circle(bot.pos(), bot.getBRadius() + 1);

			if (numSecondsHitPersistant <= 0) {
				bot.setHit(false);
			}
		}

		GameCanvas.redPen();

		if (RavenUserOptions.showBotIDs) {
			GameCanvas.textAtPos(bot.pos().x - 10, bot.pos().y - 20,
					Integer.toString(bot.ID()));
		}

		if (RavenUserOptions.showBotHealth) {
			GameCanvas.textAtPos(bot.pos().x - 40, bot.pos().y - 5,
					"H:" + Integer.toString(bot.health()));
		}

		if (RavenUserOptions.showScore) {
			GameCanvas.textAtPos(bot.pos().x - 40, bot.pos().y + 10,
					"Scr:" + Integer.toString(bot.score()));
		}
		if (RavenUserOptions.showFeelersOfSelectedBot) {
			bot.getSteering().renderFeelers();
		}
		
		if(RavenUserOptions.showPathOfSelectedBot) {
			bot.getBrain().render();
		}
	}

	private void setUpVertexBuffer() {
		//setup the vertex buffers and calculate the bounding radius
		vecBotVB = new ArrayList<Vector2D>(4);
		vecBotVB.add(new Vector2D(-3, 8));
		vecBotVB.add(new Vector2D(3, 10));
		vecBotVB.add(new Vector2D(3, -10));
		vecBotVB.add(new Vector2D(-3, -8));

		// Find the RMS vertex distance
		double avgDist = 0;
		for (Vector2D point : vecBotVB)
			if (point.lengthSq() > avgDist)
				avgDist += point.lengthSq();
		avgDist /= vecBotVB.size();
		avgDist = Math.sqrt(avgDist);
		
		// Size all the vertices at RMS distance to bot scale
		for (Vector2D point : vecBotVB) {
			Vector2D temp = point.mul(1/avgDist);
			point.x = temp.x;
			point.y = temp.y;
		}
		
	}

	@Override
	public void run() {
		render();
	}


}
