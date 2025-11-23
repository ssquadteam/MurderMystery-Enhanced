package plugily.projects.murdermystery.handlers.hologram;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import plugily.projects.murdermystery.Main;

import java.util.List;
import java.util.UUID;

public class HologramManager {

    private final Main plugin;

    public HologramManager(Main plugin) {
        this.plugin = plugin;
    }

    public Hologram createHologram(Location location, List<String> lines) {
        String name = "mm-" + UUID.randomUUID().toString();
        return DHAPI.createHologram(name, location, lines);
    }

    public Hologram createHologram(Location location, ItemStack item) {
        String name = "mm-" + UUID.randomUUID().toString();
        Hologram hologram = DHAPI.createHologram(name, location);
        DHAPI.addHologramLine(hologram, item);
        return hologram;
    }

    public void deleteHologram(Hologram hologram) {
        if (hologram != null) {
            hologram.delete();
        }
    }
    
    public void deleteHologram(String name) {
        DHAPI.removeHologram(name);
    }
}
