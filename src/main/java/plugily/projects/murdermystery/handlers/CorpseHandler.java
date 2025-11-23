/*
 * MurderMystery - Find the murderer, kill him and survive!
 * Copyright (c) 2022  Plugily Projects - maintained by Tigerpanzer_02 and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package plugily.projects.murdermystery.handlers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import com.github.unldenis.corpse.api.CorpseAPI;
import com.github.unldenis.corpse.event.AsyncCorpseInteractEvent;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import plugily.projects.minigamesbox.api.arena.IArenaState;
import plugily.projects.minigamesbox.classic.handlers.language.MessageBuilder;
import plugily.projects.murdermystery.Main;
import plugily.projects.murdermystery.api.events.game.MurderGameCorpseSpawnEvent;
import plugily.projects.murdermystery.arena.Arena;
import plugily.projects.murdermystery.arena.corpse.Corpse;
import plugily.projects.murdermystery.arena.role.Role;
import plugily.projects.murdermystery.handlers.hologram.HologramManager;
import plugily.projects.murdermystery.HookManager;

import java.util.HashMap;

/**
 * @author Plajer
 *         <p>
 *         Created at 03.02.2018
 */
public class CorpseHandler implements Listener {

  private final Main plugin;
  private final HashMap<Integer, Corpse> corpses;
  private final HologramManager hologramManager;
  private Corpse lastSpawnedCorpse;

  public CorpseHandler(Main plugin) {
    this.plugin = plugin;
    this.corpses = new HashMap<>();
    this.hologramManager = plugin.getNewHologramManager();
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
      com.github.unldenis.corpse.corpse.Corpse corpseData = CorpseAPI.getInstance().spawnCorpse(player,
          player.getLocation());
      Corpse corpse = new Corpse(hologram, corpseData);
      lastSpawnedCorpse = corpse;
      corpses.put(corpseData.getId(), corpse);
      arena.addCorpse(corpse);
    }, 2L);
  }

  public void removeCorpse(Corpse corpse) {
    if (corpse.getHologram() != null) {
      corpse.getHologram().delete();
    }
    CorpseAPI.getInstance().removeCorpse(corpse.getCorpseData());
    corpses.remove(corpse.getCorpseData().getId());
  }

  public void removeCorpses(Arena arena) {
    for (Corpse corpse : arena.getCorpses()) {
      removeCorpse(corpse);
    }
    arena.getCorpses().clear();
  }

  private Hologram getLastWordsHologram(Player player) {
    if (plugin.getLastWordsManager().hasLastWords(player)) {
      return plugin.getNewHologramManager().createHologram(
          player.getLocation().add(0, 1, 0),
          java.util.Collections.singletonList(
              new MessageBuilder(
                  plugin.getLanguageManager().getLanguageMessage("In-Game.Messages.Arena.Playing.Last-Words.Hologram"))
                  .value(plugily.projects.murdermystery.utils.MessageUtil
                      .parseToLegacy(plugin.getLastWordsManager().getLastWords(player)))
                  .build()));
    }
    return null;
  }

  @EventHandler
  public void onCorpseInteract(AsyncCorpseInteractEvent e) {
    Bukkit.getScheduler().runTask(plugin, () -> {
      Player player = e.getPlayer();
      if (!plugin.getArenaRegistry().isInArena(player)) {
        return;
      }
      Arena arena = (Arena) plugin.getArenaRegistry().getArena(player);
      if (arena.getArenaState() != IArenaState.IN_GAME) {
        return;
      }
      if (arena.isSpectatorPlayer(player) || arena.isDeathPlayer(player)) {
        return;
      }
      if (Role.isRole(Role.DETECTIVE, plugin.getUserManager().getUser(player), arena)) {
        new MessageBuilder("IN_GAME_MESSAGES_ARENA_PLAYING_CORPSE_CLICK_DETECTIVE").asKey().player(player).arena(arena)
            .sendPlayer();
      } else if (Role.isRole(Role.MURDERER, plugin.getUserManager().getUser(player), arena)) {
        new MessageBuilder("IN_GAME_MESSAGES_ARENA_PLAYING_CORPSE_CLICK_MURDERER").asKey().player(player).arena(arena)
            .sendPlayer();
      } else {
        new MessageBuilder("IN_GAME_MESSAGES_ARENA_PLAYING_CORPSE_CLICK_INNOCENT").asKey().player(player).arena(arena)
            .sendPlayer();
      }
    });
  }
}
