package io.github.zefarie.herbalis.domain;

import io.github.zefarie.herbalis.domain.consumption.ChatSlur;
import io.github.zefarie.herbalis.domain.drug.SlurStyle;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatSlurTest {

    private static final String MESSAGE = "salut les gars ca va bien ce soir";
    private static final List<String> NO_TICS = List.of();

    @Test
    void styleAucunLaisseLeMessageIntact() {
        assertEquals(MESSAGE,
                ChatSlur.apply(MESSAGE, SlurStyle.NONE, 1.0, NO_TICS, new Random(1)));
    }

    @Test
    void intensiteNulleLaisseLeMessageIntact() {
        assertEquals(MESSAGE,
                ChatSlur.apply(MESSAGE, SlurStyle.STONED, 0.0, NO_TICS, new Random(1)));
        assertEquals(MESSAGE,
                ChatSlur.apply(MESSAGE, SlurStyle.DRUNK, -0.5, NO_TICS, new Random(1)));
    }

    @Test
    void messageVideIntact() {
        assertEquals("", ChatSlur.apply("", SlurStyle.STONED, 1.0, NO_TICS, new Random(1)));
        assertEquals("   ", ChatSlur.apply("   ", SlurStyle.DRUNK, 1.0, NO_TICS, new Random(1)));
    }

    @Test
    void defonceDeformeAPleineIntensite() {
        int changed = 0;
        for (int seed = 0; seed < 50; seed++) {
            String out = ChatSlur.apply(MESSAGE, SlurStyle.STONED, 1.0,
                    NO_TICS, new Random(seed));
            // On etire et on insere, on ne retire jamais rien.
            assertTrue(out.length() >= MESSAGE.length());
            if (!out.equals(MESSAGE)) {
                changed++;
            }
        }
        assertTrue(changed >= 45, "deformation trop rare : " + changed + "/50");
    }

    @Test
    void defonceEtireLesVoyellesEtInsereDesPauses() {
        boolean stretched = false;
        boolean paused = false;
        for (int seed = 0; seed < 50; seed++) {
            String out = ChatSlur.apply(MESSAGE, SlurStyle.STONED, 1.0,
                    NO_TICS, new Random(seed));
            stretched |= out.matches(".*([aeiouy])\\1\\1.*");
            paused |= out.contains(" ... ");
        }
        assertTrue(stretched, "aucune voyelle etiree sur 50 seeds");
        assertTrue(paused, "aucune pause inseree sur 50 seeds");
    }

    @Test
    void defonceAjouteUnTicEnFinDePhrase() {
        boolean ticced = false;
        for (int seed = 0; seed < 50; seed++) {
            String out = ChatSlur.apply(MESSAGE, SlurStyle.STONED, 1.0,
                    List.of("héhé"), new Random(seed));
            ticced |= out.endsWith(" héhé");
        }
        assertTrue(ticced, "aucun tic ajoute sur 50 seeds");
    }

    @Test
    void defonceSansVoyelleNiTicLaisseUnMotSeulIntact() {
        for (int seed = 0; seed < 20; seed++) {
            assertEquals("zzz", ChatSlur.apply("zzz", SlurStyle.STONED, 1.0,
                    NO_TICS, new Random(seed)));
        }
    }

    @Test
    void ivreBegaieEtDoubleDesLettres() {
        boolean stuttered = false;
        boolean doubled = false;
        for (int seed = 0; seed < 50; seed++) {
            String out = ChatSlur.apply("bonjour tout le monde", SlurStyle.DRUNK, 1.0,
                    NO_TICS, new Random(seed));
            stuttered |= out.contains("b-bonjour") || out.contains("t-tout")
                    || out.contains("m-monde");
            doubled |= !out.contains("-") && !out.equals("bonjour tout le monde");
        }
        assertTrue(stuttered, "aucun begaiement sur 50 seeds");
        assertTrue(doubled, "aucune lettre doublee sur 50 seeds");
    }

    @Test
    void ivreInsereDesHoquetsEntreLesMots() {
        boolean hiccuped = false;
        for (int seed = 0; seed < 50; seed++) {
            String out = ChatSlur.apply("bonjour tout le monde", SlurStyle.DRUNK, 1.0,
                    List.of("*hips*"), new Random(seed));
            hiccuped |= out.contains(" *hips* ");
        }
        assertTrue(hiccuped, "aucun hoquet insere sur 50 seeds");
    }

    @Test
    void intensiteFaibleDeformeMoinsSouvent() {
        int weak = 0;
        int strong = 0;
        for (int seed = 0; seed < 100; seed++) {
            if (!ChatSlur.apply(MESSAGE, SlurStyle.STONED, 0.15,
                    NO_TICS, new Random(seed)).equals(MESSAGE)) {
                weak++;
            }
            if (!ChatSlur.apply(MESSAGE, SlurStyle.STONED, 1.0,
                    NO_TICS, new Random(seed)).equals(MESSAGE)) {
                strong++;
            }
        }
        assertTrue(weak < strong, "intensite faible (" + weak
                + ") devrait deformer moins que forte (" + strong + ")");
    }
}
