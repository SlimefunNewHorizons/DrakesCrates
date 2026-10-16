package me.jackstar.drakescrates.application.usecases;

import me.jackstar.drakescrates.domain.modality.ModalityManager;
import me.jackstar.drakescrates.domain.models.Crate;
import me.jackstar.drakescrates.domain.models.CrateType;
import me.jackstar.drakescrates.domain.models.OpenResult;
import me.jackstar.drakescrates.domain.models.Reward;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Random;

public class OpenCrateUseCase {

    private final ModalityManager modalityManager;
    private final Random random = new Random();

    public OpenCrateUseCase(ModalityManager modalityManager) {
        this.modalityManager = modalityManager;
    }

    public OpenResult execute(Player player, Crate crate, ItemStack keyItem) {
        // 1. Modality & Vanilla Guard Validation
        if (modalityManager != null && !modalityManager.canOpenCrate(player, crate)) {
            return OpenResult.failure("This crate is not allowed in your current mode (Classic is 100% vanilla).");
        }

        // 2. Validate Key if Physical
        if (crate.getType() == CrateType.PHYSICAL_KEY && keyItem != null) {
            if (keyItem.getAmount() < 1) {
                return OpenResult.failure("You need a key to open this crate!");
            }
        }

        // 3. Select Reward (Weighted Random)
        Reward reward = selectReward(crate.getRewards());
        if (reward == null) {
            return OpenResult.failure("No reward selected (Configuration error?)");
        }

        // 4. Clásico failsafe: If player is in Clásico and reward is Slimefun, block delivery
        if (modalityManager != null && modalityManager.isClasico(player) && reward.isSlimefun()) {
            return OpenResult.failure("Security error: Slimefun rewards cannot be delivered in Classic.");
        }

        // 5. Return Success
        return OpenResult.success(reward);
    }

    private Reward selectReward(List<Reward> rewards) {
        if (rewards == null || rewards.isEmpty())
            return null;

        double totalWeight = 0.0;
        for (Reward r : rewards) {
            totalWeight += r.getChance();
        }

        double randomValue = random.nextDouble() * totalWeight;
        double currentWeight = 0.0;

        for (Reward r : rewards) {
            currentWeight += r.getChance();
            if (randomValue <= currentWeight) {
                return r;
            }
        }
        return rewards.get(0); // Fallback
    }
}
