package me.mrnavastar.singularity.loader.impl;

import lombok.Getter;
import lombok.Setter;
import me.mrnavastar.protoweaver.api.ProtoConnectionHandler;
import me.mrnavastar.protoweaver.api.netty.ProtoConnection;
import me.mrnavastar.protoweaver.api.protocol.Protocol;
import me.mrnavastar.singularity.common.Constants;
import me.mrnavastar.singularity.common.networking.DataBundle;
import me.mrnavastar.singularity.common.networking.Settings;
import me.mrnavastar.singularity.common.networking.Topic;
import me.mrnavastar.singularity.loader.impl.sync.SynchronizedMinecraft;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public class Broker implements ProtoConnectionHandler {

    public static final Protocol PROTOCOL = Constants.WORMHOLE.setServerHandler(Broker.class).build();

    private static final ConcurrentLinkedQueue<DataBundle> queue = new ConcurrentLinkedQueue<>();
    private static final ConcurrentHashMap<DataBundle.Meta, CompletableFuture<Optional<DataBundle>>> requests = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Topic, HashSet<Consumer<DataBundle>>> subs = new ConcurrentHashMap<>();
    @Setter private static Consumer<Settings> settingsCallback;

    @Getter private static Settings settings = new Settings();
    @Getter private static ProtoConnection proxy;



    private static void syncSubs() {
        if (proxy == null || !proxy.isOpen()) return;
        subs.forEach((sub, callbacks) -> proxy.send(sub));
    }

    private static void putTopic(Topic topic, String id, DataBundle bundle) {
        topic.validate();
        bundle.meta().id(id).topic(topic).action(DataBundle.Action.PUT);

        if (proxy == null || !proxy.isOpen()) queue.add(bundle);
        else proxy.send(bundle);
    }

    public static void putTopic(String topic, String id, DataBundle bundle) {
        putTopic(new Topic(topic, false), id, bundle);
    }

    public static void putGlobalTopic(String topic, String id, DataBundle bundle) {
        putTopic(new Topic(topic, true), id, bundle);
    }

    private static void removeTopic(Topic topic, String id) {
        topic.validate();
        if (proxy == null || !proxy.isOpen()) return;

        proxy.send(new DataBundle.Meta()
                .id(id)
                .topic(topic)
                .action(DataBundle.Action.REMOVE));
    }

    public static void removeTopic(String topic, String id) {
        removeTopic(new Topic(topic, false), id);
    }

    public static void removeGlobalTopic(String topic, String id) {
        removeTopic(new Topic(topic, true), id);
    }

    private static CompletableFuture<Optional<DataBundle>> getTopic(Topic topic, String id) {
        topic.validate();
        DataBundle.Meta meta = new DataBundle.Meta()
                .id(id)
                .topic(topic)
                .action(DataBundle.Action.GET);

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

    public static CompletableFuture<Optional<DataBundle>> getTopic(String topic, String id) {
        return getTopic(new Topic(topic, false), id);
    }

    public static CompletableFuture<Optional<DataBundle>> getGlobalTopic(String topic, String id) {
        return getTopic(new Topic(topic, true), id);
    }

    private static void subTopic(Topic topic, Consumer<DataBundle> handler) {
        topic.validate();
        HashSet<Consumer<DataBundle>> handlers = subs.getOrDefault(topic, new HashSet<>());
        handlers.add(handler);
        subs.put(topic, handlers);
        syncSubs();
    }

    public static void subTopic(String topic, Consumer<DataBundle> handler) {
        subTopic(new Topic(topic, false), handler);
    }

    public static void subGlobalTopic(String topic, Consumer<DataBundle> handler) {
        subTopic(new Topic(topic, true), handler);
    }

    public void onReady(ProtoConnection protoConnection) {
        proxy = protoConnection;
        syncSubs();
        while (!queue.isEmpty()) proxy.send(queue.remove());
    }

    @Override
    public void handlePacket(ProtoConnection protoConnection, Object packet) {
        switch (packet) {
            case DataBundle.Meta meta -> Optional.ofNullable(requests.remove(meta)).ifPresent(request -> request.complete(Optional.empty()));

            case DataBundle data -> {
                if (data.meta().action().equals(DataBundle.Action.NONE))
                    Optional.ofNullable(requests.remove(data.meta())).ifPresent(request -> request.complete(Optional.of(data)));

                else subs.getOrDefault(data.meta().topic(), new HashSet<>()).forEach(consumer -> consumer.accept(data));
            }

            //TODO: This could be a static topic, but its not so bad like this either
            case Settings s -> {
                settings = s;
                SynchronizedMinecraft.reloadBlacklists();
                if (settingsCallback != null) settingsCallback.accept(s);
            }

            default -> PROTOCOL.logWarn("Ignoring unknown packet: " + packet);
        };
    }
}