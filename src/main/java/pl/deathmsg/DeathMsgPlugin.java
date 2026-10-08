package pl.deathmsg;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class DeathMsgPlugin extends JavaPlugin {

    private MessageService service;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        service = new MessageService(this);

        getServer().getPluginManager().registerEvents(new DeathListener(this, service), this);

        PluginCommand command = getCommand("deathmsg");
        if (command != null) {
            DeathCommand executor = new DeathCommand(this, service);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        } else {
            getLogger().warning("Brak komendy deathmsg w plugin.yml");
        }
    }
}
