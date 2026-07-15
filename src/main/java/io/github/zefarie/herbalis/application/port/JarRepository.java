package io.github.zefarie.herbalis.application.port;

import io.github.zefarie.herbalis.domain.curing.CuringJar;
import io.github.zefarie.herbalis.domain.geo.BlockPos;

import java.util.Collection;
import java.util.Optional;

/**
 * Acces aux jarres de curing posees dans le monde.
 */
public interface JarRepository {

    Optional<CuringJar> at(BlockPos pos);

    void put(CuringJar jar);

    void remove(BlockPos pos);

    Collection<CuringJar> all();
}
