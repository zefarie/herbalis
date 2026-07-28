package io.github.zefarie.herbalis.domain.consumption;

import io.github.zefarie.herbalis.domain.drug.SlurStyle;

import java.util.List;
import java.util.random.RandomGenerator;

/**
 * Deforme les messages de chat d'un joueur sous effet. Transformations
 * pures et sans etat : l'appelant fournit le style, l'intensite du
 * moment, les tics de langage (depuis messages.yml) et la source
 * d'aleatoire. Le message reste lisible, meme a intensite maximale :
 * on etire et on begaie, on ne remplace jamais les mots.
 */
public final class ChatSlur {

    /** Deformation a appliquer a un joueur : style et intensite 0 a 1. */
    public record Params(SlurStyle style, double intensity) {
    }

    private static final String VOWELS = "aeiouyàâäéèêëîïôöùûüAEIOUY";

    // Chances par mot (ou par jonction) a intensite 1.
    private static final double STRETCH_CHANCE = 0.45;
    private static final double PAUSE_CHANCE = 0.25;
    private static final double SOFTEN_CHANCE = 0.6;
    private static final double STUTTER_CHANCE = 0.35;
    private static final double DOUBLE_CHANCE = 0.25;
    private static final double HICCUP_CHANCE = 0.18;
    private static final double TIC_CHANCE = 0.5;

    private ChatSlur() {
    }

    /**
     * Applique la deformation. Retourne le message inchange si le style
     * est {@link SlurStyle#NONE} ou l'intensite nulle.
     */
    public static String apply(String message, SlurStyle style, double intensity,
                               List<String> tics, RandomGenerator random) {
        if (style == SlurStyle.NONE || intensity <= 0 || message.isBlank()) {
            return message;
        }
        double strength = Math.clamp(intensity, 0.0, 1.0);
        return switch (style) {
            case STONED -> stoned(message, strength, tics, random);
            case DRUNK -> drunk(message, strength, tics, random);
            case NONE -> message;
        };
    }

    /**
     * Elocution defoncee : voyelles etirees, pauses qui trainent,
     * exclamations adoucies, un tic amuse en fin de phrase.
     */
    private static String stoned(String message, double strength,
                                 List<String> tics, RandomGenerator random) {
        String[] words = message.split(" ");
        StringBuilder out = new StringBuilder(message.length() + 16);
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            if (random.nextDouble() < STRETCH_CHANCE * strength) {
                word = stretchVowel(word, random);
            }
            if (i > 0) {
                out.append(random.nextDouble() < PAUSE_CHANCE * strength ? " ... " : " ");
            }
            out.append(word);
        }
        String result = out.toString();
        if (result.endsWith("!") && random.nextDouble() < SOFTEN_CHANCE * strength) {
            result = result.replaceAll("!+$", "...");
        }
        return appendTic(result, tics, strength, random);
    }

    /**
     * Elocution d'ivresse : begaiement en debut de mot, lettres
     * doublees, hoquets entre les mots.
     */
    private static String drunk(String message, double strength,
                                List<String> tics, RandomGenerator random) {
        String[] words = message.split(" ");
        StringBuilder out = new StringBuilder(message.length() + 16);
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            if (word.length() >= 3 && Character.isLetter(word.charAt(0))
                    && random.nextDouble() < STUTTER_CHANCE * strength) {
                word = word.charAt(0) + "-" + word;
            } else if (word.length() >= 4
                    && random.nextDouble() < DOUBLE_CHANCE * strength) {
                word = doubleLetter(word, random);
            }
            if (i > 0) {
                out.append(' ');
                if (!tics.isEmpty() && random.nextDouble() < HICCUP_CHANCE * strength) {
                    out.append(tics.get(random.nextInt(tics.size()))).append(' ');
                }
            }
            out.append(word);
        }
        return out.toString();
    }

    /** Etire une voyelle du mot (aleatoire), 2 a 4 repetitions en plus. */
    private static String stretchVowel(String word, RandomGenerator random) {
        int[] positions = new int[word.length()];
        int count = 0;
        for (int i = 0; i < word.length(); i++) {
            if (VOWELS.indexOf(word.charAt(i)) >= 0) {
                positions[count++] = i;
            }
        }
        if (count == 0) {
            return word;
        }
        int at = positions[random.nextInt(count)];
        int extra = 2 + random.nextInt(3);
        return word.substring(0, at)
                + String.valueOf(word.charAt(at)).repeat(1 + extra)
                + word.substring(at + 1);
    }

    /** Double une lettre interieure du mot. */
    private static String doubleLetter(String word, RandomGenerator random) {
        int at = 1 + random.nextInt(word.length() - 2);
        if (!Character.isLetter(word.charAt(at))) {
            return word;
        }
        return word.substring(0, at + 1) + word.charAt(at) + word.substring(at + 1);
    }

    private static String appendTic(String message, List<String> tics,
                                    double strength, RandomGenerator random) {
        if (tics.isEmpty() || random.nextDouble() >= TIC_CHANCE * strength) {
            return message;
        }
        return message + " " + tics.get(random.nextInt(tics.size()));
    }
}
