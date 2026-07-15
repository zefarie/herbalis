package io.github.zefarie.herbalis.application.usecase;

import io.github.zefarie.herbalis.application.port.JarRepository;
import io.github.zefarie.herbalis.domain.curing.CuringJar;
import io.github.zefarie.herbalis.domain.geo.BlockPos;

import java.util.Optional;

/**
 * Pose d'une jarre de curing.
 */
public final class PlaceJarUseCase {

    private final JarRepository jars;

    public PlaceJarUseCase(JarRepository jars) {
        this.jars = jars;
    }

    /** @return la jarre posee, ou vide si la position est occupee */
    public Optional<CuringJar> execute(BlockPos pos) {
        if (jars.at(pos).isPresent()) {
            return Optional.empty();
        }
        CuringJar jar = CuringJar.empty(pos);
        jars.put(jar);
        return Optional.of(jar);
    }
}
