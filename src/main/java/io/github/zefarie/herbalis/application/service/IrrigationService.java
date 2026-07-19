package io.github.zefarie.herbalis.application.service;

import io.github.zefarie.herbalis.application.port.PipeRepository;
import io.github.zefarie.herbalis.application.port.PotRepository;
import io.github.zefarie.herbalis.application.port.SiloRepository;
import io.github.zefarie.herbalis.application.port.TankRepository;
import io.github.zefarie.herbalis.domain.geo.BlockPos;
import io.github.zefarie.herbalis.domain.irrigation.FertilizerSilo;
import io.github.zefarie.herbalis.domain.irrigation.PipeNetwork;
import io.github.zefarie.herbalis.domain.irrigation.WaterTank;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Cablage du reseau d'irrigation : resout et met en cache les caissons
 * et silos relies a chaque pot par des tuyaux, puis debite leurs stocks
 * au fil de la croissance. Le cache est invalide a chaque pose ou casse
 * d'un element du reseau.
 */
public final class IrrigationService {

    /** Caissons et silos relies a un pot. */
    public record Hookup(List<BlockPos> tanks, List<BlockPos> silos) {

        public static final Hookup NONE = new Hookup(List.of(), List.of());

        public boolean hasTank() {
            return !tanks.isEmpty();
        }

        public boolean hasSilo() {
            return !silos.isEmpty();
        }

        public boolean isConnected() {
            return hasTank() || hasSilo();
        }
    }

    private final TankRepository tanks;
    private final SiloRepository silos;
    private final PipeRepository pipes;
    private final PotRepository pots;
    private final Map<BlockPos, Hookup> cache = new HashMap<>();

    public IrrigationService(TankRepository tanks, SiloRepository silos,
                             PipeRepository pipes, PotRepository pots) {
        this.tanks = tanks;
        this.silos = silos;
        this.pipes = pipes;
        this.pots = pots;
    }

    /** Oublie le cablage connu (un element du reseau a bouge). */
    public void invalidate() {
        cache.clear();
    }

    /** Caissons et silos relies a ce pot, resolus une fois puis caches. */
    public Hookup hookupFor(BlockPos potPos) {
        return cache.computeIfAbsent(potPos, this::resolve);
    }

    private Hookup resolve(BlockPos potPos) {
        Set<BlockPos> pipeSet = pipes.all();
        if (pipeSet.isEmpty()) {
            return Hookup.NONE;
        }
        Set<BlockPos> endpoints = PipeNetwork.reachableEndpoints(potPos, pipeSet,
                pos -> tanks.at(pos).isPresent() || silos.at(pos).isPresent());
        if (endpoints.isEmpty()) {
            return Hookup.NONE;
        }
        return new Hookup(
                endpoints.stream()
                        .filter(pos -> tanks.at(pos).isPresent()).toList(),
                endpoints.stream()
                        .filter(pos -> silos.at(pos).isPresent()).toList());
    }

    /** Eau totale a disposition du pot, en points d'hydratation. */
    public double availableWater(Hookup hookup) {
        double sum = 0.0;
        for (BlockPos pos : hookup.tanks()) {
            sum += tanks.at(pos).map(WaterTank::stock).orElse(0.0);
        }
        return sum;
    }

    /** Debite l'eau consommee, caisson par caisson. */
    public void drawWater(Hookup hookup, double amount) {
        double remaining = amount;
        for (BlockPos pos : hookup.tanks()) {
            if (remaining <= 0.0) {
                return;
            }
            WaterTank tank = tanks.at(pos).orElse(null);
            if (tank == null || tank.isEmpty()) {
                continue;
            }
            double taken = Math.min(tank.stock(), remaining);
            tanks.put(tank.drained(taken));
            remaining -= taken;
        }
    }

    /** Vrai si un silo relie a au moins une dose. */
    public boolean hasFertilizerDose(Hookup hookup) {
        return hookup.silos().stream()
                .anyMatch(pos -> silos.at(pos)
                        .filter(silo -> !silo.isEmpty()).isPresent());
    }

    /** Consomme une dose sur le premier silo relie qui en a. */
    public void consumeFertilizerDose(Hookup hookup) {
        for (BlockPos pos : hookup.silos()) {
            FertilizerSilo silo = silos.at(pos).orElse(null);
            if (silo != null && !silo.isEmpty()) {
                silos.put(silo.consumed());
                return;
            }
        }
    }

    /** Nombre de pots relies a ce caisson ou silo (hologrammes). */
    public int connectedPots(BlockPos from) {
        Set<BlockPos> pipeSet = pipes.all();
        if (pipeSet.isEmpty()) {
            return 0;
        }
        return PipeNetwork.reachableEndpoints(from, pipeSet, pots::exists).size();
    }
}
