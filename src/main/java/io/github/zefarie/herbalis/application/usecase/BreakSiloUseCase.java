package io.github.zefarie.herbalis.application.usecase;

import io.github.zefarie.herbalis.application.port.SiloRepository;
import io.github.zefarie.herbalis.domain.geo.BlockPos;
import io.github.zefarie.herbalis.domain.irrigation.FertilizerSilo;

import java.util.Optional;

/**
 * Casse d'un silo : les doses restantes sont rendues en engrais.
 */
public final class BreakSiloUseCase {

    private final SiloRepository silos;

    public BreakSiloUseCase(SiloRepository silos) {
        this.silos = silos;
    }

    /** @return le silo casse, avec ses doses a rendre */
    public Optional<FertilizerSilo> execute(BlockPos pos) {
        Optional<FertilizerSilo> silo = silos.at(pos);
        silo.ifPresent(s -> silos.remove(pos));
        return silo;
    }
}
