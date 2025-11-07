package com.example.GemCrabFighter.data;

import net.runelite.api.ItemID;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class Constants {
    //Locations
    public static final List<Integer> HOME_REGIONS = Arrays.asList(7513, 7514, 7769, 7770);
    public static final List<Integer> LITH_REGIONS = Arrays.asList(14242, 6223);
    public static final WorldArea EDGEVILLE_TELE = new WorldArea(3083, 3487, 17, 14, 0);
    public static final WorldArea LITH_TELE = new WorldArea(3540, 10443, 21, 26, 0);
    public static final WorldArea LITH_TELE_DOWNSTAIRS = new WorldArea(3542, 10467, 16, 19, 0);
    public static final WorldArea RUNE_DRAGONS_DOOR = new WorldArea(1562, 5058, 12, 21, 0);
    public static final WorldArea RUNE_DRAGONS_DOOR_ENTER = new WorldArea(1570, 5072, 4, 6, 0);
    public static final WorldPoint RUNE_DRAGONS_DOOR_TILE = new WorldPoint(1572, 5074, 0);
    public static final WorldArea RUNE_DRAGONS = new WorldArea(1574, 5061, 25, 28, 0);

    // Gemstone Crab boss NPC ID
    public static final int GEMSTONE_CRAB_ID = 14779;

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

    // Potions
    public static final Set<Integer> EXTENDED_ANTIFIRE_POTS = Set.of(
            ItemID.EXTENDED_ANTIFIRE1,
            ItemID.EXTENDED_ANTIFIRE2,
            ItemID.EXTENDED_ANTIFIRE3,
            ItemID.EXTENDED_ANTIFIRE4
    );
    public static final Set<Integer> SUPER_EXTENDED_ANTIFIRE_POTS = Set.of(
            ItemID.EXTENDED_SUPER_ANTIFIRE1,
            ItemID.EXTENDED_SUPER_ANTIFIRE2,
            ItemID.EXTENDED_SUPER_ANTIFIRE3,
            ItemID.EXTENDED_SUPER_ANTIFIRE4
    );
    public static final Set<Integer> PRAYER_POTS = Set.of(
            ItemID.PRAYER_POTION1,
            ItemID.PRAYER_POTION2,
            ItemID.PRAYER_POTION3,
            ItemID.PRAYER_POTION4
    );
    public static final Set<Integer> SUPER_COMBAT_POTS = Set.of(
            ItemID.SUPER_COMBAT_POTION1,
            ItemID.SUPER_COMBAT_POTION2,
            ItemID.SUPER_COMBAT_POTION3,
            ItemID.SUPER_COMBAT_POTION4
    );
    public static final Set<Integer> DIVINE_SUPER_COMBAT_POTS = Set.of(
            ItemID.DIVINE_SUPER_COMBAT_POTION1,
            ItemID.DIVINE_SUPER_COMBAT_POTION2,
            ItemID.DIVINE_SUPER_COMBAT_POTION3,
            ItemID.DIVINE_SUPER_COMBAT_POTION4
    );
    public static final Set<Integer> DIGSITE_PENDANTS = Set.of(
            ItemID.DIGSITE_PENDANT_1,
            ItemID.DIGSITE_PENDANT_2,
            ItemID.DIGSITE_PENDANT_3,
            ItemID.DIGSITE_PENDANT_4,
            ItemID.DIGSITE_PENDANT_5
    );

}
