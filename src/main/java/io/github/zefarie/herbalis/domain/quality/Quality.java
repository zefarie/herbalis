package io.github.zefarie.herbalis.domain.quality;

/**
 * Qualite d'un produit, de 1 a 5 etoiles.
 */
public record Quality(int stars) implements Comparable<Quality> {

    public static final int MIN = 1;
    public static final int MAX = 5;

    public Quality {
        if (stars < MIN || stars > MAX) {
            throw new IllegalArgumentException("Qualite hors bornes : " + stars);
        }
    }

    public static Quality of(int stars) {
        return new Quality(Math.clamp(stars, MIN, MAX));
    }

    /** Convertit un score entre 0 et 1 en etoiles. */
    public static Quality fromScore(double score) {
        double clamped = Math.clamp(score, 0.0, 1.0);
        return new Quality(MIN + (int) Math.round(clamped * (MAX - MIN)));
    }

    /** Fraction du maximum, utile pour les interpolations (0 a 1). */
    public double ratio() {
        return (stars - MIN) / (double) (MAX - MIN);
    }

    @Override
    public int compareTo(Quality other) {
        return Integer.compare(stars, other.stars);
    }
}
