package io.github.zefarie.herbalis.application.service;

import io.github.zefarie.herbalis.application.port.JarRepository;
import io.github.zefarie.herbalis.application.port.LampRepository;
import io.github.zefarie.herbalis.application.port.PipeRepository;
import io.github.zefarie.herbalis.application.port.PotRepository;
import io.github.zefarie.herbalis.application.port.RackRepository;
import io.github.zefarie.herbalis.application.port.SiloRepository;
import io.github.zefarie.herbalis.application.port.TankRepository;
import io.github.zefarie.herbalis.domain.geo.BlockPos;

/**
 * Occupation des positions par les structures Herbalis, toutes familles
 * confondues : une position ne porte qu'une seule structure a la fois.
 */
public final class OccupancyService {

    private final PotRepository pots;
    private final RackRepository racks;
    private final JarRepository jars;
    private final PipeRepository pipes;
    private final TankRepository tanks;
    private final SiloRepository silos;
    private final LampRepository lamps;

    public OccupancyService(PotRepository pots, RackRepository racks,
                            JarRepository jars, PipeRepository pipes,
                            TankRepository tanks, SiloRepository silos,
                            LampRepository lamps) {
        this.pots = pots;
        this.racks = racks;
        this.jars = jars;
        this.pipes = pipes;
        this.tanks = tanks;
        this.silos = silos;
        this.lamps = lamps;
    }

    public boolean occupied(BlockPos pos) {
        return pots.exists(pos)
                || racks.at(pos).isPresent()
                || jars.at(pos).isPresent()
                || pipes.has(pos)
                || tanks.at(pos).isPresent()
                || silos.at(pos).isPresent()
                || lamps.has(pos);
    }
}
