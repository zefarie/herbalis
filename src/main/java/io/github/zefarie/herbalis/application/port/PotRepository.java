package io.github.zefarie.herbalis.application.port;

import io.github.zefarie.herbalis.domain.geo.BlockPos;

import java.util.Collection;

/**
 * Acces aux pots de culture poses dans le monde.
 */
public interface PotRepository {

    boolean exists(BlockPos pos);

    void add(BlockPos pos);

    void remove(BlockPos pos);

    Collection<BlockPos> all();
}
