package io.github.zefarie.herbalis.application.port;

import io.github.zefarie.herbalis.domain.geo.BlockPos;

import java.util.Set;

/**
 * Acces aux tuyaux d'irrigation poses dans le monde.
 */
public interface PipeRepository {

    boolean has(BlockPos pos);

    void add(BlockPos pos);

    void remove(BlockPos pos);

    /** Vue immuable de tous les tuyaux poses. */
    Set<BlockPos> all();
}
