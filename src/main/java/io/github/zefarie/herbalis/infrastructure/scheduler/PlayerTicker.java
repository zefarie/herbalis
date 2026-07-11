package io.github.zefarie.herbalis.infrastructure.scheduler;

import io.github.zefarie.herbalis.infrastructure.effects.EffectService;
import io.github.zefarie.herbalis.infrastructure.effects.WithdrawalService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Tick d'une seconde par joueur : sessions d'effets et blackout a chaque
 * passage, verification du manque toutes les cinq secondes.
 */
public final class PlayerTicker implements Runnable {

    private final EffectService effects;
    private final WithdrawalService withdrawal;
    private int counter;

    public PlayerTicker(EffectService effects, WithdrawalService withdrawal) {
        this.effects = effects;
        this.withdrawal = withdrawal;
    }

    @Override
    public void run() {
        long now = System.currentTimeMillis();
        boolean checkWithdrawal = ++counter % 5 == 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            effects.tick(player, now);
            if (checkWithdrawal) {
                withdrawal.tick(player, now);
            }
        }
    }
}
