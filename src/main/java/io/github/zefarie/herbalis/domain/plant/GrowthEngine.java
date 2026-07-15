package io.github.zefarie.herbalis.domain.plant;

import io.github.zefarie.herbalis.domain.drug.DrugType;
import io.github.zefarie.herbalis.domain.drug.HydrationProfile;

import java.util.ArrayList;
import java.util.List;

/**
 * Moteur de croissance : fait avancer une plante d'un tick.
 * Fonction pure, sans effet de bord, sans horloge implicite.
 */
public final class GrowthEngine {

    private GrowthEngine() {
    }

    public record GrowthTick(Plant plant, List<PlantEvent> events) {

        public boolean hasEvents() {
            return !events.isEmpty();
        }
    }

    /**
     * Avance la plante de {@code conditions.deltaMillis()} millisecondes.
     *
     * <p>Regles :</p>
     * <ul>
     *   <li>l'hydratation decroit avec le temps;</li>
     *   <li>sous le seuil de soif ou sous la lumiere minimale, la
     *       croissance est figee;</li>
     *   <li>a sec, la plante jaunit puis meurt;</li>
     *   <li>au stade final, le temps de maturite s'accumule et pilote la
     *       fenetre de recolte.</li>
     * </ul>
     */
    public static GrowthTick tick(Plant plant, DrugType drug, GrowthConditions conditions) {
        if (plant.isDead()) {
            return new GrowthTick(plant, List.of());
        }

        List<PlantEvent> events = new ArrayList<>(2);
        HydrationProfile hydrationProfile = drug.hydration();
        long delta = conditions.deltaMillis();

        // Hydratation : decroissance, puis echantillon pondere par la duree
        // du tick (les ticks de rattrapage pesent leur juste poids).
        double hydration = Math.max(0.0,
                plant.hydration() - hydrationProfile.decayPerMinute() * delta / 60_000.0);
        double hydrationSum = plant.hydrationSum() + hydration * delta;
        long samples = plant.hydrationSamples() + delta;

        // Secheresse : jaunissement puis mort.
        long dryMillis = hydration <= 0.0 ? plant.dryMillis() + delta : 0L;
        PlantState state = plant.state();
        if (dryMillis >= hydrationProfile.deathDelay().toMillis()) {
            Plant dead = plant.ticked(plant.stage(), plant.stageGrowthMillis(),
                    plant.ripenMillis(), hydration, hydrationSum, samples,
                    dryMillis, PlantState.DEAD);
            return new GrowthTick(dead, List.of(new PlantEvent.Died()));
        }
        if (dryMillis >= hydrationProfile.witherDelay().toMillis()) {
            if (state == PlantState.HEALTHY) {
                events.add(new PlantEvent.Withered());
            }
            state = PlantState.WITHERED;
        } else if (state == PlantState.WITHERED) {
            state = PlantState.HEALTHY;
            events.add(new PlantEvent.Recovered());
        }

        boolean lightOk = conditions.lightLevel() >= drug.growth().minLight();
        boolean hydrationOk = hydration > hydrationProfile.thirstyThreshold();

        int stage = plant.stage();
        long stageGrowth = plant.stageGrowthMillis();
        long ripen = plant.ripenMillis();

        if (drug.growth().isFinalStage(stage)) {
            // Fenetre de recolte : la maturite avance quoi qu'il arrive.
            boolean wasOptimal = drug.harvestWindow().isOptimal(ripen);
            ripen += delta;
            if (wasOptimal && !drug.harvestWindow().isOptimal(ripen)) {
                events.add(new PlantEvent.HarvestWindowClosed());
            }
        } else if (lightOk && hydrationOk && state != PlantState.WITHERED) {
            double multiplier = plant.isFertilizedThisStage()
                    ? drug.fertilizer().speedMultiplier()
                    : 1.0;
            stageGrowth += Math.round(delta * multiplier);

            long needed = drug.growth().durationOf(stage).toMillis();
            if (stageGrowth >= needed) {
                stage++;
                stageGrowth = 0L;
                boolean isFinal = drug.growth().isFinalStage(stage);
                events.add(new PlantEvent.StageAdvanced(stage, isFinal));
            }
        }

        Plant next = plant.ticked(stage, stageGrowth, ripen,
                hydration, hydrationSum, samples, dryMillis, state);
        return new GrowthTick(next, List.copyOf(events));
    }

    /** Vrai si la croissance est actuellement figee par manque de lumiere. */
    public static boolean isLightStarved(Plant plant, DrugType drug, int lightLevel) {
        return !plant.isDead()
                && !drug.growth().isFinalStage(plant.stage())
                && lightLevel < drug.growth().minLight();
    }

    /** Vrai si la plante est recoltable (stade final atteint et vivante). */
    public static boolean isHarvestable(Plant plant, DrugType drug) {
        return !plant.isDead() && drug.growth().isFinalStage(plant.stage());
    }
}
