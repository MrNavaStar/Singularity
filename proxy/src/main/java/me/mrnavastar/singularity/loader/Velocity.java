package me.mrnavastar.singularity.loader;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import me.mrnavastar.protoweaver.api.ProtoConnectionHandler;
import me.mrnavastar.protoweaver.api.netty.ProtoConnection;
import me.mrnavastar.protoweaver.api.protocol.Protocol;
import me.mrnavastar.protoweaver.proxy.api.ProtoProxy;
import me.mrnavastar.protoweaver.proxy.api.ProtoServer;
import me.mrnavastar.singularity.common.Constants;
import me.mrnavastar.singularity.common.networking.DataBundle;
import me.mrnavastar.singularity.common.networking.Profile;
import me.mrnavastar.singularity.common.networking.Topic;
import me.mrnavastar.sqlib.api.types.JavaTypes;
import me.mrnavastar.sqlib.api.types.SQLibType;
import me.mrnavastar.sqlib.impl.SQLPrimitive;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
    private static final SQLibType<DataBundle> DATA_BUNDLE = new SQLibType<>(SQLPrimitive.STRING, v -> GSON.toJsonTree(v).toString(), v -> GSON.fromJson(v, DataBundle.class));
    private static final HashMap<ProtoServer, HashSet<Topic>> subs = new HashMap<>();
    private static final ConcurrentHashMap<UUID, ProtoServer> playerLocations = new ConcurrentHashMap<>();
    private static final Protocol WORMHOLE = Constants.WORMHOLE.setClientHandler(Velocity.class).load();

    @Inject
    private Logger logger;
    private ProtoServer server;

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.info(Constants.SINGULARITY_BOOT_MESSAGE);
        SingularityConfig.load(logger);
    }

    @Subscribe
    public void onLogin(PostLoginEvent event) {
        UserCache.addUser(event.getPlayer());
    }

    @Subscribe
    public void onServerPreConnect(ServerPreConnectEvent event) {
        UUID player = event.getPlayer().getUniqueId();
        event.getResult().getServer().flatMap(current -> ProtoProxy.getConnectedServer(WORMHOLE, current.getServerInfo().getName())).ifPresent(server -> {
            if (event.getPreviousServer() == null) {
                Topic topic = new Topic(Topic.TopicType.PLAYER, Constants.PLAYER_TOPIC);
                SingularityConfig.getServerStore(server)
                        .flatMap(store -> store.getContainer("uuid", player))
                        .flatMap(c -> c.get(DATA_BUNDLE, topic.databaseKey()))
                        .map(data -> data.meta(new DataBundle.Meta().id(player).topic(topic).action(DataBundle.Action.PUT)))
                        .ifPresent(data -> server.getConnection().send(data));
            }
            else playerLocations.put(player, server);
        });
    }

    @Override
    public void onReady(ProtoConnection connection) {
        ProtoProxy.getConnectedServer(WORMHOLE, connection.getRemoteAddress()).ifPresent(s -> {
            server = s;
            SingularityConfig.getServerSettings(server).ifPresent(connection::send);
        });
    }

    @Override
    public void handlePacket(ProtoConnection connection, Object packet) {
        switch (packet) {
            case Topic sub -> {
                HashSet<Topic> topics = subs.getOrDefault(server, new HashSet<>());
                topics.add(sub);
                subs.put(server, topics);
            }

            case DataBundle.Meta meta -> {
                switch (meta.action()) {
                    case GET -> SingularityConfig.getServerStore(server)
                            .flatMap(store -> store.getContainer("uuid", meta.id()))
                            .flatMap(c -> c.get(DATA_BUNDLE, meta.topic().topic().replace(":", "_")))
                            .ifPresentOrElse(data -> connection.send(data.meta(meta)), () -> connection.send(meta));

                    case REMOVE -> SingularityConfig.getServerStore(server)
                            .flatMap(store -> store.getContainer("uuid", meta.id()))
                            .ifPresent(c -> c.clear(meta.topic().databaseKey()));
                }
            }

            case DataBundle data -> {
                if (!DataBundle.Action.PUT.equals(data.meta().action())) return;

                // TODO: Perhaps change how data is propagated - currently player data will only send to the server the
                //  player is going to be on. Should this be different?
                // Forward data to subscribed servers
                SingularityConfig.getSameGroup(server).stream()
                        .filter(s ->  {
                            if (data.meta().propagate() || !data.meta().topic().type().equals(Topic.TopicType.PLAYER)) return true;
                            Optional<ProtoServer> location = Optional.ofNullable(playerLocations.get(data.meta().id()));
                            return location.isPresent() && location.get().equals(s);
                        })
                        .filter(s -> subs.getOrDefault(s, new HashSet<>()).contains(data.meta().topic()))
                        .forEach(s -> s.getConnection().send(data));

                // Store data in database
                SingularityConfig.getServerStore(server)
                        .ifPresent(store -> store.getOrCreateContainer("uuid", data.meta().id(), c -> c.put(JavaTypes.UUID, "uuid", data.meta().id()))
                                .put(DATA_BUNDLE, data.meta().topic().databaseKey(), data));
            }

            case Profile profile -> {
                switch (profile.getProperty()) {
                    case UUID_LOOKUP -> connection.send(UserCache.getUserByUUID(profile));
                    case NAME_LOOKUP -> connection.send(UserCache.getUserByName(profile));
                }
            }

            default -> WORMHOLE.logWarn("Ignoring unknown packet: " + packet);
        }
    }
}