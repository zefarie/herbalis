package io.github.zefarie.herbalis.application.usecase;

import io.github.zefarie.herbalis.application.port.ConsumerRepository;
import io.github.zefarie.herbalis.domain.consumption.ConsumptionEngine;
import io.github.zefarie.herbalis.domain.drug.DrugRegistry;
import io.github.zefarie.herbalis.domain.drug.DrugType;
import io.github.zefarie.herbalis.domain.quality.Quality;

import java.util.Optional;
import java.util.UUID;

/**
 * Consommation d'un produit : met a jour le profil du joueur et fournit
 * la chronologie d'effets a derouler.
 */
public final class ConsumeUseCase {

    private final ConsumerRepository consumers;
    private final DrugRegistry drugs;

    public ConsumeUseCase(ConsumerRepository consumers, DrugRegistry drugs) {
        this.consumers = consumers;
        this.drugs = drugs;
    }

    public Optional<ConsumptionEngine.ConsumeOutcome> execute(
            UUID playerId, String drugId, Quality quality, long now) {
        DrugType drug = drugs.byId(drugId).orElse(null);
        if (drug == null) {
            return Optional.empty();
        }
        var profile = consumers.of(playerId, now);
        var outcome = ConsumptionEngine.consume(profile, now, quality, drug);
        consumers.put(playerId, outcome.profile());
        return Optional.of(outcome);
    }
}
