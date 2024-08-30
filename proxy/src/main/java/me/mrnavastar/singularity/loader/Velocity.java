package me.mrnavastar.singularity.loader;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import me.mrnavastar.protoweaver.api.ProtoConnectionHandler;
import me.mrnavastar.protoweaver.api.netty.ProtoConnection;
import me.mrnavastar.protoweaver.api.protocol.Protocol;
import me.mrnavastar.protoweaver.proxy.api.ProtoProxy;
import me.mrnavastar.protoweaver.proxy.api.ProtoServer;
import me.mrnavastar.singularity.common.Constants;
import me.mrnavastar.singularity.common.networking.PlayerData;
import me.mrnavastar.singularity.common.networking.ServerData;
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
    private static final SQLibType<PlayerData> PLAYER_DATA = new SQLibType<>(SQLPrimitive.STRING, v -> GSON.toJsonTree(v).toString(), v -> GSON.fromJson(v, PlayerData.class));
    private static final SQLibType<ServerData> SERVER_DATA = new SQLibType<>(SQLPrimitive.STRING, v -> GSON.toJsonTree(v).toString(), v -> GSON.fromJson(v, ServerData.class));
    private static final ConcurrentHashMap<UUID, ProtoServer> playerLocations = new ConcurrentHashMap<>();
    private static final Protocol PROTOCOL = Constants.PROTOCOL.setClientHandler(Velocity.class).load();

    @Inject
    private ProxyServer proxy;
    @Inject
    private Logger logger;
    private ProtoServer server;

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.info(Constants.SINGULARITY_BOOT_MESSAGE);
        SingularityConfig.load(proxy, logger);
    }

    @Subscribe
    public void onLogin(PostLoginEvent event) {
        UserCache.addUser(event.getPlayer());
    }

    @Subscribe
    public void onServerPreConnect(ServerPreConnectEvent event) {
        UUID player = event.getPlayer().getUniqueId();
        event.getResult().getServer().flatMap(current -> ProtoProxy.getConnectedServer(PROTOCOL, current.getServerInfo().getName())).ifPresent(server -> {
            if (event.getPreviousServer() == null) SingularityConfig.getServerStore(server)
                    .flatMap(store -> store.getContainer("player", player)
                    .flatMap(c -> c.get(PLAYER_DATA, "data")))
                    .ifPresent(data -> server.getConnection().send(data));
            else playerLocations.put(player, server);
        });
    }

    @Override
    public void onReady(ProtoConnection connection) {
        ProtoProxy.getConnectedServer(PROTOCOL, connection.getRemoteAddress()).ifPresent(s -> {
            server = s;
            SingularityConfig.getServerSettings(server).ifPresent(connection::send);
        });
    }

    @Override
    public void handlePacket(ProtoConnection connection, Object packet) {
        switch (packet) {
            case PlayerData data -> {
                Optional.ofNullable(playerLocations.get(data.getPlayer())).ifPresent(oldServer -> {
                    if (SingularityConfig.inSameGroup(server, oldServer)) oldServer.getConnection().send(data);
                });

                SingularityConfig.getServerStore(server).ifPresent(store -> store.getOrCreateContainer("player", data.getPlayer(), container -> container.put(JavaTypes.UUID, "player", data.getPlayer()))
                        .put(PLAYER_DATA, "data", data));
            }

            case ServerData data -> SingularityConfig.getServerStore(server)
                    .ifPresent(store -> store.getOrCreateContainer("player", Constants.SERVER_DATA, container -> container.put(JavaTypes.UUID, "player", Constants.SERVER_DATA))
                    .put(SERVER_DATA, "data", data));

            case UUID request -> UserCache.getUser(request).ifPresentOrElse(connection::send, () -> connection.send(request));
            case String request -> UserCache.getUser(request).ifPresentOrElse(connection::send, () -> connection.send(request));

            default -> throw new IllegalStateException("Unexpected value: " + packet);
        }
    }
}