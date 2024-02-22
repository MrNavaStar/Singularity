package me.mrnavastar.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import me.mrnavastar.protoweaver.api.ProtoConnectionHandler;
import me.mrnavastar.protoweaver.api.ProtoWeaver;
import me.mrnavastar.protoweaver.api.netty.ProtoConnection;
import me.mrnavastar.protoweaver.api.protocol.Protocol;
import me.mrnavastar.protoweaver.proxy.api.ProtoProxy;
import me.mrnavastar.singularity.common.Constants;
import me.mrnavastar.singularity.common.networking.Networking;
import me.mrnavastar.singularity.common.networking.Settings;
import me.mrnavastar.singularity.common.networking.SyncData;
import org.slf4j.Logger;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Plugin(
        id = Constants.MOD_ID,
        name = Constants.MOD_NAME,
        version = Constants.VERSION
)
public class Velocity implements ProtoConnectionHandler {

    private static final ConcurrentHashMap<UUID, SyncData> syncData = new ConcurrentHashMap<>();

    private Settings settings = new Settings(true);
    private final Protocol protocol = Networking.getSyncProtocol().modify()
            .setClientHandler(Velocity.class)
            .build();

    @Inject
    private Logger logger;

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        ProtoWeaver.load(protocol);
    }

    @Subscribe
    public void onPlayerConnect(ServerPostConnectEvent event) {
        Player player = event.getPlayer();
        player.getCurrentServer().ifPresent(server -> {
            SyncData data = syncData.get(player.getUniqueId());
            ProtoProxy.send(server.getServerInfo().getAddress(), data);
        });
    }

    @Override
    public void onReady(ProtoConnection connection) {
        connection.send(settings);
    }

    @Override
    public void handlePacket(ProtoConnection connection, Object packet) {
        if (packet instanceof SyncData data) syncData.put(data.getId(), data);
    }
}