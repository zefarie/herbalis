package io.github.zefarie.herbalis.infrastructure.fx;

import io.github.zefarie.herbalis.infrastructure.config.HerbalisConfig;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Retours sensoriels : chaque action du joueur a une reponse visuelle et
 * sonore. Toutes les particules et tous les sons passent par ici, et
 * peuvent etre desactives dans config.yml.
 */
public final class Fx {

    private static final Color LEAF_GREEN = Color.fromRGB(0x4a, 0x8f, 0x3c);
    private static final Color LEAF_LIGHT = Color.fromRGB(0x86, 0xef, 0xac);
    private static final Color AURA_PURPLE = Color.fromRGB(0xc4, 0xb5, 0xfd);
    private static final Color AURA_AMBER = Color.fromRGB(0xfc, 0xd3, 0x4d);

    private final HerbalisConfig config;

    public Fx(HerbalisConfig config) {
        this.config = config;
    }

    // ----------------------------------------------------------------
    // Culture
    // ----------------------------------------------------------------

    public void potPlaced(Location loc) {
        particles(loc, w -> w.spawnParticle(Particle.BLOCK, center(loc), 18,
                0.25, 0.1, 0.25, 0.0, Material.DIRT.createBlockData()));
        sound(loc, "minecraft:block.rooted_dirt.place", 0.9f, 1.0f);
    }

    public void planted(Location loc) {
        particles(loc, w -> {
            w.spawnParticle(Particle.BLOCK, soil(loc), 20,
                    0.2, 0.08, 0.2, 0.0, Material.ROOTED_DIRT.createBlockData());
            w.spawnParticle(Particle.HAPPY_VILLAGER, soil(loc), 4, 0.15, 0.1, 0.15, 0.0);
        });
        sound(loc, "minecraft:item.crop.plant", 0.9f, 1.05f);
        sound(loc, "minecraft:block.rooted_dirt.step", 0.7f, 0.9f);
    }

    public void watered(Location loc) {
        particles(loc, w -> {
            w.spawnParticle(Particle.SPLASH, above(loc), 22, 0.22, 0.15, 0.22, 0.0);
            w.spawnParticle(Particle.FALLING_WATER, above(loc), 10, 0.18, 0.2, 0.18, 0.0);
            // Gouttes qui perlent de la canopee pendant quelques secondes.
            w.spawnParticle(Particle.DRIPPING_WATER, plantHeart(loc), 10,
                    0.28, 0.3, 0.28, 0.0);
        });
        sound(loc, "minecraft:entity.generic.splash", 0.55f, 1.35f);
        sound(loc, "minecraft:block.water.ambient", 0.8f, 1.6f);
    }

    public void canRefilled(Location loc) {
        particles(loc, w ->
                w.spawnParticle(Particle.SPLASH, center(loc), 16, 0.2, 0.2, 0.2, 0.0));
        sound(loc, "minecraft:item.bucket.fill", 0.8f, 1.1f);
    }

    public void fertilized(Location loc) {
        particles(loc, w ->
                w.spawnParticle(Particle.HAPPY_VILLAGER, soil(loc), 14, 0.25, 0.2, 0.25, 0.0));
        sound(loc, "minecraft:item.bone_meal.use", 0.9f, 1.0f);
    }

    public void stageUp(Location loc) {
        particles(loc, w -> {
            w.spawnParticle(Particle.HAPPY_VILLAGER, plantHeart(loc), 10, 0.2, 0.25, 0.2, 0.0);
            leaves(w, plantHeart(loc), LEAF_LIGHT, 6);
        });
        sound(loc, "minecraft:block.moss.place", 0.8f, 1.5f);
    }

    /** Passage au stade final : la floraison merite un peu plus d'eclat. */
    public void bloomed(Location loc) {
        particles(loc, w -> {
            w.spawnParticle(Particle.HAPPY_VILLAGER, plantHeart(loc), 16, 0.3, 0.35, 0.3, 0.0);
            leaves(w, plantHeart(loc), LEAF_GREEN, 10);
            w.spawnParticle(Particle.SPORE_BLOSSOM_AIR, plantHeart(loc), 8, 0.3, 0.3, 0.3, 0.0);
        });
        sound(loc, "minecraft:block.cherry_leaves.place", 0.9f, 1.2f);
        sound(loc, "minecraft:block.amethyst_block.chime", 0.5f, 1.4f);
    }

