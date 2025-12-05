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

package plugily.projects.murdermystery.commands.arguments.game;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import plugily.projects.minigamesbox.classic.commands.arguments.data.CommandArgument;
import plugily.projects.minigamesbox.classic.commands.arguments.data.LabelData;
import plugily.projects.minigamesbox.classic.commands.arguments.data.LabeledCommandArgument;
import plugily.projects.murdermystery.Main;
import plugily.projects.murdermystery.commands.arguments.ArgumentsRegistry;
import plugily.projects.murdermystery.handlers.skins.SkinSelectorGUI;

/**
 * Command argument for opening the skin selector GUI
 */
public class SkinSelectorGUIArgument {

    public SkinSelectorGUIArgument(ArgumentsRegistry registry) {
        registry.mapArgument("murdermystery", new LabeledCommandArgument("skingui", "murdermystery.skins.use",
                CommandArgument.ExecutorType.PLAYER,
                new LabelData("/mm skingui", "/mm skingui",
                        "\u00267Open the skin selector GUI\n\u00266Permission: \u00267murdermystery.skins.use")) {
            @Override
            public void execute(CommandSender sender, String[] args) {
                Player player = (Player) sender;
                Main plugin = (Main) registry.getPlugin();

                SkinSelectorGUI skinGUI = new SkinSelectorGUI(plugin);
                skinGUI.openSkinSelector(player);
            }
        });
    }
}
