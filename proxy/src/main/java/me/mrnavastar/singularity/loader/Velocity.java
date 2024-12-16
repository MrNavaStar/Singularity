package me.mrnavastar.singularity.loader;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import me.mrnavastar.protoweaver.api.ProtoConnectionHandler;
import me.mrnavastar.protoweaver.api.ProtoWeaver;
import me.mrnavastar.protoweaver.api.netty.ProtoConnection;
import me.mrnavastar.protoweaver.api.protocol.Protocol;
import me.mrnavastar.protoweaver.proxy.api.ProtoProxy;
import me.mrnavastar.protoweaver.proxy.api.ProtoServer;
import me.mrnavastar.singularity.common.Constants;
import me.mrnavastar.singularity.common.networking.DataBundle;
import me.mrnavastar.singularity.common.networking.Topic;
import me.mrnavastar.sqlib.api.DataContainer;
import me.mrnavastar.sqlib.api.types.GsonTypes;
import me.mrnavastar.sqlib.api.types.JavaTypes;
import me.mrnavastar.sqlib.api.types.SQLibType;
import org.slf4j.Logger;

import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@Plugin(
        id = Constants.SINGULARITY_ID,
        name = Constants.SINGULARITY_NAME,
        version = Constants.SINGULARITY_VERSION,
        authors = Constants.SINGULARITY_AUTHOR,
        dependencies = {
            @Dependency(id = "protoweaver"),
            @Dependency(id = "sqlib")
        }
)
public class Velocity implements ProtoConnectionHandler {

    private static final Gson GSON = new Gson();
    private static final Protocol WORMHOLE = Constants.WORMHOLE.setClientHandler(Velocity.class).build();
    private static final Topic PLAYER_TOPIC = new Topic(Constants.PLAYER_TOPIC, false);
    private static final Type DATA_BUNDLE_TYPE = new TypeToken<HashMap<String, byte[]>>(){}.getType();
    private static final SQLibType<DataBundle> DATA_BUNDLE = new SQLibType<>(GsonTypes.ELEMENT, v -> GSON.toJsonTree(v.data()), v -> new DataBundle().data(GSON.fromJson(v, DATA_BUNDLE_TYPE)));

    private static final HashMap<ProtoServer, HashSet<Topic>> subs = new HashMap<>();
    private static final ConcurrentHashMap<UUID, ProtoServer> playerLocations = new ConcurrentHashMap<>();

