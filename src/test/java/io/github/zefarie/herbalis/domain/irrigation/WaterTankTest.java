package io.github.zefarie.herbalis.domain.irrigation;

import io.github.zefarie.herbalis.domain.TestFixtures;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WaterTankTest {

    @Test
    void seRemplitSansDeborderEtSeVideSansPasserSousZero() {
        WaterTank tank = WaterTank.empty(TestFixtures.pos(), TankSize.CUVE);
        assertTrue(tank.isEmpty());

        WaterTank filled = tank.filled(100.0, 250.0).filled(200.0, 250.0);
        assertEquals(250.0, filled.stock(), 0.001);

        WaterTank drained = filled.drained(300.0);
        assertTrue(drained.isEmpty());
        assertEquals(0.0, drained.stock(), 0.001);
    }

    @Test
    void leRatioDeRemplissagePiloteLetatVisuel() {
        WaterTank tank = WaterTank.empty(TestFixtures.pos(), TankSize.CITERNE);
        assertEquals(TankVisualState.EMPTY, TankVisualState.of(tank.fillRatio(400.0)));
        assertEquals(TankVisualState.LOW,
                TankVisualState.of(tank.filled(100.0, 400.0).fillRatio(400.0)));
        assertEquals(TankVisualState.MID,
                TankVisualState.of(tank.filled(200.0, 400.0).fillRatio(400.0)));
        assertEquals(TankVisualState.FULL,
                TankVisualState.of(tank.filled(400.0, 400.0).fillRatio(400.0)));
    }

    @Test
    void leSiloCompteSesDoses() {
        FertilizerSilo silo = FertilizerSilo.empty(TestFixtures.pos());
        assertTrue(silo.isEmpty());
        FertilizerSilo loaded = silo.filled(2).filled(2).filled(2);
        assertEquals(2, loaded.doses());
        assertEquals(SiloVisualState.FULL, SiloVisualState.of(loaded.doses(), 2));
        FertilizerSilo used = loaded.consumed();
        assertEquals(1, used.doses());
        assertEquals(SiloVisualState.PARTIAL, SiloVisualState.of(used.doses(), 2));
    }
}
