package me.jackstar.drakescrates.presentation.editor;

import me.jackstar.drakescraft.utils.ItemBuilder;
import me.jackstar.drakescraft.utils.MessageUtils;
import me.jackstar.drakescrates.domain.models.Crate;
import me.jackstar.drakescrates.domain.models.Reward;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class CratePreviewManager implements Listener {

    public void openPreview(Player player, Crate crate) {
        if (player == null || crate == null) return;

        List<Reward> rewards = crate.getRewards();
        int size = Math.min(54, Math.max(27, ((rewards.size() + 8) / 9) * 9 + 18));
        PreviewHolder holder = new PreviewHolder();
        Inventory inv = Bukkit.createInventory(holder, size,
                MessageUtils.parse("<gold><b>Preview:</b></gold> " + crate.getId()));

        // Fill background borders
        ItemStack pane = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name("<dark_gray> ").build();
        for (int i = 0; i < 9; i++) inv.setItem(i, pane);
        for (int i = size - 9; i < size; i++) inv.setItem(i, pane);

        // Header info on slot 4
        inv.setItem(4, new ItemBuilder(crate.getPreviewItem() != null ? crate.getPreviewItem().getType() : Material.CHEST)
                .name(crate.getDisplayName())
                .lore(
                        "<gray>Modalidad: " + crate.getModality().getDisplayName(),
                        crate.hasSlimefunRewards()
                                ? "<light_purple>⚡ Contenido Slimefun incluido</light_purple>"
                                : "<green>🌱 100% Contenido Vainilla</green>",
                        "<gray>Total Recompensas: <yellow>" + rewards.size() + "</yellow>"
                )
                .build());

        // Back button on bottom middle
        inv.setItem(size - 5, new ItemBuilder(Material.BARRIER)
                .name("<red><b>Close Preview</b></red>")
                .lore("<gray>Click to return.</gray>")
                .build());

        int slot = 9;
        for (Reward r : rewards) {
            if (slot >= size - 9) break;

            ItemStack display = r.getDisplayItem().clone();
            List<String> lore = new ArrayList<>();
            lore.add("<gray>Probabilidad: <yellow><b>" + String.format("%.2f", r.getChance()) + "%</b></yellow>");

            if (r.isSlimefun()) {
                lore.add("<light_purple><b>[SLIMEFUN]</b></light_purple> <gray>Technical/magic item</gray>");
                if (player.getWorld().getName().toLowerCase().startsWith("clasico")) {
                    lore.add("<red>⚠ Disabled in Classic (Vanilla)</red>");
                }
            } else if (r.isVanilla()) {
                lore.add("<green><b>[VANILLA]</b></green> <gray>Safe traditional item</gray>");
            }

            if (!r.getCommands().isEmpty()) {
                lore.add("<aqua><b>[COMMAND]</b></aqua> <gray>Runs additional actions</gray>");
            }

            ItemStack icon = new ItemBuilder(display)
                    .name(r.getDisplayName())
                    .lore(lore.toArray(new String[0]))
                    .build();

            inv.setItem(slot++, icon);
        }

        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getView().getTopInventory().getHolder() instanceof PreviewHolder)) return;

        event.setCancelled(true);
        if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.BARRIER) {
            player.closeInventory();
        }
    }

    private static final class PreviewHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
