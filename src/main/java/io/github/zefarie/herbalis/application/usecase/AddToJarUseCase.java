package io.github.zefarie.herbalis.application.usecase;

import io.github.zefarie.herbalis.application.port.JarRepository;
import io.github.zefarie.herbalis.domain.curing.CuringJar;
import io.github.zefarie.herbalis.domain.drug.DrugRegistry;
import io.github.zefarie.herbalis.domain.drug.DrugType;
import io.github.zefarie.herbalis.domain.geo.BlockPos;
import io.github.zefarie.herbalis.domain.quality.Quality;

/**
 * Depot d'une tete sechee dans une jarre de curing.
 */
public final class AddToJarUseCase {

    public sealed interface Result {
        record Success(CuringJar jar) implements Result {
        }

        record JarFull() implements Result {
        }

        record MixedDrugs() implements Result {
        }

        /** Une jarre moisie doit etre videe avant de resservir. */
        record Moldy() implements Result {
        }

        record NoJar() implements Result {
        }
    }

    private final JarRepository jars;
    private final DrugRegistry drugs;

    public AddToJarUseCase(JarRepository jars, DrugRegistry drugs) {
        this.jars = jars;
        this.drugs = drugs;
    }

    public Result execute(BlockPos pos, String drugId, Quality quality, long now) {
        CuringJar jar = jars.at(pos).orElse(null);
        if (jar == null) {
            return new Result.NoJar();
        }
        DrugType drug = drugs.byId(drugId).orElse(null);
        if (drug == null) {
            return new Result.NoJar();
        }
        if (jar.isMoldy(now, drug.curing())) {
            return new Result.Moldy();
        }
        if (!jar.isEmpty() && !jar.drugId().equals(drugId)) {
            return new Result.MixedDrugs();
        }
        if (!jar.canAccept(drugId, drug.curing().capacity())) {
            return new Result.JarFull();
        }
        CuringJar updated = jar.withBud(drugId, quality, now);
        jars.put(updated);
        return new Result.Success(updated);
    }
}
