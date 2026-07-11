package io.github.zefarie.herbalis.infrastructure.listener;

import io.github.zefarie.herbalis.infrastructure.effects.BlackoutService;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Fige les joueurs en blackout : la tete peut tourner, le corps reste au sol.
 */
public final class MoveListener implements Listener {

    private final BlackoutService blackout;

    public MoveListener(BlackoutService blackout) {
        this.blackout = blackout;
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!blackout.isBlackedOut(event.getPlayer().getUniqueId())) {
            return;
        }
        if (!event.hasChangedPosition()) {
            return;
        }
        Location locked = event.getFrom().clone();
        locked.setYaw(event.getTo().getYaw());
        locked.setPitch(event.getTo().getPitch());
        event.setTo(locked);
    }
}
