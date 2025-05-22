/*
 * Copyright (c) 2018 Abex
 * Copyright (c) 2017, Adam <Adam@sigterm.info>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package ca.plugins.yamablindfold;

import java.util.Collection;
import java.util.Set;
import lombok.experimental.UtilityClass;
import static net.runelite.api.Constants.CHUNK_SIZE;
import net.runelite.api.Tile;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;

@UtilityClass
class Utils
{
	void populateTilesFromWorldPoints(final WorldView worldView, final Collection<WorldPoint> worldPoints, final Set<Tile> tiles)
	{
		tiles.clear();

		final var sceneTiles = worldView.getScene().getTiles();

		for (final var worldPoint : worldPoints)
		{
			final var localPoint = toLocalInstance(worldView, worldPoint);
			if (localPoint == null)
			{
				continue;
			}

			final var tile = sceneTiles[0][localPoint.getSceneX()][localPoint.getSceneY()];
			if (tile == null)
			{
				continue;
			}

			tiles.add(tile);
		}
	}

	void populateTilesFromWorldAreas(final WorldView worldView, final Collection<WorldArea> worldAreas, final Set<Tile> tiles)
	{
		tiles.clear();

		final var sceneTiles = worldView.getScene().getTiles();

		for (final var worldArea : worldAreas)
		{
			final var lp1 = toLocalInstance(worldView, worldArea.toWorldPoint());
			if (lp1 == null)
			{
				continue;
			}

			final var lp2 = toLocalInstance(worldView,
				new WorldPoint(worldArea.getX() + worldArea.getWidth() - 1, worldArea.getY() + worldArea.getHeight() - 1, worldArea.getPlane()));
			if (lp2 == null)
			{
				continue;
			}

			for (int x = lp1.getSceneX(); x <= lp2.getSceneX(); ++x)
			{
				for (int y = lp1.getSceneY(); y <= lp2.getSceneY(); ++y)
				{
					final var tile = sceneTiles[0][x][y];
					if (tile == null)
					{
						continue;
					}
					tiles.add(tile);
				}
			}
		}
	}

	// Adapted from
	// https://github.com/runelite/runelite/blob/master/runelite-api/src/main/java/net/runelite/api/coords/WorldPoint.java
	private LocalPoint toLocalInstance(final WorldView wv, final WorldPoint wp)
	{
		final var itc = wv.getInstanceTemplateChunks();
		final var bx = wv.getBaseX();
		final var by = wv.getBaseY();
		final var z = 0;

		WorldPoint worldPoint = null;

		for (int x = 0; x < itc[z].length; ++x)
		{
			for (int y = 0; y < itc[z][x].length; ++y)
			{
				final var cd = itc[z][x][y];
				final var r = cd >> 1 & 0x3;
				final var tcy = (cd >> 3 & 0x7FF) * CHUNK_SIZE;
				final var tcx = (cd >> 14 & 0x3FF) * CHUNK_SIZE;
				final var plane = cd >> 24 & 0x3;

				if (wp.getX() >= tcx && wp.getX() < tcx + CHUNK_SIZE &&
					wp.getY() >= tcy && wp.getY() < tcy + CHUNK_SIZE &&
					plane == wp.getPlane())
				{
					var p = new WorldPoint(bx + x * CHUNK_SIZE + (wp.getX() & (CHUNK_SIZE - 1)),
						by + y * CHUNK_SIZE + (wp.getY() & (CHUNK_SIZE - 1)), z);
					p = rotate(p, r);
					worldPoint = p;
					break;
				}
			}
		}

		if (worldPoint == null)
		{
			return null;
		}

		return LocalPoint.fromWorld(wv, worldPoint);
	}

	// Taken from
	// https://github.com/runelite/runelite/blob/master/runelite-api/src/main/java/net/runelite/api/coords/WorldPoint.java
	private WorldPoint rotate(final WorldPoint point, final int rotation)
	{
		final int chunkX = point.getX() & -CHUNK_SIZE;
		final int chunkY = point.getY() & -CHUNK_SIZE;
		final int x = point.getX() & (CHUNK_SIZE - 1);
		final int y = point.getY() & (CHUNK_SIZE - 1);
		switch (rotation)
		{
			case 1:
				return new WorldPoint(chunkX + y, chunkY + (CHUNK_SIZE - 1 - x), point.getPlane());
			case 2:
				return new WorldPoint(chunkX + (CHUNK_SIZE - 1 - x), chunkY + (CHUNK_SIZE - 1 - y), point.getPlane());
			case 3:
				return new WorldPoint(chunkX + (CHUNK_SIZE - 1 - y), chunkY + x, point.getPlane());
		}
		return point;
	}
}
