package me.mrnavastar.singularity.loader;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import me.mrnavastar.protoweaver.api.ProtoConnectionHandler;
import me.mrnavastar.protoweaver.api.netty.ProtoConnection;
import me.mrnavastar.protoweaver.proxy.api.ProtoProxy;
import me.mrnavastar.singularity.common.Constants;
import me.mrnavastar.singularity.common.networking.Settings;
import me.mrnavastar.singularity.common.networking.SyncData;
import org.slf4j.Logger;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Plugin(
        id = Constants.SINGULARITY_ID,
        name = Constants.SINGULARITY_NAME,
        version = Constants.SINGULARITY_VERSION,
        authors = Constants.SINGULARITY_AUTHOR
)
public class Velocity implements ProtoConnectionHandler {

    private static final ConcurrentHashMap<UUID, SyncData> syncData = new ConcurrentHashMap<>();
    private Settings settings = new Settings();
    @Inject
    private Logger logger;

    static {
        Constants.PROTOCOL.setClientHandler(Velocity.class).load();
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.info(Constants.SINGULARITY_BOOT_MESSAGE);
    }

    @Subscribe
    public void onPlayerConnect(ServerPostConnectEvent event) {
        Player player = event.getPlayer();
        player.getCurrentServer().ifPresent(server -> {
            SyncData data = syncData.get(player.getUniqueId());
            if (data != null) ProtoProxy.send(server.getServerInfo().getAddress(), data);
        });
    }

    @Override
    public void onReady(ProtoConnection connection) {
        connection.send(settings);
    }

    @Override
    public void handlePacket(ProtoConnection connection, Object packet) {
        switch (packet) {
            case Settings s -> settings = s;
            case SyncData data -> syncData.put(data.getId(), data);
            default -> logger.warn("Ignoring unknown packet: {}", packet);
        }
    }
}