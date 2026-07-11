package io.github.zefarie.herbalis.domain.drug;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Registre des types de drogues connus. Rempli au demarrage depuis la
 * config (un fichier par drogue), interroge partout ailleurs.
 */
public final class DrugRegistry {

    private final Map<String, DrugType> byId = new LinkedHashMap<>();

    public void register(DrugType type) {
        if (byId.putIfAbsent(type.id(), type) != null) {
            throw new IllegalArgumentException("Drogue deja enregistree : " + type.id());
        }
    }

    public Optional<DrugType> byId(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    public Collection<DrugType> all() {
        return byId.values();
    }

    public void clear() {
        byId.clear();
    }
}
