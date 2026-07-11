package io.github.zefarie.herbalis.application.port;

import io.github.zefarie.herbalis.domain.consumption.ConsumerProfile;

import java.util.UUID;

/**
 * Acces aux profils de consommation des joueurs.
 */
public interface ConsumerRepository {

    /** Profil du joueur, cree vierge si inconnu. */
    ConsumerProfile of(UUID playerId, long now);

    void put(UUID playerId, ConsumerProfile profile);
}
