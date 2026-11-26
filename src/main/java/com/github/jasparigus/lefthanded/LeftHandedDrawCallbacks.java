package com.github.jasparigus.lefthanded;

import net.runelite.api.GameObject;
import net.runelite.api.Model;
import net.runelite.api.Projection;
import net.runelite.api.Renderable;
import net.runelite.api.Scene;
import net.runelite.api.SceneTileModel;
import net.runelite.api.SceneTilePaint;
import net.runelite.api.Texture;
import net.runelite.api.TileObject;
import net.runelite.api.WorldView;
import net.runelite.api.hooks.DrawCallbacks;

import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

/**
 * Wraps the renderer occupying the client's draw callbacks slot (the GPU plugin) and mirrors the
 * local player's mesh for the duration of its {@code drawTemp} call - the point where the game
 * client hands the actor's live model to the renderer, which reads it synchronously while
 * uploading (verified: edits made at BeforeRender or the renderable draw callback never reach the
 * upload, and unrelated models sharing the same arrays would be corrupted by persistent edits).
 * Vertices and face winding are negated around the delegate call; both operations are
 * self-inverse, so the model is exactly restored afterwards, leaving no plugin-owned state and
 * never persistently touching arrays shared with other actors' models.
 */
final class LeftHandedDrawCallbacks implements DrawCallbacks
{
	private final DrawCallbacks delegate;
	private final BooleanSupplier mirrorActive;
	private final Predicate<GameObject> isLocalPlayerEntity;


	LeftHandedDrawCallbacks(DrawCallbacks delegate, BooleanSupplier mirrorActive, Predicate<GameObject> isLocalPlayerEntity)
	{
		this.delegate = delegate;
		this.mirrorActive = mirrorActive;
		this.isLocalPlayerEntity = isLocalPlayerEntity;
	}

	DrawCallbacks getDelegate()
	{
		return delegate;
	}

	/** Hard kill-switch: cleared on plugin shutdown. Must not depend on config after shutdown -
	 * the Config proxy keeps returning the last saved values (e.g. ALWAYS) even once the plugin
	 * is disabled, so a wrapper left in the slot by a renderer that reclaimed it would otherwise
	 * mirror forever. */
	private volatile boolean enabled = true;

	public void setEnabled(boolean enabled)
	{
		this.enabled = enabled;
	}

	@Override
	public void drawTemp(Projection worldProjection, Scene scene, GameObject gameObject,
		Model model, int orientation, int x, int y, int z)
	{
		// The model reaching drawTemp is a shared scratch instance (identical for all entities),
		// so the only per-entity discriminator here is the scene entity itself.
		final boolean mirror = enabled && model != null && model.getVerticesCount() > 0
			&& mirrorActive.getAsBoolean() && isLocalPlayerEntity.test(gameObject);
		if (!mirror)
		{
			delegate.drawTemp(worldProjection, scene, gameObject, model, orientation, x, y, z);
			return;
		}
		// Only this one draw is mirrored; restoring in finally guarantees shared arrays
		// (face indices alias other actors' models) are never left edited.
		MeshMirror.mirrorModelX(model);
		try
		{
			delegate.drawTemp(worldProjection, scene, gameObject, model, orientation, x, y, z);
		}
		finally
		{
			MeshMirror.mirrorModelX(model);
		}
	}

	@Override
	public void drawDynamic(Projection worldProjection, Scene scene, TileObject tileObject,
		Renderable renderable, Model model, int orientation, int x, int y, int z)
	{
		delegate.drawDynamic(worldProjection, scene, tileObject, renderable, model, orientation, x, y, z);
	}

	@Override
	public void drawDynamic(int renderThreadId, Projection worldProjection, Scene scene, TileObject tileObject,
		Renderable renderable, Model model, int orientation, int x, int y, int z)
	{
		delegate.drawDynamic(renderThreadId, worldProjection, scene, tileObject, renderable, model, orientation, x, y, z);
	}

	// --- everything else: pure delegation ---
	// NOT optional: the DrawCallbacks interface defaults are empty/no-op, so any method not
	// forwarded here would silently drop that part of the render (objects, zones, scene passes).

