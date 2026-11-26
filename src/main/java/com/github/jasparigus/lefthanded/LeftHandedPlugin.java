package com.github.jasparigus.lefthanded;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.api.Player;
import net.runelite.api.PlayerComposition;
import net.runelite.api.events.ClientTick;
import net.runelite.api.hooks.DrawCallbacks;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Objects;

/**
 * Mirrors the local player's mesh (sword side, asymmetrical gear) for a left-handed appearance.
 *
 * <p>Takes over the client's draw callbacks slot by wrapping the active renderer (the GPU plugin),
 * and flips the live model's vertex x-values and face winding inside {@code drawTemp} - the call
 * site where the game client hands the actor's model to the renderer, which reads it synchronously
 * during upload; the flip is restored immediately afterwards (verified: edits made at
 * BeforeRender or the renderable draw callback never reach the upload).
 */
@Slf4j
@PluginDescriptor(
	name = "Left Handed",
	internalName = "left-handed",
	description = "Mirror your character mesh for a left-handed appearance",
	tags = {"left", "handed", "mirror", "bow"}
)
public class LeftHandedPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private LeftHandedConfig config;

	@Inject
	private ClientThread clientThread;

	/** null while the slot holds the vanilla renderer (or is empty: CPU mode). */
	private LeftHandedDrawCallbacks wrapper;

	private boolean bowEquipped;
	private String lastMatchedWeapon;
	private boolean equipmentDumped;
	private String declinedRenderer;

	@Override
	protected void startUp() throws Exception
	{
		log.debug("Left Handed started!");
	}

	@Override
	protected void shutDown() throws Exception
	{
		detach();
		bowEquipped = false;
		lastMatchedWeapon = null;
		equipmentDumped = false;
		declinedRenderer = null;
		log.debug("Left Handed stopped!");
	}

	@Subscribe
	public void onClientTick(ClientTick clientTick)
	{
		// attach() is polled rather than event-driven: it idempotently self-heals every slot
		// transition (GPU plugin toggles reclaiming/emptying the slot, other model-swap plugins
		// stacking on top) within one tick.
		if (config.mode() != LeftHandedConfig.LeftHandMode.OFF)
		{
			attach();
			if (config.mode() == LeftHandedConfig.LeftHandMode.BOW_ONLY)
			{
				bowEquipped = detectBow();
			}
		}
	}

	/**
	 * Wraps the renderer currently in the draw callbacks slot. Must run on the client thread.
	 * CPU mode (null slot) is unsupported: the CPU renderer draws without DrawCallbacks, so
	 * there is no vertex-accurate hook point; the wrapper is left detached there.
	 */
	private void attach()
	{
		final DrawCallbacks current = client.getDrawCallbacks();
		if (current == null)
		{
			if (wrapper != null)
			{
				wrapper = null; // GPU went away
			}
			logOnce(null);
			return;
		}
		if (current == wrapper)
		{
			return; // already attached
		}
		if (wrapper != null)
		{
			// We were silently stacked under or evicted by another renderer (GPU restart reclaims
			// the slot). Adopt whatever sits on top now.
			log.debug("Left Handed: draw callbacks slot changed to {}, re-wrapping", current.getClass().getName());
		}
		wrapper = new LeftHandedDrawCallbacks(current, this::mirrorActive,
			gameObject -> gameObject.getRenderable() == client.getLocalPlayer());
		client.setDrawCallbacks(wrapper);
		declinedRenderer = null;
		log.debug("Left Handed: mirroring wrapper installed over {}", current.getClass().getName());
	}

	private void detach()
	{
		final LeftHandedDrawCallbacks w = wrapper;
		wrapper = null;
		if (w == null)
		{
			return;
		}
		// Hard kill first: takes effect on the render thread immediately, so mirroring stops even
		// if the slot restore below is deferred or never lands.
		w.setEnabled(false);
		// shutDown() may run on the AWT thread (plugin panel toggle) while setDrawCallbacks
		// asserts the client thread - throwing from shutDown() aborts the whole plugin stop,
		// leaving this plugin subscribed, its config live, and the wrapper mirroring forever.
		final DrawCallbacks delegate = w.getDelegate();
		clientThread.invoke(() ->
		{
			if (client.getDrawCallbacks() == w)
			{
				client.setDrawCallbacks(delegate);
			}
			else
			{
				// Slot was reclaimed by the renderer (e.g. it re-installs itself); restoring over
				// it would evict the renderer. The disabled wrapper just delegates from here on.
				log.debug("Left Handed: draw-callbacks slot no longer ours on detach; leaving disabled wrapper in place");
			}
		});
	}

	private boolean mirrorActive()
	{
		switch (config.mode())
		{
			case ALWAYS:
				return true;
			case BOW_ONLY:
				return bowEquipped;
			default:
				return false;
		}
	}

	private boolean detectBow()
	{
		final Player player = client.getLocalPlayer();
		if (player == null)
		{
			return false;
		}
		final PlayerComposition composition = player.getPlayerComposition();
		if (composition == null)
		{
			return false;
		}
		// Scan every equipment slot and match by item name. getEquipmentId(KitType.WEAPON) is
		// unusable on this client: its logs showed the weapon slot reporting helmet and ammo IDs,
		// and this fork's kit enum diverges from the raw array layout past index 5.
		final int[] equipment = composition.getEquipmentIds();
		if (equipment == null)
		{
			return false;
		}
		if (!equipmentDumped)
		{
			equipmentDumped = true;
			log.debug("Left Handed: raw equipment {}", Arrays.toString(equipment));
		}
		String matched = null;
		for (final int raw : equipment)
		{
			final int itemId;
			if (raw >= PlayerComposition.ITEM_OFFSET)
			{
				itemId = raw - PlayerComposition.ITEM_OFFSET;
			}
			else if (raw > 0 && raw < PlayerComposition.KIT_OFFSET)
			{
				itemId = raw; // raw-id convention (kits live in [KIT_OFFSET, ITEM_OFFSET))
			}
			else
			{
				continue; // kit or empty
			}
			final ItemComposition item = client.getItemDefinition(itemId);
			final String name = item == null ? "" : item.getMembersName().toLowerCase();
			if (!name.contains("bow"))
			{
				continue;
			}
			// OSRS crossbows are one-handed, so mirroring them is a taste call - config decides.
			if (name.contains("crossbow") && !config.mirrorCrossbows())
			{
				continue;
			}
			matched = item.getMembersName();
			break;
		}
		if (!Objects.equals(matched, lastMatchedWeapon))
		{
			lastMatchedWeapon = matched;
			log.debug("Left Handed: bow equipment {}", matched == null ? "<none>" : matched);
		}
		return matched != null;
	}

	private void logOnce(String renderer)
	{
		final String key = renderer == null ? "<none>" : renderer;
		if (declinedRenderer != null && declinedRenderer.equals(key))
		{
			return;
		}
		declinedRenderer = key;
		log.debug("Left Handed: no wrapping possible (draw callbacks slot: {})", key);
	}

	@Provides
	LeftHandedConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(LeftHandedConfig.class);
	}
}
