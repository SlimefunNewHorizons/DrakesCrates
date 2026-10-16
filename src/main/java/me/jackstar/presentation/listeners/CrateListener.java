package me.jackstar.drakescrates.presentation.listeners;

import me.jackstar.drakescraft.utils.MessageUtils;
import me.jackstar.drakescrates.application.repositories.CrateRepository;
import me.jackstar.drakescrates.application.repositories.VirtualKeyRepository;
import me.jackstar.drakescrates.application.usecases.OpenCrateUseCase;
import me.jackstar.drakescrates.domain.modality.ModalityManager;
import me.jackstar.drakescrates.domain.models.Crate;
import me.jackstar.drakescrates.domain.models.CrateType;
import me.jackstar.drakescrates.domain.models.Key;
import me.jackstar.drakescrates.domain.models.OpenResult;
import me.jackstar.drakescrates.presentation.animation.CrateAnimation;
import me.jackstar.drakescrates.presentation.editor.CratePreviewManager;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public class CrateListener implements Listener {

    private final CrateRepository crateRepository;
    private final VirtualKeyRepository virtualKeyRepository;
    private final ModalityManager modalityManager;
    private final OpenCrateUseCase openCrateUseCase;
    private CrateAnimation crateAnimation;
    private final CratePreviewManager cratePreviewManager;

    public CrateListener(CrateRepository crateRepository, VirtualKeyRepository virtualKeyRepository,
                         ModalityManager modalityManager, OpenCrateUseCase openCrateUseCase,
                         CrateAnimation crateAnimation, CratePreviewManager cratePreviewManager) {
        this.crateRepository = crateRepository;
        this.virtualKeyRepository = virtualKeyRepository;
        this.modalityManager = modalityManager;
        this.openCrateUseCase = openCrateUseCase;
        this.crateAnimation = crateAnimation;
        this.cratePreviewManager = cratePreviewManager;
    }

    public void setCrateAnimation(CrateAnimation crateAnimation) {
        this.crateAnimation = crateAnimation;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if ((event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.LEFT_CLICK_BLOCK)
                || event.getClickedBlock() == null) {
            return;
        }

        final Player player = event.getPlayer();
        final Location clickedLocation = event.getClickedBlock().getLocation();
        final Optional<Crate> crateOptional = crateRepository.findCrateByLocation(clickedLocation);
        if (crateOptional.isEmpty()) return;

        event.setCancelled(true);
        Crate crate = crateOptional.get();

        // Left Click: Preview rewards
        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            cratePreviewManager.openPreview(player, crate);
            return;
        }

        // Modality & Clásico check!
        if (modalityManager != null && !modalityManager.canOpenCrate(player, crate)) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            MessageUtils.send(player, modalityManager.getDeniedReason(player, crate));
            return;
        }

        if (crateAnimation.isOpening(player)) {
            MessageUtils.send(player, "<red>You are already opening a crate.</red>");
            return;
        }

        ItemStack keyForValidation = null;
        boolean usingVirtualKey = false;
        Optional<Key> requiredKey = crateRepository.findRequiredKeyForCrate(crate.getId());

        if (crate.getType() != CrateType.FREE) {
            if (requiredKey.isEmpty()) {
                MessageUtils.send(player, "<red>This crate has an invalid key configuration.</red>");
                return;
            }

            keyForValidation = findMatchingKeyInInventory(player, requiredKey.get());
            if (keyForValidation == null) {
                // Check if player has virtual keys!
                if (virtualKeyRepository != null && virtualKeyRepository.getBalance(player.getUniqueId(), requiredKey.get().getId()) >= 1) {
                    usingVirtualKey = true;
                } else {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
                    MessageUtils.send(player, "<red>You need a physical or virtual key to open this crate.</red>");
                    return;
                }
            }
        }

        OpenResult result = openCrateUseCase.execute(player, crate, keyForValidation);
        if (!result.isSuccess()) {
            MessageUtils.send(player, "<red>" + result.getMessage() + "</red>");
            return;
        }

        if (!crateAnimation.start(player, crate, result.getWinningReward())) {
            MessageUtils.send(player, "<red>The crate could not be opened. Your key was not consumed.</red>");
            return;
        }

        // Consume key
        if (usingVirtualKey && requiredKey.isPresent()) {
            virtualKeyRepository.takeKeys(player.getUniqueId(), requiredKey.get().getId(), 1);
        } else if (keyForValidation != null) {
            consumeOneKey(keyForValidation);
        }
    }

    private ItemStack findMatchingKeyInInventory(Player player, Key key) {
        ItemStack target = key.getItem();
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack == null || stack.getAmount() < 1) continue;
            if (stack.isSimilar(target)) return stack;
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
}
