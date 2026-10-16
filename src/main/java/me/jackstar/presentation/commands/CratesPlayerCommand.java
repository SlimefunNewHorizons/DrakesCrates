package me.jackstar.drakescrates.presentation.commands;

import me.jackstar.drakescraft.utils.MessageUtils;
import me.jackstar.drakescrates.application.repositories.VirtualKeyRepository;
import me.jackstar.drakescrates.presentation.gui.VirtualCrateMenu;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class CratesPlayerCommand implements CommandExecutor {

    private final VirtualCrateMenu virtualCrateMenu;
    private final VirtualKeyRepository virtualKeyRepository;

    public CratesPlayerCommand(VirtualCrateMenu virtualCrateMenu, VirtualKeyRepository virtualKeyRepository) {
        this.virtualCrateMenu = virtualCrateMenu;
        this.virtualKeyRepository = virtualKeyRepository;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                             @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtils.send(sender, "<red>Only players can run this command.</red>");
            return true;
        }

        if (args.length > 0 && "balance".equalsIgnoreCase(args[0])) {
            Map<String, Integer> balances = virtualKeyRepository.getAllBalances(player.getUniqueId());
            MessageUtils.send(player, "<gold>✦ <b>Your Virtual Keys:</b></gold>");
            if (balances.isEmpty()) {
                MessageUtils.send(player, "<gray>You do not have any virtual keys right now.</gray>");
            } else {
                balances.forEach((id, bal) -> MessageUtils.send(player, "<gray>• <yellow>" + id + "</yellow>: <aqua>" + bal + "</aqua></gray>"));
            }
            return true;
        }

        // Default: Open virtual crates menu
        virtualCrateMenu.openMenu(player);
        return true;
    }
}
