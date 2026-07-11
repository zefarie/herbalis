package io.github.zefarie.herbalis.infrastructure.effects;

import io.github.zefarie.herbalis.application.port.ConsumerRepository;
import io.github.zefarie.herbalis.domain.consumption.ConsumptionEngine;
import io.github.zefarie.herbalis.domain.consumption.WithdrawalState;
import io.github.zefarie.herbalis.domain.drug.DrugRegistry;
import io.github.zefarie.herbalis.domain.drug.DrugType;
import io.github.zefarie.herbalis.infrastructure.config.HerbalisConfig;
import io.github.zefarie.herbalis.infrastructure.config.Messages;
import io.github.zefarie.herbalis.infrastructure.fx.Fx;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Symptomes de manque des joueurs addicts : nausee breve, fatigue,
 * battements de coeur et lignes d'ambiance, a intervalles aleatoires.
 * Le manque cesse en consommant, ou en tenant assez longtemps pour que
 * l'addiction retombe sous le seuil.
 */
public final class WithdrawalService {

    private final ConsumerRepository consumers;
    private final DrugRegistry drugs;
    private final HerbalisConfig config;
    private final Messages messages;
    private final Fx fx;

    private final Map<UUID, Long> nextSymptomAt = new ConcurrentHashMap<>();
    private final Set<UUID> inWithdrawal = ConcurrentHashMap.newKeySet();

    public WithdrawalService(ConsumerRepository consumers, DrugRegistry drugs,
                             HerbalisConfig config, Messages messages, Fx fx) {
        this.consumers = consumers;
        this.drugs = drugs;
        this.config = config;
        this.messages = messages;
        this.fx = fx;
    }

    /** Tick toutes les 5 secondes environ, par joueur en ligne. */
    public void tick(Player player, long now) {
        // V1 : profil unique par joueur, les seuils viennent de la premiere
        // drogue enregistree (la weed).
        DrugType drug = drugs.all().stream().findFirst().orElse(null);
        if (drug == null) {
            return;
        }
        UUID id = player.getUniqueId();
        var profile = consumers.of(id, now);
        WithdrawalState state = ConsumptionEngine.withdrawal(
                profile, now, drug.consumption());

        if (state == WithdrawalState.NONE) {
            if (inWithdrawal.remove(id)) {
                nextSymptomAt.remove(id);
                player.sendMessage(messages.msg("manque.fin"));
            }
            return;
        }

        if (inWithdrawal.add(id)) {
            player.sendMessage(messages.msg("manque.debut"));
            nextSymptomAt.put(id, now + symptomDelay() / 2);
            return;
        }

        long next = nextSymptomAt.getOrDefault(id, 0L);
        if (now < next) {
            return;
        }
        nextSymptomAt.put(id, now + symptomDelay());

        switch (ThreadLocalRandom.current().nextInt(3)) {
            case 0 -> PotionEffects.one(player, "minecraft:nausea", 110, 0);
            case 1 -> PotionEffects.one(player, "minecraft:mining_fatigue", 160, 0);
            default -> {
                // Juste le coeur qui s'emballe.
            }
        }
        fx.withdrawalShiver(player);
        player.sendActionBar(messages.random("manque.ambiance"));
    }

    public void forget(UUID playerId) {
        nextSymptomAt.remove(playerId);
        inWithdrawal.remove(playerId);
    }

    private long symptomDelay() {
        long min = config.withdrawalSymptomMin().toMillis();
        long max = Math.max(min + 1, config.withdrawalSymptomMax().toMillis());
        return ThreadLocalRandom.current().nextLong(min, max);
    }
}
