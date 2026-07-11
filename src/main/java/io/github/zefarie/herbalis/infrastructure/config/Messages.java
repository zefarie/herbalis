package io.github.zefarie.herbalis.infrastructure.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Acces centralise a messages.yml. Tous les textes joueur passent par
 * ici, en MiniMessage exclusivement.
 */
public final class Messages {

    private volatile FileConfiguration yaml;
    private final MiniMessage mini = MiniMessage.miniMessage();
    private final TagResolver prefixResolver;

    public Messages(FileConfiguration yaml) {
        this.yaml = yaml;
        this.prefixResolver = TagResolver.resolver("prefix",
                (args, ctx) -> net.kyori.adventure.text.minimessage.tag.Tag
                        .selfClosingInserting(mini.deserialize(
                                this.yaml.getString("general.prefix", ""))));
    }

    /** Recharge messages.yml a chaud. */
    public void reload(FileConfiguration newYaml) {
        this.yaml = newYaml;
    }

    /** Message simple par cle, avec placeholders optionnels. */
    public Component msg(String key, TagResolver... resolvers) {
        String raw = yaml.getString(key);
        if (raw == null) {
            return Component.text("[message manquant : " + key + "]");
        }
        return deserialize(raw, resolvers);
    }

    /** Une entree aleatoire d'une liste de messages (lignes d'ambiance). */
    public Component random(String key, TagResolver... resolvers) {
        List<String> options = yaml.getStringList(key);
        if (options.isEmpty()) {
            return msg(key, resolvers);
        }
        String raw = options.get(ThreadLocalRandom.current().nextInt(options.size()));
        return deserialize(raw, resolvers);
    }

    /** Chaine brute de la config (pour composer des placeholders). */
    public String raw(String key, String fallback) {
        return yaml.getString(key, fallback);
    }

    /** Deserialise une chaine MiniMessage arbitraire avec le prefixe dispo. */
    public Component deserialize(String raw, TagResolver... resolvers) {
        TagResolver all = TagResolver.resolver(
                TagResolver.resolver(resolvers), prefixResolver);
        return mini.deserialize(raw, all);
    }

    /** Liste de composants (lore d'items). */
    public List<Component> list(String key, TagResolver... resolvers) {
        return yaml.getStringList(key).stream()
                .map(line -> deserialize(line, resolvers))
                .toList();
    }

    public static TagResolver ph(String name, String value) {
        return Placeholder.parsed(name, value);
    }

    public static TagResolver ph(String name, Component value) {
        return Placeholder.component(name, value);
    }
}
