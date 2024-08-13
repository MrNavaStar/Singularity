package me.mrnavastar.singularity.loader;

import com.velocitypowered.api.proxy.ProxyServer;
import me.mrnavastar.protoweaver.api.netty.ProtoConnection;
import me.mrnavastar.singularity.common.networking.Settings;
import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.InetSocketAddress;
import java.util.*;

public class SingularityConfig {

    private static final HashMap<InetSocketAddress, String> groups = new HashMap<>();
    private static final HashMap<InetSocketAddress, Settings> settings = new HashMap<>();
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

    public static Optional<Settings> getServerSettings(ProtoConnection server) {
        return Optional.ofNullable(settings.get(server.getRemoteAddress()));
    }

    public static boolean inSameGroup(ProtoConnection server1, ProtoConnection server2) {
        return Objects.equals(groups.get(server1.getRemoteAddress()), groups.get(server2.getRemoteAddress()));
    }

    public static void registerBlacklist(String name) {
        blacklists.add(name);
    }

    // Yeah ik this function is awful but like gimme a break
    @SuppressWarnings("unchecked")
    public static void load(ProxyServer proxy, Logger logger) {
        try {
            Yaml yaml = new Yaml();
            Map<String, Object> map = yaml.load(new FileInputStream("plugins/singularity/singularity.yaml"));

            Optional.ofNullable(map.get("groups")).ifPresent(o1 -> {
                if (!(o1 instanceof Map g)) return;

                g.forEach((k, v) -> {
                    if (!(k instanceof String groupName) || !(v instanceof String servers)) return;

                    Optional.ofNullable(map.get(groupName)).ifPresent(o2 -> {
                        if (!(o2 instanceof Map s)) return;

                        Settings groupSettings = new Settings();
                        blacklists.forEach(blacklist -> {
                            if (s.get(blacklist) instanceof Boolean enabled && !enabled) groupSettings.nbtBlacklists.add(blacklist);
                        });

                        if (s.get("singularity.player") instanceof Boolean enabled) groupSettings.syncPlayerData = enabled;
                        if (s.get("singularity.stats") instanceof Boolean enabled) groupSettings.syncPlayerStats = enabled;
                        if (s.get("singularity.advancements") instanceof Boolean enabled) groupSettings.syncPlayerAdvancements = enabled;

                        proxy.getAllServers().stream()
                                .filter(server -> List.of(servers.split("\n")).contains(server.getServerInfo().getName()))
                                .map(server -> server.getServerInfo().getAddress()).toList()
                                .forEach(a -> {
                                    settings.put(a, groupSettings);
                                    groups.put(a, groupName);
                                });

                    });
                });
            });
        } catch (FileNotFoundException ignore) {
            logger.info("No config found, loading defaults");
            new File("plugins/singularity");

            proxy.getAllServers().forEach(s -> settings.put(s.getServerInfo().getAddress(), new Settings().setDefault()));
        }
    }
}
