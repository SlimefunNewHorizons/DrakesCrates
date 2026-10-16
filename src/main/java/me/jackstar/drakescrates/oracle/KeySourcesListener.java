package me.jackstar.drakescrates.oracle;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

/**
 * De donde salen las llaves del Oraculo (config `key-sources` en oracle.yml).
 *
 * <p>Fuentes no farmeables por diseno:</p>
 * <ul>
 *   <li><b>Votos</b> (VotifierPlus, puerto 44649 ya abierto): un voto por sitio y dia. Se escucha
 *       {@code VotifierEvent} por reflexion para no depender del jar de Votifier en compilacion.</li>
 *   <li><b>Jefes</b>: matar entidades cuyo nombre visible contenga una de las palabras configuradas
 *       (jefes de DrakesBosses / MultiverseCreatures / Odysseia). Un jefe es raro y caro de invocar.</li>
 * </ul>
 * <p>Mobs comunes y muertes del jugador quedaron fuera a proposito: cualquier granja los vuelve
 * llaves infinitas.</p>
 */
public final class KeySourcesListener implements Listener {

    private final JavaPlugin plugin;
    private final OracleService service;
    private final java.util.function.Supplier<ConfigurationSection> config;

    public KeySourcesListener(JavaPlugin plugin, OracleService service, java.util.function.Supplier<ConfigurationSection> config) {
        this.plugin = plugin;
        this.service = service;
        this.config = config;
    }

    public void register() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        registerVotifier();
    }

    private ConfigurationSection sources() {
        ConfigurationSection root = config.get();
        return root == null ? null : root.getConfigurationSection("key-sources");
    }

    // ---------------------------------------------------------------- votos

    @SuppressWarnings("unchecked")
    private void registerVotifier() {
        ConfigurationSection s = sources();
        if (s == null || !s.getBoolean("votes.enabled", true)) return;
        if (Bukkit.getPluginManager().getPlugin("Votifier") == null && Bukkit.getPluginManager().getPlugin("VotifierPlus") == null) {
            plugin.getLogger().info("[Oraculo] Votifier no esta: sin llaves por voto.");
            return;
        }
        try {
            Class<? extends Event> evt = (Class<? extends Event>) Class.forName("com.vexsoftware.votifier.model.VotifierEvent");
            Method getVote = evt.getMethod("getVote");
            Bukkit.getPluginManager().registerEvent(evt, this, EventPriority.NORMAL, (listener, event) -> {
                try {
                    Object vote = getVote.invoke(event);
                    String username = String.valueOf(vote.getClass().getMethod("getUsername").invoke(vote));
                    String servicio = String.valueOf(vote.getClass().getMethod("getServiceName").invoke(vote));
                    Bukkit.getScheduler().runTask(plugin, () -> onVote(username, servicio));
                } catch (ReflectiveOperationException e) {
                    plugin.getLogger().log(Level.WARNING, "[Oraculo] voto ilegible", e);
                }
            }, plugin, true);
            plugin.getLogger().info("[Oraculo] Llaves por voto activas (VotifierEvent).");
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            plugin.getLogger().warning("[Oraculo] Votifier presente pero sin VotifierEvent: sin llaves por voto.");
        }
    }

    private void onVote(String username, String servicio) {
        ConfigurationSection s = sources();
        if (s == null) return;
        String relicary = s.getString("votes.relicary", "hercules").toLowerCase(Locale.ROOT);
        int amount = Math.max(1, s.getInt("votes.keys", 1));
        OfflinePlayer target = Bukkit.getOfflinePlayer(username);
        if (target == null || (!target.hasPlayedBefore() && !target.isOnline())) {
            plugin.getLogger().info("[Oraculo] voto de " + username + " (" + servicio + ") sin jugador conocido; ignorado.");
            return;
        }
        try {
            service.give(target.getUniqueId(), relicary, amount);
        } catch (RuntimeException e) {
            plugin.getLogger().warning("[Oraculo] no pude dar llave por voto a " + username + ": " + e.getMessage());
            return;
        }
        plugin.getLogger().info("[Oraculo] +" + amount + " llave(s) " + relicary + " a " + username + " por votar en " + servicio);
        Player online = target.getPlayer();
        if (online != null) {
            online.sendMessage("§6[Oracle] §aThank you for voting on §e" + servicio + "§a! You received §e" + amount
                    + " §akey(s) for §e" + relicary + "§a. Open it with §f/oraculo§a.");
        }
    }

    // ---------------------------------------------------------------- jefes

    @org.bukkit.event.EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBossDeath(EntityDeathEvent event) {
        ConfigurationSection s = sources();
        if (s == null || !s.getBoolean("bosses.enabled", true)) return;
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        String nombre = event.getEntity().getCustomName();
        if (nombre == null) return;
        String plano = org.bukkit.ChatColor.stripColor(nombre).toLowerCase(Locale.ROOT);
        List<String> palabras = s.getStringList("bosses.name-contains");
        boolean esJefe = palabras.stream().anyMatch(p -> p != null && !p.isBlank() && plano.contains(p.toLowerCase(Locale.ROOT)));
        if (!esJefe) return;
        double chance = s.getDouble("bosses.chance", 1.0);
        if (Math.random() > chance) return;
        String relicary = s.getString("bosses.relicary", "hefesto").toLowerCase(Locale.ROOT);
        int amount = Math.max(1, s.getInt("bosses.keys", 1));
        try {
            service.give(killer.getUniqueId(), relicary, amount);
        } catch (RuntimeException e) {
            return;
        }
        plugin.getLogger().info("[Oraculo] +" + amount + " llave(s) " + relicary + " a " + killer.getName() + " por derrotar a " + plano);
        killer.sendMessage("§6[Oracle] §aThe boss dropped §e" + amount + " §akey(s) for §e" + relicary + "§a. Open it with §f/oraculo§a.");
    }
}
