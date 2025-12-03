package com.example.GemCrabFighter.data;

import net.runelite.api.ItemID;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class Constants {
    //Locations
    public static final List<Integer> CRAB_REGIONS = Arrays.asList(4911, 4913, 5424);
    public static final List<Integer> FEROX_REGIONS = Arrays.asList(12344, 12600);
    public static final List<Integer> CIVITAS_REGIONS = Arrays.asList(6704, 6705);
    public static final List<Integer> TEL_TEKLAN_REGIONS = Arrays.asList(4912);

    // Gemstone Crab boss NPC ID
    public static final int GEMSTONE_CRAB_ID = 14779;
    public static final int RENU_ID = 13350;

    // Gemstone Crab shell NPC ID
    public static final int GEMSTONE_CRAB_SHELL_ID = 14780;
    public static final int TUNNEL_OBJECT_ID = 57631;

    // HP widget ID constants
    public static final int BOSS_HP_BAR_WIDGET_ID = 19857428;

    // Maximum distance to highlight tunnel (in tiles)
    public static final int MAX_TUNNEL_DISTANCE = 20;

    // Distance from the crab to considered in the area
    public static final int DISTANCE_THRESHOLD = 13;

    // Minutes at the boss required to count as a kill
    // Also, used as a cooldown for mining so its not counted multiple times
    public static final long KILL_THRESHOLD_MILLISECONDS = 5*60*1000; // 5 minutes

    // Location of each crab from its center
    public static final WorldPoint EAST_CRAB = new WorldPoint(1353, 3112, 0);
    public static final WorldPoint SOUTH_CRAB = new WorldPoint(1239,3043, 0);
    public static final WorldPoint NORTH_CRAB = new WorldPoint(1273,3173, 0);

    // Crab Chat messages
    public static final String GEMSTONE_CRAB_DEATH_MESSAGE = "The gemstone crab burrows away, leaving a piece of its shell behind.";
    public static final String GEMSTONE_CRAB_MINE_SUCCESS_MESSAGE = "You swing your pick at the crab shell.";
    public static final String GEMSTONE_CRAB_MINE_FAIL_MESSAGE = "Your understanding of the gemstone crab is not great enough to mine its shell.";
    public static final String GEMSTONE_CRAB_GEM_MINE_MESSAGE = "You mine an uncut ";
    public static final String GEMSTONE_CRAB_TOP16_MESSAGE = "You gained enough understanding of the crab to mine from its remains.";
    public static final String GEMSTONE_CRAB_TOP3_MESSAGE = "The top three crab crushers were ";
}
