package me.mrnavastar.singularity.loader;

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
    public static class GroupStore {
        private final String groupName;
        private final ConcurrentHashMap<String, DataStore> topics = new ConcurrentHashMap<>();

        public GroupStore(String groupName) {
            this.groupName = groupName;
            topics.put(Constants.PLAYER_TOPIC, SQLib.getDatabase().dataStore(Constants.SINGULARITY_ID,  "static_" + groupName + "_player_data"));
        }

        public GroupStore() {
            this.groupName = "default";
            topics.put(Constants.PLAYER_TOPIC, SQLib.getDatabase().dataStore(Constants.SINGULARITY_ID,  "default_player_data"));
        }

        public DataStore getTopicStore(Topic topic) {
            return Optional.ofNullable(topics.get(topic.topic())).orElseGet(() -> {
                DataStore store = SQLib.getDatabase().dataStore(Constants.SINGULARITY_ID,  "static_" + groupName + "_" + topic.databaseKey());
                topics.put(topic.topic(), store);
                return store;
            });
        }
    }

    private static final ConcurrentHashMap<ProtoServer, String> groups = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<ProtoServer, Settings> settings = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, GroupStore> groupStores = new ConcurrentHashMap<>();
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

    public static Optional<Settings> getServerSettings(ProtoServer server) {
        return Optional.ofNullable(settings.get(server));
    }

    public static Optional<GroupStore> getServerStore(ProtoServer server) {
        return Optional.ofNullable(groups.get(server)).map(groupStores::get);
    }

    public static DataStore getGlobalStore(Topic topic) {
        return Optional.ofNullable(globalStores.get(topic.topic())).orElseGet(() -> {
            DataStore store = SQLib.getDatabase().dataStore(Constants.SINGULARITY_ID,  "global_" + topic.databaseKey());
            globalStores.put(topic.topic(), store);
            return store;
        });
    }

    public static List<ProtoServer> getSameGroup(ProtoServer server) {
         return Optional.ofNullable(groups.get(server))
                 .map(mainGroup -> groups.entrySet().stream().filter(entry -> entry.getValue().equals(mainGroup) && !entry.getKey().equals(server))
                    .map(Map.Entry::getKey).toList()).orElseGet(ArrayList::new);
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

                        groupStores.put(groupName, new GroupStore(groupName));

                        ProtoProxy.getRegisteredServers().stream()
                                .filter(server -> List.of(servers.split("\n")).contains(server.getName()))
                                .forEach(server -> {
                                    settings.put(server, groupSettings);
                                    groups.put(server, "config_" + groupName);
                                });
                    });
                });
            });
        } catch (FileNotFoundException ignore) {
            logger.info("No config found, loading defaults");

            groupStores.put("default", new GroupStore());
            ProtoProxy.getRegisteredServers().forEach(server -> {
                settings.put(server, new Settings().setDefault());
                groups.put(server, "default");
            });
        }
    }
}