    public void withered(Location loc) {
        particles(loc, w ->
                w.spawnParticle(Particle.SMOKE, plantHeart(loc), 6, 0.15, 0.2, 0.15, 0.01));
        sound(loc, "minecraft:block.sand.idle", 0.8f, 0.6f);
    }

    public void recovered(Location loc) {
        particles(loc, w -> {
            w.spawnParticle(Particle.HAPPY_VILLAGER, plantHeart(loc), 10, 0.2, 0.25, 0.2, 0.0);
            w.spawnParticle(Particle.FALLING_WATER, above(loc), 6, 0.15, 0.1, 0.15, 0.0);
        });
        sound(loc, "minecraft:block.big_dripleaf.tilt_up", 0.9f, 1.2f);
    }

    public void died(Location loc) {
        particles(loc, w -> {
            w.spawnParticle(Particle.SMOKE, plantHeart(loc), 14, 0.2, 0.3, 0.2, 0.02);
            w.spawnParticle(Particle.ASH, plantHeart(loc), 10, 0.2, 0.3, 0.2, 0.0);
        });
        sound(loc, "minecraft:block.sweet_berry_bush.break", 0.7f, 0.6f);
    }

    public void harvested(Location loc) {
        particles(loc, w -> {
            leaves(w, plantHeart(loc), LEAF_GREEN, 16);
            w.spawnParticle(Particle.BLOCK, plantHeart(loc), 12,
                    0.25, 0.25, 0.25, 0.0, Material.OAK_LEAVES.createBlockData());
        });
        sound(loc, "minecraft:entity.sheep.shear", 0.8f, 1.25f);
        sound(loc, "minecraft:block.sweet_berry_bush.pick_berries", 0.9f, 1.0f);
    }

    /** Scintillement discret d'une plante en fenetre de recolte optimale. */
    public void harvestSparkle(Location loc) {
        particles(loc, w -> w.spawnParticle(Particle.END_ROD,
                loc.clone().add(0.5, 1.1, 0.5), 1, 0.18, 0.2, 0.18, 0.006));
    }

    /** Une feuille se detache de la canopee et tombe. */
    public void leafFall(Location loc) {
        particles(loc, w -> {
            ThreadLocalRandom rng = ThreadLocalRandom.current();
            Location at = loc.clone().add(
                    0.5 + rng.nextDouble(-0.3, 0.3),
                    0.8 + rng.nextDouble(0.35),
                    0.5 + rng.nextDouble(-0.3, 0.3));
            w.spawnParticle(Particle.TINTED_LEAVES, at, 1,
                    0.0, 0.0, 0.0, 0.0, LEAF_GREEN);
        });
    }

    /** Taille reussie : coupe nette, la plante appreciera. */
    public void pruned(Location loc) {
        particles(loc, w -> {
            leaves(w, plantHeart(loc), LEAF_LIGHT, 8);
            w.spawnParticle(Particle.HAPPY_VILLAGER, plantHeart(loc), 6,
                    0.2, 0.25, 0.2, 0.0);
        });
        sound(loc, "minecraft:entity.sheep.shear", 0.8f, 1.4f);
        sound(loc, "minecraft:block.azalea.break", 0.7f, 1.2f);
    }

    /** Taille ratee : la plante encaisse mal. */
    public void pruneMissed(Location loc) {
        particles(loc, w -> {
            leaves(w, plantHeart(loc), LEAF_GREEN, 10);
            w.spawnParticle(Particle.SMOKE, plantHeart(loc), 5, 0.15, 0.2, 0.15, 0.01);
        });
        sound(loc, "minecraft:entity.sheep.shear", 0.8f, 0.7f);
        sound(loc, "minecraft:block.sweet_berry_bush.break", 0.8f, 0.6f);
    }

