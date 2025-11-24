package plugily.projects.murdermystery.arena.corpse;

import org.bukkit.entity.ArmorStand;
import plugily.projects.murdermystery.handlers.hologram.Hologram;

/**
 * @author Plajer
 *         <p>
 *         Created at 07.08.2018
 */
public class Stand {

  private final Hologram hologram;
  private final ArmorStand stand;

  public Stand(Hologram hologram, ArmorStand stand) {
    this.hologram = hologram;
    this.stand = stand;
  }

  public Hologram getHologram() {
    return hologram;
  }

  public ArmorStand getStand() {
    return stand;
  }
}
