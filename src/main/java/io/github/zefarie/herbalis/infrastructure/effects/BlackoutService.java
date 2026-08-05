package io.github.zefarie.herbalis.infrastructure.effects;

import io.github.zefarie.herbalis.domain.drug.DrugType;
import io.github.zefarie.herbalis.domain.drug.SlurStyle;
import io.github.zefarie.herbalis.infrastructure.config.Messages;
import io.github.zefarie.herbalis.infrastructure.fx.Fx;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Blackout apres abus : ecran noir, joueur cloue au sol, camera qui
 * tangue, reveil progressif. Le listener de mouvement fige le joueur
 * tant que {@link #isBlackedOut} est vrai.
 */
public final class BlackoutService {

    private record Blackout(long until, long nextPulseAt, SlurStyle slurStyle) {
    }

    private final Messages messages;
    private final Fx fx;
    private final Map<UUID, Blackout> active = new ConcurrentHashMap<>();

    public BlackoutService(Messages messages, Fx fx) {
        this.messages = messages;
        this.fx = fx;
    }

    public boolean isBlackedOut(UUID playerId) {
        return active.containsKey(playerId);
    }

    /** Style de chat deforme du blackout en cours, sinon {@link SlurStyle#NONE}. */
    public SlurStyle slurStyle(UUID playerId) {
        Blackout blackout = active.get(playerId);
        return blackout == null ? SlurStyle.NONE : blackout.slurStyle();
    }

    public void start(Player player, DrugType drug, long now) {
        long durationMillis = drug.consumption().blackoutDuration().toMillis();
        active.put(player.getUniqueId(), new Blackout(now + durationMillis, now,
                drug.effects().chatSlur().style()));

        int ticks = (int) (durationMillis / 50) + 60;
        PotionEffects.one(player, "minecraft:blindness", ticks, 0);
        PotionEffects.one(player, "minecraft:darkness", ticks, 0);
        PotionEffects.one(player, "minecraft:slowness", ticks, 4);

        player.showTitle(Title.title(
                messages.msg("blackout.title"),
                messages.msg("blackout.sous-titre"),
                Title.Times.times(Duration.ofMillis(400),
                        Duration.ofMillis(durationMillis), Duration.ofSeconds(2))));
        fx.blackoutHit(player);
        fx.heartbeat(player);
    }

    /** A appeler chaque seconde : pulsations de nausee puis reveil. */
    public void tick(Player player, long now) {
        Blackout blackout = active.get(player.getUniqueId());
        if (blackout == null) {
            return;
        }
        if (now >= blackout.until()) {
            wake(player);
            return;
        }
        if (now >= blackout.nextPulseAt()) {
            PotionEffects.one(player, "minecraft:nausea", 120, 0);
            fx.heartbeat(player);
            active.put(player.getUniqueId(),
                    new Blackout(blackout.until(), now + 4_000, blackout.slurStyle()));
        }
    }

    /** Reapplique l'ecran noir a un joueur qui se reconnecte en plein blackout. */
    public void reapply(Player player, long now) {
        Blackout blackout = active.get(player.getUniqueId());
        if (blackout == null || now >= blackout.until()) {
            active.remove(player.getUniqueId());
            return;
        }
        int ticks = (int) ((blackout.until() - now) / 50) + 60;
        PotionEffects.one(player, "minecraft:blindness", ticks, 0);
        PotionEffects.one(player, "minecraft:darkness", ticks, 0);
        PotionEffects.one(player, "minecraft:slowness", ticks, 4);
    }

    private void wake(Player player) {
        active.remove(player.getUniqueId());
        PotionEffects.clear(player, "minecraft:blindness");
        PotionEffects.clear(player, "minecraft:darkness");
        PotionEffects.clear(player, "minecraft:slowness");

        // Reveil vaseux : quelques secondes de flottement.
        PotionEffects.one(player, "minecraft:nausea", 160, 0);
        PotionEffects.one(player, "minecraft:slowness", 160, 0);

        player.showTitle(Title.title(
                messages.msg("blackout.reveil-titre"),
                messages.msg("blackout.reveil-sous-titre"),
                Title.Times.times(Duration.ofSeconds(1),
                        Duration.ofSeconds(3), Duration.ofSeconds(2))));
        player.sendMessage(messages.msg("blackout.reveil-message"));
        fx.wakeUp(player);
    }
}
