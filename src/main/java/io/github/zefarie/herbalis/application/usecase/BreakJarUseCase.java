package io.github.zefarie.herbalis.application.usecase;

import io.github.zefarie.herbalis.application.port.JarRepository;
import io.github.zefarie.herbalis.domain.curing.CuringJar;
import io.github.zefarie.herbalis.domain.geo.BlockPos;

import java.util.Optional;

/**
 * Casse d'une jarre de curing. Le contenu est rendu tel quel
 * (l'affinage en cours est perdu).
 */
public final class BreakJarUseCase {

    private final JarRepository jars;

    public BreakJarUseCase(JarRepository jars) {
        this.jars = jars;
    }

    /** @return la jarre detruite avec son contenu, si presente */
    public Optional<CuringJar> execute(BlockPos pos) {
        Optional<CuringJar> jar = jars.at(pos);
        jar.ifPresent(j -> jars.remove(pos));
        return jar;
    }
}
