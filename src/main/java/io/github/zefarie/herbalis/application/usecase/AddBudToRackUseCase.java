package io.github.zefarie.herbalis.application.usecase;

import io.github.zefarie.herbalis.application.port.RackRepository;
import io.github.zefarie.herbalis.domain.drug.DrugRegistry;
import io.github.zefarie.herbalis.domain.drug.DrugType;
import io.github.zefarie.herbalis.domain.drying.DryingRack;
import io.github.zefarie.herbalis.domain.geo.BlockPos;
import io.github.zefarie.herbalis.domain.quality.Quality;

/**
 * Depot d'une tete fraiche sur un rack de sechage.
 */
public final class AddBudToRackUseCase {

    public sealed interface Result {
        record Success(DryingRack rack) implements Result {
        }

        record RackFull() implements Result {
        }

        record MixedDrugs() implements Result {
        }

        record NoRack() implements Result {
        }
    }

    private final RackRepository racks;
    private final DrugRegistry drugs;

    public AddBudToRackUseCase(RackRepository racks, DrugRegistry drugs) {
        this.racks = racks;
        this.drugs = drugs;
    }

    public Result execute(BlockPos pos, String drugId, Quality quality, long now) {
        DryingRack rack = racks.at(pos).orElse(null);
        if (rack == null) {
            return new Result.NoRack();
        }
        DrugType drug = drugs.byId(drugId).orElse(null);
        if (drug == null) {
            return new Result.NoRack();
        }
        if (!rack.isEmpty() && !rack.drugId().equals(drugId)) {
            return new Result.MixedDrugs();
        }
        if (!rack.canAccept(drugId, drug.drying().capacity())) {
            return new Result.RackFull();
        }
        DryingRack updated = rack.withBud(drugId, quality, now);
        racks.put(updated);
        return new Result.Success(updated);
    }
}
