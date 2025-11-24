package plugily.projects.murdermystery.handlers.hologram;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import plugily.projects.murdermystery.Main;

import java.util.ArrayList;
import java.util.List;

public class HologramManager {

    private final Main plugin;
    private final List<Hologram> holograms = new ArrayList<>();

    public HologramManager(Main plugin) {
        this.plugin = plugin;
    }

    public Hologram createHologram(Location location, List<String> lines) {
        Hologram hologram = new Hologram(location, lines);
        holograms.add(hologram);
        // Spawn for nearby players?
        // For now, we let the caller handle spawning or we can iterate online players.
        // But typically holograms are spawned for specific viewers or tracked.
        // We'll expose spawn methods on the Hologram itself.
        return hologram;
    }

    public Hologram createHologram(Location location, ItemStack item) {
        Hologram hologram = new Hologram(location, item);
        holograms.add(hologram);
        return hologram;
    }

    public void deleteHologram(Hologram hologram) {
        if (hologram != null) {
            holograms.remove(hologram);
            // In a real system, we'd track who sees it and send destroy packets.
            // For this implementation, we rely on the caller to clean up or we iterate
            // online players.
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                hologram.destroy(player);
            }
        }
    }
}
