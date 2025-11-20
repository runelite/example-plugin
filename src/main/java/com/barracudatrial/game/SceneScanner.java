package com.barracudatrial.game;

import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Utility for scanning game scenes for tiles and objects
 */
public class SceneScanner
{
	private final Client client;

	public SceneScanner(Client client)
	{
		this.client = client;
	}

	/**
	 * Scans all tiles (regular + extended) and returns GameObjects matching the filter
	 */
	public List<GameObject> scanForGameObjects(Predicate<GameObject> filter)
	{
		List<GameObject> results = new ArrayList<>();

		WorldView worldView = client.getTopLevelWorldView();
		if (worldView == null)
		{
			return results;
		}

		Scene scene = worldView.getScene();
		if (scene == null)
		{
			return results;
		}

		// Scan regular tiles
		scanTileArray(scene.getTiles(), filter, results);

		// Scan extended tiles
		scanTileArray(scene.getExtendedTiles(), filter, results);

		return results;
	}

	/**
	 * Scans all tiles (regular + extended) and returns NPCs matching the filter
	 */
	public List<NPC> scanForNPCs(Predicate<NPC> filter)
	{
		List<NPC> results = new ArrayList<>();

		WorldView worldView = client.getTopLevelWorldView();
		if (worldView == null)
		{
			return results;
		}

		for (NPC npc : worldView.npcs())
		{
			if (npc != null && filter.test(npc))
			{
				results.add(npc);
			}
		}

		return results;
	}

	/**
	 * Helper to scan a tile array for GameObjects
	 */
	private void scanTileArray(Tile[][][] tiles, Predicate<GameObject> filter, List<GameObject> results)
	{
		if (tiles == null)
		{
			return;
		}

		for (int plane = 0; plane < tiles.length; plane++)
		{
			if (tiles[plane] == null)
			{
				continue;
			}

			for (int x = 0; x < tiles[plane].length; x++)
			{
				if (tiles[plane][x] == null)
				{
					continue;
				}

				for (int y = 0; y < tiles[plane][x].length; y++)
				{
					Tile tile = tiles[plane][x][y];
					if (tile == null)
					{
						continue;
					}

					for (GameObject gameObject : tile.getGameObjects())
					{
						if (gameObject != null && filter.test(gameObject))
						{
							results.add(gameObject);
						}
					}
				}
			}
		}
	}

	/**
	 * Finds a GameObject at a specific WorldPoint
	 */
	public GameObject findGameObjectAt(WorldPoint worldPoint)
	{
		WorldView worldView = client.getTopLevelWorldView();
		if (worldView == null)
		{
			return null;
		}

		Scene scene = worldView.getScene();
		if (scene == null)
		{
			return null;
		}

		net.runelite.api.coords.LocalPoint localPoint = net.runelite.api.coords.LocalPoint.fromWorld(worldView, worldPoint);
		if (localPoint == null)
		{
			return null;
		}

		Tile[][][] tiles = scene.getTiles();
		if (tiles == null || worldPoint.getPlane() >= tiles.length)
		{
			return null;
		}

		Tile tile = tiles[worldPoint.getPlane()][localPoint.getSceneX()][localPoint.getSceneY()];
		if (tile == null)
		{
			return null;
		}

		// Return first non-null GameObject
		for (GameObject gameObject : tile.getGameObjects())
		{
			if (gameObject != null)
			{
				return gameObject;
			}
		}

		return null;
	}
}
