package raven.armory.model;

import java.util.List;

import javax.swing.SwingUtilities;
import javax.swing.event.EventListenerList;

import raven.game.interfaces.IDrawable;
import raven.game.interfaces.IRavenBot;
import raven.math.Geometry;
import raven.math.Vector2D;
import raven.messaging.Dispatcher;
import raven.messaging.RavenMessage;
import raven.script.RavenScript;
import raven.ui.GameCanvas;

public class Slug extends RavenProjectile {

	private double slugTimePersist;
	private EventListenerList listeners;

	public Slug(IRavenBot shooter, Vector2D target) {
		super(target,
				shooter.getWorld(),
				shooter.ID(),
				shooter.pos(),
				shooter.facing(),
				RavenScript.getInt("Slug_Damage"),
				RavenScript.getDouble("Slug_Scale"),
				RavenScript.getDouble("Slug_MaxSpeed"),
				RavenScript.getInt("Slug_Mass"),
				RavenScript.getDouble("Slug_MaxForce"));
		slugTimePersist = RavenScript.getDouble("Slug_Persistance");
	}

	private void TestForImpact()
	{
		// a rail gun slug travels VERY fast. It only gets the chance to update once 
		isImpacted = true;

		//first find the closest wall that this ray intersects with. Then we
		//can test against all entities within this range.
		Double distToClosestImpact = Geometry.FindClosestPointOfIntersectionWithWalls(origin,
				position,
				impactPoint,
				world.getMap().getWalls());


		//test to see if the ray between the current position of the slug and 
		//the start position intersects with any bots.
		List<IRavenBot> hits = GetListOfIntersectingBots(origin, position);

		//if no bots hit just return;
		if (hits.isEmpty()) return;

		//give some damage to the hit bots
		for (IRavenBot bot : hits)
		{
			//send a message to the bot to let it know it's been hit, and who the
			//shot came from
			Dispatcher.dispatchMsg(Dispatcher.SEND_MSG_IMMEDIATELY,
					shooterID,
					bot.ID(),
					RavenMessage.MSG_TAKE_THAT_MF,
					damageInflicted);

		}
	}

	//These projectiles are visible if the current time is less than OriginTime + timePersist
	public boolean IsVisibleToPlayer()
	{
		return slugTimePersist > 0;
	}

	public void update(double delta)
	{
		if (!HasImpacted())
		{
			//calculate the steering force
			Vector2D DesiredVelocity = vTarget.sub(position);
			DesiredVelocity.normalize();
			DesiredVelocity = DesiredVelocity.mul(maxSpeed());	

			Vector2D sf = DesiredVelocity.sub(velocity);

			//update the position
			Vector2D accel = sf.div(mass);

			velocity = velocity.add(accel);

			//make sure the slug does not exceed maximum velocity
			velocity.truncate(maxSpeed);

			//update the position
			position = position.add(velocity); 

			TestForImpact();
		}
		else if (!IsVisibleToPlayer())
		{
			isDead = true;
		}
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
}
