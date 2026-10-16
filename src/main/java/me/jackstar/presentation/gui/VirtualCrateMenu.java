package me.jackstar.drakescrates.presentation.gui;

import me.jackstar.drakescraft.utils.ItemBuilder;
import me.jackstar.drakescraft.utils.MessageUtils;
import me.jackstar.drakescrates.application.repositories.CrateRepository;
import me.jackstar.drakescrates.application.repositories.VirtualKeyRepository;
import me.jackstar.drakescrates.application.usecases.OpenCrateUseCase;
import me.jackstar.drakescrates.domain.modality.ModalityManager;
import me.jackstar.drakescrates.domain.models.Crate;
import me.jackstar.drakescrates.domain.models.CrateModality;
import me.jackstar.drakescrates.domain.models.CrateType;
import me.jackstar.drakescrates.domain.models.Key;
import me.jackstar.drakescrates.domain.models.OpenResult;
import me.jackstar.drakescrates.domain.models.Reward;
import me.jackstar.drakescrates.presentation.animation.RouletteAnimation;
import me.jackstar.drakescrates.presentation.editor.CratePreviewManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class VirtualCrateMenu implements Listener {

    private final JavaPlugin plugin;
    private final CrateRepository crateRepository;
    private final VirtualKeyRepository virtualKeyRepository;
    private final ModalityManager modalityManager;
    private final OpenCrateUseCase openCrateUseCase;
    private final RouletteAnimation rouletteAnimation;
    private final CratePreviewManager previewManager;

    // Track player's selected filter category: "all", "clasico", "slimefun"
    private final Map<UUID, CrateModality> playerFilters = new HashMap<>();

    public VirtualCrateMenu(JavaPlugin plugin, CrateRepository crateRepository,
                            VirtualKeyRepository virtualKeyRepository, ModalityManager modalityManager,
                            OpenCrateUseCase openCrateUseCase, RouletteAnimation rouletteAnimation,
                            CratePreviewManager previewManager) {
        this.plugin = plugin;
        this.crateRepository = crateRepository;
        this.virtualKeyRepository = virtualKeyRepository;
        this.modalityManager = modalityManager;
        this.openCrateUseCase = openCrateUseCase;
        this.rouletteAnimation = rouletteAnimation;
        this.previewManager = previewManager;
    }

    public void openMenu(Player player) {
        CrateModality filter = playerFilters.computeIfAbsent(player.getUniqueId(),
                k -> modalityManager.getCurrentModality(player));
        openMenu(player, filter);
    }

    public void openMenu(Player player, CrateModality filter) {
        playerFilters.put(player.getUniqueId(), filter);

        VirtualMenuHolder holder = new VirtualMenuHolder(filter);
        Inventory inv = Bukkit.createInventory(holder, 54,
                MessageUtils.parse("<gradient:#ffd700:#ff8c00><b>DrakesCrates - Virtual Menu</b></gradient>"));

        // Frame
        ItemStack pane = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name("<dark_gray> ").build();
        for (int i = 0; i < 9; i++) inv.setItem(i, pane);
        for (int i = 45; i < 54; i++) inv.setItem(i, pane);

        // Header Tabs
        // Tab 1: Clásico (Vainilla)
        boolean isClasicoActive = filter == CrateModality.CLASICO;
        inv.setItem(2, new ItemBuilder(isClasicoActive ? Material.EMERALD : Material.GRASS_BLOCK)
                .name("<green><b>🌱 Classic Mode (100% Vanilla)</b></green>")
                .lore(
                        "<gray>Exclusive crates allowed",
                        "<gray>in the Classic world.",
                        "",
                        isClasicoActive ? "<green>✔ Active Filter" : "<yellow>Click to filter"
                )
                .build());

        // Tab 2: Todas
        boolean isAllActive = filter == CrateModality.ALL;
        inv.setItem(4, new ItemBuilder(isAllActive ? Material.NETHER_STAR : Material.ENDER_EYE)
                .name("<gold><b>🌐 All Crates</b></gold>")
                .lore(
                        "<gray>View the full catalog",
                        "<gray>for every game mode.",
                        "",
                        isAllActive ? "<green>✔ Active Filter" : "<yellow>Click to view all"
                )
                .build());

        // Tab 3: Slimefun & Tech
        boolean isSfActive = filter == CrateModality.SLIMEFUN;
        inv.setItem(6, new ItemBuilder(isSfActive ? Material.AMETHYST_SHARD : Material.REDSTONE_BLOCK)
                .name("<light_purple><b>⚡ Slimefun & Technology</b></light_purple>")
                .lore(
                        "<gray>Technology and magic crates",
                        "<gray>with advanced Slimefun items.",
                        "",
                        isSfActive ? "<green>✔ Active Filter" : "<yellow>Click to filter"
                )
                .build());

        // Info item on slot 8
        boolean inClasico = modalityManager.isClasico(player);
        inv.setItem(8, new ItemBuilder(inClasico ? Material.OAK_SAPLING : Material.BEACON)
                .name("<yellow><b>Your Current Mode</b></yellow>")
                .lore(
                        inClasico ? "<green>You are in: <b>Classic (Vanilla)</b></green>"
                                  : "<aqua>You are in: <b>Slimefun / Tech</b></aqua>",
                        "<gray>World: <white>" + player.getWorld().getName() + "</white>",
                        "",
                        inClasico ? "<red>⚠ Slimefun is restricted in this world.</red>"
                                  : "<green>✔ Slimefun is allowed in this world.</green>"
                )
                .build());

        // Fill crates in slots 10-43
        List<Crate> list = new ArrayList<>(crateRepository.getAllCrates());
        int[] crateSlots = {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
        };

        int idx = 0;
        for (Crate crate : list) {
            if (idx >= crateSlots.length) break;

            // Apply filter
            if (filter == CrateModality.CLASICO && (crate.getModality() == CrateModality.SLIMEFUN || crate.hasSlimefunRewards())) {
                continue;
            }
            if (filter == CrateModality.SLIMEFUN && crate.getModality() == CrateModality.CLASICO) {
                continue;
            }

            int slot = crateSlots[idx++];
            holder.slotMap.put(slot, crate);

            inv.setItem(slot, buildCrateIcon(player, crate));
        }

        // Close / Help button on 49
        inv.setItem(49, new ItemBuilder(Material.BARRIER)
                .name("<red><b>Close Menu</b></red>")
                .lore("<gray>Click to exit.</gray>")
                .build());

        player.openInventory(inv);
    }

    private ItemStack buildCrateIcon(Player player, Crate crate) {
        ItemStack base = crate.getPreviewItem() != null ? crate.getPreviewItem().clone() : new ItemStack(Material.CHEST);
        boolean inClasico = modalityManager.isClasico(player);
        boolean allowed = modalityManager.canOpenCrate(player, crate);

        // Key counts
        Optional<Key> reqKey = crateRepository.findRequiredKeyForCrate(crate.getId());
        int physicalKeys = 0;
        int virtualKeys = 0;
        String keyName = "Free";

        if (reqKey.isPresent()) {
            keyName = reqKey.get().getId();
            physicalKeys = countPhysicalKeys(player, reqKey.get());
            virtualKeys = virtualKeyRepository.getBalance(player.getUniqueId(), reqKey.get().getId());
        }

        List<String> lore = new ArrayList<>();
        lore.add("<gray>Mode: " + crate.getModality().getDisplayName());

        if (crate.hasSlimefunRewards()) {
            lore.add("<light_purple>⚡ Contains Slimefun items</light_purple>");
        } else {
            lore.add("<green>🌱 100% Vanilla Content</green>");
        }

        lore.add("");

        if (!allowed) {
            lore.add("<red><b>🔒 LOCKED IN CLASSIC</b></red>");
            lore.add("<gray>Not allowed in Classic (100% Vanilla).</gray>");
            lore.add("<gray>Travel to Skyblock or Oneblock to open it.</gray>");
        } else {
            lore.add("<green><b>✔ Available to open</b></green>");
        }

        lore.add("");
        if (crate.getType() != CrateType.FREE) {
            lore.add("<gray>Physical Keys: <yellow>" + physicalKeys + "</yellow>");
            lore.add("<gray>Virtual Keys: <aqua>" + virtualKeys + "</aqua>");
            lore.add("<gray>Total Keys: <gold><b>" + (physicalKeys + virtualKeys) + "</b></gold>");
        } else {
            lore.add("<green>This Crate is Free!</green>");
        }

        lore.add("");
        if (allowed) {
            lore.add("<yellow>Left Click:</yellow> <white>Spin Roulette (1x)</white>");
            lore.add("<gold>Shift + Click:</gold> <white>Instant Quick Open</white>");
        }
        lore.add("<aqua>Right Click:</aqua> <white>Preview Rewards</white>");

        ItemBuilder builder = new ItemBuilder(base.getType())
                .name(crate.getDisplayName())
                .lore(lore.toArray(new String[0]));

        if (!allowed) {
            builder.type(Material.IRON_CHAIN);
        } else if (physicalKeys + virtualKeys > 0 || crate.getType() == CrateType.FREE) {
            builder.glowing();
        }

        return builder.build();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getView().getTopInventory().getHolder() instanceof VirtualMenuHolder holder)) return;

        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 54) return;

        // Tab filters
        if (slot == 2) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
            openMenu(player, CrateModality.CLASICO);
            return;
        }
        if (slot == 4) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
            openMenu(player, CrateModality.ALL);
            return;
        }
        if (slot == 6) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
            openMenu(player, CrateModality.SLIMEFUN);
            return;
        }
        if (slot == 49) {
            player.closeInventory();
            return;
        }

        Crate crate = holder.slotMap.get(slot);
        if (crate == null) return;

        // Right-click: Preview
        if (event.isRightClick()) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.5f);
            previewManager.openPreview(player, crate);
            return;
        }

        // Left-click: Open
        if (event.isLeftClick()) {
            // Check modality restriction first!
            if (!modalityManager.canOpenCrate(player, crate)) {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                MessageUtils.send(player, modalityManager.getDeniedReason(player, crate));
                return;
            }

            if (rouletteAnimation.isOpening(player)) {
                MessageUtils.send(player, "<red>You are already opening a crate.</red>");
                return;
            }

            // Key check & consumption
            boolean isFast = event.isShiftClick();
            handleVirtualOpen(player, crate, isFast);
        }
    }

    private void handleVirtualOpen(Player player, Crate crate, boolean fastOpen) {
        Optional<Key> reqKey = crateRepository.findRequiredKeyForCrate(crate.getId());
        ItemStack physicalKey = null;
        boolean useVirtualKey = false;

        if (crate.getType() != CrateType.FREE) {
            if (reqKey.isEmpty()) {
                MessageUtils.send(player, "<red>Invalid key configuration for this crate.</red>");
                return;
            }

            Key key = reqKey.get();
            physicalKey = findMatchingKeyInInventory(player, key);

            if (physicalKey != null) {
                // Use physical key
            } else if (virtualKeyRepository.getBalance(player.getUniqueId(), key.getId()) >= 1) {
                useVirtualKey = true;
            } else {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
                MessageUtils.send(player, "<red>You do not have enough keys to open this crate (" + key.getId() + ").</red>");
                return;
            }
        }

        // Execute OpenCrateUseCase
        OpenResult result = openCrateUseCase.execute(player, crate, physicalKey);
        if (!result.isSuccess()) {
            MessageUtils.send(player, "<red>" + result.getMessage() + "</red>");
            return;
        }

        // Deduct Key Transactionally
        if (useVirtualKey) {
            if (!virtualKeyRepository.takeKeys(player.getUniqueId(), reqKey.get().getId(), 1)) {
                MessageUtils.send(player, "<red>The virtual key could not be deducted. Operation cancelled.</red>");
                return;
            }
        } else if (physicalKey != null) {
            consumeOneKey(physicalKey);
        }

        Reward winReward = result.getWinningReward();

        if (fastOpen) {
            // Instant delivery
            player.closeInventory();
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);
            rouletteAnimation.grantReward(player, winReward);
            MessageUtils.send(player, "<gold>✦ <b>Quick Open!</b> Reward won: </gold>" + winReward.getId());
        } else {
            // Roulette Animation
            player.closeInventory();
            if (!rouletteAnimation.start(player, crate, winReward)) {
                // Refund if failed to start
                if (useVirtualKey) {
                    virtualKeyRepository.addKeys(player.getUniqueId(), reqKey.get().getId(), 1);
                } else if (physicalKey != null) {
                    player.getInventory().addItem(reqKey.get().getItem());
                }
                MessageUtils.send(player, "<red>The animation could not start. Your key was returned.</red>");
            }
        }
    }

    private int countPhysicalKeys(Player player, Key key) {
        int count = 0;
        ItemStack target = key.getItem();
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.isSimilar(target)) {
                count += item.getAmount();
            }
        }
        return count;
    }

    private ItemStack findMatchingKeyInInventory(Player player, Key key) {
        ItemStack target = key.getItem();
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack != null && stack.getAmount() >= 1 && stack.isSimilar(target)) {
                return stack;
            }
        }
        return null;
    }

    private void consumeOneKey(ItemStack stack) {
        int amount = stack.getAmount();
        if (amount <= 1) {
            stack.setAmount(0);
        } else {
            stack.setAmount(amount - 1);
        }
    }

    private static final class VirtualMenuHolder implements InventoryHolder {
        private final CrateModality currentFilter;
        private final Map<Integer, Crate> slotMap = new HashMap<>();

        public VirtualMenuHolder(CrateModality currentFilter) {
            this.currentFilter = currentFilter;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
