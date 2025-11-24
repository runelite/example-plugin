package com.barracudatrial.game.route;

import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

public class JubblyJiveToadPillarWaypoint implements RouteWaypoint
{
	@Getter
	JubblyJiveToadPillar pillar;

	public RouteWaypoint(JubblyJiveToadPillar pillar)
	{
		this.lap = 1;
		this.type = TOAD_PILLAR;
		this.location = pillar.getLocation();
		this.pillar = pillar;
	}

	public RouteWaypoint(int lap, JubblyJiveToadPillar pillar)
	{
		this.lap = lap;
		this.type = TOAD_PILLAR;
		this.location = pillar.getLocation();
		this.pillar = pillar;
	}
}