    @Inject
    private Logger logger;

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.info(Constants.SINGULARITY_BOOT_MESSAGE);
        SingularityConfig.load(logger);
        ProtoWeaver.load(WORMHOLE);
    }

    @Subscribe
    public void onServerPreConnect(ServerPreConnectEvent event) {
        UUID player = event.getPlayer().getUniqueId();
        Optional<RegisteredServer> s = event.getResult().getServer();

        // Send player data to subscribed servers and store player network location
        s.flatMap(current -> ProtoProxy.getConnectedServer(WORMHOLE, current.getServerInfo().getAddress())).ifPresent(server -> {
            System.out.println("Player joining");

            if (event.getPreviousServer() == null) {
                System.out.println("lets send their data to " + server);

                SingularityConfig.getSyncGroup(server)
                        .flatMap(store -> store.getTopicStore(PLAYER_TOPIC).getContainer("id", player.toString()))
                        .flatMap(c -> c.get(DATA_BUNDLE, "data"))
                        .map(data -> data.meta(new DataBundle.Meta().id(player.toString()).topic(PLAYER_TOPIC).action(DataBundle.Action.PUT)))
                        .ifPresent(data -> {
                            server.getConnection().send(data);
                            System.out.println("Data : " + data);
                        });
            }
            else playerLocations.put(player, server);
        });
    }

    @Override
    public void onReady(ProtoConnection connection) {
        ProtoProxy.getConnectedServer(WORMHOLE, connection.getRemoteAddress())
                .flatMap(SingularityConfig::getSyncGroup)
                .ifPresent(group -> {
                    connection.send(group.getSettings());
                    System.out.println("settings send to " + connection.getRemoteAddress());
                });
    }

    @Override
    public void handlePacket(ProtoConnection connection, Object packet) {
        System.out.println("Got packet from: " + connection + " of type: " + packet);

        System.out.println();
        ProtoProxy.getRegisteredServers().forEach(server -> {
            System.out.println(server);
            System.out.println(connection.getRemoteAddress());
            System.out.println(server.getAddress().equals(connection.getRemoteAddress()));
        });
        System.out.println();

        ProtoProxy.getConnectedServer(WORMHOLE, connection.getRemoteAddress()).ifPresent(server -> {
            System.out.println("Server " + server + " is connected, able to handle packet");

            switch (packet) {
                // Handle a topic subscription
                case Topic sub -> {
                    HashSet<Topic> topics = subs.getOrDefault(server, new HashSet<>());
                    topics.add(sub);
                    subs.put(server, topics);
                }

                // Process data bundle actions such as requesting and removing data
                case DataBundle.Meta meta -> {
                    switch (meta.action()) {
                        case GET -> SingularityConfig.getSyncGroup(server)
                                .flatMap(store -> store.getTopicStore(meta.topic()).getContainer("id", meta.id()))
                                .flatMap(c -> c.get(DATA_BUNDLE, "data"))
                                .ifPresentOrElse(data -> connection.send(data.meta(meta)), () -> connection.send(meta));

                        case REMOVE -> SingularityConfig.getSyncGroup(server)
                                .flatMap(store -> store.getTopicStore(meta.topic()).getContainer("id", meta.id()))
                                .ifPresent(DataContainer::delete);
                    }
                }

                case DataBundle bundle -> {
                    if (!DataBundle.Action.PUT.equals(bundle.meta().action())) return;
                    Topic topic = bundle.meta().topic();

                    Stream<ProtoServer> servers;
                    if (topic.global()) servers = ProtoProxy.getConnectedServers(WORMHOLE).stream();
                    else {
                        Optional<SingularityConfig.SyncGroup> group = SingularityConfig.getSyncGroup(server);
                        if (group.isEmpty()) return;
                        servers = group.get().getServers().stream();
                    }

                    System.out.println("got server list: " + servers);

                    // Forward data to subscribed servers
                    servers.filter(s -> !s.equals(server))
                            .filter(s ->  {
                                DataBundle.Propagation propagation = bundle.meta().propagation();
                                if (propagation.equals(DataBundle.Propagation.ALL)) return true;
                                if (propagation.equals(DataBundle.Propagation.NONE)) return false;

                                // if propagation is set to PLAYER
                                // TODO: This is maybe a race condition
                                try {
                                    UUID player = UUID.fromString(bundle.meta().id());
                                    Optional<ProtoServer> location = Optional.ofNullable(playerLocations.get(player));

                                    System.out.println("Player location found? : " + location.isPresent());
                                    if (location.isPresent()) System.out.println("Player location: " + location.get());

                                    return location.isPresent() && location.get().equals(s);
                                } catch (IllegalArgumentException ignore) {
                                    return false;
                                }
                            })
                            .filter(s -> subs.getOrDefault(s, new HashSet<>()).contains(topic))
                            .forEach(s -> {
                                System.out.println("forwarding data to:" + s);
                                s.getConnection().send(bundle);
                            });

                    if (!bundle.meta().persist()) return;

                    System.out.println("Storing packet data");

                    // Store data in database
                    if (topic.global()) {
                        SingularityConfig.getGlobalStore(topic)
                                .getOrCreateDefaultContainer(JavaTypes.STRING, "id", bundle.meta().id())
                                .put(DATA_BUNDLE, "data", bundle);
                        return;
                    }

                    SingularityConfig.getSyncGroup(server)
                            .ifPresent(store -> store.getTopicStore(topic)
                                    .getOrCreateDefaultContainer(JavaTypes.STRING, "id", bundle.meta().id())
                                    .put(DATA_BUNDLE, "data", bundle));
                }
                default -> WORMHOLE.logWarn("Ignoring unknown packet: " + packet);
            }
        });
    }
}