	@Override
	public void draw(Projection projection, Scene scene, Renderable renderable, int orientation, int x, int y, int z, long hash)
	{
		delegate.draw(projection, scene, renderable, orientation, x, y, z, hash);
	}

	@Override
	public void drawScenePaint(Scene scene, SceneTilePaint paint, int plane, int tileX, int tileZ)
	{
		delegate.drawScenePaint(scene, paint, plane, tileX, tileZ);
	}

	@Override
	public void drawSceneTileModel(Scene scene, SceneTileModel model, int tileX, int tileZ)
	{
		delegate.drawSceneTileModel(scene, model, tileX, tileZ);
	}

	@Override
	public void draw(int overlayColor)
	{
		delegate.draw(overlayColor);
	}

	@Override
	public void drawScene(double cameraX, double cameraY, double cameraZ, double cameraPitch, double cameraYaw, int plane)
	{
		delegate.drawScene(cameraX, cameraY, cameraZ, cameraPitch, cameraYaw, plane);
	}

	@Override
	public void postDrawScene()
	{
		delegate.postDrawScene();
	}

	@Override
	public void animate(Texture texture, int diff)
	{
		delegate.animate(texture, diff);
	}

	@Override
	public void loadScene(Scene scene)
	{
		delegate.loadScene(scene);
	}

	@Override
	public void swapScene(Scene scene)
	{
		delegate.swapScene(scene);
	}

	@Override
	public boolean tileInFrustum(Scene scene, float pitchSin, float pitchCos, float yawSin, float yawCos,
		int cameraX, int cameraY, int cameraZ, int plane, int msx, int msy)
	{
		return delegate.tileInFrustum(scene, pitchSin, pitchCos, yawSin, yawCos, cameraX, cameraY, cameraZ, plane, msx, msy);
	}

	@Override
	public boolean zoneInFrustum(int zoneX, int zoneZ, int maxY, int minY)
	{
		return delegate.zoneInFrustum(zoneX, zoneZ, maxY, minY);
	}

	@Override
	public void loadScene(WorldView worldView, Scene scene)
	{
		delegate.loadScene(worldView, scene);
	}

	@Override
	public void despawnWorldView(WorldView worldView)
	{
		delegate.despawnWorldView(worldView);
	}

	@Override
	public void preSceneDraw(Scene scene, Projection entityProjection,
		float cameraX, float cameraY, float cameraZ, float cameraPitch, float cameraYaw,
		int minLevel, int level, int maxLevel, Set<Integer> hideRoofIds)
	{
		delegate.preSceneDraw(scene, entityProjection, cameraX, cameraY, cameraZ, cameraPitch, cameraYaw,
			minLevel, level, maxLevel, hideRoofIds);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void preSceneDraw(Scene scene,
		float cameraX, float cameraY, float cameraZ, float cameraPitch, float cameraYaw,
		int minLevel, int level, int maxLevel, Set<Integer> hideRoofIds)
	{
		delegate.preSceneDraw(scene, cameraX, cameraY, cameraZ, cameraPitch, cameraYaw,
			minLevel, level, maxLevel, hideRoofIds);
	}

	@Override
	public void postSceneDraw(Scene scene)
	{
		delegate.postSceneDraw(scene);
	}

	@Override
	public void drawPass(Projection entityProjection, Scene scene, int pass)
	{
		delegate.drawPass(entityProjection, scene, pass);
	}

	@Override
	public void drawZoneOpaque(Projection entityProjection, Scene scene, int zx, int zz)
	{
		delegate.drawZoneOpaque(entityProjection, scene, zx, zz);
	}

	@Override
	public void drawZoneAlpha(Projection entityProjection, Scene scene, int level, int zx, int zz)
	{
		delegate.drawZoneAlpha(entityProjection, scene, level, zx, zz);
	}

	@Override
	public void invalidateZone(Scene scene, int zx, int zz)
	{
		delegate.invalidateZone(scene, zx, zz);
	}
}
