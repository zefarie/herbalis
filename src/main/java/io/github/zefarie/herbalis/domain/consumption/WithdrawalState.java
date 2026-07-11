package io.github.zefarie.herbalis.domain.consumption;

/**
 * Etat de manque d'un joueur addict.
 */
public enum WithdrawalState {
    /** Pas de manque : non addict, ou consommation recente. */
    NONE,
    /** Manque actif : symptomes periodiques jusqu'a consommation ou desintoxication. */
    WITHDRAWAL
}
