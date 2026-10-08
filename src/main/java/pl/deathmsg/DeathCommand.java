package pl.deathmsg;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** /deathmsg reload | /deathmsg test <kategoria> */
public final class DeathCommand implements TabExecutor {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final DeathMsgPlugin plugin;
    private final MessageService service;

    public DeathCommand(DeathMsgPlugin plugin, MessageService service) {
        this.plugin = plugin;
        this.service = service;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("deathmsg.admin")) {
            sender.sendMessage(MM.deserialize("<red>Nie masz uprawnień."));
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(MM.deserialize("<yellow>Użycie: /deathmsg <reload|test> [kategoria]"));
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            sender.sendMessage(MM.deserialize("<green>Konfiguracja DeathMsg przeładowana."));
            return true;
        }

        if (args[0].equalsIgnoreCase("test")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(MM.deserialize("<red>Tę komendę może użyć tylko gracz."));
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(MM.deserialize("<yellow>Użycie: /deathmsg test <kategoria> <gray>(np. pvp, mob, FALL, CREEPER)"));
                return true;
            }
            Component preview = service.preview(args[1], player);
            if (preview == null) {
                sender.sendMessage(MM.deserialize("<red>Nie ma takiej kategorii w configu: <yellow>" + MM.escapeTags(args[1])));
            } else {
                sender.sendMessage(preview);
            }
            return true;
        }

        sender.sendMessage(MM.deserialize("<yellow>Użycie: /deathmsg <reload|test> [kategoria]"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("deathmsg.admin")) {
            return List.of();
        }
        List<String> options = new ArrayList<>();
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            for (String option : List.of("reload", "test")) {
                if (option.startsWith(prefix)) {
                    options.add(option);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("test")) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            for (String key : service.categoryKeys()) {
                if (key.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    options.add(key);
                }
            }
        }
        return options;
    }
}
