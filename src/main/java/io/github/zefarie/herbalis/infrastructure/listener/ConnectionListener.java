package io.github.zefarie.herbalis.infrastructure.listener;

import io.github.zefarie.herbalis.infrastructure.effects.BlackoutService;
import io.github.zefarie.herbalis.infrastructure.effects.WithdrawalService;
import io.github.zefarie.herbalis.infrastructure.hud.HologramService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Connexions : reprise du blackout en cours, oubli des timers de manque
 * et nettoyage de l'hologramme du joueur. Les sessions d'effets reposent
 * sur des timestamps et reprennent d'elles-memes au tick suivant.
 */
public final class ConnectionListener implements Listener {

    private final BlackoutService blackout;
    private final WithdrawalService withdrawal;
    private final HologramService holograms;

    public ConnectionListener(BlackoutService blackout,
                              WithdrawalService withdrawal,
                              HologramService holograms) {
        this.blackout = blackout;
        this.withdrawal = withdrawal;
        this.holograms = holograms;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        blackout.reapply(event.getPlayer(), System.currentTimeMillis());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        withdrawal.forget(event.getPlayer().getUniqueId());
        holograms.hide(event.getPlayer());
    }
}
