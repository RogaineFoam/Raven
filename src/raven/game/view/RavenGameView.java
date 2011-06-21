/**
 * 
 */
package raven.game.view;

import java.util.List;

import raven.armory.model.RavenProjectile;
import raven.game.interfaces.IDrawable;
import raven.game.interfaces.IRavenBot;
import raven.game.interfaces.IRavenGame;
import raven.game.model.Grave;
import raven.math.Vector2D;
import raven.ui.GameCanvas;
import raven.ui.RavenUserOptions;
import raven.utils.Log;

/**
 * @author chester
 *
 */
public class RavenGameView implements IDrawable {

	private IRavenGame game;
	
	public RavenGameView(IRavenGame g){
		game = g;
	}
	
	/* (non-Javadoc)
	 * @see raven.game.IRavenGame#render()
	 */
	@Override
	public void render() {
		Log.trace("game", "Rendering game");
		// TODO render the map
		
		for(Grave grave : game.getGraves()){
			// This used to be grave.render();
		}
		
		
		// render all the bots unless the user has selected the option to only
		// render those bots that are in the fov of the selected bot
		if (game.selectedBot() != null && RavenUserOptions.onlyShowBotsInTargetsFOV) {
			Log.trace("game", "Rendering FOV bots only");
			List<IRavenBot> visibleBots = game.getAllBotsInFOV(game.selectedBot());

			//bugfix - render only visible bots, but render selected too!
			for (IRavenBot bot : visibleBots) {
				// TODO render bots	
			}
			// render Selected Bot
			
		} else {
			Log.trace("game", "Rendering all bots");
			// render all the bots
			for (IRavenBot bot : game.getBots()) {
				//TODO render bots
			}			
		}
		
		// render any projectiles
		Log.trace("game", "Rendering projectiles");
		for (RavenProjectile projectile : game.getProjectiles()) {
			// TODO render projectileviews
		}
		
		// render a red circle around the selected bot (blue if possessed)
		Log.trace("game", "Rendering selected bot");
		if (game.selectedBot() != null) {
			if (game.selectedBot().isPossessed()) {
				GameCanvas.bluePen();
			} else {
				GameCanvas.redPen();
			}
			GameCanvas.hollowBrush();
			GameCanvas.circle(game.selectedBot().pos(), game.selectedBot().getBRadius() + 1);

			if (RavenUserOptions.showOpponentsSensedBySelectedBot) {
				game.selectedBot().getSensoryMem().renderBoxesAroundRecentlySensed();
			}
			
			// render a square around the bot's target
			Log.trace("game", "Rendering selected bot target");
			if (RavenUserOptions.showTargetOfSelectedBot && game.selectedBot().getTargetBot() != null) {
				GameCanvas.thickRedPen();
				
				Vector2D p = game.selectedBot().getTargetBot().pos();
				double b = game.selectedBot().getTargetBot().getBRadius();
				
				GameCanvas.line(p.x - b, p.y - b, p.x + b, p.y - b);
				GameCanvas.line(p.x + b, p.y - b, p.x + b, p.y + b);
				GameCanvas.line(p.x + b, p.y + b, p.x - b, p.y + b);
				GameCanvas.line(p.x - b, p.y + b, p.x - b, p.y - b);
			}
			
			// render the path of the bot
			if (RavenUserOptions.showPathOfSelectedBot) {
				Log.trace("game", "Rendering selected bot path");
				game.selectedBot().getBrain().render();
			}
			
			// display the bot's goal stack
			if (RavenUserOptions.showGoalsOfSelectedBot) {
				Log.trace("game", "Rendering selected bot goals");
				Vector2D p = new Vector2D(game.selectedBot().pos().x - 50, game.selectedBot().pos().y);
				game.selectedBot().getBrain().renderAtPos(p);
			}
		}
	}

	@Override
	public void run() {
		render();
	}

}
