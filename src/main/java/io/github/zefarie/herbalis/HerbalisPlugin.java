package io.github.zefarie.herbalis;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Point d'entree du plugin Herbalis.
 * Le cablage complet des couches (domaine, application, infrastructure)
 * est effectue dans {@code onEnable}.
 */
public final class HerbalisPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("Herbalis initialise.");
    }

    @Override
    public void onDisable() {
        getLogger().info("Herbalis arrete.");
    }
}
