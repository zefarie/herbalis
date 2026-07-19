package io.github.zefarie.herbalis.application.port;

import io.github.zefarie.herbalis.domain.geo.BlockPos;

import java.util.Collection;

/**
 * Acces aux lampes horticoles posees dans le monde.
 */
public interface LampRepository {

    boolean has(BlockPos pos);

    void add(BlockPos pos);

    void remove(BlockPos pos);

    Collection<BlockPos> all();
}
