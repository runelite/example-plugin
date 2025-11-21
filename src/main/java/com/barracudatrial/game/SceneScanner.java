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
		List<GameObject> matchingGameObjects = new ArrayList<>();

		WorldView topLevelWorldView = client.getTopLevelWorldView();
		if (topLevelWorldView == null)
		{
			return matchingGameObjects;
		}

		Scene scene = topLevelWorldView.getScene();
		if (scene == null)
		{
			return matchingGameObjects;
		}

		scanTileArrayForGameObjects(scene.getTiles(), filter, matchingGameObjects);
		scanTileArrayForGameObjects(scene.getExtendedTiles(), filter, matchingGameObjects);

		return matchingGameObjects;
	}

	/**
	 * Scans all tiles (regular + extended) and returns NPCs matching the filter
	 */
	public List<NPC> scanForNPCs(Predicate<NPC> filter)
	{
		List<NPC> matchingNPCs = new ArrayList<>();

		WorldView topLevelWorldView = client.getTopLevelWorldView();
		if (topLevelWorldView == null)
		{
			return matchingNPCs;
		}

		for (NPC npc : topLevelWorldView.npcs())
		{
			if (npc != null && filter.test(npc))
			{
				matchingNPCs.add(npc);
			}
		}

		return matchingNPCs;
	}

	private void scanTileArrayForGameObjects(Tile[][][] tileArray, Predicate<GameObject> filter, List<GameObject> matchingGameObjects)
	{
		if (tileArray == null)
		{
			return;
		}

		for (int planeIndex = 0; planeIndex < tileArray.length; planeIndex++)
		{
			if (tileArray[planeIndex] == null)
			{
				continue;
			}

			for (int xIndex = 0; xIndex < tileArray[planeIndex].length; xIndex++)
			{
				if (tileArray[planeIndex][xIndex] == null)
				{
					continue;
				}

				for (int yIndex = 0; yIndex < tileArray[planeIndex][xIndex].length; yIndex++)
				{
					Tile tile = tileArray[planeIndex][xIndex][yIndex];
					if (tile == null)
					{
						continue;
					}

					for (GameObject gameObject : tile.getGameObjects())
					{
						if (gameObject != null && filter.test(gameObject))
						{
							matchingGameObjects.add(gameObject);
						}
					}
				}
			}
		}
	}

	/**
	 * Finds a GameObject at a specific WorldPoint
	 */
	public GameObject findGameObjectAtWorldPoint(WorldPoint worldPoint)
	{
		WorldView topLevelWorldView = client.getTopLevelWorldView();
		if (topLevelWorldView == null)
		{
			return null;
		}

		Scene scene = topLevelWorldView.getScene();
		if (scene == null)
		{
			return null;
		}

		net.runelite.api.coords.LocalPoint localPoint = net.runelite.api.coords.LocalPoint.fromWorld(topLevelWorldView, worldPoint);
		if (localPoint == null)
		{
			return null;
		}

		Tile[][][] tileArray = scene.getTiles();
		if (tileArray == null || worldPoint.getPlane() >= tileArray.length)
		{
			return null;
		}

		Tile tile = tileArray[worldPoint.getPlane()][localPoint.getSceneX()][localPoint.getSceneY()];
		if (tile == null)
		{
			return null;
		}

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
