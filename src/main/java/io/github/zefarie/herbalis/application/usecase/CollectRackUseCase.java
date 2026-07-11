package io.github.zefarie.herbalis.application.usecase;

import io.github.zefarie.herbalis.application.port.RackRepository;
import io.github.zefarie.herbalis.domain.drug.DrugRegistry;
import io.github.zefarie.herbalis.domain.drug.DrugType;
import io.github.zefarie.herbalis.domain.drying.DryingRack;
import io.github.zefarie.herbalis.domain.drying.DryingSlot;
import io.github.zefarie.herbalis.domain.geo.BlockPos;
import io.github.zefarie.herbalis.domain.quality.Quality;
import io.github.zefarie.herbalis.domain.quality.QualityCalculator;

import java.util.ArrayList;
import java.util.List;

/**
 * Recuperation du contenu d'un rack. Les tetes pas encore seches
 * subissent un malus de qualite proportionnel au sechage manquant.
 */
public final class CollectRackUseCase {

    public sealed interface Result {
        /**
         * @param drugId    drogue sechee
         * @param qualities qualites finales, une entree par tete
         * @param anyEarly  vrai si au moins une tete a ete retiree trop tot
         */
        record Success(String drugId, List<Quality> qualities, boolean anyEarly)
                implements Result {
        }

        record Empty() implements Result {
        }

        record NoRack() implements Result {
        }
    }

    private final RackRepository racks;
    private final DrugRegistry drugs;

    public CollectRackUseCase(RackRepository racks, DrugRegistry drugs) {
        this.racks = racks;
        this.drugs = drugs;
    }

    public Result execute(BlockPos pos, long now) {
        DryingRack rack = racks.at(pos).orElse(null);
        if (rack == null) {
            return new Result.NoRack();
        }
        if (rack.isEmpty()) {
            return new Result.Empty();
        }
        DrugType drug = drugs.byId(rack.drugId()).orElse(null);
        if (drug == null) {
            racks.put(rack.emptied());
            return new Result.Empty();
        }

        List<Quality> qualities = new ArrayList<>(rack.slots().size());
        boolean anyEarly = false;
        for (DryingSlot slot : rack.slots()) {
            double ratio = slot.completionRatio(now, drug.drying().duration());
            if (ratio < 1.0) {
                anyEarly = true;
            }
            qualities.add(QualityCalculator.afterDrying(slot.quality(), ratio));
        }

        racks.put(rack.emptied());
        return new Result.Success(drug.id(), List.copyOf(qualities), anyEarly);
    }
}
