package me.jackstar.drakescrates.presentation.commands;

import me.jackstar.drakescraft.utils.MessageUtils;
import me.jackstar.drakescrates.application.repositories.CrateRepository;
import me.jackstar.drakescrates.application.repositories.VirtualKeyRepository;
import me.jackstar.drakescrates.domain.models.Key;
import me.jackstar.drakescrates.presentation.editor.CrateEditorManager;
import me.jackstar.drakescrates.presentation.gui.VirtualCrateMenu;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class DrakesCratesCommand implements CommandExecutor {

    private final CrateRepository crateRepository;
    private final VirtualKeyRepository virtualKeyRepository;
    private final CrateEditorManager crateEditorManager;
    private final VirtualCrateMenu virtualCrateMenu;
    private final Runnable reloadAction;

    public DrakesCratesCommand(CrateRepository crateRepository, VirtualKeyRepository virtualKeyRepository,
                               CrateEditorManager crateEditorManager, VirtualCrateMenu virtualCrateMenu,
                               Runnable reloadAction) {
        this.crateRepository = crateRepository;
        this.virtualKeyRepository = virtualKeyRepository;
        this.crateEditorManager = crateEditorManager;
        this.virtualCrateMenu = virtualCrateMenu;
        this.reloadAction = reloadAction;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                             @NotNull String[] args) {
        if (!sender.hasPermission("drakescrates.admin")) {
            // If normal player types /crates or /dc without args, open virtual menu!
            if (sender instanceof Player player) {
                if (virtualCrateMenu != null) {
                    virtualCrateMenu.openMenu(player);
                    return true;
                }
            }
            MessageUtils.send(sender, "<red>You do not have permission to use this command.</red>");
            return true;
        }

        if (args.length == 0) {
            if (sender instanceof Player player && virtualCrateMenu != null) {
                virtualCrateMenu.openMenu(player);
                return true;
            }
            sendUsage(sender, label);
            return true;
        }

        String sub = args[0].toLowerCase();

        if ("menu".equals(sub)) {
            if (!(sender instanceof Player player)) {
                MessageUtils.send(sender, "<red>Only players can open the menu.</red>");
                return true;
            }
            if (virtualCrateMenu != null) virtualCrateMenu.openMenu(player);
            return true;
        }

        if ("reload".equals(sub)) {
            if (reloadAction != null) {
                reloadAction.run();
            } else {
                crateRepository.reload();
            }
            MessageUtils.send(sender, "<green>DrakesCrates reloaded successfully.</green>");
            return true;
        }

        if ("givekey".equals(sub)) {
            return handleGiveKey(sender, label, args);
        }

        if ("givevirtualkey".equals(sub) || "givevk".equals(sub)) {
            return handleGiveVirtualKey(sender, label, args);
        }

        if ("takekey".equals(sub)) {
            return handleTakeKey(sender, label, args);
        }

        if ("balance".equals(sub) || "keys".equals(sub)) {
            return handleBalance(sender, label, args);
        }

        if ("editor".equals(sub)) {
            if (!(sender instanceof Player player)) {
                MessageUtils.send(sender, "<red>Only players can open the editor.</red>");
                return true;
            }
            crateEditorManager.openEditor(player, args.length > 1 ? args[1] : null);
            return true;
        }

        sendUsage(sender, label);
        return true;
    }

    private boolean handleGiveKey(CommandSender sender, String label, String[] args) {
        if (args.length < 4) {
            MessageUtils.send(sender, "<red>Usage: /" + label + " givekey <player> <key_id> <amount> [physical|virtual]</red>");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null || !target.isOnline()) {
            MessageUtils.send(sender, "<red>Player not found or offline.</red>");
            return true;
        }

        String keyId = args[2].toLowerCase();
        Key key = crateRepository.findKeyById(keyId).orElse(null);
        if (key == null) {
            MessageUtils.send(sender, "<red>Unknown key ID: <gray>" + keyId + "</gray></red>");
            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[3]);
        } catch (NumberFormatException ex) {
            MessageUtils.send(sender, "<red>Amount must be a valid whole number.</red>");
            return true;
        }
        if (amount < 1) {
            MessageUtils.send(sender, "<red>Amount must be at least 1.</red>");
            return true;
        }

        boolean isVirtual = args.length > 4 && "virtual".equalsIgnoreCase(args[4]);

        if (isVirtual) {
            virtualKeyRepository.addKeys(target.getUniqueId(), keyId, amount);
            MessageUtils.send(sender, "<green>Delivered <yellow>" + amount + "</yellow> virtual key(s) <gray>(" + keyId + ")</gray> to <aqua>" + target.getName() + "</aqua>.</green>");
            MessageUtils.send(target, "<green>You received <yellow>" + amount + "</yellow> virtual key(s) <gray>(" + keyId + ")</gray>.</green>");
            return true;
        }

        ItemStack keyItem = key.getItem().clone();
        keyItem.setAmount(Math.min(amount, keyItem.getMaxStackSize()));
        int left = giveStacked(target, keyItem, amount);

        int delivered = amount - left;
        MessageUtils.send(sender, "<green>Delivered <yellow>" + delivered + "</yellow> physical key(s) <gray>(" + keyId + ")</gray> to <aqua>" + target.getName() + "</aqua>.</green>");

        if (left > 0) {
            MessageUtils.send(sender, "<red>" + left + " key(s) did not fit in the inventory and were added as virtual keys.</red>");
            virtualKeyRepository.addKeys(target.getUniqueId(), keyId, left);
            MessageUtils.send(target, "<yellow>" + left + " key(s) were converted to virtual keys because your inventory was full.</yellow>");
        } else {
            MessageUtils.send(target, "<green>You received <yellow>" + delivered + "</yellow> physical key(s) <gray>(" + keyId + ")</gray>.</green>");
        }
        return true;
    }

    private boolean handleGiveVirtualKey(CommandSender sender, String label, String[] args) {
        if (args.length < 4) {
            MessageUtils.send(sender, "<red>Usage: /" + label + " givevirtualkey <player> <key_id> <amount></red>");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null || !target.isOnline()) {
            MessageUtils.send(sender, "<red>Player not found or offline.</red>");
            return true;
        }

        String keyId = args[2].toLowerCase();
        int amount;
        try {
            amount = Integer.parseInt(args[3]);
        } catch (NumberFormatException ex) {
            MessageUtils.send(sender, "<red>Invalid amount.</red>");
            return true;
        }
        if (amount < 1) return true;

        virtualKeyRepository.addKeys(target.getUniqueId(), keyId, amount);
        MessageUtils.send(sender, "<green>Added <yellow>" + amount + "</yellow> virtual key(s) <gray>(" + keyId + ")</gray> to <aqua>" + target.getName() + "</aqua>.</green>");
        MessageUtils.send(target, "<green>You received <yellow>" + amount + "</yellow> virtual key(s) <gray>(" + keyId + ")</gray>.</green>");
        return true;
    }

    private boolean handleTakeKey(CommandSender sender, String label, String[] args) {
        if (args.length < 4) {
            MessageUtils.send(sender, "<red>Usage: /" + label + " takekey <player> <key_id> <amount></red>");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null || !target.isOnline()) {
            MessageUtils.send(sender, "<red>Player not found.</red>");
            return true;
        }

        String keyId = args[2].toLowerCase();
        int amount = Integer.parseInt(args[3]);

        if (virtualKeyRepository.takeKeys(target.getUniqueId(), keyId, amount)) {
            MessageUtils.send(sender, "<green>Removed <yellow>" + amount + "</yellow> virtual key(s) from " + target.getName() + ".</green>");
        } else {
            MessageUtils.send(sender, "<red>The player does not have enough virtual keys to remove.</red>");
        }
        return true;
    }

    private boolean handleBalance(CommandSender sender, String label, String[] args) {
        Player target = sender instanceof Player p ? p : null;
        if (args.length > 1) {
            target = Bukkit.getPlayerExact(args[1]);
        }
        if (target == null) {
            MessageUtils.send(sender, "<red>Specify a valid player.</red>");
            return true;
        }

        Map<String, Integer> balances = virtualKeyRepository.getAllBalances(target.getUniqueId());
        MessageUtils.send(sender, "<gold>✦ <b>Virtual Keys for " + target.getName() + ":</b></gold>");
        if (balances.isEmpty()) {
            MessageUtils.send(sender, "<gray>No virtual keys are registered.</gray>");
        } else {
            balances.forEach((id, bal) -> MessageUtils.send(sender, "<gray>• <yellow>" + id + "</yellow>: <aqua>" + bal + "</aqua></gray>"));
        }
        return true;
    }

    private int giveStacked(Player target, ItemStack baseItem, int totalAmount) {
        int left = totalAmount;
        int max = Math.max(1, baseItem.getMaxStackSize());

        while (left > 0) {
            int batch = Math.min(left, max);
            ItemStack stack = baseItem.clone();
            stack.setAmount(batch);
            var notFit = target.getInventory().addItem(stack);
            if (!notFit.isEmpty()) {
                int remainderAmount = 0;
                for (ItemStack remainder : notFit.values()) {
                    remainderAmount += remainder.getAmount();
                }
                int delivered = batch - remainderAmount;
                left -= delivered;
                return left;
            }
            left -= batch;
        }
        return 0;
    }

    private void sendUsage(CommandSender sender, String label) {
        MessageUtils.send(sender, "<yellow><b>DrakesCrates Commands:</b></yellow>");
        MessageUtils.send(sender, "<gray>/" + label + " menu - Opens the virtual crates menu</gray>");
        MessageUtils.send(sender, "<gray>/" + label + " givekey <player> <key_id> <amount> [physical|virtual]</gray>");
        MessageUtils.send(sender, "<gray>/" + label + " givevirtualkey <player> <key_id> <amount></gray>");
        MessageUtils.send(sender, "<gray>/" + label + " takekey <player> <key_id> <amount></gray>");
        MessageUtils.send(sender, "<gray>/" + label + " balance [player]</gray>");
        MessageUtils.send(sender, "<gray>/" + label + " editor [crate_id]</gray>");
        MessageUtils.send(sender, "<gray>/" + label + " reload</gray>");
    }
}
