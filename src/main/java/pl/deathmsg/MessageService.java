package pl.deathmsg;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.projectiles.ProjectileSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/** Ustala kategorię śmierci i składa gotową wiadomość z configu. */
public final class MessageService {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    /** Wartości podstawiane za %victim%, %killer%, %weapon%, %mob%, %world%, %x%, %y%, %z%. */
    public record Context(String victim, String killer, String weapon, String mob,
                          String world, int x, int y, int z) {
    }

    /** Lista możliwych wiadomości (do losowania) i kontekst do podstawienia. */
    public record Result(List<String> lines, Context context) {
    }

    private final DeathMsgPlugin plugin;

    public MessageService(DeathMsgPlugin plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------ rozpoznawanie

    public Result resolve(Player victim) {
        EntityDamageEvent last = victim.getLastDamageCause();
        String killer = "";
        String weapon = "";
        String mob = "";
        List<String> lines = List.of();

        if (last instanceof EntityDamageByEntityEvent byEntity) {
            Entity damager = byEntity.getDamager();

            if (damager instanceof Projectile projectile) {
                ProjectileSource shooter = projectile.getShooter();
                if (shooter instanceof Player shooterPlayer) {
                    if (!shooterPlayer.getUniqueId().equals(victim.getUniqueId())) {
                        killer = shooterPlayer.getName();
                        weapon = itemName(shooterPlayer.getInventory().getItemInMainHand());
                        lines = lines("pvp-projectile");
                    }
                } else if (shooter instanceof LivingEntity shooterMob) {
                    mob = mobName(shooterMob);
                    lines = lines("mobs." + mobId(shooterMob));
                    if (lines.isEmpty()) {
                        lines = lines("mob-projectile");
                    }
                }
            } else if (damager instanceof Player attacker) {
                if (!attacker.getUniqueId().equals(victim.getUniqueId())) {
                    killer = attacker.getName();
                    ItemStack hand = attacker.getInventory().getItemInMainHand();
                    if (hand.getType().isAir()) {
                        lines = lines("pvp-fist");
                    } else {
                        weapon = itemName(hand);
                        lines = lines("pvp");
                    }
                    if (lines.isEmpty()) {
                        lines = lines("pvp");
                    }
                }
            } else if (damager instanceof LivingEntity livingDamager) {
                mob = mobName(livingDamager);
                lines = lines("mobs." + mobId(livingDamager));
                if (lines.isEmpty()) {
                    lines = lines("mob");
                }
            }
        }

        if (lines.isEmpty() && last != null) {
            lines = lines("causes." + last.getCause().name());
        }
        if (lines.isEmpty()) {
            lines = lines("default");
        }

        Location loc = victim.getLocation();
        Context ctx = new Context(victim.getName(), killer, weapon, mob,
                victim.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        return new Result(lines, ctx);
    }

    /** Podgląd komunikatu dla komendy /deathmsg test. Zwraca null, gdy kategoria nie istnieje. */
    public Component preview(String key, Player sender) {
        List<String> lines = lines(key);
        if (lines.isEmpty()) {
            lines = lines("causes." + key.toUpperCase(Locale.ROOT));
        }
        if (lines.isEmpty()) {
            lines = lines("mobs." + key.toUpperCase(Locale.ROOT));
        }
        if (lines.isEmpty()) {
            return null;
        }
        Location loc = sender.getLocation();
        Context ctx = new Context(sender.getName(), "Steve", "Diamentowy miecz", "Creeper",
                sender.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        return format(pick(lines), ctx);
    }

    /** Wszystkie dostępne klucze kategorii (do podpowiedzi w komendzie). */
    public List<String> categoryKeys() {
        List<String> keys = new ArrayList<>();
        ConfigurationSection messages = plugin.getConfig().getConfigurationSection("messages");
        if (messages == null) {
            return keys;
        }
        for (String key : messages.getKeys(false)) {
            if (!key.equals("causes") && !key.equals("mobs")) {
                keys.add(key);
            }
        }
        addKeys(keys, "messages.causes");
        addKeys(keys, "messages.mobs");
        return keys;
    }

    private void addKeys(List<String> target, String path) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection(path);
        if (section != null) {
            Set<String> keys = section.getKeys(false);
            target.addAll(keys);
        }
    }

    // ------------------------------------------------------------- formatowanie

    public Component format(String template, Context ctx) {
        String text = template
                .replace("%victim%", escape(ctx.victim()))
                .replace("%killer%", escape(ctx.killer()))
                .replace("%weapon%", escape(ctx.weapon()))
                .replace("%mob%", escape(ctx.mob()))
                .replace("%world%", escape(ctx.world()))
                .replace("%x%", String.valueOf(ctx.x()))
                .replace("%y%", String.valueOf(ctx.y()))
                .replace("%z%", String.valueOf(ctx.z()));
        String prefix = plugin.getConfig().getString("settings.prefix", "");
        return MM.deserialize(prefix + text);
    }

    public String pick(List<String> lines) {
        return lines.get(ThreadLocalRandom.current().nextInt(lines.size()));
    }

    private static String escape(String value) {
        return MM.escapeTags(value == null ? "" : value);
    }

    private List<String> lines(String path) {
        return plugin.getConfig().getStringList("messages." + path);
    }

    // ---------------------------------------------------------------- nazwy

    private String mobId(Entity entity) {
        return entity.getType().getKey().getKey().toUpperCase(Locale.ROOT);
    }

    private String mobName(Entity entity) {
        Component custom = entity.customName();
        if (custom != null) {
            return PLAIN.serialize(custom);
        }
        String id = mobId(entity);
        return plugin.getConfig().getString("mob-names." + id, pretty(id));
    }

    private String itemName(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return plugin.getConfig().getString("item-names.AIR", "pięść");
        }
        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                Component name = meta.displayName();
                if (name != null) {
                    return PLAIN.serialize(name);
                }
            }
        }
        String id = item.getType().getKey().getKey().toUpperCase(Locale.ROOT);
        return plugin.getConfig().getString("item-names." + id, pretty(id));
    }

    /** DIAMOND_SWORD -> Diamond Sword */
    private static String pretty(String id) {
        StringBuilder sb = new StringBuilder();
        for (String part : id.toLowerCase(Locale.ROOT).split("_")) {
            if (part.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }
}