    public void broken(Location loc) {
        particles(loc, w -> w.spawnParticle(Particle.BLOCK, center(loc), 16,
                0.25, 0.2, 0.25, 0.0, Material.DECORATED_POT.createBlockData()));
        sound(loc, "minecraft:block.decorated_pot.break", 0.8f, 1.1f);
    }

    // ----------------------------------------------------------------
    // Sechage et conditionnement
    // ----------------------------------------------------------------

    public void rackPlaced(Location loc) {
        sound(loc, "minecraft:block.ladder.place", 0.9f, 0.9f);
    }

    public void rackAdd(Location loc) {
        particles(loc, w -> leaves(w, center(loc), LEAF_GREEN, 5));
        sound(loc, "minecraft:block.vine.place", 0.9f, 1.0f);
    }

    /** Particules ambiantes discretes quand un rack est pret, visibles de loin. */
    public void rackReadyAmbient(Location loc) {
        particles(loc, w -> w.spawnParticle(Particle.END_ROD,
                center(loc).add(0, 0.65, 0), 2, 0.18, 0.1, 0.18, 0.005));
    }

    public void rackReadyChime(Location loc) {
        sound(loc, "minecraft:block.amethyst_block.chime", 0.6f, 0.8f);
    }

    public void rackCollect(Location loc, boolean early) {
        particles(loc, w -> leaves(w, center(loc), early ? LEAF_GREEN : AURA_AMBER, 10));
        sound(loc, "minecraft:block.vine.break", 0.9f, early ? 0.8f : 1.1f);
    }

    public void rackBroken(Location loc) {
        particles(loc, w -> w.spawnParticle(Particle.BLOCK, center(loc), 14,
                0.25, 0.25, 0.25, 0.0, Material.SPRUCE_PLANKS.createBlockData()));
        sound(loc, "minecraft:block.ladder.break", 0.9f, 0.9f);
    }

    public void pouchFilled(Player player) {
        sound(player.getLocation(), "minecraft:item.bundle.insert", 0.9f, 1.0f);
    }

    // ----------------------------------------------------------------
    // Curing
    // ----------------------------------------------------------------

    public void jarPlaced(Location loc) {
        sound(loc, "minecraft:block.decorated_pot.place", 0.9f, 1.2f);
    }

    public void jarAdd(Location loc) {
        particles(loc, w -> leaves(w, center(loc), AURA_AMBER, 4));
        sound(loc, "minecraft:block.decorated_pot.insert", 0.9f, 1.0f);
    }

    /** Particules ambiantes d'une jarre affinee, visibles de loin. */
    public void jarReadyAmbient(Location loc) {
        particles(loc, w -> w.spawnParticle(Particle.END_ROD,
                center(loc).add(0, 0.15, 0), 1, 0.12, 0.1, 0.12, 0.004));
    }

    public void jarReadyChime(Location loc) {
        sound(loc, "minecraft:block.amethyst_block.chime", 0.6f, 1.1f);
    }

    /** Une jarre moisie suinte : spores discretes. */
    public void jarMoldyAmbient(Location loc) {
        particles(loc, w -> w.spawnParticle(Particle.SPORE_BLOSSOM_AIR,
                center(loc).add(0, 0.2, 0), 2, 0.12, 0.12, 0.12, 0.0));
    }

    public void jarCollect(Location loc, boolean moldy) {
        particles(loc, w -> leaves(w, center(loc),
                moldy ? Color.fromRGB(0x6b, 0x72, 0x5a) : AURA_AMBER, 8));
        sound(loc, "minecraft:block.decorated_pot.insert_fail", 0.8f,
                moldy ? 0.6f : 1.2f);
        if (!moldy) {
            sound(loc, "minecraft:block.amethyst_block.resonate", 0.5f, 1.3f);
        }
    }

    public void jarBroken(Location loc) {
        particles(loc, w -> w.spawnParticle(Particle.BLOCK, center(loc), 14,
                0.2, 0.2, 0.2, 0.0, Material.GLASS.createBlockData()));
        sound(loc, "minecraft:block.decorated_pot.break", 0.8f, 1.3f);
    }

