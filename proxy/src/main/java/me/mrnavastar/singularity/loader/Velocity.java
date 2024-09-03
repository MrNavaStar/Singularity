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
    private static final SQLibType<DataBundle> PLAYER_DATA = new SQLibType<>(SQLPrimitive.STRING, v -> GSON.toJsonTree(v).toString(), v -> GSON.fromJson(v, DataBundle.class));
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
            if (event.getPreviousServer() == null) SingularityConfig.getServerStore(server)
                    .flatMap(store -> store.getContainer("id", player)
                    .flatMap(c -> c.get(PLAYER_DATA, "data")))
                    .ifPresent(data -> server.getConnection().send(data));
            else playerLocations.put(player, server);
        });
    }

    /*@Subscribe
    public void onCommand(CommandExecuteEvent event) {
        event.getCommandSource()

        if (event.getCommand().contains("whitelist")) event.setResult(CommandExecuteEvent.CommandResult.denied());
    }*/

    @Override
    public void onReady(ProtoConnection connection) {
        ProtoProxy.getConnectedServer(WORMHOLE, connection.getRemoteAddress()).ifPresent(s -> {
            server = s;
            SingularityConfig.getServerSettings(server).ifPresent(connection::send);
        });
    }

    private void sendProperty(ProtoConnection connection, Profile profile, String property) {
        SingularityConfig.getServerStore(server)
                .flatMap(store -> store.getContainer("player", profile.getUuid()))
                .flatMap(data -> data.get(JavaTypes.BOOL, property))
                .ifPresentOrElse(prop -> {
                    connection.send(profile.setPropertyValue(prop));
                }, () -> connection.send(profile.setPropertyValue(false)));
    }

    @Override
    public void handlePacket(ProtoConnection connection, Object packet) {
        switch (packet) {
            case DataBundle data -> {
                Optional.ofNullable(playerLocations.get(data.getId())).ifPresent(nextServer -> {
                    if (SingularityConfig.inSameGroup(server, nextServer)) nextServer.getConnection().send(data);
                });

                if (data.getId().equals(Constants.STATIC_DATA)) {
                    System.out.println(data.get(Constants.OPERATORS, Profile.class));
                    return;
                }

                SingularityConfig.getServerStore(server).ifPresent(store -> store.getOrCreateContainer("id", data.getId(), container -> container.put(JavaTypes.UUID, "id", data.getId()))
                        .put(PLAYER_DATA, "data", data));
            }

            case Profile profile -> {
                switch (profile.getProperty()) {
                    case UUID_LOOKUP -> connection.send(UserCache.getUserByUUID(profile));
                    case NAME_LOOKUP -> connection.send(UserCache.getUserByName(profile));

                    case OP -> sendProperty(connection, profile.setProperty(Profile.Property.OP), "op");
                    case WHITELISTED -> sendProperty(connection, profile.setProperty(Profile.Property.WHITELISTED), "whitelist");
                    case BANNED -> sendProperty(connection, profile.setProperty(Profile.Property.BANNED), "banned");
                }
            }

            default -> throw new IllegalStateException("Unexpected value: " + packet);
        }
    }
}