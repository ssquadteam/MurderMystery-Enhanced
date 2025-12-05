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
package plugily.projects.murdermystery.handlers.skins;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import plugily.projects.minigamesbox.api.user.IUser;
import plugily.projects.minigamesbox.classic.handlers.language.MessageBuilder;
import plugily.projects.minigamesbox.classic.utils.helper.ItemBuilder;
import plugily.projects.minigamesbox.classic.utils.version.xseries.XMaterial;
import plugily.projects.minigamesbox.inventory.common.item.SimpleClickableItem;
import plugily.projects.minigamesbox.inventory.normal.NormalFastInv;
import plugily.projects.murdermystery.Main;
import plugily.projects.murdermystery.handlers.skins.sword.SwordSkin;
import plugily.projects.murdermystery.handlers.skins.sword.SwordSkinManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * GUI for selecting sword skins
 */
public class SkinSelectorGUI {

    private final Main plugin;

    public SkinSelectorGUI(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * Opens the skin selector GUI for a player
     * 
     * @param player The player to open the GUI for
     */
    public void openSkinSelector(Player player) {
        SwordSkinManager skinManager = plugin.getSwordSkinManager();
        List<SwordSkin> availableSkins = new ArrayList<>();

        // Get all skins the player has permission for
        for (SwordSkin skin : skinManager.getRegisteredSwordSkins()) {
            if (!skin.hasPermission() || player.hasPermission(skin.getPermission())) {
                availableSkins.add(skin);
            }
        }

        // Calculate inventory size (multiples of 9, minimum 9, maximum 54)
        int size = Math.min(54, Math.max(9, ((availableSkins.size() / 9) + 1) * 9));

        NormalFastInv gui = new NormalFastInv(size,
                new MessageBuilder("COMMANDS_SWORD_SKINS_GUI_TITLE").asKey().build());

        String currentSkinName = skinManager.getPlayerCurrentSkinName(player);

        // Get all skin names from the manager using reflection or by iterating
        int slot = 0;
        for (Map.Entry<String, SwordSkin> entry : getSkinsByName(skinManager).entrySet()) {
            String skinName = entry.getKey();
            SwordSkin skin = entry.getValue();

            // Check if player has permission for this skin
            if (skin.hasPermission() && !player.hasPermission(skin.getPermission())) {
                continue;
            }

            if (slot >= size) {
                break; // Don't overflow the inventory
            }

            ItemStack displayItem = skin.getItemStack().clone();
            ItemMeta meta = displayItem.getItemMeta();
            if (meta != null) {
                // Set display name
                boolean isSelected = skinName.equals(currentSkinName);
                String displayName = isSelected
                        ? ChatColor.GREEN + skinName + " " + ChatColor.GRAY + "(Selected)"
                        : ChatColor.YELLOW + skinName;
                meta.setDisplayName(displayName);

                // Set lore
                List<String> lore = new ArrayList<>();
                if (isSelected) {
                    lore.add(ChatColor.GREEN + "Currently selected!");
                } else {
                    lore.add(ChatColor.GRAY + "Click to select this skin");
                }
                meta.setLore(lore);
                displayItem.setItemMeta(meta);
            }

            final String finalSkinName = skinName;
            gui.addItem(new SimpleClickableItem(displayItem, event -> {
                selectSkin(player, finalSkinName);
                player.closeInventory();
            }));
            slot++;
        }

        gui.refresh();
        gui.open(player);
    }

    /**
     * Selects a skin for a player
     */
    private void selectSkin(Player player, String skinName) {
        SwordSkinManager skinManager = plugin.getSwordSkinManager();
        SwordSkin skin = skinManager.getSkinByName(skinName);

        if (skin == null) {
            player.sendMessage(ChatColor.RED + "Skin not found: " + skinName);
            return;
        }

        // Check permission
        if (skin.hasPermission() && !player.hasPermission(skin.getPermission())) {
            player.sendMessage(
                    new MessageBuilder("COMMANDS_SWORD_SKINS_NO_PERMISSION").asKey().value(skinName).build());
            return;
        }

        // Save the selection
        IUser user = plugin.getUserManager().getUser(player);
        user.setStatistic("SELECTED_SWORD_SKIN", skinName.hashCode());

        player.sendMessage(new MessageBuilder("COMMANDS_SWORD_SKINS_SKIN_SELECTED").asKey().value(skinName).build());
    }

    /**
     * Gets all skins by name from the manager
     */
    private Map<String, SwordSkin> getSkinsByName(SwordSkinManager manager) {
        return manager.getSkinsByName();
    }
}
