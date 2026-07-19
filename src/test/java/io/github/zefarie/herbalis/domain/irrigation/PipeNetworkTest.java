package io.github.zefarie.herbalis.domain.irrigation;

import io.github.zefarie.herbalis.domain.TestFixtures;
import io.github.zefarie.herbalis.domain.geo.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PipeNetworkTest {

    private static BlockPos at(int x, int y, int z) {
        return new BlockPos(TestFixtures.WORLD, x, y, z);
    }

    @Test
    void unPotRelieAuCaissonParUneLigneDeTuyaux() {
        BlockPos pot = at(0, 64, 0);
        BlockPos tank = at(4, 64, 0);
        Set<BlockPos> pipes = Set.of(at(1, 64, 0), at(2, 64, 0), at(3, 64, 0));

        Set<BlockPos> found = PipeNetwork.reachableEndpoints(
                pot, pipes, tank::equals);
        assertEquals(Set.of(tank), found);
    }

    @Test
    void sansTuyauAdjacentRienNestRelie() {
        BlockPos pot = at(0, 64, 0);
        // La ligne demarre a deux blocs du pot : reseau injoignable.
        Set<BlockPos> pipes = Set.of(at(2, 64, 0), at(3, 64, 0));

        assertTrue(PipeNetwork.reachableEndpoints(pot, pipes, p -> true).isEmpty());
    }

    @Test
    void unTuyauManquantCoupeLeReseau() {
        BlockPos pot = at(0, 64, 0);
        BlockPos tank = at(4, 64, 0);
        // Trou en (2, 64, 0) : le caisson est de l'autre cote de la coupure.
        Set<BlockPos> pipes = Set.of(at(1, 64, 0), at(3, 64, 0));

        assertTrue(PipeNetwork.reachableEndpoints(pot, pipes, tank::equals).isEmpty());
    }

    @Test
    void leReseauMonteEtTourne() {
        BlockPos pot = at(0, 64, 0);
        BlockPos tank = at(2, 66, 3);
        Set<BlockPos> pipes = Set.of(
                at(0, 65, 0), at(0, 66, 0),
                at(1, 66, 0), at(2, 66, 0),
                at(2, 66, 1), at(2, 66, 2));

        assertEquals(Set.of(tank), PipeNetwork.reachableEndpoints(
                pot, pipes, tank::equals));
    }

    @Test
    void unReseauDessertPlusieursPointsEtFiltre() {
        BlockPos tank = at(0, 64, 0);
        BlockPos potA = at(3, 64, 1);
        BlockPos potB = at(3, 64, -1);
        BlockPos decor = at(2, 65, 0);
        Set<BlockPos> pipes = Set.of(at(1, 64, 0), at(2, 64, 0), at(3, 64, 0));
        Set<BlockPos> potsSet = Set.of(potA, potB);

        Set<BlockPos> found = PipeNetwork.reachableEndpoints(
                tank, pipes, potsSet::contains);
        assertEquals(potsSet, found);
        assertTrue(found.stream().noneMatch(decor::equals));
    }
}
