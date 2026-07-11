package io.github.zefarie.herbalis.infrastructure.effects;

import io.github.zefarie.herbalis.domain.consumption.ConsumptionEngine;
import io.github.zefarie.herbalis.domain.consumption.EffectPhase;
import io.github.zefarie.herbalis.domain.consumption.EffectTimeline;
import io.github.zefarie.herbalis.domain.drug.DrugRegistry;
import io.github.zefarie.herbalis.domain.drug.DrugType;
import io.github.zefarie.herbalis.infrastructure.config.Messages;
import io.github.zefarie.herbalis.infrastructure.fx.Fx;
import io.github.zefarie.herbalis.infrastructure.persistence.SqliteSessionStore;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Deroule les sessions d'effets : montee par paliers, plateau avec fumee
 * et aura, descente, fin. Tick une fois par seconde sur le thread principal.
 */
public final class EffectService {

    private static final class Session {
        final String drugId;
        final EffectTimeline timeline;
        final long startedAt;
        int riseStep;
        boolean comedownAnnounced;
        long nextAmbientAt;

        Session(String drugId, EffectTimeline timeline, long startedAt) {
            this.drugId = drugId;
            this.timeline = timeline;
            this.startedAt = startedAt;
        }
    }

    private final Messages messages;
    private final Fx fx;
    private final DrugRegistry drugs;
    private final SqliteSessionStore store;
    private final BlackoutService blackout;
    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();

    public EffectService(Messages messages, Fx fx, DrugRegistry drugs,
                         SqliteSessionStore store, BlackoutService blackout) {
        this.messages = messages;
        this.fx = fx;
        this.drugs = drugs;
        this.store = store;
        this.blackout = blackout;
    }

    /** Demarre une session apres consommation (ou un blackout en cas d'abus). */
    public void start(Player player, DrugType drug,
                      ConsumptionEngine.ConsumeOutcome outcome, long now) {
        if (outcome.blackout()) {
            sessions.remove(player.getUniqueId());
            store.delete(player.getUniqueId());
            blackout.start(player, drug, now);
            return;
        }
        Session session = new Session(drug.id(), outcome.timeline(), now);
        session.nextAmbientAt = now + ambientDelay();
        sessions.put(player.getUniqueId(), session);
        store.save(player.getUniqueId(),
                new SqliteSessionStore.StoredSession(drug.id(), outcome.timeline(), now));

        fx.jointLit(player);
        player.sendActionBar(messages.msg("consommation.allumage"));
    }

    /** Reprend les sessions persistees (demarrage, reconnexion). */
    public void restore(UUID playerId, SqliteSessionStore.StoredSession stored, long now) {
        EffectPhase phase = stored.timeline().phaseAt(now - stored.startedAt());
        if (phase == EffectPhase.DONE) {
            store.delete(playerId);
            return;
        }
        Session session = new Session(stored.drugId(), stored.timeline(), stored.startedAt());
        // On ne rejoue pas les paliers deja passes.
        session.riseStep = switch (phase) {
            case RISE -> 0;
            default -> 3;
        };
        session.comedownAnnounced = phase == EffectPhase.COMEDOWN;
        session.nextAmbientAt = now + ambientDelay();
        sessions.put(playerId, session);
    }

    public boolean hasSession(UUID playerId) {
        return sessions.containsKey(playerId);
    }

    /** Tick d'une seconde pour un joueur en ligne. */
    public void tick(Player player, long now) {
        blackout.tick(player, now);

        Session session = sessions.get(player.getUniqueId());
        if (session == null) {
            return;
        }
        DrugType drug = drugs.byId(session.drugId).orElse(null);
        if (drug == null) {
            end(player, session, false);
            return;
        }

        long elapsed = now - session.startedAt;
        EffectTimeline timeline = session.timeline;
        int stars = timeline.quality().stars();

        switch (timeline.phaseAt(elapsed)) {
            case RISE -> {
                double progress = timeline.riseProgress(elapsed);
                fx.smokePuff(player);
                if (progress >= 0.35 && session.riseStep < 1) {
                    session.riseStep = 1;
                    player.sendActionBar(messages.msg("consommation.montee-1"));
                }
                if (progress >= 0.7 && session.riseStep < 2) {
                    session.riseStep = 2;
                    player.sendActionBar(messages.msg("consommation.montee-2"));
                    // Premiers effets, encore timides.
                    PotionEffects.applyPersistent(player, drug.effects().highEffects(), 1, 100);
                }
            }
            case HIGH -> {
                if (session.riseStep < 3) {
                    session.riseStep = 3;
                    player.showTitle(Title.title(
                            messages.msg("consommation.high-titre"),
                            messages.msg("consommation.high-sous-titre"),
                            Title.Times.times(Duration.ofMillis(600),
                                    Duration.ofSeconds(3), Duration.ofMillis(1200))));
                }
                PotionEffects.applyPersistent(player, drug.effects().highEffects(), stars, 90);
                fx.smokePuff(player);
                fx.highAura(player, timeline.intensity());
                if (now >= session.nextAmbientAt) {
                    session.nextAmbientAt = now + ambientDelay();
                    player.sendActionBar(messages.random("consommation.ambiance-high"));
                }
            }
            case COMEDOWN -> {
                if (!session.comedownAnnounced) {
                    session.comedownAnnounced = true;
                    PotionEffects.remove(player, drug.effects().highEffects());
                    PotionEffects.applyOneShots(player,
                            drug.effects().comedownEffects(), stars, 160);
                    player.sendActionBar(messages.msg("consommation.descente"));
                    fx.comedownStart(player);
                }
                PotionEffects.applyPersistent(player, drug.effects().comedownEffects(), stars, 90);
                if (now >= session.nextAmbientAt) {
                    session.nextAmbientAt = now + ambientDelay();
                    player.sendActionBar(messages.random("consommation.ambiance-descente"));
                }
            }
            case DONE -> end(player, session, true);
        }
    }

    /** Le joueur se deconnecte : la session reste en base, timestamps a l'appui. */
    public void forget(UUID playerId) {
        sessions.remove(playerId);
    }

    private void end(Player player, Session session, boolean gracefully) {
        sessions.remove(player.getUniqueId());
        store.delete(player.getUniqueId());
        DrugType drug = drugs.byId(session.drugId).orElse(null);
        if (drug != null) {
            PotionEffects.remove(player, drug.effects().highEffects());
            PotionEffects.remove(player, drug.effects().comedownEffects());
        }
        if (gracefully) {
            player.sendActionBar(messages.msg("consommation.fin"));
        }
    }

    private static long ambientDelay() {
        return ThreadLocalRandom.current().nextLong(18_000, 35_000);
    }
}