    // ----------------------------------------------------------------
    // Consommation
    // ----------------------------------------------------------------

    public void jointLit(Player player) {
        sound(player.getLocation(), "minecraft:item.flintandsteel.use", 0.7f, 1.2f);
        sound(player.getLocation(), "minecraft:block.campfire.crackle", 1.0f, 1.0f);
    }

    /** Fumee en spirale qui s'echappe de la tete du joueur, visible par tous. */
    public void smokePuff(Player player) {
        particles(player.getLocation(), w -> {
            Location head = player.getEyeLocation().add(0, 0.25, 0);
            double phase = (player.getTicksLived() % 60) / 60.0 * Math.PI * 2;
            for (int i = 0; i < 3; i++) {
                double angle = phase + i * 0.9;
                double radius = 0.10 + i * 0.05;
                Location at = head.clone().add(
                        Math.cos(angle) * radius,
                        i * 0.14,
                        Math.sin(angle) * radius);
                w.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, at, 0,
                        Math.cos(angle + 1.2) * 0.015,
                        0.045,
                        Math.sin(angle + 1.2) * 0.015, 1.0);
            }
        });
    }

    /** Aura coloree discrete en peripherie du joueur pendant le high. */
    public void highAura(Player player, double intensity) {
        particles(player.getLocation(), w -> {
            ThreadLocalRandom rng = ThreadLocalRandom.current();
            int count = 1 + (int) Math.round(2 * intensity);
            for (int i = 0; i < count; i++) {
                double angle = rng.nextDouble(Math.PI * 2);
                double radius = 1.6 + rng.nextDouble(0.7);
                Location at = player.getLocation().add(
                        Math.cos(angle) * radius,
                        0.6 + rng.nextDouble(1.2),
                        Math.sin(angle) * radius);
                Color color = rng.nextBoolean() ? AURA_PURPLE : AURA_AMBER;
                w.spawnParticle(Particle.DUST, at, 1,
                        new Particle.DustOptions(color, 0.7f));
            }
        });
    }

    public void comedownStart(Player player) {
        sound(player.getLocation(), "minecraft:block.respawn_anchor.deplete", 0.5f, 0.7f);
    }

    public void blackoutHit(Player player) {
        sound(player.getLocation(), "minecraft:entity.player.breath", 0.9f, 0.5f);
    }

    public void heartbeat(Player player) {
        player.playSound(player.getLocation(), "minecraft:entity.warden.heartbeat", 1.0f, 1.0f);
    }

    public void withdrawalShiver(Player player) {
        player.playSound(player.getLocation(), "minecraft:entity.warden.heartbeat", 0.6f, 1.3f);
    }

    public void wakeUp(Player player) {
        player.playSound(player.getLocation(), "minecraft:block.amethyst_block.resonate", 0.8f, 0.6f);
    }

    // ----------------------------------------------------------------
    // Interne
    // ----------------------------------------------------------------

    private void particles(Location loc, java.util.function.Consumer<World> spawner) {
        if (config.particlesEnabled() && loc.getWorld() != null) {
            spawner.accept(loc.getWorld());
        }
    }

    private void sound(Location loc, String key, float volume, float pitch) {
        if (config.soundsEnabled() && loc.getWorld() != null) {
            loc.getWorld().playSound(loc, key, volume, pitch);
        }
    }

    private void leaves(World world, Location at, Color color, int count) {
        world.spawnParticle(Particle.TINTED_LEAVES, at, count, 0.25, 0.25, 0.25, 0.0, color);
    }

    private static Location center(Location loc) {
        return loc.clone().add(0.5, 0.4, 0.5);
    }

    private static Location soil(Location loc) {
        return loc.clone().add(0.5, 0.42, 0.5);
    }

    private static Location above(Location loc) {
        return loc.clone().add(0.5, 1.05, 0.5);
    }

    private static Location plantHeart(Location loc) {
        return loc.clone().add(0.5, 0.85, 0.5);
    }
}
