package io.github.zefarie.herbalis.infrastructure.listener;

import io.github.zefarie.herbalis.application.usecase.ConsumeUseCase;
import io.github.zefarie.herbalis.domain.drug.DrugRegistry;
import io.github.zefarie.herbalis.domain.drug.DrugType;
import io.github.zefarie.herbalis.domain.quality.Quality;
import io.github.zefarie.herbalis.infrastructure.config.Messages;
import io.github.zefarie.herbalis.infrastructure.effects.EffectService;
import io.github.zefarie.herbalis.infrastructure.fx.Fx;
import io.github.zefarie.herbalis.infrastructure.item.HerbalisItemType;
import io.github.zefarie.herbalis.infrastructure.item.ItemFactory;
import io.github.zefarie.herbalis.infrastructure.item.ItemKeys;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Consommation d'un joint, taffe par taffe : le composant consumable
 * vanilla gere l'animation, cet ecouteur declenche la chimie et rend le
 * joint entame tant qu'il reste des taffes. Clic droit sur un joueur :
 * on lui passe le joint.
 */
public final class ConsumeListener implements Listener {

    private final Messages messages;
    private final DrugRegistry drugs;
    private final ItemFactory items;
    private final Fx fx;
    private final ConsumeUseCase consume;
    private final EffectService effects;

    public ConsumeListener(Messages messages, DrugRegistry drugs,
                           ItemFactory items, Fx fx,
                           ConsumeUseCase consume, EffectService effects) {
        this.messages = messages;
        this.drugs = drugs;
        this.items = items;
        this.fx = fx;
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
        DrugType drug = drugs.byId(drugId).orElse(null);
        Quality quality = ItemKeys.qualityOf(event.getItem()).orElse(Quality.of(1));
        long now = System.currentTimeMillis();

        // Une taffe : le joint diminue et revient en main s'il en reste.
        // (Les joints d'avant les taffes comptent comme des joints frais.)
        if (drug != null) {
            int puffs = ItemKeys.puffsOf(event.getItem())
                    .orElse(drug.consumption().puffsPerJoint());
            if (puffs > 1) {
                event.setReplacement(items.joint(drug, quality, puffs - 1));
            }
        }

        consume.execute(player.getUniqueId(), drugId, quality, now)
                .ifPresent(outcome -> drugs.byId(drugId).ifPresent(d ->
                        effects.start(player, d, outcome, now)));
    }

    /** Passer le joint : clic droit sur un joueur, le joint change de main. */
    @EventHandler(ignoreCancelled = true)
    public void onPassJoint(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND
                || !(event.getRightClicked() instanceof Player target)) {
            return;
        }
        Player giver = event.getPlayer();
        ItemStack held = giver.getInventory().getItemInMainHand();
        if (ItemKeys.typeOf(held)
                .filter(type -> type == HerbalisItemType.JOINT).isEmpty()) {
            return;
        }
        event.setCancelled(true);

        ItemStack passed = held.clone();
        passed.setAmount(1);
        held.subtract();
        // Directement dans la main libre du receveur, sinon l'inventaire.
        if (target.getInventory().getItemInMainHand().isEmpty()) {
            target.getInventory().setItemInMainHand(passed);
        } else {
            target.getInventory().addItem(passed).values().forEach(rest ->
                    target.getWorld().dropItemNaturally(target.getLocation(), rest));
        }
        fx.jointPassed(giver, target);
        fx.actionBar(giver, messages.msg("consommation.joint-passe",
                Messages.ph("joueur", target.getName())));
        fx.actionBar(target, messages.msg("consommation.joint-recu",
                Messages.ph("joueur", giver.getName())));
    }
}
