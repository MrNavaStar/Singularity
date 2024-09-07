package me.mrnavastar.singularity.loader.impl;

import com.mojang.authlib.GameProfile;
import me.mrnavastar.protoweaver.api.ProtoConnectionHandler;
import me.mrnavastar.protoweaver.api.netty.ProtoConnection;
import me.mrnavastar.protoweaver.api.protocol.Protocol;
import me.mrnavastar.singularity.common.Constants;
import me.mrnavastar.singularity.common.networking.DataBundle;
import me.mrnavastar.singularity.common.networking.Profile;
import me.mrnavastar.singularity.common.networking.Subscription;
import me.mrnavastar.singularity.loader.impl.sync.SynchronizedUserCache;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class Broker implements ProtoConnectionHandler {

    public static final Protocol PROTOCOL = Constants.WORMHOLE.setServerHandler(Broker.class).build();
    private static final ConcurrentHashMap<DataBundle.Meta, CompletableFuture<Optional<DataBundle>>> requests = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, ArrayList<Consumer<DataBundle>>> callbacks = new ConcurrentHashMap<>();
    private static ProtoConnection proxy;

    public static void putPlayerTopic(UUID player, String topic, DataBundle bundle) {
        if (proxy == null || !proxy.isOpen()) return;
        bundle.meta().id(player).topic(topic).action(DataBundle.Action.PUT);
        proxy.send(bundle);
    }

    public static void putStaticTopic(String topic, DataBundle bundle) {
        putPlayerTopic(Constants.STATIC_DATA, topic, bundle);
    }

    public static void putGlobalTopic(String topic, DataBundle bundle) {
        putPlayerTopic(Constants.GLOBAL_DATA, topic, bundle);
    }

    public static void removePlayerTopic(UUID player, String topic) {
        if (proxy == null || !proxy.isOpen()) return;
        proxy.send(new DataBundle().meta().id(player).topic(topic).action(DataBundle.Action.REMOVE));
    }

    public static void removeStaticTopic(String topic) {
        removePlayerTopic(Constants.STATIC_DATA, topic);
    }

    public static void removeGlobalTopic(String topic) {
        removePlayerTopic(Constants.GLOBAL_DATA, topic);
    }

    public static CompletableFuture<Optional<DataBundle>> getPlayerTopic(UUID player, String topic) {
        DataBundle.Meta meta = new DataBundle.Meta().id(player).topic(topic).action(DataBundle.Action.GET);

        return Optional.ofNullable(requests.get(meta)).orElseGet(() -> {
            CompletableFuture<Optional<DataBundle>> future = new CompletableFuture<>();
            requests.put(meta, future);

            if (proxy == null || !proxy.isOpen()) future.complete(Optional.empty());
            else proxy.send(meta);
            return future;
        });
    }

    public static CompletableFuture<Optional<DataBundle>> getStaticTopic(String topic) {
        return getPlayerTopic(Constants.STATIC_DATA, topic);
    }

    public static CompletableFuture<Optional<DataBundle>> getGlobalTopic(String topic) {
        return getPlayerTopic(Constants.GLOBAL_DATA, topic);
    }

    public static void subPlayerTopic(String topic, BiConsumer<UUID, DataBundle> handler) {
        if (proxy == null || !proxy.isOpen()) return;
        Subscription subscription = new Subscription(player, topic);
        ArrayList<Consumer<DataBundle>> handlers = callbacks.getOrDefault(subscription.toString(), new ArrayList<>());
        handlers.add(handler);
        callbacks.put(subscription.toString(), handlers);
        proxy.send(subscription);
    }

    public static void subStaticTopic(String topic, Consumer<DataBundle> handler) {
        subPlayerTopic(Constants.STATIC_DATA, topic, handler);
    }

    public static void subGlobalTopic(String topic, Consumer<DataBundle> handler) {
        subPlayerTopic(Constants.GLOBAL_DATA, topic, handler);
    }

    public void onReady(ProtoConnection protoConnection) {
        proxy = protoConnection;
    }

    @Override
    public void handlePacket(ProtoConnection protoConnection, Object packet) {
        switch (packet) {
            case DataBundle data -> callbacks.getOrDefault(data.meta().toString(), new ArrayList<>()).forEach(consumer -> consumer.accept(data));

            case Profile profile -> {
                switch (profile.getProperty()) {
                    case NAME_LOOKUP, UUID_LOOKUP -> SynchronizedUserCache.update(profile, new GameProfile(profile.getUuid(), profile.getName()));
                    case BAD_LOOKUP -> SynchronizedUserCache.update(profile, null);
                }
            }

            default -> log(Level.WARN, "Ignoring unknown packet: " + packet);
        };
    }

    protected static void log(Level level, String message) {
        LogManager.getLogger().log(level, "[" + Constants.SINGULARITY_NAME + "]: " + message);
    }
}