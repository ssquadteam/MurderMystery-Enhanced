package plugily.projects.murdermystery.arena.special;

import org.bukkit.Location;
import eu.decentsoftware.holograms.api.holograms.Hologram;

/**
 * @author Plajer
 *         <p>
 *         Created at 15.10.2018
 */
public class SpecialBlock {

  private final Location location;
  private final SpecialBlockType specialBlockType;
  private Hologram armorStandHologram;

  public SpecialBlock(Location location, SpecialBlockType specialBlockType) {
    this.location = location;
    this.specialBlockType = specialBlockType;
  }

  public Location getLocation() {
    return location;
  }

  public SpecialBlockType getSpecialBlockType() {
    return specialBlockType;
  }

  public Hologram getArmorStandHologram() {
    return armorStandHologram;
  }

  public void setArmorStandHologram(Hologram armorStandHologram) {
    this.armorStandHologram = armorStandHologram;
  }

  public enum SpecialBlockType {
    HORSE_PURCHASE, MYSTERY_CAULDRON, PRAISE_DEVELOPER, RAPID_TELEPORTATION
  }

}
