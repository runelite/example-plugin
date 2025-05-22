package ca.plugins.yamablindfold;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Constants;
import net.runelite.api.GameState;
import net.runelite.api.Projection;
import net.runelite.api.Renderable;
import net.runelite.api.Scene;
import net.runelite.api.SceneTileModel;
import net.runelite.api.SceneTilePaint;
import net.runelite.api.Texture;
import net.runelite.api.Tile;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.hooks.DrawCallbacks;

@Singleton
class YamaBlindfold implements DrawCallbacks
{
	@Inject
	private Client client;
	@Inject
	private YamaBlindfoldConfig config;

	private final Collection<WorldArea> drawableWorldAreas = new ArrayList<>();
	private final Collection<WorldPoint> stoneWorldPoints = new ArrayList<>();
	private final Collection<WorldPoint> nonDrawableWorldPoints = new ArrayList<>();

	private final Set<Tile> stoneTiles = new HashSet<>();
	private final Set<Tile> drawableTiles = new HashSet<>();
	private final Set<Tile> nonDrawableTiles = new HashSet<>();

	private DrawCallbacks drawCallbacks;

	void init()
	{
		assert client.isClientThread();

		if (!client.isGpu())
		{
			return;
		}

		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		final var drawCallbacks = client.getDrawCallbacks();
		if (drawCallbacks == null)
		{
			return;
		}

		populateWorldPoints();
		populateTiles();

		if (drawCallbacks != this)
		{
			this.drawCallbacks = drawCallbacks;
			client.setDrawCallbacks(this);
		}
	}

	void shutDown()
	{
		assert client.isClientThread();

		drawableWorldAreas.clear();
		stoneWorldPoints.clear();
		nonDrawableWorldPoints.clear();

		drawableTiles.clear();
		stoneTiles.clear();
		nonDrawableTiles.clear();

		if (client.getDrawCallbacks() == this)
		{
			client.setDrawCallbacks(drawCallbacks);
		}

		drawCallbacks = null;
	}

	@Override
	public void drawScenePaint(final Scene scene, final SceneTilePaint paint, final int plane, final int tileX, final int tileY)
	{
		if (drawCallbacks == null)
		{
			return;
		}

		if (!config.hideTerrain())
		{
			drawCallbacks.drawScenePaint(scene, paint, plane, tileX, tileY);
			return;
		}

		if (plane != 0 ||
			tileX < 0 ||
			tileY < 0 ||
			tileX >= Constants.SCENE_SIZE ||
			tileY >= Constants.SCENE_SIZE)
		{
			return;
		}

		final Tile tile = scene.getTiles()[plane][tileX][tileY];

		if (tile != null && shouldDrawTile(tile))
		{
			drawCallbacks.drawScenePaint(scene, paint, plane, tileX, tileY);
		}
	}

	@Override
	public void drawSceneTileModel(final Scene scene, final SceneTileModel model, final int tileX, final int tileY)
	{
		if (drawCallbacks == null)
		{
			return;
		}

		if (!config.hideTerrain())
		{
			drawCallbacks.drawSceneTileModel(scene, model, tileX, tileY);
			return;
		}

		final var plane = client.getTopLevelWorldView().getPlane();

		if (plane != 0 ||
			tileX < 0 ||
			tileY < 0 ||
			tileX >= Constants.SCENE_SIZE ||
			tileY >= Constants.SCENE_SIZE)
		{
			return;
		}

		final Tile tile = scene.getTiles()[plane][tileX][tileY];

		if (tile != null && shouldDrawTile(tile))
		{
			drawCallbacks.drawSceneTileModel(scene, model, tileX, tileY);
		}
	}

	@Override
	public void drawScene(final double cameraX, final double cameraY, final double cameraZ, final double cameraPitch, final double cameraYaw, final int plane)
	{
		if (drawCallbacks != null)
		{
			drawCallbacks.drawScene(cameraX, cameraY, cameraZ, cameraPitch, cameraYaw, plane);
		}
	}

	@Override
	public void postDrawScene()
	{
		if (drawCallbacks != null)
		{
			drawCallbacks.postDrawScene();
		}
	}

	@Override
	public void animate(final Texture texture, final int diff)
	{
		if (drawCallbacks != null)
		{
			drawCallbacks.animate(texture, diff);
		}
	}

	@Override
	public boolean tileInFrustum(final Scene scene, final int pitchSin, final int pitchCos, final int yawSin, final int yawCos, final int cameraX, final int cameraY, final int cameraZ, final int plane, final int msx, final int msy)
	{
		if (drawCallbacks != null)
		{
			drawCallbacks.tileInFrustum(scene, pitchSin, pitchCos, yawSin, yawCos, cameraX, cameraY, cameraZ, plane, msx, msy);
		}
		return true;
	}

	@Override
	public void loadScene(final Scene scene)
	{
		if (drawCallbacks != null)
		{
			drawCallbacks.loadScene(scene);
		}
	}

	@Override
	public void swapScene(final Scene scene)
	{
		if (drawCallbacks != null)
		{
			drawCallbacks.swapScene(scene);
		}
	}

	@Override
	public void draw(final int overlayColor)
	{
		if (drawCallbacks != null)
		{
			drawCallbacks.draw(overlayColor);
		}
	}

	@Override
	public void draw(final Projection projection, final Scene scene, final Renderable renderable, final int orientation, final int x, final int y, final int z, final long hash)
	{
		if (drawCallbacks != null)
		{
			drawCallbacks.draw(projection, scene, renderable, orientation, x, y, z, hash);
		}
	}

	private boolean shouldDrawTile(final Tile tile)
	{
		return (config.showTerrainStepStones() && stoneTiles.contains(tile)) ||
			(!nonDrawableTiles.contains(tile) && drawableTiles.contains(tile));
	}

