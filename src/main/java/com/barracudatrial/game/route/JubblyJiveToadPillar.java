package com.barracudatrial.game.route;

import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

public record JubblyJiveToadPillar(
    WorldPoint location,
	int parentObjectId,
    int noToadObjectId,
    int toadObjectId,
    int clockboxParentObjectId,
    int clickboxNoopObjectId,
    int clickboxObjectId
) {}

