package me.mrnavastar.singularity.loader;

import com.google.common.collect.Sets;
import lombok.Getter;
import me.mrnavastar.protoweaver.proxy.api.ProtoProxy;
import me.mrnavastar.protoweaver.proxy.api.ProtoServer;
import me.mrnavastar.singularity.common.Constants;
import me.mrnavastar.singularity.common.networking.Settings;
import me.mrnavastar.singularity.common.networking.Topic;
import me.mrnavastar.sqlib.SQLib;
import me.mrnavastar.sqlib.api.DataStore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentBuilder;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SingularityConfig {

    @Getter
    public static class SyncGroup {
        private final String name;
        private final Set<ProtoServer> servers = Sets.newConcurrentHashSet();
        private final Settings settings;
        private final Set<Pattern> patterns;
        private final ConcurrentHashMap<String, DataStore> topics = new ConcurrentHashMap<>();

        public SyncGroup(String name, String[] patterns) {
            this.name = name;
            this.settings = new Settings().setDefault();
            this.patterns = Arrays.stream(patterns).map(Pattern::compile).collect(Collectors.toSet());
        }

        private void add(ProtoServer server) {
            for (Pattern pattern : patterns) {
                if (pattern.matcher(server.getName()).matches()) {
                    servers.add(server);
                    break;
                }
            }
        }

        private void remove(ProtoServer server) {
            servers.remove(server);
        }

        public DataStore getTopicStore(Topic topic) {
            return Optional.ofNullable(topics.get(topic.topic())).orElseGet(() -> {
                DataStore store = SQLib.getDatabase().dataStore(Constants.SINGULARITY_ID,  "static_" + name + "_" + topic.databaseKey());
                topics.put(topic.topic(), store);
                return store;
            });
        }

        public Component getPrettyName(boolean clickable) {
            String click = clickable ? String.format("<click:run_command:'/singularity config %s'>", name) : "<click:run_command:>";
            return MiniMessage.miniMessage().deserialize((String.format("\n---------------- [ %s<blue>%s</blue></click> ] ----------------", click, name)));
        }

        public Component getPretty() {
            ComponentBuilder<TextComponent, TextComponent.Builder> builder = Component.text();
            builder.append(getPrettyName(true));
            servers.forEach(server -> {
                String color = server.isConnected(Broker.PROTOCOL) ? "green" : "red";
                builder.append(MiniMessage.miniMessage().deserialize(String.format("\n• <i>%s</i> : <%s>%s</%s>", server.getName(), color, server.getAddress(), color)));
            });
            return builder.build();
        }
    }

    private static final File file = new File("plugins/singularity/singularity.yaml");
    private static final ConcurrentHashMap<String, SyncGroup> groups = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, DataStore> globalStores = new ConcurrentHashMap<>();
    @Getter
    private static final ArrayList<String> registeredBlacklists = new ArrayList<>();

    static {
        registerBlacklist("singularity.attributes");
        registerBlacklist("singularity.credits");
        registerBlacklist("singularity.effects");
        registerBlacklist("singularity.ender");
        registerBlacklist("singularity.food");
        registerBlacklist("singularity.health");
        registerBlacklist("singularity.inventory");
        registerBlacklist("singularity.location");
        registerBlacklist("singularity.parrot");
        registerBlacklist("singularity.score");
        registerBlacklist("singularity.scoreboard");
        registerBlacklist("singularity.spawn");
        registerBlacklist("singularity.team");
        registerBlacklist("singularity.vehicle");
        registerBlacklist("singularity.xp");
    }

    public static Collection<SyncGroup> getGroups() {
        return groups.values();
    }

    public static Optional<SyncGroup> getGroup(String name) {
        return Optional.ofNullable(groups.get(name));
    }

    public static Optional<SyncGroup> getSyncGroup(ProtoServer server) {
        return groups.values().stream().filter(group -> group.getServers().contains(server)).findFirst();
    }

    public static void addToSyncGroup(ProtoServer server) {
        groups.forEach((name, group) -> group.add(server));
    }

    public static void removeFromSyncGroup(ProtoServer server) {
        groups.forEach((name, group) -> group.remove(server));
    }

    public static DataStore getGlobalStore(Topic topic) {
        return Optional.ofNullable(globalStores.get(topic.topic())).orElseGet(() -> {
            DataStore store = SQLib.getDatabase().dataStore(Constants.SINGULARITY_ID,  "global_" + topic.databaseKey());
            globalStores.put(topic.topic(), store);
            return store;
        });
    }

    public static void registerBlacklist(String name) {
        registeredBlacklists.add(name);
    }

    // Yeah ik this function is awful but like gimme a break
    @SuppressWarnings("unchecked")
    public static void load(Logger logger, boolean createIfMissing) {
        try {
            Yaml yaml = new Yaml();
            Map<String, Object> map = yaml.load(new FileInputStream(file.getPath()));
            List<ProtoServer> registeredServers = new ArrayList<>(ProtoProxy.getRegisteredServers());

            Optional.ofNullable(map.get("groups")).ifPresent(o1 -> {
                if (!(o1 instanceof Map g)) return;

                g.forEach((k, v) -> {
                    if (!(k instanceof String groupName) || !(v instanceof String servers)) return;

                    SyncGroup group = new SyncGroup(groupName, servers.split("\n"));
                    registeredServers.forEach(group::add);
                    registeredServers.removeAll(group.getServers());
                    groups.put(groupName, group);

                    Optional.ofNullable(map.get(groupName)).ifPresent(o2 -> {
                        if (!(o2 instanceof Map s)) return;

                        registeredBlacklists.forEach(blacklist -> {
                            if (s.get(blacklist) instanceof Boolean enabled) {
                                if (enabled) group.settings.nbtBlacklists.remove(blacklist);
                                else group.settings.nbtBlacklists.add(blacklist);
                            }
                        });

                        if (s.get("singularity.player") instanceof Boolean enabled) group.settings.syncPlayerData = enabled;
                        if (s.get("singularity.gamemode") instanceof Boolean enabled) group.settings.syncPlayerGameMode = enabled;
                        if (s.get("singularity.stats") instanceof Boolean enabled) group.settings.syncPlayerStats = enabled;
                        if (s.get("singularity.advancements") instanceof Boolean enabled) group.settings.syncPlayerAdvancements = enabled;
                        if (s.get("singularity.ops") instanceof Boolean enabled) group.settings.syncOps = enabled;
                        if (s.get("singularity.whitelist") instanceof Boolean enabled) group.settings.syncWhitelist = enabled;
                        if (s.get("singularity.bans") instanceof Boolean enabled) group.settings.syncBans = enabled;
                    });
                });
            });
        } catch (FileNotFoundException ignore) {
            if (!createIfMissing) {
                logger.error("Failed to load config");
                return;
            }
            logger.info("No config found, loading defaults");

            try (InputStream s = SingularityConfig.class.getClassLoader().getResourceAsStream("singularity.yaml")) {
                file.getParentFile().mkdirs();
                file.createNewFile();
                if (s != null) Files.write(file.toPath(), s.readAllBytes());
            } catch (IOException e) {
                logger.error("Failed to load config", e);
            }
            load(logger, false);
        }
    }
}
