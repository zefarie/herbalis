package io.github.zefarie.herbalis.domain.irrigation;

import io.github.zefarie.herbalis.domain.geo.BlockPos;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Resolution du reseau d'irrigation : un pot n'est alimente que s'il est
 * physiquement relie a un caisson par une chaine de tuyaux adjacents
 * (6-adjacence). Fonction pure sur des positions.
 */
public final class PipeNetwork {

    private PipeNetwork() {
    }

    /**
     * Points d'interet relies a {@code start} par le reseau : parcours en
     * largeur des tuyaux adjacents, en collectant les positions voisines
     * du reseau qui satisfont {@code endpoint}.
     *
     * @param start    position de depart (pot, caisson ou silo), hors reseau
     * @param pipes    positions de tous les tuyaux poses
     * @param endpoint filtre des positions a collecter en peripherie
     * @return les positions atteignables, dans l'ordre de decouverte
     */
    public static Set<BlockPos> reachableEndpoints(BlockPos start,
                                                   Set<BlockPos> pipes,
                                                   Predicate<BlockPos> endpoint) {
        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> frontier = new ArrayDeque<>();
        for (BlockPos neighbor : start.neighbors()) {
            if (pipes.contains(neighbor) && visited.add(neighbor)) {
                frontier.add(neighbor);
            }
        }
        Set<BlockPos> found = new LinkedHashSet<>();
        while (!frontier.isEmpty()) {
            BlockPos pipe = frontier.poll();
            for (BlockPos neighbor : pipe.neighbors()) {
                if (pipes.contains(neighbor)) {
                    if (visited.add(neighbor)) {
                        frontier.add(neighbor);
                    }
                } else if (!neighbor.equals(start) && endpoint.test(neighbor)) {
                    found.add(neighbor);
                }
            }
        }
        return found;
    }
}
