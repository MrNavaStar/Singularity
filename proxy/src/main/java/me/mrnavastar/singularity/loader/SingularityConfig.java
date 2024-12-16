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
import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SingularityConfig {

    @Getter
    public static class SyncGroup {
        private final String name;
        private final Set<ProtoServer> servers = Sets.newConcurrentHashSet();
        private final Settings settings;
        private final ConcurrentHashMap<String, DataStore> topics = new ConcurrentHashMap<>();

        public SyncGroup(String name, Settings settings) {
            this.name = name;
            this.settings = settings;
            topics.put(Constants.PLAYER_TOPIC, SQLib.getDatabase().dataStore(Constants.SINGULARITY_ID,  "static_" + name + "_player_data"));
        }

        public SyncGroup() {
            this.name = "default";
            this.settings = new Settings();
            topics.put(Constants.PLAYER_TOPIC, SQLib.getDatabase().dataStore(Constants.SINGULARITY_ID,  "default_player_data"));
        }

        public void addServer(ProtoServer server) {
            servers.add(server);
        }

        public DataStore getTopicStore(Topic topic) {
            return Optional.ofNullable(topics.get(topic.topic())).orElseGet(() -> {
                DataStore store = SQLib.getDatabase().dataStore(Constants.SINGULARITY_ID,  "static_" + name + "_" + topic.databaseKey());
                topics.put(topic.topic(), store);
                return store;
            });
        }
    }

    private static final ConcurrentHashMap<String, SyncGroup> groups = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, DataStore> globalStores = new ConcurrentHashMap<>();
    private static final ArrayList<String> blacklists = new ArrayList<>();

    static {
        registerBlacklist("singularity.ender");
        registerBlacklist("singularity.food");
        registerBlacklist("singularity.gamemode");
        registerBlacklist("singularity.health");
        registerBlacklist("singularity.inventory");
        registerBlacklist("singularity.location");
        registerBlacklist("singularity.score");
        registerBlacklist("singularity.spawn");
        registerBlacklist("singularity.xp");
    }

    public static Optional<SyncGroup> getSyncGroup(ProtoServer server) {
        System.out.println("Sync Group in: " + server);

        Optional<SyncGroup> g =groups.values().stream().filter(group -> group.getServers().contains(server)).findFirst();

        System.out.println(g);
        return g;
    }

    public static DataStore getGlobalStore(Topic topic) {
        return Optional.ofNullable(globalStores.get(topic.topic())).orElseGet(() -> {
            DataStore store = SQLib.getDatabase().dataStore(Constants.SINGULARITY_ID,  "global_" + topic.databaseKey());
            globalStores.put(topic.topic(), store);
            return store;
        });
    }

    public static void registerBlacklist(String name) {
        blacklists.add(name);
    }

    // Yeah ik this function is awful but like gimme a break
    @SuppressWarnings("unchecked")
    public static void load(Logger logger) {
        try {
            Yaml yaml = new Yaml();
            Map<String, Object> map = yaml.load(new FileInputStream("config/singularity.yaml"));

            Optional.ofNullable(map.get("groups")).ifPresent(o1 -> {
                if (!(o1 instanceof Map g)) return;

                g.forEach((k, v) -> {
                    if (!(k instanceof String groupName) || !(v instanceof String servers)) return;

                    Optional.ofNullable(map.get(groupName)).ifPresent(o2 -> {
                        if (!(o2 instanceof Map s)) return;

                        Settings groupSettings = new Settings().setDefault();
                        blacklists.forEach(blacklist -> {
                            if (s.get(blacklist) instanceof Boolean enabled) {
                                if (enabled) groupSettings.nbtBlacklists.remove(blacklist);
                                else groupSettings.nbtBlacklists.add(blacklist);
                            }
                        });

                        if (s.get("singularity.player") instanceof Boolean enabled) groupSettings.syncPlayerData = enabled;
                        if (s.get("singularity.stats") instanceof Boolean enabled) groupSettings.syncPlayerStats = enabled;
                        if (s.get("singularity.advancements") instanceof Boolean enabled) groupSettings.syncPlayerAdvancements = enabled;
                        if (s.get("singularity.ops") instanceof Boolean enabled) groupSettings.syncOps = enabled;
                        if (s.get("singularity.whitelist") instanceof Boolean enabled) groupSettings.syncWhitelist = enabled;
                        if (s.get("singularity.bans") instanceof Boolean enabled) groupSettings.syncBans = enabled;

                        SyncGroup store = new SyncGroup(groupName, groupSettings);
                        groups.put("config_" + groupName, store);

                        ProtoProxy.getRegisteredServers().stream()
                                .filter(server -> List.of(servers.split("\n")).contains(server.getName()))
                                .forEach(store::addServer);
                    });
                });
            });
        } catch (FileNotFoundException ignore) {
            logger.info("No config found, loading defaults");

            SyncGroup group = new SyncGroup();
            groups.put(group.getName(), group);
            ProtoProxy.getRegisteredServers().forEach(group::addServer);
        }
    }
}
