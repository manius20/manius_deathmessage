package pl.deathmsg;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

/** Podmienia wbudowany komunikat śmierci na ten z configu. */
public final class DeathListener implements Listener {

    private final DeathMsgPlugin plugin;
    private final MessageService service;

    public DeathListener(DeathMsgPlugin plugin, MessageService service) {
        this.plugin = plugin;
        this.service = service;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        MessageService.Result result = service.resolve(victim);

        if (result.lines().isEmpty()) {
            // Nic nie skonfigurowano dla tej przyczyny.
            if (!plugin.getConfig().getBoolean("settings.fallback-to-vanilla", true)) {
                event.deathMessage(null);
            }
            return;
        }

        Component message = service.format(service.pick(result.lines()), result.context());
        event.deathMessage(null);

        String scope = plugin.getConfig().getString("settings.broadcast-scope", "ALL");
        if (scope.equalsIgnoreCase("WORLD")) {
            for (Player player : victim.getWorld().getPlayers()) {
                player.sendMessage(message);
            }
            Bukkit.getConsoleSender().sendMessage(message);
        } else {
            Bukkit.broadcast(message);
        }
    }
}
