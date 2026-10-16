package me.jackstar.drakescrates.presentation.animation;

import me.jackstar.drakescraft.utils.MessageUtils;
import me.jackstar.drakescrates.compat.SlimefunHook;
import me.jackstar.drakescrates.domain.models.Crate;
import me.jackstar.drakescrates.domain.models.Reward;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

public class RouletteAnimation implements CrateAnimation, Listener {

    private static final int INVENTORY_SIZE = 27;
    private static final int ROLL_START_SLOT = 9;
    private static final int ROLL_END_SLOT = 17;
    private static final int WIN_SLOT = 13;
    private static final int WINDOW_SIZE = 9;

    private final JavaPlugin plugin;
    private final int totalSteps;
    private final long tickPeriod;
    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();

    public RouletteAnimation(JavaPlugin plugin, int totalSteps, long tickPeriod) {
        this.plugin = plugin;
        this.totalSteps = Math.max(20, totalSteps);
        this.tickPeriod = Math.max(1L, tickPeriod);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean start(Player player, Crate crate, Reward winReward) {
        if (player == null || !player.isOnline() || crate == null || winReward == null) {
            return false;
        }

        UUID playerId = player.getUniqueId();
        if (sessions.containsKey(playerId)) {
            return false;
        }

        Inventory inventory = Bukkit.createInventory(null, INVENTORY_SIZE,
                MessageUtils.parse("<gradient:gold:yellow><b>Opening:</b></gradient> " + safeId(crate.getId())));
        setupFrame(inventory);

        Session session = new Session(playerId, inventory, crate, winReward);
        sessions.put(playerId, session);

        player.openInventory(inventory);
        session.start();
        return true;
    }

    @Override
    public boolean isOpening(Player player) {
        return player != null && sessions.containsKey(player.getUniqueId());
    }

    public void grantReward(Player player, Reward reward) {
        if (player == null || !player.isOnline() || reward == null) return;
        dispatchRewardCommands(player, reward);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Session session = sessions.get(player.getUniqueId());
        if (session != null && event.getView().getTopInventory().equals(session.inventory)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Session session = sessions.get(player.getUniqueId());
        if (session != null && event.getView().getTopInventory().equals(session.inventory)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        Session session = sessions.get(player.getUniqueId());
        if (session == null || session.finishing) return;
        if (event.getInventory().equals(session.inventory)) {
            session.finish(true);
        }
    }

    public void shutdown() {
        for (Session session : sessions.values()) {
            session.cancelTask();
        }
        sessions.clear();
        HandlerList.unregisterAll(this);
    }

    private void setupFrame(Inventory inventory) {
        ItemStack frame = createFrameItem();
        for (int slot = 0; slot < INVENTORY_SIZE; slot++) {
            if (slot < ROLL_START_SLOT || slot > ROLL_END_SLOT) {
                inventory.setItem(slot, frame.clone());
            }
        }
        inventory.setItem(WIN_SLOT - 9, createMarkerItem());
        inventory.setItem(WIN_SLOT + 9, createMarkerItem());
    }

    private ItemStack createFrameItem() {
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.displayName(MessageUtils.parse("<dark_gray> "));
            pane.setItemMeta(meta);
        }
        return pane;
    }

    private ItemStack createMarkerItem() {
        ItemStack marker = new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        ItemMeta meta = marker.getItemMeta();
        if (meta != null) {
            meta.displayName(MessageUtils.parse("<aqua><b>WINNING REWARD</b></aqua>"));
            marker.setItemMeta(meta);
        }
        return marker;
    }

    private ItemStack chooseRandomRewardDisplay(Crate crate) {
        List<Reward> rewards = crate.getRewards();
        Reward reward = rewards.get(ThreadLocalRandom.current().nextInt(rewards.size()));
        return reward.getDisplayItem().clone();
    }

    private void dispatchRewardCommands(Player player, Reward reward) {
        // 1. Direct Item Delivery (Slimefun or Vanilla)
        ItemStack rewardItem = reward.createRewardItem();
        if (rewardItem != null && !rewardItem.getType().isAir()) {
            giveRewardItem(player, rewardItem);
        }

        // 2. Extra Commands Execution
        List<String> commands = reward.getCommands();
        if (commands != null && !commands.isEmpty()) {
            for (String rawCommand : commands) {
                try {
                    if (rawCommand == null || rawCommand.isBlank()) continue;
                    String command = rawCommand.replace("%player%", player.getName());
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                } catch (Exception ex) {
                    plugin.getLogger().log(Level.WARNING,
                            "Failed to run reward command '" + rawCommand + "' for player '" + player.getName() + "'.", ex);
                }
            }
        }
    }

    private String safeId(String id) {
        return id == null ? "crate" : id.toLowerCase(Locale.ROOT);
    }

    private void giveRewardItem(Player player, ItemStack item) {
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(item);
        if (leftovers.isEmpty()) return;

        leftovers.values().forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
        MessageUtils.send(player, "<yellow>Your inventory was full; part of the reward dropped at your feet.</yellow>");
    }

    private final class Session {
        private final UUID playerId;
        private final Inventory inventory;
        private final Crate crate;
        private final Reward winReward;
        private final List<ItemStack> ribbon;
        private BukkitRunnable task;
        private int step;
        private boolean finishing;

        private Session(UUID playerId, Inventory inventory, Crate crate, Reward winReward) {
            this.playerId = playerId;
            this.inventory = inventory;
            this.crate = crate;
            this.winReward = winReward;
            this.ribbon = buildRibbon(crate, winReward);
        }

        private void start() {
            this.task = new BukkitRunnable() {
                @Override
                public void run() {
                    Player player = Bukkit.getPlayer(playerId);
                    if (player == null || !player.isOnline()) {
                        finish(false);
                        return;
                    }

                    try {
                        renderWindow(step);
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.6F, 1.7F);

                        if (step >= totalSteps) {
                            finish(true);
                            return;
                        }
                        step++;
                    } catch (Exception ex) {
                        plugin.getLogger().log(Level.WARNING,
                                "Roulette animation failed for player '" + player.getName() + "'.", ex);
                        finish(true);
                    }
                }
            };
            this.task.runTaskTimer(plugin, 0L, tickPeriod);
        }

        private void renderWindow(int currentStep) {
            for (int i = 0; i < WINDOW_SIZE; i++) {
                inventory.setItem(ROLL_START_SLOT + i, ribbon.get(currentStep + i).clone());
            }
        }

        private List<ItemStack> buildRibbon(Crate crate, Reward winReward) {
            int totalSize = totalSteps + WINDOW_SIZE;
            int winIndex = totalSteps + 4;
            List<ItemStack> items = new ArrayList<>(totalSize);
            for (int i = 0; i < totalSize; i++) {
                items.add(chooseRandomRewardDisplay(crate));
            }
            items.set(winIndex, winReward.getDisplayItem().clone());
            return items;
        }

        private void finish(boolean grantReward) {
            if (finishing) return;
            finishing = true;
            cancelTask();

            sessions.remove(playerId);

            Player player = Bukkit.getPlayer(playerId);
            if (player == null || !player.isOnline()) return;

            if (grantReward) {
                try {
                    dispatchRewardCommands(player, winReward);
                    MessageUtils.send(player, "<gold>✦ <b>Winner!</b> Reward: <yellow>" + safeId(winReward.getId()) + "</yellow></gold>");
                    player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 1.2F);
                } catch (Exception ex) {
                    plugin.getLogger().log(Level.WARNING,
                            "Failed to grant reward '" + safeId(winReward.getId()) + "' to " + player.getName() + ".", ex);
                }
            }

            Bukkit.getScheduler().runTask(plugin, (Runnable) player::closeInventory);
        }

        private void cancelTask() {
            if (task != null) {
                task.cancel();
                task = null;
            }
        }
    }
}
