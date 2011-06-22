/**
 * 
 */
package raven.armory.model;

import javax.swing.SwingUtilities;
import javax.swing.event.EventListenerList;

import raven.game.interfaces.IDrawable;
import raven.game.interfaces.IRavenBot;
import raven.game.interfaces.IRenderInvoker;
import raven.goals.fuzzy.FuzzyModule;
import raven.goals.fuzzy.FuzzyVariable;
import raven.goals.fuzzy.FzSet;
import raven.goals.fuzzy.FzVery;
import raven.math.Vector2D;
import raven.script.RavenScript;
import raven.systems.RavenObject;

/**
 * @author chester
 *
 */
public class Blaster extends RavenWeapon implements IRenderInvoker{

	private static int blasterDefaultRounds = RavenScript.getInt("Blaster_DefaultRounds");
	private static final int blasterMaxRounds = RavenScript.getInt("Blaster_MaxRoundsCarried");
	private static double blasterFiringFreq = RavenScript.getDouble("Blaster_FiringFreq");
	private static double blasterIdealRange = RavenScript.getDouble("Blaster_IdealRange");
	private static double blasterMaxSpeed = RavenScript.getDouble("Blaster_MaxSpeed");
	
	private EventListenerList listeners;

	public Blaster(IRavenBot owner)
	{
		super(RavenObject.BLASTER, blasterDefaultRounds, blasterMaxRounds, blasterFiringFreq, blasterIdealRange, blasterMaxSpeed, owner);

		InitializeFuzzyModule();
		listeners = new EventListenerList();
	}

	@Override
	public boolean ShootAt(Vector2D position){
		if(timeUntilAvailable <= 0){
			getOwner().getWorld().addBolt(getOwner(), position);
			//time next available is 1second/times per second!
			UpdateTimeWeaponIsNextAvailable();
			getOwner().getWorld().getMap().addSoundTrigger(getOwner(), RavenScript.getDouble("Blaster_SoundRange"));
			return true;
		}
		
		else return false;
	}

	@Override
	public double GetDesireability(double distanceToTarget){
		getFuzzyModule().Fuzzify("DistToTarget", distanceToTarget);
		double desire = getFuzzyModule().Defuzzify("Desireability", FuzzyModule.DefuzzifyMethod.max_av);
		setLastDesireability(desire);

		return desire;

	}

	@Override
	protected void InitializeFuzzyModule(){
		FuzzyVariable DistToTarget = getFuzzyModule().CreateFLV("DistToTarget");
		FzSet Target_Close = DistToTarget.AddLeftShoulderSet("Target_Close", 0, 25, 150);
		FzSet Target_Medium = DistToTarget.AddTriangularSet("Target_Medium", 25, 150, 300);
		FzSet Target_Far = DistToTarget.AddRightShoulderSet("Target_Far", 150, 300, 1000);

		FuzzyVariable Desirability = getFuzzyModule().CreateFLV("Desirability"); 
		FzSet Desirable = Desirability.AddTriangularSet("Desirable", 25, 50, 75);
		FzSet Undesirable = Desirability.AddLeftShoulderSet("Undesirable", 0, 25, 50);

		getFuzzyModule().AddRule(Target_Close, Desirable);
		getFuzzyModule().AddRule(Target_Medium, new FzVery(Undesirable));
		getFuzzyModule().AddRule(Target_Far, new FzVery(Undesirable));
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
