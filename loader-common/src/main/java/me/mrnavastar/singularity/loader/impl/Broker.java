package me.mrnavastar.singularity.loader.impl;

import com.mojang.authlib.GameProfile;
import lombok.Getter;
import lombok.Setter;
import me.mrnavastar.protoweaver.api.ProtoConnectionHandler;
import me.mrnavastar.protoweaver.api.netty.ProtoConnection;
import me.mrnavastar.protoweaver.api.protocol.Protocol;
import me.mrnavastar.singularity.common.Constants;
import me.mrnavastar.singularity.common.networking.DataBundle;
import me.mrnavastar.singularity.common.networking.Profile;
import me.mrnavastar.singularity.common.networking.Settings;
import me.mrnavastar.singularity.common.networking.Subscription;
import me.mrnavastar.singularity.loader.impl.sync.SynchronizedMinecraft;
import me.mrnavastar.singularity.loader.impl.sync.SynchronizedUserCache;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class Broker implements ProtoConnectionHandler {

    public static final Protocol PROTOCOL = Constants.WORMHOLE.setServerHandler(Broker.class).build();
    private static final ConcurrentHashMap<DataBundle.Meta, CompletableFuture<Optional<DataBundle>>> requests = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, ArrayList<BiConsumer<UUID, DataBundle>>> callbacks = new ConcurrentHashMap<>();
    @Setter private static Consumer<Settings> settingsCallback;
    @Getter private static Settings settings = new Settings();
    @Getter private static ProtoConnection proxy;

    private static void validateNamespace(String topic) {
        String[] names = topic.split(":");
        if (names.length < 2) throw new IllegalArgumentException("Invalid Topic! Topic names should be namespaced: 'namespace:field`");
    }

    public static void putPlayerTopic(UUID player, String topic, DataBundle bundle) {
        validateNamespace(topic);

        if (proxy == null || !proxy.isOpen()) return;
        bundle.meta()
                .id(player)
                .topic(topic)
                .action(DataBundle.Action.PUT)
                .topicType(player.equals(Constants.STATIC_DATA) ? Subscription.TopicType.STATIC : Subscription.TopicType.PLAYER);
        proxy.send(bundle);
    }

    public static void putStaticTopic(String topic, DataBundle bundle) {
        putPlayerTopic(Constants.STATIC_DATA, topic, bundle);
    }

    public static void removePlayerTopic(UUID player, String topic) {
        if (proxy == null || !proxy.isOpen()) return;
        proxy.send(new DataBundle.Meta()
                .id(player)
                .topic(topic)
                .action(DataBundle.Action.REMOVE)
                .topicType(player.equals(Constants.STATIC_DATA) ? Subscription.TopicType.STATIC : Subscription.TopicType.PLAYER));
    }

    public static void removeStaticTopic(String topic) {
        removePlayerTopic(Constants.STATIC_DATA, topic);
    }

    public static CompletableFuture<Optional<DataBundle>> getPlayerTopic(UUID player, String topic) {
        DataBundle.Meta meta = new DataBundle.Meta()
                .id(player)
                .topic(topic)
                .action(DataBundle.Action.GET)
                .topicType(player.equals(Constants.STATIC_DATA) ? Subscription.TopicType.STATIC : Subscription.TopicType.PLAYER);

        return Optional.ofNullable(requests.get(meta)).orElseGet(() -> {
            CompletableFuture<Optional<DataBundle>> future = new CompletableFuture<>();

            if (proxy == null || !proxy.isOpen()) future.complete(Optional.empty());
            else {
                requests.put(meta, future);
                proxy.send(meta);
            }
            return future;
        });
    }

    public static CompletableFuture<Optional<DataBundle>> getStaticTopic(String topic) {
        return getPlayerTopic(Constants.STATIC_DATA, topic);
    }

    private static void subTopic(Subscription.TopicType type, String topic, BiConsumer<UUID, DataBundle> handler) {
        validateNamespace(topic);

        if (proxy == null || !proxy.isOpen()) return;
        Subscription subscription = new Subscription(type, topic);
        ArrayList<BiConsumer<UUID, DataBundle>> handlers = callbacks.getOrDefault(subscription.toString(), new ArrayList<>());
        handlers.add(handler);
        callbacks.put(subscription.toString(), handlers);
        proxy.send(subscription);
    }

    public static void subPlayerTopic(String topic, BiConsumer<UUID, DataBundle> handler) {
        subTopic(Subscription.TopicType.PLAYER, topic, handler);
    }

    public static void subStaticTopic(String topic, Consumer<DataBundle> handler) {
        subTopic(Subscription.TopicType.STATIC, topic, (uuid, bundle) -> handler.accept(bundle));
    }

    public void onReady(ProtoConnection protoConnection) {
        proxy = protoConnection;
    }

    @Override
    public void handlePacket(ProtoConnection protoConnection, Object packet) {
        switch (packet) {
            case DataBundle.Meta meta -> Optional.ofNullable(requests.remove(meta)).ifPresent(request -> request.complete(Optional.empty()));

            case DataBundle data -> {
                if (data.meta().action().equals(DataBundle.Action.NONE))
                    Optional.ofNullable(requests.remove(data.meta())).ifPresent(request -> request.complete(Optional.of(data)));

                else callbacks.getOrDefault(data.meta().toString(), new ArrayList<>()).forEach(consumer -> consumer.accept(data.meta().id(), data));
            }

            //TODO: Would be nice to move this into a global topic, but we would need to implement global topics in
            // a not terrible way
            case Profile profile -> {
                switch (profile.getProperty()) {
                    case NAME_LOOKUP, UUID_LOOKUP -> SynchronizedUserCache.update(profile, new GameProfile(profile.getUuid(), profile.getName()));
                    case BAD_LOOKUP -> SynchronizedUserCache.update(profile, null);
                }
            }

            case Settings s -> {
                settings = s;
                SynchronizedMinecraft.reloadBlacklists();
                if (settingsCallback != null) settingsCallback.accept(s);
            }

            default -> PROTOCOL.logWarn("Ignoring unknown packet: " + packet);
        };
    }
}