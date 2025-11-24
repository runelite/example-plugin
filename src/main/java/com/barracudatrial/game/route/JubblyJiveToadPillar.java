package com.barracudatrial.game.route;

import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

@Getter
public class JubblyJiveToadPillar
{
    private final WorldPoint location;
    private final int parentObjectId;
    private final int noToadObjectId;
    private final int toadObjectId;
    private final int clickboxParentObjectId;
    private final int clickboxNoopObjectId;
    private final int clickboxObjectId;

    public JubblyJiveToadPillar(
        WorldPoint location,
        int parentObjectId,
        int noToadObjectId,
        int toadObjectId,
        int clickboxParentObjectId,
        int clickboxNoopObjectId,
        int clickboxObjectId
    ) {
        this.location = location;
        this.parentObjectId = parentObjectId;
        this.noToadObjectId = noToadObjectId;
        this.toadObjectId = toadObjectId;
        this.clickboxParentObjectId = clickboxParentObjectId;
        this.clickboxNoopObjectId = clickboxNoopObjectId;
        this.clickboxObjectId = clickboxObjectId;
    }

    public boolean matchesAnyObjectId(int id)
    {
        return id == parentObjectId
                || id == noToadObjectId
                || id == toadObjectId
                || id == clickboxParentObjectId
                || id == clickboxNoopObjectId
                || id == clickboxObjectId;
    }
}
