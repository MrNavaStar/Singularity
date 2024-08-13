package me.mrnavastar.singularity.loader;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import me.mrnavastar.protoweaver.api.ProtoConnectionHandler;
import me.mrnavastar.protoweaver.api.netty.ProtoConnection;
import me.mrnavastar.singularity.common.Constants;
import me.mrnavastar.singularity.common.networking.PlayerData;
import me.mrnavastar.singularity.common.networking.ServerData;
import me.mrnavastar.sqlib.SQLib;
import me.mrnavastar.sqlib.api.DataStore;
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

    private static Set<Velocity> instances;
    private static final Gson GSON = new Gson();
    private static final SQLibType<PlayerData> PLAYER_DATA = new SQLibType<>(SQLPrimitive.STRING, v -> GSON.toJsonTree(v).toString(), v -> GSON.fromJson(v, PlayerData.class));
    private static final DataStore dataStore = SQLib.getDatabase().dataStore(Constants.SINGULARITY_ID, "data");
    private static final ConcurrentHashMap<UUID, ProtoConnection> servers = new ConcurrentHashMap<>();
    private static Logger logger;

    @Inject
    private ProxyServer proxy;
    @Inject
    private Logger l;
    private ProtoConnection server;

    static {
        Constants.PROTOCOL.setClientHandler(Velocity.class).load();
    }

    public Velocity() {
        // Skip adding the first instance of this class to the set
        if (instances == null) instances = Collections.newSetFromMap(new ConcurrentHashMap<>());
        else instances.add(this);
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger = l;
        logger.info(Constants.SINGULARITY_BOOT_MESSAGE);
        SingularityConfig.load(proxy, logger);
    }

    @Subscribe
    public void onServerPreConnect(ServerPreConnectEvent event) {
        if (server == null) {
            instances.forEach(i -> i.onServerPreConnect(event));
            return;
        }

        UUID player = event.getPlayer().getUniqueId();
        // First server connected to
        if (event.getPreviousServer() == null) {
            event.getResult().getServer().ifPresent(s -> {
                if (!s.getServerInfo().getAddress().equals(server.getRemoteAddress())) return;
                dataStore.getContainer("player", player).flatMap(c -> c.get(PLAYER_DATA, "data")).ifPresent(server::send);
            });
            return;
        }

        // Grab reference to server the player will be on
        event.getResult().getServer().ifPresent(s -> {
            if (!s.getServerInfo().getAddress().equals(server.getRemoteAddress())) return;
            servers.put(player, server);
            System.out.println("Got ref to: " + servers.get(player).getRemoteAddress());
        });
    }

    @Override
    public void onReady(ProtoConnection server) {
        this.server = server;
        SingularityConfig.getServerSettings(server).ifPresent(server::send);
    }

    @Override
    public void onDisconnect(ProtoConnection connection) {
        instances.remove(this);
    }

    @Override
    public void handlePacket(ProtoConnection server, Object packet) {
        switch (packet) {
            case PlayerData data -> {
                Optional.ofNullable(servers.get(data.getPlayer())).ifPresent(c -> {
                    if (SingularityConfig.inSameGroup(server, c)) c.send(data);
                });
                dataStore.getOrCreateContainer("player", data.getPlayer(), container -> container.put(JavaTypes.UUID, "player", data.getPlayer()))
                        .put(PLAYER_DATA, "data", data);
            }
            case ServerData data -> {
                logger.info("got server data");
            }
            default -> logger.warn("Ignoring unknown packet: {}", packet);
        }
    }
}