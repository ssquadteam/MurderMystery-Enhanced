package plugily.projects.murdermystery.handlers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import plugily.projects.murdermystery.handlers.hologram.Hologram;
import plugily.projects.minigamesbox.classic.handlers.language.MessageBuilder;
import plugily.projects.murdermystery.Main;
import plugily.projects.murdermystery.api.events.game.MurderGameCorpseSpawnEvent;
import plugily.projects.murdermystery.arena.Arena;
import plugily.projects.murdermystery.arena.corpse.Corpse;
import plugily.projects.murdermystery.handlers.hologram.HologramManager;
import plugily.projects.murdermystery.HookManager;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Plajer
 *         <p>
 *         Created at 03.02.2018
 */
public class CorpseHandler implements Listener {

  private final Main plugin;
  private final HashMap<Integer, Corpse> corpses;

  public CorpseHandler(Main plugin) {
    this.plugin = plugin;
    this.corpses = new HashMap<>();
    // run bit later than hook manager to ensure it's not null
    Bukkit.getScheduler().runTaskLater(plugin, () -> {
      if (plugin.getHookManager().isFeatureEnabled(HookManager.HookFeature.CORPSES)) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
      }
    }, 20 * 7);
  }

  public void spawnCorpse(Player player, Arena arena) {
    if (arena == null) {
      return;
    }
    // we need to delay it as player is not yet dead/respawned
    Bukkit.getScheduler().runTaskLater(plugin, () -> {
      MurderGameCorpseSpawnEvent murderGameCorpseSpawnEvent = new MurderGameCorpseSpawnEvent(arena, player.getPlayer(),
          player.getLocation());
      Bukkit.getPluginManager().callEvent(murderGameCorpseSpawnEvent);
      if (murderGameCorpseSpawnEvent.isCancelled()) {
        return;
      }

      Hologram hologram = getLastWordsHologram(player);
      Corpse corpse = new Corpse(hologram, player, player.getLocation());

      corpses.put(corpse.getId(), corpse);
      arena.addCorpse(corpse);

      for (Player p : arena.getPlayers()) {
        corpse.spawn(p);
        if (hologram != null) {
          hologram.spawn(p);
        }
      }
    }, 2L);
  }

  public void removeCorpse(Corpse corpse) {
    if (corpse.getHologram() != null) {
      plugin.getNewHologramManager().deleteHologram(corpse.getHologram());
    }
    for (Player p : Bukkit.getOnlinePlayers()) {
      corpse.destroy(p);
    }
    corpses.remove(corpse.getId());
  }

  public void removeCorpses(Arena arena) {
    for (Corpse corpse : arena.getCorpses()) {
      removeCorpse(corpse);
    }
    arena.getCorpses().clear();
  }

  private Hologram getLastWordsHologram(Player player) {
    if (plugin.getLastWordsManager().getLastWords(player) == null) {
      return null;
    }
    List<String> lines = new ArrayList<>();
    lines.add(new MessageBuilder("ingame.corpses.last-words-hologram-header").asKey().build());

    String line = new MessageBuilder("ingame.corpses.last-words-hologram-line").asKey().build();
    line = line.replace("%last_words%", plugin.getLastWordsManager().getLastWords(player));
    lines.add(line);

    return plugin.getNewHologramManager().createHologram(player.getLocation().clone().add(0, 1, 0), lines);
  }

  @org.bukkit.event.EventHandler
  public void onCorpseInteract(org.bukkit.event.player.PlayerInteractEntityEvent event) {
    if (!(event.getRightClicked() instanceof org.bukkit.entity.Interaction)) {
      return;
    }

    org.bukkit.entity.Interaction interaction = (org.bukkit.entity.Interaction) event.getRightClicked();
    Corpse targetCorpse = null;

    for (Corpse corpse : corpses.values()) {
      if (corpse.getInteraction() != null && corpse.getInteraction().equals(interaction)) {
        targetCorpse = corpse;
        break;
      }
    }

    if (targetCorpse == null)
      return;

    Player player = event.getPlayer();
    if (!plugin.getArenaRegistry().isInArena(player)) {
      return;
    }
    Arena arena = (Arena) plugin.getArenaRegistry().getArena(player);
    if (arena.getArenaState() != plugily.projects.minigamesbox.api.arena.IArenaState.IN_GAME) {
      return;
    }
    if (arena.isSpectatorPlayer(player) || arena.isDeathPlayer(player)) {
      return;
    }

    plugily.projects.minigamesbox.api.user.IUser user = plugin.getUserManager().getUser(player);
    if (plugily.projects.murdermystery.arena.role.Role.isRole(plugily.projects.murdermystery.arena.role.Role.DETECTIVE,
        user, arena)) {
      new MessageBuilder("IN_GAME_MESSAGES_ARENA_PLAYING_CORPSE_CLICK_DETECTIVE").asKey().player(player).arena(arena)
          .sendPlayer();
    } else if (plugily.projects.murdermystery.arena.role.Role
        .isRole(plugily.projects.murdermystery.arena.role.Role.MURDERER, user, arena)) {
      new MessageBuilder("IN_GAME_MESSAGES_ARENA_PLAYING_CORPSE_CLICK_MURDERER").asKey().player(player).arena(arena)
          .sendPlayer();
    } else {
      new MessageBuilder("IN_GAME_MESSAGES_ARENA_PLAYING_CORPSE_CLICK_INNOCENT").asKey().player(player).arena(arena)
          .sendPlayer();
    }
  }
}
