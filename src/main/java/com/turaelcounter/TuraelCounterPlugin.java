package com.turaelcounter;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.ui.overlay.infobox.Counter;
import net.runelite.api.VarPlayer;
import net.runelite.api.Varbits;
import net.runelite.api.Client;
import net.runelite.client.game.ItemManager;
import java.util.HashSet;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.ChatMessageType;
import net.runelite.client.util.ImageUtil;

import java.util.ArrayList;
import java.util.HashSet;

@Slf4j
@PluginDescriptor(
	name = "Turael Counter",
	description = "Counts streak resets"
)
public class TuraelCounterPlugin extends Plugin
{
	private Counter counter;
	@Inject
	private Client client;

	@Inject
	private TuraelCounterConfig config;

	@Inject
	private InfoBoxManager infoBoxManager;

	@Inject
	private OverlayManager overlayManager;

	private TuraelStreakInfobox infobox;

	@Inject
	private ItemManager itemManager;

	@Inject
	private SkillIconManager skillIconManager;

	private int streakReset = 0;

	private int streakVarbit = Varbits.SLAYER_TASK_STREAK;

	private int previousStreakValue = -1;

	private int overlayVisible;

	private HashSet<Integer> desiredTaskSet = new HashSet<Integer>();

	private boolean isStreakReset = false;

	@Override
	protected void startUp()
	{
		infoBoxManager.addInfoBox(new TuraelStreakInfobox(itemManager.getImage(25912), this));
		loadConfiguredTasks();
		removeUndesiredTasks();
		streakReset = config.streakReset();
	}

	@Override
	protected void shutDown()
	{
		infoBoxManager.removeIf(TuraelStreakInfobox.class::isInstance);
		config.streakReset(streakReset);
	}

	@Provides
	TuraelCounterConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(TuraelCounterConfig.class);
	}

	private void updateStreakResetCount()
	{
		streakReset++;
	}

	private void resetStreakCounter()
	{
		streakReset = 0;
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged varbitChanged)
	{
		int varbitId = varbitChanged.getVarbitId();
//		value for the task
		int slayerTaskCreature = client.getVarpValue(VarPlayer.SLAYER_TASK_CREATURE);

//		this gives name of the task
		String taskName;
		taskName = client.getEnum(EnumID.SLAYER_TASK_CREATURE)
				.getStringValue(slayerTaskCreature);


		if (varbitId == streakVarbit)
		{
			int currentStreakValue = client.getVarbitValue(Varbits.SLAYER_TASK_STREAK);

			if (previousStreakValue != 0 && currentStreakValue < previousStreakValue) {
				updateStreakResetCount();
			}
			previousStreakValue = currentStreakValue;
		}

		if (desiredTaskSet.contains(slayerTaskCreature) && !isStreakReset)
		{
			log.info("Desired task achieved, resetting streak count");
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", taskName + " task obtained in " + streakReset + " tasks!", null);
			resetStreakCounter();
			isStreakReset = true;
		}

		if (!desiredTaskSet.contains(slayerTaskCreature))
		{
			isStreakReset = false;
		}

	}

	public int getStreakReset()
	{
		return streakReset;
	}

	public void loadConfiguredTasks()
	{
		if (config.isAbyssalDemonDesired()) {
			desiredTaskSet.add(42); // Abyssal Demon task ID
		}

		if (config.isSmokeDevilDesired()) {
			desiredTaskSet.add(95); // Smoke Devils task ID
		}

		if (config.isTzKalZukDesired()) {
			desiredTaskSet.add(105); // TzKal-Zuk task ID
		}

		if (config.isHellhoundDesired()) {
			desiredTaskSet.add(31); // Hellhounds task ID
		}

		if (config.isGargoyleDesired()) {
			desiredTaskSet.add(46); // Gargoyles task ID
		}

		if (config.isLizardmenDesired()) {
			desiredTaskSet.add(90); // Lizardmen task ID
		}

		if (config.isRevenantDesired()) {
			desiredTaskSet.add(107); // Revenants task ID
		}

		if (config.isHydraDesired()) {
			desiredTaskSet.add(113); // Hydras task ID
		}

	}

	public void removeUndesiredTasks()
	{
		if (!config.isAbyssalDemonDesired())
		{
			desiredTaskSet.remove(42);
		}

		if (!config.isSmokeDevilDesired())
		{
			desiredTaskSet.remove((95));
		}

		if (!config.isTzKalZukDesired()) {
			desiredTaskSet.remove(105); // TzKal-Zuk task ID
		}

		if (!config.isHellhoundDesired()) {
			desiredTaskSet.remove(31); // Hellhounds task ID
		}

		if (!config.isGargoyleDesired()) {
			desiredTaskSet.remove(46); // Gargoyles task ID
		}

		if (!config.isLizardmenDesired()) {
			desiredTaskSet.remove(90); // Lizardmen task ID
		}

		if (!config.isRevenantDesired()) {
			desiredTaskSet.remove(107); // Revenants task ID
		}

		if (!config.isHydraDesired()) {
			desiredTaskSet.remove(113); // Hydras task ID
		}
	}

	@Subscribe
	public void onConfigChanged (ConfigChanged event)
	{
		loadConfiguredTasks();
		removeUndesiredTasks();
	}


}

//Creature ID: 105, Name: TzKal-Zuk

//		slayer id info

