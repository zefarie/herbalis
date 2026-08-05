package io.github.zefarie.herbalis.infrastructure.listener;

import io.github.zefarie.herbalis.domain.consumption.ChatSlur;
import io.github.zefarie.herbalis.domain.drug.SlurStyle;
import io.github.zefarie.herbalis.infrastructure.config.Messages;
import io.github.zefarie.herbalis.infrastructure.effects.EffectService;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Deforme les messages des joueurs sous effet : elocution trainante
 * pendant le high, begaiement d'ivresse, charabia en plein blackout.
 * Priorite LOW : le message est deforme avant que les plugins de chat
 * ne le formatent.
 */
public final class ChatListener implements Listener {

    private static final PlainTextComponentSerializer PLAIN =
            PlainTextComponentSerializer.plainText();

    private final EffectService effects;
    private final Messages messages;

    public ChatListener(EffectService effects, Messages messages) {
        this.effects = effects;
        this.messages = messages;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onChat(AsyncChatEvent event) {
        var params = effects.chatSlurFor(event.getPlayer().getUniqueId(),
                System.currentTimeMillis());
        if (params.isEmpty()) {
            return;
        }
        String plain = PLAIN.serialize(event.message());
        String slurred = ChatSlur.apply(plain, params.get().style(),
                params.get().intensity(), tics(params.get().style()),
                ThreadLocalRandom.current());
        if (!slurred.equals(plain)) {
            event.message(Component.text(slurred));
        }
    }

    private List<String> tics(SlurStyle style) {
        return switch (style) {
            case STONED -> messages.rawList("consommation.tics-defonce");
            case DRUNK -> messages.rawList("consommation.tics-ivre");
            case NONE -> List.of();
        };
    }
}
