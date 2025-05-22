package ca.plugins.yamablindfold;

import com.google.inject.Provides;
import java.util.Arrays;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.GroundObject;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GroundObjectSpawned;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.gameval.ObjectID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.GameEventManager;

@Singleton
@PluginDescriptor(
	name = "Yama Blindfold",
	description = "Hides scenery and terrain in the Yama instance",
	tags = {"yama", "blindfold", "hide", "scenery", "terrain", "gpu", "render"},
	conflicts = {"blindfold"}
)
public class YamaBlindfoldPlugin extends Plugin
{
	private static final int REGION_ID = 6045;

	/**
	 * https://chisel.weirdgloop.org/moid/object_id.html#56246-56372
	 */
	private static final Set<Integer> GAME_OBJECT_IDS_WHITELIST = Set.of(
		ObjectID.YAMA_PORTAL,
		ObjectID.YAMA_EXIT,
		ObjectID.YAMA_FIREWALL,
		ObjectID.YAMA_SHADOW_POOL,
		ObjectID.THRONE_DEMONIC01_STONE01,
		ObjectID.FLOORKIT_SUMMONING03_FULL01,
		ObjectID.FLOORKIT_SUMMONING03_FULL02,
		ObjectID.FLOORKIT_SUMMONING03_INACTIVE,
		ObjectID.FLOORKIT_SUMMONING03_FULL01_DEACTIVATE,
		ObjectID.FLOORKIT_SUMMONING03_FULL02_DEACTIVATE
	);
	private static final Set<Integer> GROUND_OBJECT_IDS_WHITELIST = Set.of(
		ObjectID.YAMA_SHADOW_POOL,
		ObjectID.YAMA_STEPPINGSTONE
	);

	@Inject
	private Client client;
	@Inject
	private ClientThread clientThread;
	@Inject
	private GameEventManager gameEventManager;
	@Inject
	private YamaBlindfoldConfig config;
	@Inject
	private YamaBlindfold yamaBlindfold;

	private boolean enabled;

	@Provides
	YamaBlindfoldConfig provideConfig(final ConfigManager configManager)
	{
		return configManager.getConfig(YamaBlindfoldConfig.class);
	}

	@Override
	protected void startUp()
	{
		clientThread.invokeLater(() -> {
			if (client.getGameState() == GameState.LOGGED_IN && inRegion())
			{
				init();
			}
		});
	}

	@Override
	protected void shutDown()
	{
		enabled = false;

		clientThread.invoke(() -> {
			yamaBlindfold.shutDown();

			if (config.hideScenery() && client.getGameState() == GameState.LOGGED_IN)
			{
				client.setGameState(GameState.LOADING);
			}
		});
	}

	private void init()
	{
		enabled = true;
		yamaBlindfold.init();
		gameEventManager.simulateGameEvents(this);
	}

	@Subscribe
	@SuppressWarnings("unused")
	public void onConfigChanged(final ConfigChanged e)
	{
		if (!enabled || !e.getGroup().equals(YamaBlindfoldConfig.CONFIG_GROUP))
		{
			return;
		}

		if (e.getKey().equals(YamaBlindfoldConfig.CONFIG_KEY_HIDE_SCENERY))
		{
			clientThread.invokeLater(() ->
			{
				if (client.getGameState() == GameState.LOGGED_IN)
				{
					client.setGameState(GameState.LOADING);
				}
			});
		}
	}

	@Subscribe
	@SuppressWarnings("unused")
	public void onGameStateChanged(final GameStateChanged event)
	{
		switch (event.getGameState())
		{
			case LOGGED_IN:
				if (inRegion())
				{
					if (enabled)
					{
						yamaBlindfold.populateTiles();
					}
					else
					{
						init();
					}
				}
				else
				{
					if (enabled)
					{
						shutDown();
					}
				}
				break;
			case HOPPING:
			case LOGIN_SCREEN:
				if (enabled)
				{
					shutDown();
				}
				break;
			default:
				break;
		}
	}

	@Subscribe
	@SuppressWarnings("unused")
	public void onGameObjectSpawned(final GameObjectSpawned event)
	{
		if (!enabled || !config.hideScenery())
		{
			return;
		}

		final GameObject gameObject = event.getGameObject();
		final int id = gameObject.getId();

		if (GAME_OBJECT_IDS_WHITELIST.contains(id))
		{
			return;
		}

		client.getTopLevelWorldView().getScene().removeGameObject(gameObject);
	}

	@Subscribe
	@SuppressWarnings("unused")
	public void onGroundObjectSpawned(final GroundObjectSpawned event)
	{
		if (!enabled || !config.hideScenery())
		{
			return;
		}

		final GroundObject groundObject = event.getGroundObject();
		final int id = groundObject.getId();

		if (GROUND_OBJECT_IDS_WHITELIST.contains(id))
		{
			return;
		}

		event.getTile().setGroundObject(null);
	}

	@Subscribe
	@SuppressWarnings("unused")
	public void onScriptPreFired(final ScriptPreFired event)
	{
		if (!enabled || !config.hideFadeTransition())
		{
			return;
		}

		final int id = event.getScriptId();

		if (id == 948)
		{
			event.getScriptEvent().getArguments()[4] = 255;
			event.getScriptEvent().getArguments()[5] = 0;
		}
		else if (id == 1514)
		{
			event.getScriptEvent().getArguments()[3] = 25;
		}
	}

	private boolean inRegion()
	{
		final var wv = client.getTopLevelWorldView();
		return wv.isInstance() && Arrays.stream(wv.getMapRegions()).anyMatch(i -> i == REGION_ID);
	}
}
