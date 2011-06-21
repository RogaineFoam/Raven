package raven.armory.model;

import javax.swing.SwingUtilities;
import javax.swing.event.EventListenerList;

import raven.game.interfaces.IDrawable;
import raven.game.interfaces.IRavenBot;
import raven.math.Geometry;
import raven.math.Vector2D;
import raven.messaging.Dispatcher;
import raven.messaging.RavenMessage;
import raven.script.RavenScript;

public class Pellet extends RavenProjectile {

	private double pelletTimePersist;
	private EventListenerList listeners;


	public Pellet(IRavenBot shooter, Vector2D target) {
		super(target,
				shooter.getWorld(),
				shooter.ID(),
				shooter.pos(),
				shooter.facing(),
				RavenScript.getInt("Pellet_Damage"),
				RavenScript.getDouble("Pellet_Scale"),
				RavenScript.getDouble("Pellet_MaxSpeed"),
				RavenScript.getInt("Pellet_Mass"),
				RavenScript.getDouble("Pellet_MaxForce"));
		pelletTimePersist = RavenScript.getDouble("Pellet_Persistance");
		this.setPos(shooter.pos());
		listeners = new EventListenerList();
	}


	private boolean isVisibleToPlayer(double delta)
	{
		if(pelletTimePersist > 0)
		{
			pelletTimePersist -= delta; 
		}

		return pelletTimePersist > 0;
	}

	public void update(double delta)
	{
		if (!HasImpacted())
		{
			//calculate the steering force
			Vector2D DesiredVelocity = this.velocity().mul(this.maxSpeed());

			Vector2D sf = DesiredVelocity.sub(this.velocity());

			//update the position
			Vector2D accel = sf.div(this.mass());

			this.setVelocity(this.velocity().add(accel));

			//make sure vehicle does not exceed maximum velocity
			this.velocity().truncate(this.maxSpeed());

			//update the position
			this.setPos(this.pos().add(this.velocity()));

			TestForImpact();
		}
		else if (!isVisibleToPlayer(delta))
		{
			isDead = true;
		}
	}

	private void TestForImpact()
	{
		//a shot gun shell is an instantaneous projectile so it only gets the chance
		//to update once 
		isImpacted = true;

		//first find the closest wall that this ray intersects with. Then we
		//can test against all entities within this range.
		Double distToClosestImpact = Geometry.FindClosestPointOfIntersectionWithWalls(origin,
				position,
				impactPoint,
				world.getMap().getWalls());

		//test to see if the ray between the current position of the shell and 
		//the start position intersects with any bots.
		IRavenBot hit = GetClosestIntersectingBot(pos().sub(velocity), pos());

		//if no bots hit just return;
		if (hit == null)
			return;

		//determine the impact point with the bot's bounding circle so that the
		//shell can be rendered properly
		// note: this will not be null since we already know it hit it!
		impactPoint = Geometry.GetLineSegmentCircleClosestIntersectionPoint(origin,
				impactPoint,
				hit.pos(),
				hit.getBRadius());

		//send a message to the bot to let it know it's been hit, and who the
		//shot came from
		Dispatcher.dispatchMsg(Dispatcher.SEND_MSG_IMMEDIATELY,
				shooterID,
				hit.ID(),
				RavenMessage.MSG_TAKE_THAT_MF,
				damageInflicted);
	}
	
	@Override
	public void addDrawableListener(IDrawable drawable) {
		listeners.add(IDrawable.class, drawable);
	}

	@Override
	public void removeDrawableListeners() {
		IDrawable[] draws = listeners.getListeners(IDrawable.class);
		for(IDrawable d : draws){
			listeners.remove(IDrawable.class, d);
		}
	}

	@Override
	public void notifyDrawables() {
		if(!shouldDraw()) return;
		for(IDrawable drawable : listeners.getListeners(IDrawable.class)){
			SwingUtilities.invokeLater(drawable);
		}
	}

	@Override
	public boolean shouldDraw() {
		// maps update so often, may as well.
		return true;
	}


	public double getPelletTimePersist() {
		return pelletTimePersist;
	}
}
