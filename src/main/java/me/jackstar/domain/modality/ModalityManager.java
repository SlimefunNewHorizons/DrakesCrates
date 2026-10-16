package me.jackstar.drakescrates.domain.modality;

import me.jackstar.drakescraft.utils.MessageUtils;
import me.jackstar.drakescrates.domain.models.Crate;
import me.jackstar.drakescrates.domain.models.CrateModality;
import net.kyori.adventure.text.Component;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class ModalityManager {

    private final JavaPlugin plugin;
    private final Set<String> clasicoWorlds = new HashSet<>();
    private final Set<String> slimefunWorlds = new HashSet<>();

    public ModalityManager(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        clasicoWorlds.clear();
        slimefunWorlds.clear();

        FileConfiguration config = plugin.getConfig();
        List<String> cw = config.getStringList("modalities.clasico-worlds");
        if (cw != null && !cw.isEmpty()) {
            for (String w : cw) {
                clasicoWorlds.add(w.toLowerCase(Locale.ROOT).trim());
            }
        } else {
            clasicoWorlds.add("clasico");
            clasicoWorlds.add("clasico_nether");
            clasicoWorlds.add("clasico_the_end");
        }

        List<String> sw = config.getStringList("modalities.slimefun-worlds");
        if (sw != null && !sw.isEmpty()) {
            for (String w : sw) {
                slimefunWorlds.add(w.toLowerCase(Locale.ROOT).trim());
            }
        }
    }

    public boolean isClasico(World world) {
        if (world == null) return false;
        String name = world.getName().toLowerCase(Locale.ROOT).trim();
        if (clasicoWorlds.contains(name)) return true;
        return name.startsWith("clasico");
    }

    public boolean isClasico(Player player) {
        if (player == null || !player.isOnline()) return false;
        return isClasico(player.getWorld());
    }

    public CrateModality getCurrentModality(Player player) {
        if (isClasico(player)) {
            return CrateModality.CLASICO;
        }
        return CrateModality.SLIMEFUN;
    }

    /**
     * Determines whether the player can open the given crate in their current world/modality.
     * Enforces the strict vanilla rule for Clásico: No Slimefun content allowed.
     */
    public boolean canOpenCrate(Player player, Crate crate) {
        if (player == null || crate == null) return false;

        boolean inClasico = isClasico(player);

        if (inClasico) {
            // In Clásico, ANY crate marked as SLIMEFUN or containing Slimefun rewards is FORBIDDEN.
            if (crate.getModality() == CrateModality.SLIMEFUN) {
                return false;
            }
            if (crate.hasSlimefunRewards()) {
                return false;
            }
            return true;
        }

        return true;
    }

    public Component getDeniedReason(Player player, Crate crate) {
        if (isClasico(player) && (crate.getModality() == CrateModality.SLIMEFUN || crate.hasSlimefunRewards())) {
            return MessageUtils.parse(
                    "<red>✦ <b>[Classic Mode - Vanilla Restriction]</b></red><newline>"
                    + "<gray>This crate contains <light_purple>Slimefun</light_purple> rewards and is forbidden in Classic (100% Vanilla).<newline>"
                    + "To open it, travel to Skyblock, Oneblock, or another mode with Slimefun enabled.</gray>"
            );
        }
        return MessageUtils.parse("<red>You cannot open this crate in your current mode.</red>");
    }
}
