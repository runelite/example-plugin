package com.rr.bosses.yama;

import com.google.inject.Provides;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.party.PartyMember;
import net.runelite.client.party.PartyService;
import net.runelite.client.party.WSClient;
import net.runelite.client.party.events.UserJoin;
import net.runelite.client.party.events.UserPart;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;

import javax.inject.Inject;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.List;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
@Slf4j
@PluginDescriptor(
		name = "Yama Utilities",
		description = "This plugin contains various QoL features for the Yama encounter.",
		tags = {"combat", "bosses", "bossing", "pve", "pvm"})
public class YamaUtilitiesPlugin extends Plugin {

	private boolean currentlyInsideYamasDomain;
	private boolean currentlyFightingYama;
	private int remainingTicksOfShadowGlyphProtection;
	private int remainingTicksOfFireGlyphProtection;
	private NPC voiceOfYama;
	private PartyPluginDuoInfo partyPluginDuoInfo;
	private DuoNameAutoFillWidget duoNameAutoFillWidget;

	private final Map<String, Integer> personalDamage = new HashMap<>(); // key = enemy name, value = damage dealt to the enemy by the player
	private final Map<String, Integer> totalDamage = new HashMap<>(); // key = enemy name, value = total damage dealt to the enemy
	public static final int VOICE_OF_YAMA_NPC_ID = 14185;
	public static final int YAMAS_NPC_ID = 14176;
	public static final int JUDGE_OF_YAMA_NPC_ID = 14180;
	public static final int VOID_FLARE_NPC_ID = 14179;
	public static final int YAMAS_DOMAIN_REGION_ID = 6045;
	public static final int OUTSIDE_CHASM_OF_FIRE_REGION_ID = 5689;
	public static final int INSIDE_CHASM_OF_FIRE_REGION_ID = 5789;
	public static final int VAR_CLIENT_INT_CAMERA_LOAD_ID = 384;
	public static final int SHADOW_GLYPH_STEPPED_ON_OBJECT_ID = 56338;
	public static final int FIRE_GLYPH_STEPPED_ON_OBJECT_ID = 56339;
	public static final int GLYPH_PROTECTION_DURATION_IN_TICKS = 11;
	public static final String YAMA = "Yama";
	public static final String JUDGE_OF_YAMA = "Judge of Yama";
	public static final String VOID_FLARE = "Void Flare";
	public static final DecimalFormat DMG_FORMAT = new DecimalFormat("#,##0");
	public static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("##0.0");
	public static final Set<Integer> CHASM_OF_FIRE_REGION_IDS = Set.of(
			OUTSIDE_CHASM_OF_FIRE_REGION_ID, INSIDE_CHASM_OF_FIRE_REGION_ID
	);
	public static final Set<String> ENEMY_NAMES = Set.of(
			YAMA, JUDGE_OF_YAMA, VOID_FLARE
	);
	public static final Set<String> PHASE_TRANSITION_OVERHEAD_TEXTS = Set.of(
			"Begone", "You bore me.", "Enough."
	);

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private YamaUtilitiesConfig config;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private WSClient wsClient;

	@Inject
	private PartyService partyService;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private HighlightPlayerOverlay highlightPlayerOverlay;

	public YamaUtilitiesPlugin()
	{
		this.partyPluginDuoInfo = new PartyPluginDuoInfo(false);
	}

	@Override
	protected void startUp() throws Exception {
		initializeBossDamageMaps();
		this.partyPluginDuoInfo.resetFields();
		checkPlayerCurrentLocation();
		wsClient.registerMessage(PartyPluginDuoInfo.class);
		overlayManager.add(highlightPlayerOverlay);
	}

	@Override
	protected void shutDown() throws Exception {
		wsClient.unregisterMessage(PartyPluginDuoInfo.class);
		overlayManager.remove(highlightPlayerOverlay);
	}

