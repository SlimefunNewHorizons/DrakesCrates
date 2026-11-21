package me.jackstar.drakescrates.presentation.editor;

import me.jackstar.drakescraft.utils.MessageUtils;
import me.jackstar.drakescrates.application.repositories.CrateRepository;
import me.jackstar.drakescrates.domain.models.Crate;
import me.jackstar.drakescrates.domain.models.Reward;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class CrateEditorManager implements Listener {

    private final CrateRepository crateRepository;
    private final ConcurrentMap<UUID, EditorSession> sessions = new ConcurrentHashMap<>();

    public CrateEditorManager(CrateRepository crateRepository) {
        this.crateRepository = crateRepository;
    }

    public void openEditor(Player player, String crateId) {
        if (player == null) {
            return;
        }
        if (crateId != null && !crateId.isBlank()) {
            Optional<Crate> crate = crateRepository.findCrateById(crateId);
            if (crate.isPresent()) {
                openRewardEditor(player, crate.get());
                return;
            }
            MessageUtils.send(player, "<red>Crate not found: <gray>" + crateId + "</gray></red>");
        }
        openCrateSelector(player);
    }

    static int computeChestSize(int itemCount) {
        if (itemCount <= 0) {
            return 9;
        }
        return Math.min(54, Math.max(9, ((itemCount - 1) / 9 + 1) * 9));
    }

    private void openCrateSelector(Player player) {
        List<Crate> crates = new ArrayList<>(crateRepository.getAllCrates());
        int size = computeChestSize(crates.size());
        Inventory inventory = Bukkit.createInventory(new CrateSelectorHolder(), size, MessageUtils.parse("<gold>Crates Editor</gold>"));

        for (int i = 0; i < crates.size() && i < size; i++) {
            Crate crate = crates.get(i);
            ItemStack item = crate.getPreviewItem().clone();
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
                lore.add(MessageUtils.parse("<gray>ID: <yellow>" + crate.getId() + "</yellow>"));
                lore.add(MessageUtils.parse("<gray>Click to edit rewards chances</gray>"));
                meta.displayName(crate.getDisplayName());
                meta.lore(lore);
                item.setItemMeta(meta);
            }
            inventory.setItem(i, item);
        }
        player.openInventory(inventory);
    }

    private void openRewardEditor(Player player, Crate crate) {
        List<Reward> rewards = crate.getRewards();
        int size = computeChestSize(rewards.size());
        Inventory inventory = Bukkit.createInventory(
                new RewardEditorHolder(normalize(crate.getId())),
                size,
                MessageUtils.parse("<gold>Edit Chances: </gold><yellow>" + crate.getId()));

        for (int i = 0; i < rewards.size() && i < size; i++) {
            Reward reward = rewards.get(i);
            inventory.setItem(i, buildRewardEditorItem(reward));
        }

        sessions.put(player.getUniqueId(), new EditorSession(normalize(crate.getId())));
        player.openInventory(inventory);
    }

    private ItemStack buildRewardEditorItem(Reward reward) {
        ItemStack item = reward.getDisplayItem().clone();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
            lore.add(MessageUtils.parse("<gray>ID: <yellow>" + reward.getId() + "</yellow>"));
            lore.add(MessageUtils.parse("<gray>Current chance: <gold>" + reward.getChance() + "%</gold>"));
            lore.add(MessageUtils.parse("<dark_gray> "));
            lore.add(MessageUtils.parse("<green>Left click: +1%</green>"));
            lore.add(MessageUtils.parse("<red>Right click: -1%</red>"));
            lore.add(MessageUtils.parse("<green>Shift + Left: +10%</green>"));
            lore.add(MessageUtils.parse("<red>Shift + Right: -10%</red>"));
            meta.displayName(reward.getDisplayName());
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (!(holder instanceof CrateSelectorHolder) && !(holder instanceof RewardEditorHolder)) {
            return;
        }
        event.setCancelled(true);
        if (event.getRawSlot() < 0 || event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
            return;
        }

        if (holder instanceof CrateSelectorHolder) {
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType().isAir()) {
                return;
            }
            String crateId = extractCrateIdFromItem(clicked);
            if (crateId == null) {
                return;
            }
            crateRepository.findCrateById(crateId).ifPresent(crate -> openRewardEditor(player, crate));
            return;
        }

        RewardEditorHolder rewardHolder = (RewardEditorHolder) holder;
        EditorSession session = sessions.get(player.getUniqueId());
        if (session == null || !session.crateId.equals(rewardHolder.crateId)) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) {
            return;
        }

        String rewardId = extractRewardIdFromItem(clicked);
        if (rewardId == null) {
            return;
        }

        Crate crate = crateRepository.findCrateById(session.crateId).orElse(null);
        if (crate == null) {
            MessageUtils.send(player, "<red>Crate not found. Try reloading.</red>");
            return;
        }

        Reward reward = crate.getRewards().stream()
                .filter(r -> normalize(r.getId()).equals(rewardId))
                .findFirst()
                .orElse(null);
        if (reward == null) {
            MessageUtils.send(player, "<red>Reward not found.</red>");
            return;
        }

        double delta;
        if (event.isLeftClick()) {
            delta = event.isShiftClick() ? 10.0D : 1.0D;
        } else if (event.isRightClick()) {
            delta = event.isShiftClick() ? -10.0D : -1.0D;
        } else {
            return;
        }

        double newChance = Math.max(0.1D, reward.getChance() + delta);
        boolean updated = crateRepository.updateRewardChance(crate.getId(), reward.getId(), newChance);
        if (!updated) {
            MessageUtils.send(player, "<red>Could not update chance in file.</red>");
            return;
        }

        MessageUtils.send(player, "<green>Updated <yellow>" + reward.getId() + "</yellow> chance to <gold>" + newChance + "%</gold>.</green>");
        crateRepository.findCrateById(crate.getId()).ifPresent(refreshed -> {
            double total = refreshed.getRewards().stream().mapToDouble(Reward::getChance).sum();
            if (total < 99.0D || total > 101.0D) {
                MessageUtils.send(player, "<yellow>Warning:</yellow> total crate chance is <gold>" + String.format(Locale.US, "%.2f", total)
                        + "%</gold>. Recommended around 100% for predictable balance.");
            }
            openRewardEditor(player, refreshed);
        });
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (holder instanceof CrateSelectorHolder || holder instanceof RewardEditorHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        sessions.remove(event.getPlayer().getUniqueId());
    }

    private String extractCrateIdFromItem(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null || meta.lore() == null) {
            return null;
        }
        for (net.kyori.adventure.text.Component line : meta.lore()) {
            String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(line);
            if (plain.startsWith("ID: ")) {
                return normalize(plain.substring("ID: ".length()));
            }
        }
        return null;
    }

    private String extractRewardIdFromItem(ItemStack item) {
        return extractCrateIdFromItem(item);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }

    private record EditorSession(String crateId) {
    }

    private static final class CrateSelectorHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    private static final class RewardEditorHolder implements InventoryHolder {
        private final String crateId;

        private RewardEditorHolder(String crateId) {
            this.crateId = crateId;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
