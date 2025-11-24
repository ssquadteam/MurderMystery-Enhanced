package plugily.projects.murdermystery.arena.corpse;

import plugily.projects.murdermystery.handlers.hologram.Hologram;

/**
 * @author Plajer
 *         <p>
 *         Created at 07.08.2018
 */
public class Corpse {

  private final Hologram hologram;
  private final com.github.unldenis.corpse.corpse.Corpse corpseData;

  public Corpse(Hologram hologram, com.github.unldenis.corpse.corpse.Corpse corpseData) {
    this.hologram = hologram;
    this.corpseData = corpseData;
  }

  public Hologram getHologram() {
    return hologram;
  }

  public com.github.unldenis.corpse.corpse.Corpse getCorpseData() {
    return corpseData;
  }
}