//Turael tasks
//Creature ID: 1, Name: Monkeys
//Creature ID: 2, Name: Goblins
//Creature ID: 3, Name: Rats
//Creature ID: 4, Name: Spiders
//Creature ID: 5, Name: Birds
//Creature ID: 6, Name: Cows
//Creature ID: 7, Name: Scorpions
//Creature ID: 8, Name: Bats
//Creature ID: 9, Name: Wolves
//Creature ID: 10, Name: Zombies
//Creature ID: 11, Name: Skeletons
//Creature ID: 12, Name: Ghosts
//Creature ID: 13, Name: Bears
//Creature ID: 39, Name: Crawling Hands
//Creature ID: 37, Name: Cave Crawlers
//Creature ID: 38, Name: Banshees
//Creature ID: 53, Name: Kalphite
//Creature ID: 62, Name: Cave Slimes
//Creature ID: 63, Name: Cave Bugs
//Creature ID: 37, Name: Cave Crawlers
//Creature ID: 76, Name: Minotaurs
//Creature ID: 121, Name: Sourhogs
//Creature ID: 57, Name: Dwarves
//Creature ID: 68, Name: Lizards

//Creature ID: 14, Name: Hill Giants
//Creature ID: 15, Name: Ice Giants
//Creature ID: 16, Name: Fire Giants
//Creature ID: 17, Name: Moss Giants
//Creature ID: 18, Name: Trolls
//Creature ID: 19, Name: Ice Warriors
//Creature ID: 20, Name: Ogres
//Creature ID: 21, Name: Hobgoblins
//Creature ID: 22, Name: Dogs
//Creature ID: 23, Name: Ghouls
//Creature ID: 24, Name: Green Dragons
//Creature ID: 25, Name: Blue Dragons
//Creature ID: 26, Name: Red Dragons
//Creature ID: 27, Name: Black Dragons
//Creature ID: 28, Name: Lesser Demons
//Creature ID: 29, Name: Greater Demons
//Creature ID: 30, Name: Black Demons
//Creature ID: 31, Name: Hellhounds
//Creature ID: 32, Name: Shadow Warriors
//Creature ID: 33, Name: Werewolves
//Creature ID: 34, Name: Vampyres
//Creature ID: 35, Name: Dagannoth
//Creature ID: 36, Name: Turoth

//Creature ID: 40, Name: Infernal Mages
//Creature ID: 41, Name: Aberrant Spectres
//Creature ID: 42, Name: Abyssal Demons
//Creature ID: 43, Name: Basilisks
//Creature ID: 44, Name: Cockatrice
//Creature ID: 45, Name: Kurask
//Creature ID: 46, Name: Gargoyles
//Creature ID: 47, Name: Pyrefiends
//Creature ID: 48, Name: Bloodveld
//Creature ID: 49, Name: Dust Devils
//Creature ID: 50, Name: Jellies
//Creature ID: 51, Name: Rockslugs
//Creature ID: 52, Name: Nechryael
//Creature ID: 54, Name: Earth Warriors
//Creature ID: 55, Name: Otherworldly Beings
//Creature ID: 56, Name: Elves

//Creature ID: 58, Name: Bronze Dragons
//Creature ID: 59, Name: Iron Dragons
//Creature ID: 60, Name: Steel Dragons
//Creature ID: 61, Name: Wall Beasts

//Creature ID: 64, Name: Shades
//Creature ID: 65, Name: Crocodiles
//Creature ID: 66, Name: Dark Beasts
//Creature ID: 67, Name: Mogres
//Creature ID: 69, Name: Fever Spiders
//Creature ID: 70, Name: Harpie Bug Swarms
//Creature ID: 71, Name: Sea Snakes
//Creature ID: 72, Name: Skeletal Wyverns
//Creature ID: 73, Name: Killerwatts
//Creature ID: 74, Name: Mutated Zygomites
//Creature ID: 75, Name: Icefiends
//Creature ID: 77, Name: Fleshcrawlers
//Creature ID: 78, Name: Catablepon
//Creature ID: 79, Name: Ankou
//Creature ID: 80, Name: Cave Horrors
//Creature ID: 81, Name: Jungle Horrors
//Creature ID: 82, Name: Goraks
//Creature ID: 83, Name: Suqahs
//Creature ID: 84, Name: Brine Rats
//Creature ID: 85, Name: Minions of Scabaras
//Creature ID: 86, Name: Terror Dogs
//Creature ID: 87, Name: Molanisks
//Creature ID: 88, Name: Waterfiends
//Creature ID: 89, Name: Spiritual Creatures
//Creature ID: 90, Name: Lizardmen
//Creature ID: 91, Name: Magic Axes
//Creature ID: 92, Name: Cave Kraken
//Creature ID: 93, Name: Mithril Dragons
//Creature ID: 94, Name: Aviansies
//Creature ID: 95, Name: Smoke Devils
//Creature ID: 96, Name: TzHaar
//Creature ID: 97, Name: TzTok-Jad
//Creature ID: 98, Name: Bosses
//Creature ID: 99, Name: Mammoths
//Creature ID: 100, Name: Rogues
//Creature ID: 101, Name: Ents
//Creature ID: 102, Name: Bandits
//Creature ID: 103, Name: Dark Warriors
//Creature ID: 104, Name: Lava Dragons
//Creature ID: 105, Name: TzKal-Zuk
//Creature ID: 106, Name: Fossil Island Wyverns
//Creature ID: 107, Name: Revenants
//Creature ID: 108, Name: Adamant Dragons
//Creature ID: 109, Name: Rune Dragons
//Creature ID: 110, Name: Chaos Druids
//Creature ID: 111, Name: Wyrms
//Creature ID: 112, Name: Drakes
//Creature ID: 113, Name: Hydras
//Creature ID: 114, Name: Temple Spiders
//Creature ID: 115, Name: Undead Druids
//Creature ID: 116, Name: Sulphur Lizards
//Creature ID: 117, Name: Brutal Black Dragons
//Creature ID: 118, Name: Sand Crabs
//Creature ID: 119, Name: Black Knights
//Creature ID: 120, Name: Pirates

//Creature ID: 122, Name: Warped Creatures

