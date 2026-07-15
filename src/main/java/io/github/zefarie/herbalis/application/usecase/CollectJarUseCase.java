package io.github.zefarie.herbalis.application.usecase;

import io.github.zefarie.herbalis.application.port.JarRepository;
import io.github.zefarie.herbalis.domain.curing.CuringJar;
import io.github.zefarie.herbalis.domain.curing.CuringSlot;
import io.github.zefarie.herbalis.domain.drug.DrugRegistry;
import io.github.zefarie.herbalis.domain.drug.DrugType;
import io.github.zefarie.herbalis.domain.geo.BlockPos;
import io.github.zefarie.herbalis.domain.quality.Quality;
import io.github.zefarie.herbalis.domain.quality.QualityCalculator;

import java.util.ArrayList;
import java.util.List;

/**
 * Recuperation du contenu d'une jarre de curing. Affinage complet :
 * bonus d'etoiles. Jarre moisie : tout le contenu est ruine. Retrait
 * anticipe : qualite inchangee, le bonus est perdu.
 */
public final class CollectJarUseCase {

    public sealed interface Result {
        /**
         * @param drugId    drogue affinee
         * @param qualities qualites finales, une entree par tete
         * @param moldy     vrai si la jarre avait moisi (contenu ruine)
         * @param anyEarly  vrai si au moins une tete n'etait pas affinee
         */
        record Success(String drugId, List<Quality> qualities, boolean moldy,
                       boolean anyEarly) implements Result {
        }

        record Empty() implements Result {
        }

        record NoJar() implements Result {
        }
    }

    private final JarRepository jars;
    private final DrugRegistry drugs;

    public CollectJarUseCase(JarRepository jars, DrugRegistry drugs) {
        this.jars = jars;
        this.drugs = drugs;
    }

    public Result execute(BlockPos pos, long now) {
        CuringJar jar = jars.at(pos).orElse(null);
        if (jar == null) {
            return new Result.NoJar();
        }
        if (jar.isEmpty()) {
            return new Result.Empty();
        }
        DrugType drug = drugs.byId(jar.drugId()).orElse(null);
        if (drug == null) {
            jars.put(jar.emptied());
            return new Result.Empty();
        }

        boolean moldy = jar.isMoldy(now, drug.curing());
        List<Quality> qualities = new ArrayList<>(jar.slots().size());
        boolean anyEarly = false;
        for (CuringSlot slot : jar.slots()) {
            boolean cured = slot.isCured(now, drug.curing().duration());
            if (!cured) {
                anyEarly = true;
            }
            qualities.add(QualityCalculator.afterCuring(
                    slot.quality(), cured, moldy, drug.curing().bonusStars()));
        }

        jars.put(jar.emptied());
        return new Result.Success(drug.id(), List.copyOf(qualities), moldy, anyEarly);
    }
}