	@Provides
    YamaUtilitiesConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(YamaUtilitiesConfig.class);
	}

	@Subscribe(priority = 1) // run prior to plugins so that the member is joined by the time the plugins see it.
	public void onUserJoin(final UserJoin message)
	{
		sendPartyPluginDuoLocationMessage();
	}

	@Subscribe(priority = 1) // run prior to plugins so that the member is joined by the time the plugins see it.
	public void onUserPart(final UserPart message)
	{
		if (message.getMemberId() == this.partyPluginDuoInfo.getMemberId())
		{
			log.debug("Resetting duo partner info.");
			this.partyPluginDuoInfo.resetFields();
		}
	}

	@Subscribe
	public void onPartyPluginDuoInfo(PartyPluginDuoInfo event)
	{
		clientThread.invoke(() ->
		{
			log.debug("PartyPluginDuoLocation received with memberId = {} and in Yama's Domain = {}",
					event.getMemberId(),
					event.isCurrentlyInsideYamasDomain());
			if (event.getMemberId() == this.partyPluginDuoInfo.getMemberId())
			{
				this.partyPluginDuoInfo = event;
			}
		});
	}

	@Subscribe
	public void onOverheadTextChanged(OverheadTextChanged event)
	{
		if (!this.currentlyInsideYamasDomain)
		{
			return;
		}

		String actorName = event.getActor().getName();
		if (actorName == null)
		{
			return;
		}

		if (actorName.equalsIgnoreCase(YAMA) && PHASE_TRANSITION_OVERHEAD_TEXTS.contains(event.getOverheadText()))
		{
			this.currentlyFightingYama = false;
			log.debug("currentlyFightingYama set to false due to Yama phase change dialog.");
		}
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event)
	{
		if (!this.currentlyInsideYamasDomain)
		{
			return;
		}

		Hitsplat hitsplat = event.getHitsplat();

		if (hitsplat.getAmount() == 0)
		{
			return;
		}

		Actor actor = event.getActor();
		if (!(actor instanceof NPC))
		{
			return;
		}

		NPC npc = (NPC) actor;
		String npcName = Text.removeTags(npc.getName());

		if (npcName == null)
		{
			return;
		}

		if (hitsplat.isMine())
		{
			personalDamage.computeIfPresent(npcName, (k,v) -> v + hitsplat.getAmount());
		}
		totalDamage.computeIfPresent(npcName, (k,v) -> v + hitsplat.getAmount());

	}

	@Subscribe
	public void onGameTick(GameTick gameTick)
	{
		if (this.voiceOfYama != null &&
				partyService.isInParty() &&
				this.partyPluginDuoInfo.isCurrentlyInsideYamasDomain())
		{
			String duoDisplayName = partyService.getMemberById(this.partyPluginDuoInfo.getMemberId()).getDisplayName();
			this.voiceOfYama.setOverheadText(duoDisplayName + " has entered Yama's Domain");
		}

		if (this.remainingTicksOfFireGlyphProtection > 0)
		{
			this.remainingTicksOfFireGlyphProtection--;
		}

		if (this.remainingTicksOfShadowGlyphProtection > 0)
		{
			this.remainingTicksOfShadowGlyphProtection--;
		}
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		if (event.getNpc().getId() == VOICE_OF_YAMA_NPC_ID)
		{
			this.voiceOfYama = event.getNpc();
			log.debug("Voice of Yama spawned.");
		}

		if (!this.currentlyInsideYamasDomain)
		{
			return;
		}

		if (event.getNpc().getId() == YAMAS_NPC_ID)
		{
			log.debug("Yama spawned.");
			initializeBossDamageMaps();
			this.currentlyFightingYama = true;
		}
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		if (event.getNpc().getId() == VOICE_OF_YAMA_NPC_ID)
		{
			this.voiceOfYama = null;
			log.debug("Voice of Yama despawned.");
		}

		if (!this.currentlyInsideYamasDomain)
		{
			return;
		}

		int npcId = event.getNpc().getId();
		if (npcId != YAMAS_NPC_ID && npcId != JUDGE_OF_YAMA_NPC_ID)
		{
			return;
		}

		if (npcId == JUDGE_OF_YAMA_NPC_ID)
		{
			this.currentlyFightingYama = true;
			log.debug("currentlyFightingYama set to true due to Judge despawning");
		}

		if (npcId == YAMAS_NPC_ID)
		{
			this.currentlyFightingYama = false;

			if (this.config.printDamageToChat())
			{
				List<String> messages = new ArrayList<>(Collections.emptyList());
				int damageToYama = personalDamage.get(YAMA);

				String yamaDamageChatMessage = new ChatMessageBuilder()
						.append(ChatColorType.NORMAL)
						.append("Damage dealt to Yama - ")
						.append(Color.RED, DMG_FORMAT.format(damageToYama) + " (" + DECIMAL_FORMAT.format((double) damageToYama / totalDamage.get(YAMA) * 100) + "%)")
						.build();

				messages.add(yamaDamageChatMessage);

				int damageToJudge = personalDamage.get(JUDGE_OF_YAMA);
				String judgeDamageChatMessage = new ChatMessageBuilder()
						.append(ChatColorType.NORMAL)
						.append("Damage dealt to Judge of Yama - ")
						.append(Color.RED, DMG_FORMAT.format(damageToJudge) + " (" + DECIMAL_FORMAT.format((double) damageToJudge / totalDamage.get(JUDGE_OF_YAMA) * 100) + "%)")
						.build();

				messages.add(judgeDamageChatMessage);

				for (String message: messages)
				{
					chatMessageManager.queue(QueuedMessage.builder()
							.type(ChatMessageType.GAMEMESSAGE)
							.runeLiteFormattedMessage(message)
							.build());
				}
			}
		}
	}

	@Subscribe
	public void onVarClientIntChanged(VarClientIntChanged event)
	{
		if (event.getIndex() == VAR_CLIENT_INT_CAMERA_LOAD_ID)
		{
			checkPlayerCurrentLocation();
		}

		if (event.getIndex() != VarClientInt.INPUT_TYPE) {
			return;
		}

		if (client.getVarcIntValue(VarClientInt.INPUT_TYPE) != 2 && client.getVarcIntValue(VarClientInt.INPUT_TYPE) != 8)
		{
			return;
		}
		//Add widget to join duo partner at the Voice of Yama and Friends List if in a Party Plugin together.
		clientThread.invokeLater(() -> {
			String title = client.getWidget(InterfaceID.Chatbox.MES_TEXT).getText();
			Pattern titlePattern = Pattern.compile("(Whose fight would you like to join\\? You must be on their friends list\\.|Enter name of friend to add to list)");
			Matcher titleMatcher = titlePattern.matcher(title);
			if (titleMatcher.find() && partyService.isInParty())
			{
				String duoDisplayName = getDuoPartnerDisplayName();
				log.debug("duoDisplayName: {}", duoDisplayName);
				if (duoDisplayName != null)
				{
					log.debug("Creating duo widget.");
					Widget mesLayerWidget = client.getWidget(InterfaceID.Chatbox.MES_LAYER);
					duoNameAutoFillWidget = new DuoNameAutoFillWidget(mesLayerWidget, client);
					duoNameAutoFillWidget.showWidget(duoDisplayName, config);
				}
			}
		});
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned gameObjectSpawned)
	{
		if (!this.currentlyInsideYamasDomain)
		{
			return;
		}

		if (!this.currentlyFightingYama)
		{
			return;
		}

		int objectId = gameObjectSpawned.getGameObject().getId();

		if (objectId == SHADOW_GLYPH_STEPPED_ON_OBJECT_ID)
		{
			log.debug("Shadow glyph activated.");
			this.remainingTicksOfShadowGlyphProtection = GLYPH_PROTECTION_DURATION_IN_TICKS;
		}
		if (objectId == FIRE_GLYPH_STEPPED_ON_OBJECT_ID)
		{
			log.debug("Fire glyph activated.");
			this.remainingTicksOfFireGlyphProtection = GLYPH_PROTECTION_DURATION_IN_TICKS;
		}
	}

	private String getDuoPartnerDisplayName()
	{
		if (!partyService.isInParty())
		{
			return null;
		}

		if (this.partyPluginDuoInfo.getMemberId() != 0L)
		{
			return partyService.getMemberById(this.partyPluginDuoInfo.getMemberId()).getDisplayName();
		}

		log.debug("party size = {}", partyService.getMembers().size());
		for (PartyMember partyMember: partyService.getMembers())
		{
			if (partyMember.getMemberId() != partyService.getLocalMember().getMemberId())
			{
				return partyService.getMemberById(partyMember.getMemberId()).getDisplayName();
			}
		}

		return null;
	}

	private void initializeBossDamageMaps()
	{
		for (String enemyName: ENEMY_NAMES)
		{
			personalDamage.put(enemyName, 0);
			totalDamage.put(enemyName, 0);
		}
	}

	private void checkPlayerCurrentLocation()
	{
		if (client.getLocalPlayer() == null)
		{
			return;
		}

		int currentRegionId = WorldPoint.fromLocalInstance(client, client.getLocalPlayer().getLocalLocation()).getRegionID();
		boolean updatedCurrentlyInsideYamasDomain = YAMAS_DOMAIN_REGION_ID == currentRegionId;

		if (updatedCurrentlyInsideYamasDomain != this.currentlyInsideYamasDomain)
		{
			this.currentlyInsideYamasDomain = updatedCurrentlyInsideYamasDomain;
			log.debug("currentlyInsideYamasDomain updated to: {}", this.currentlyInsideYamasDomain);

			sendPartyPluginDuoLocationMessage();
		}
	}

	private void sendPartyPluginDuoLocationMessage()
	{
		if (!partyService.isInParty())
		{
			return;
		}

		log.debug("Sending updated location to party plugin. currentlyInsideYamasDomain: {}", this.currentlyInsideYamasDomain);
		PartyPluginDuoInfo duoInfo = PartyPluginDuoInfo.builder()
				.currentlyInsideYamasDomain(this.currentlyInsideYamasDomain)
				.build();
		partyService.send(duoInfo);
	}
}
