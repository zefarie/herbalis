package io.github.zefarie.herbalis.infrastructure.scheduler;

import io.github.zefarie.herbalis.application.port.JarRepository;
import io.github.zefarie.herbalis.application.port.PlantEnvironment;
import io.github.zefarie.herbalis.domain.curing.CuringJar;
import io.github.zefarie.herbalis.domain.curing.JarVisualState;
import io.github.zefarie.herbalis.domain.drug.DrugRegistry;
import io.github.zefarie.herbalis.domain.geo.BlockPos;
import io.github.zefarie.herbalis.infrastructure.fx.Fx;
import io.github.zefarie.herbalis.infrastructure.render.DisplayRenderer;
import io.github.zefarie.herbalis.infrastructure.render.PosCodec;

import java.util.HashMap;
import java.util.Map;

/**
 * Surveille les jarres de curing : bascule les modeles quand l'affinage
 * avance, carillonne quand une jarre est prete et signale de loin les
 * jarres pretes ou moisies.
 */
public final class JarTicker implements Runnable {

    private final JarRepository jars;
    private final DrugRegistry drugs;
    private final PlantEnvironment environment;
    private final DisplayRenderer renderer;
    private final Fx fx;
    private final Map<BlockPos, JarVisualState> lastStates = new HashMap<>();

    public JarTicker(JarRepository jars, DrugRegistry drugs,
                     PlantEnvironment environment, DisplayRenderer renderer, Fx fx) {
        this.jars = jars;
        this.drugs = drugs;
        this.environment = environment;
        this.renderer = renderer;
        this.fx = fx;
    }

    @Override
    public void run() {
        long now = System.currentTimeMillis();
        for (CuringJar jar : jars.all()) {
            if (!environment.isLoaded(jar.pos())) {
                lastStates.remove(jar.pos());
                continue;
            }
            JarVisualState state = drugs.byId(jar.drugId())
                    .map(drug -> jar.visualState(now, drug.curing()))
                    .orElse(JarVisualState.EMPTY);

            JarVisualState previous = lastStates.put(jar.pos(), state);
            if (previous != null && previous != state) {
                renderer.updateJar(jar.pos(), state);
                if (state == JarVisualState.READY) {
                    PosCodec.corner(jar.pos()).ifPresent(fx::jarReadyChime);
                }
            }
            if (state == JarVisualState.READY) {
                PosCodec.corner(jar.pos()).ifPresent(fx::jarReadyAmbient);
            } else if (state == JarVisualState.MOLDY) {
                PosCodec.corner(jar.pos()).ifPresent(fx::jarMoldyAmbient);
            }
        }
    }
}
