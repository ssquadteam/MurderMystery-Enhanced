package plugily.projects.murdermystery.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;

public class MessageUtil {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection();

    public static Component parse(String input) {
        return MINI_MESSAGE.deserialize(replaceLegacy(input));
    }

    public static String parseToLegacy(String input) {
        return LEGACY_SERIALIZER.serialize(parse(input));
    }

    public static void sendMessage(CommandSender sender, String message) {
        // If the server supports Adventure natively (Paper), we could cast sender to
        // Audience.
        // But for compatibility, we might need to convert to legacy if the server is
        // old,
        // or use an Adventure platform.
        // For now, we'll assume the user might want to use MiniMessage in config,
        // and we convert it to legacy for standard Bukkit sendMessage if needed,
        // or just send the component if we had the platform.

        // Since we don't have the Adventure platform set up in this plugin,
        // we'll provide the parsing logic.
        // Ideally, we would use platform.sender(sender).sendMessage(parse(message));

        sender.sendMessage(parseToLegacy(message));
    }

    private static String replaceLegacy(String input) {
        if (input == null)
            return null;
        return input
                .replace("&0", "<black>")
                .replace("&1", "<dark_blue>")
                .replace("&2", "<dark_green>")
                .replace("&3", "<dark_aqua>")
                .replace("&4", "<dark_red>")
                .replace("&5", "<dark_purple>")
                .replace("&6", "<gold>")
                .replace("&7", "<gray>")
                .replace("&8", "<dark_gray>")
                .replace("&9", "<blue>")
                .replace("&a", "<green>")
                .replace("&b", "<aqua>")
                .replace("&c", "<red>")
                .replace("&d", "<light_purple>")
                .replace("&e", "<yellow>")
                .replace("&f", "<white>")
                .replace("&k", "<obfuscated>")
                .replace("&l", "<bold>")
                .replace("&m", "<strikethrough>")
                .replace("&n", "<underlined>")
                .replace("&o", "<italic>")
                .replace("&r", "<reset>");
    }
}
