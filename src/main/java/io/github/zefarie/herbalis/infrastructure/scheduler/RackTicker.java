package io.github.zefarie.herbalis.infrastructure.scheduler;

import io.github.zefarie.herbalis.application.port.PlantEnvironment;
import io.github.zefarie.herbalis.application.port.RackRepository;
import io.github.zefarie.herbalis.domain.drug.DrugRegistry;
import io.github.zefarie.herbalis.domain.drying.DryingRack;
import io.github.zefarie.herbalis.domain.drying.RackVisualState;
import io.github.zefarie.herbalis.domain.geo.BlockPos;
import io.github.zefarie.herbalis.infrastructure.fx.Fx;
import io.github.zefarie.herbalis.infrastructure.render.DisplayRenderer;
import io.github.zefarie.herbalis.infrastructure.render.PosCodec;

import java.util.HashMap;
import java.util.Map;

/**
 * Surveille les racks : bascule les modeles quand le sechage avance et
 * signale de loin, par une particule discrete, les racks prets.
 */
public final class RackTicker implements Runnable {

    private final RackRepository racks;
    private final DrugRegistry drugs;
    private final PlantEnvironment environment;
    private final DisplayRenderer renderer;
    private final Fx fx;
    private final Map<BlockPos, RackVisualState> lastStates = new HashMap<>();

    public RackTicker(RackRepository racks, DrugRegistry drugs,
                      PlantEnvironment environment, DisplayRenderer renderer, Fx fx) {
        this.racks = racks;
        this.drugs = drugs;
        this.environment = environment;
        this.renderer = renderer;
        this.fx = fx;
    }

    @Override
    public void run() {
        long now = System.currentTimeMillis();
        for (DryingRack rack : racks.all()) {
            if (!environment.isLoaded(rack.pos())) {
                lastStates.remove(rack.pos());
                continue;
            }
            RackVisualState state = drugs.byId(rack.drugId())
                    .map(drug -> rack.visualState(now, drug.drying().duration()))
                    .orElse(RackVisualState.EMPTY);

            RackVisualState previous = lastStates.put(rack.pos(), state);
            if (previous != null && previous != state) {
                renderer.updateRack(rack.pos(), state);
                if (state == RackVisualState.READY) {
                    PosCodec.corner(rack.pos()).ifPresent(fx::rackReadyChime);
                }
            }
            if (state == RackVisualState.READY) {
                PosCodec.corner(rack.pos()).ifPresent(fx::rackReadyAmbient);
            }
        }
        lastStates.keySet().removeIf(pos -> racks.at(pos).isEmpty());
    }
}
