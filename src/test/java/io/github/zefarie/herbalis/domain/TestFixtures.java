package io.github.zefarie.herbalis.domain;

import io.github.zefarie.herbalis.domain.drug.ConsumptionRules;
import io.github.zefarie.herbalis.domain.drug.CuringProfile;
import io.github.zefarie.herbalis.domain.drug.DrugType;
import io.github.zefarie.herbalis.domain.drug.DryingProfile;
import io.github.zefarie.herbalis.domain.drug.EffectProfile;
import io.github.zefarie.herbalis.domain.drug.EffectSpec;
import io.github.zefarie.herbalis.domain.drug.FertilizerProfile;
import io.github.zefarie.herbalis.domain.drug.GrowthProfile;
import io.github.zefarie.herbalis.domain.drug.HarvestWindow;
import io.github.zefarie.herbalis.domain.drug.HydrationProfile;
import io.github.zefarie.herbalis.domain.drug.ToppingProfile;
import io.github.zefarie.herbalis.domain.geo.BlockPos;
import io.github.zefarie.herbalis.domain.quality.QualityWeights;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Fixtures du domaine : une weed de test aux valeurs par defaut de la
 * config livree.
 */
public final class TestFixtures {

    public static final UUID WORLD = UUID.fromString(
            "00000000-0000-0000-0000-000000000001");

    private TestFixtures() {
    }

    public static BlockPos pos() {
        return new BlockPos(WORLD, 10, 64, -20);
    }

    public static DrugType weed() {
        return new DrugType(
                "weed",
                "Weed",
                new GrowthProfile(4,
                        List.of(Duration.ofMinutes(8), Duration.ofMinutes(8),
                                Duration.ofMinutes(8), Duration.ofMinutes(8)),
                        12, 2, 5),
                new HydrationProfile(2.5, 40.0, 25.0,
                        Duration.ofMinutes(4), Duration.ofMinutes(12)),
                new FertilizerProfile(0.5, 0.25),
                new HarvestWindow(Duration.ofMinutes(10), Duration.ofMinutes(20)),
                new DryingProfile(Duration.ofMinutes(20), 6),
                new ToppingProfile(List.of(2, 3), 0.30, 0.60,
                        Duration.ofMinutes(2), 1, 2, 1),
                new CuringProfile(Duration.ofMinutes(45), Duration.ofMinutes(90), 1, 6),
                new EffectProfile(Duration.ofSeconds(15),
                        Duration.ofMinutes(2), Duration.ofMinutes(6), 0.35,
                        List.of(new EffectSpec("minecraft:regeneration", 0, true, false)),
                        List.of(new EffectSpec("minecraft:slowness", 0, false, false),
                                new EffectSpec("minecraft:nausea", 0, false, true))),
                new ConsumptionRules(3, Duration.ofMinutes(5), Duration.ofSeconds(30),
                        12.0, 4.0, 0.6,
                        8.0, 1.5, 50.0, Duration.ofMinutes(45)),
                new QualityWeights(0.40, 0.25, 0.20, 0.15));
    }
}
