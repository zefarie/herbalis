package io.github.zefarie.herbalis.infrastructure.hud;

import io.github.zefarie.herbalis.domain.geo.BlockPos;
import io.github.zefarie.herbalis.infrastructure.config.HerbalisConfig;
import io.github.zefarie.herbalis.infrastructure.render.PosCodec;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Hologrammes d'etat : quand un joueur regarde une plante, un pot, un
 * rack ou une jarre, un TextDisplay prive flotte au-dessus avec ses
 * informations (eau, terreau, qualite, alertes). Chaque joueur a son
 * propre hologramme, invisible pour les autres.
 */
public final class HologramService {

    /** Voile sombre translucide derriere le texte. */
    private static final Color BACKGROUND = Color.fromARGB(0x52000000);

    /** Portee d'affichage du texte (fraction de la distance de rendu). */
    private static final float VIEW_RANGE = 0.2f;

    private final Plugin plugin;
    private final HerbalisConfig config;
    private final HudService hud;
    private final Map<UUID, TextDisplay> displays = new HashMap<>();
    private final Map<UUID, BlockPos> targets = new HashMap<>();

    public HologramService(Plugin plugin, HerbalisConfig config, HudService hud) {
        this.plugin = plugin;
        this.config = config;
        this.hud = hud;
    }

    /** Tick regulier : suit la cible du regard du joueur. */
    public void tick(Player player, long now) {
        if (!config.hudEnabled()) {
            hide(player);
            return;
        }
        BlockPos pos = hud.targetPos(player).orElse(null);
        if (pos == null) {
            hide(player);
            return;
        }
        HudService.Hologram hologram = hud.buildHologram(pos, now).orElse(null);
        if (hologram == null) {
            hide(player);
            return;
        }
        show(player, pos, hologram);
    }

    private void show(Player player, BlockPos pos, HudService.Hologram hologram) {
        Location loc = PosCodec.corner(pos)
                .map(corner -> corner.add(0.5, hologram.height(), 0.5))
                .orElse(null);
        if (loc == null) {
            hide(player);
            return;
        }
        TextDisplay display = displays.get(player.getUniqueId());
        if (display == null || !display.isValid()) {
            display = spawn(player, loc);
            if (display == null) {
                return;
            }
            displays.put(player.getUniqueId(), display);
        } else if (!display.getWorld().equals(loc.getWorld())
                || display.getLocation().distanceSquared(loc) > 1.0e-3) {
            display.teleport(loc);
        }
        targets.put(player.getUniqueId(), pos);
        display.text(hologram.text());
    }

    private TextDisplay spawn(Player player, Location loc) {
        if (loc.getWorld() == null) {
            return null;
        }
        return loc.getWorld().spawn(loc, TextDisplay.class, display -> {
            display.setPersistent(false);
            display.setVisibleByDefault(false);
            display.setBillboard(Display.Billboard.CENTER);
            display.setAlignment(TextDisplay.TextAlignment.CENTER);
            display.setShadowed(false);
            display.setSeeThrough(false);
            display.setBackgroundColor(BACKGROUND);
            display.setViewRange(VIEW_RANGE);
            player.showEntity(plugin, display);
        });
    }

    /** Retire l'hologramme du joueur, s'il en a un. */
    public void hide(Player player) {
        TextDisplay display = displays.remove(player.getUniqueId());
        targets.remove(player.getUniqueId());
        if (display != null) {
            display.remove();
        }
    }

    /** Retire tous les hologrammes (arret du plugin). */
    public void clearAll() {
        displays.values().forEach(TextDisplay::remove);
        displays.clear();
        targets.clear();
    }
}
