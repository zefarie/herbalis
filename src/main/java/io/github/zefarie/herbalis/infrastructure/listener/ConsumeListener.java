package io.github.zefarie.herbalis.infrastructure.listener;

import io.github.zefarie.herbalis.application.usecase.ConsumeUseCase;
import io.github.zefarie.herbalis.domain.quality.Quality;
import io.github.zefarie.herbalis.infrastructure.config.Messages;
import io.github.zefarie.herbalis.infrastructure.effects.EffectService;
import io.github.zefarie.herbalis.infrastructure.item.HerbalisItemType;
import io.github.zefarie.herbalis.infrastructure.item.ItemKeys;
import io.github.zefarie.herbalis.domain.drug.DrugRegistry;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;

/**
 * Consommation d'un joint : le composant consumable vanilla gere
 * l'animation, cet ecouteur declenche la chimie.
 */
public final class ConsumeListener implements Listener {

    private final Messages messages;
    private final DrugRegistry drugs;
    private final ConsumeUseCase consume;
    private final EffectService effects;

    public ConsumeListener(Messages messages, DrugRegistry drugs,
                           ConsumeUseCase consume, EffectService effects) {
        this.messages = messages;
        this.drugs = drugs;
        this.consume = consume;
        this.effects = effects;
    }

    @EventHandler(ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        if (ItemKeys.typeOf(event.getItem())
                .filter(type -> type == HerbalisItemType.JOINT).isEmpty()) {
            return;
        }
        Player player = event.getPlayer();
        if (!player.hasPermission("herbalis.consume")) {
            event.setCancelled(true);
            player.sendMessage(messages.msg("erreurs.permission"));
            return;
        }
        String drugId = ItemKeys.drugOf(event.getItem()).orElse("");
        Quality quality = ItemKeys.qualityOf(event.getItem()).orElse(Quality.of(1));
        long now = System.currentTimeMillis();

        consume.execute(player.getUniqueId(), drugId, quality, now)
                .ifPresent(outcome -> drugs.byId(drugId).ifPresent(drug ->
                        effects.start(player, drug, outcome, now)));
    }
}