	private void populateWorldPoints()
	{
		// Entrance
		drawableWorldAreas.add(new WorldArea(1500, 10049, 7, 6, 0));
		// Walk-way
		drawableWorldAreas.add(new WorldArea(1503, 10055, 1, 13, 0));
		// Arena
		drawableWorldAreas.add(new WorldArea(1489, 10068, 27, 26, 0));
		// West Portal Landing
		drawableWorldAreas.add(new WorldArea(1478, 10076, 3, 3, 0));
		// East Portal Landing
		drawableWorldAreas.add(new WorldArea(1528, 10076, 3, 3, 0));
		// West Judge
		drawableWorldAreas.add(new WorldArea(1477, 10096, 5, 5, 0));
		// East Judge
		drawableWorldAreas.add(new WorldArea(1527, 10096, 5, 5, 0));

		// West Portal Landing Corners
		nonDrawableWorldPoints.add(new WorldPoint(1478, 10076, 0));
		nonDrawableWorldPoints.add(new WorldPoint(1480, 10076, 0));
		nonDrawableWorldPoints.add(new WorldPoint(1478, 10078, 0));
		nonDrawableWorldPoints.add(new WorldPoint(1480, 10078, 0));

		// East Portal Landing Corners
		nonDrawableWorldPoints.add(new WorldPoint(1528, 10076, 0));
		nonDrawableWorldPoints.add(new WorldPoint(1530, 10076, 0));
		nonDrawableWorldPoints.add(new WorldPoint(1528, 10078, 0));
		nonDrawableWorldPoints.add(new WorldPoint(1530, 10078, 0));

		// West Scar
		nonDrawableWorldPoints.add(new WorldPoint(1493, 10086, 0));
		nonDrawableWorldPoints.add(new WorldPoint(1494, 10086, 0));
		nonDrawableWorldPoints.add(new WorldPoint(1495, 10086, 0));
		nonDrawableWorldPoints.add(new WorldPoint(1495, 10087, 0));
		nonDrawableWorldPoints.add(new WorldPoint(1496, 10087, 0));
		nonDrawableWorldPoints.add(new WorldPoint(1496, 10088, 0));
		nonDrawableWorldPoints.add(new WorldPoint(1497, 10088, 0));
		nonDrawableWorldPoints.add(new WorldPoint(1498, 10088, 0));
		nonDrawableWorldPoints.add(new WorldPoint(1498, 10089, 0));

		// East Scar
		nonDrawableWorldPoints.add(new WorldPoint(1512, 10086, 0));
		nonDrawableWorldPoints.add(new WorldPoint(1513, 10086, 0));
		nonDrawableWorldPoints.add(new WorldPoint(1510, 10087, 0));
		nonDrawableWorldPoints.add(new WorldPoint(1511, 10087, 0));
		nonDrawableWorldPoints.add(new WorldPoint(1512, 10087, 0));
		nonDrawableWorldPoints.add(new WorldPoint(1508, 10088, 0));
		nonDrawableWorldPoints.add(new WorldPoint(1509, 10088, 0));
		nonDrawableWorldPoints.add(new WorldPoint(1510, 10088, 0));
		nonDrawableWorldPoints.add(new WorldPoint(1511, 10088, 0));
		nonDrawableWorldPoints.add(new WorldPoint(1508, 10089, 0));

		// West Stones
		stoneWorldPoints.add(new WorldPoint(1479, 10079, 0));
		stoneWorldPoints.add(new WorldPoint(1478, 10082, 0));
		stoneWorldPoints.add(new WorldPoint(1480, 10082, 0));
		stoneWorldPoints.add(new WorldPoint(1477, 10085, 0));
		stoneWorldPoints.add(new WorldPoint(1479, 10085, 0));
		stoneWorldPoints.add(new WorldPoint(1481, 10085, 0));
		stoneWorldPoints.add(new WorldPoint(1477, 10088, 0));
		stoneWorldPoints.add(new WorldPoint(1479, 10088, 0));
		stoneWorldPoints.add(new WorldPoint(1481, 10088, 0));
		stoneWorldPoints.add(new WorldPoint(1478, 10091, 0));
		stoneWorldPoints.add(new WorldPoint(1480, 10091, 0));
		stoneWorldPoints.add(new WorldPoint(1479, 10094, 0));
		// East Stones
		stoneWorldPoints.add(new WorldPoint(1529, 10079, 0));
		stoneWorldPoints.add(new WorldPoint(1528, 10082, 0));
		stoneWorldPoints.add(new WorldPoint(1530, 10082, 0));
		stoneWorldPoints.add(new WorldPoint(1527, 10085, 0));
		stoneWorldPoints.add(new WorldPoint(1529, 10085, 0));
		stoneWorldPoints.add(new WorldPoint(1531, 10085, 0));
		stoneWorldPoints.add(new WorldPoint(1527, 10088, 0));
		stoneWorldPoints.add(new WorldPoint(1529, 10088, 0));
		stoneWorldPoints.add(new WorldPoint(1531, 10088, 0));
		stoneWorldPoints.add(new WorldPoint(1528, 10091, 0));
		stoneWorldPoints.add(new WorldPoint(1530, 10091, 0));
		stoneWorldPoints.add(new WorldPoint(1529, 10094, 0));
	}

	void populateTiles()
	{
		final var wv = client.getTopLevelWorldView();
		Utils.populateTilesFromWorldPoints(wv, stoneWorldPoints, stoneTiles);
		Utils.populateTilesFromWorldPoints(wv, nonDrawableWorldPoints, nonDrawableTiles);
		Utils.populateTilesFromWorldAreas(wv, drawableWorldAreas, drawableTiles);
	}
}
