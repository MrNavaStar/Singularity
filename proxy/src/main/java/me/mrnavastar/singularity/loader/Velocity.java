package me.mrnavastar.singularity.loader;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import me.mrnavastar.protoweaver.api.ProtoConnectionHandler;
import me.mrnavastar.protoweaver.api.netty.ProtoConnection;
import me.mrnavastar.singularity.common.Constants;
import me.mrnavastar.singularity.common.networking.Settings;
import me.mrnavastar.singularity.common.networking.SyncData;
import me.mrnavastar.sqlib.SQLib;
import me.mrnavastar.sqlib.api.DataStore;
import me.mrnavastar.sqlib.api.types.JavaTypes;
import me.mrnavastar.sqlib.api.types.SQLibType;
import me.mrnavastar.sqlib.impl.SQLPrimitive;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;
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

    private static HashSet<Velocity> instances;
    private static final Gson GSON = new Gson();
    private static final SQLibType<SyncData> SYNC_DATA = new SQLibType<>(SQLPrimitive.STRING, v -> GSON.toJsonTree(v).toString(), v -> GSON.fromJson(v, SyncData.class));
    private static final DataStore dataStore = SQLib.getDatabase().dataStore(Constants.SINGULARITY_ID, "data");

    @Inject
    private Logger logger;
    private ProtoConnection server;
    private Settings settings = new Settings();
    private final ConcurrentHashMap<UUID, ProtoConnection> servers = new ConcurrentHashMap<>();

    static {
        Constants.PROTOCOL.setClientHandler(Velocity.class).load();
    }

    public Velocity() {
        // Skip adding the first instance of this class to the set
        if (instances == null) instances = new HashSet<>();
        else instances.add(this);
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.info(Constants.SINGULARITY_BOOT_MESSAGE);
    }

    @Subscribe
    public void onProxyDisconnect(DisconnectEvent event) {
        if (server == null) {
            instances.forEach(i -> i.onProxyDisconnect(event));
            return;
        }
        // Ask last server for data
        event.getPlayer().getCurrentServer().ifPresent(s -> {
            if (!s.getServerInfo().getAddress().equals(server.getRemoteAddress())) return;
            server.send(event.getPlayer().getUniqueId());
        });
    }

    @Subscribe
    public void onServerPreConnect(ServerPreConnectEvent event) {
        if (server == null) {
            instances.forEach(i -> i.onServerPreConnect(event));
            return;
        }
        // TODO: Check if this is creates a concurrent modification exception
        if (!server.isOpen()) {
            instances.remove(this);
            return;
        }

        UUID player = event.getPlayer().getUniqueId();
        RegisteredServer prev = event.getPreviousServer();
        // First server connected to
        if (prev == null) {
            event.getResult().getServer().ifPresent(s -> {
                if (!s.getServerInfo().getAddress().equals(server.getRemoteAddress())) return;
                dataStore.getContainer("player", player).flatMap(c -> c.get(SYNC_DATA, "data")).ifPresent(server::send);
            });
            return;
        }
        // Ask previous server for data
        if (prev.getServerInfo().getAddress().equals(server.getRemoteAddress())) {
            server.send(player);
            return;
        }
        // Grab reference to server the player will be on
        event.getResult().getServer().ifPresent(s -> {
            if (!s.getServerInfo().getAddress().equals(server.getRemoteAddress())) return;
            servers.put(player, server);
        });
    }

    @Override
    public void onReady(ProtoConnection server) {
        this.server = server;
        server.send(settings);
    }

    @Override
    public void handlePacket(ProtoConnection server, Object packet) {
        switch (packet) {
            case Settings s -> settings = s;
            case SyncData data -> {
                Optional.ofNullable(servers.get(data.getPlayer())).ifPresent(c -> c.send(data));
                dataStore.getOrCreateContainer("player", data.getPlayer(), container -> container.put(JavaTypes.UUID, "player", data.getPlayer())).put(SYNC_DATA, "data", data);
            }
            default -> logger.warn("Ignoring unknown packet: {}", packet);
        }
    }
}