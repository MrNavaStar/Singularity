package me.mrnavastar.singularity.loader;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.server.ServerRegisteredEvent;
import com.velocitypowered.api.event.proxy.server.ServerUnregisteredEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import me.mrnavastar.protoweaver.api.ProtoConnectionHandler;
import me.mrnavastar.protoweaver.api.ProtoWeaver;
import me.mrnavastar.protoweaver.proxy.api.ProtoProxy;
import me.mrnavastar.protoweaver.proxy.api.ProtoServer;
import me.mrnavastar.singularity.common.Constants;
import me.mrnavastar.singularity.common.networking.DataBundle;
import me.mrnavastar.singularity.common.networking.Topic;
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

    private static final ConcurrentHashMap<UUID, ProtoServer> playerLocations = new ConcurrentHashMap<>();

    private Logger logger;
    private final ProxyServer proxy;

    @Inject
    public Velocity(ProxyServer proxy, Logger logger) {
        this.proxy = proxy;
        this.logger = logger;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.info(Constants.SINGULARITY_BOOT_MESSAGE);
        SingularityConfig.load(logger, true);
        ProtoWeaver.load(Broker.PROTOCOL);
        Commands.register(proxy.getCommandManager(), this);
    }

    @Subscribe
    public void onServerRegistered(ServerRegisteredEvent event) {
        ServerInfo server = event.registeredServer().getServerInfo();
        ProtoProxy.getRegisteredServer(server.getName()).ifPresent(SingularityConfig::addToSyncGroup);
    }

    @Subscribe
    public void onServerUnregistered(ServerUnregisteredEvent event) {
        ServerInfo server = event.unregisteredServer().getServerInfo();
        ProtoProxy.getRegisteredServer(server.getName()).ifPresent(SingularityConfig::removeFromSyncGroup);
    }

    @Subscribe
    public void onServerPreConnect(ServerPreConnectEvent event) {
        Broker.getSubscriptions().forEach((topic, servers) -> {
            if (!topic.behaviour().equals(Topic.Behaviour.PLAYER)) return;

            UUID player = event.getPlayer().getUniqueId();
            event.getResult().getServer().flatMap(current -> ProtoProxy.getConnectedServer(Broker.PROTOCOL, current.getServerInfo().getAddress())).ifPresent(server -> {
                if (event.getPreviousServer() == null) Broker.doPlayerTopicBehaviour(topic, server, player.toString());
                else playerLocations.put(player, server);
            });
        });
    }

    public static Optional<ProtoServer> getPlayerLocation(DataBundle bundle) {
        try {
            UUID player = UUID.fromString(bundle.meta().id());
            return Optional.ofNullable(playerLocations.get(player));
        } catch (IllegalArgumentException ignore) {
            return Optional.empty();
        }
    }
}