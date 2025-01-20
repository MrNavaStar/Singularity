package me.mrnavastar.singularity.loader;

import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.Getter;
import me.mrnavastar.protoweaver.api.ProtoConnectionHandler;
import me.mrnavastar.protoweaver.api.netty.ProtoConnection;
import me.mrnavastar.protoweaver.api.protocol.Protocol;
import me.mrnavastar.protoweaver.proxy.api.ProtoProxy;
import me.mrnavastar.protoweaver.proxy.api.ProtoServer;
import me.mrnavastar.singularity.common.Constants;
import me.mrnavastar.singularity.common.networking.DataBundle;
import me.mrnavastar.singularity.common.networking.Topic;
import me.mrnavastar.sqlib.api.DataContainer;
import me.mrnavastar.sqlib.api.types.GsonTypes;
import me.mrnavastar.sqlib.api.types.JavaTypes;
import me.mrnavastar.sqlib.api.types.SQLibType;

import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Broker implements ProtoConnectionHandler {

    private static final Gson GSON = new Gson();
    public static final Protocol PROTOCOL = Constants.PROTOCOL.setClientHandler(Broker.class).build();
    private static final Type DATA_BUNDLE_TYPE = new TypeToken<ConcurrentHashMap<String, byte[]>>(){}.getType();
    private static final SQLibType<DataBundle> DATA_BUNDLE = new SQLibType<>(GsonTypes.ELEMENT, v -> GSON.toJsonTree(v.data()), v -> new DataBundle().data(GSON.fromJson(v, DATA_BUNDLE_TYPE)));

    @Getter
    private static final ConcurrentHashMap<Topic, Set<ProtoServer>> subscriptions = new ConcurrentHashMap<>();

    public static void doPlayerTopicBehaviour(Topic topic, ProtoServer server, String id) {
        SingularityConfig.getSyncGroup(server)
                .flatMap(store -> store.getTopicStore(topic).getContainer("id", id))
                .flatMap(c -> c.get(DATA_BUNDLE, "data"))
                .map(data -> data.meta(new DataBundle.Meta().id(id).topic(topic).action(DataBundle.Action.PUT)))
                .ifPresent(data -> server.getConnection(PROTOCOL).ifPresent(con -> con.send(data)));
    }

    private void storeBundle(ProtoServer server, DataBundle bundle) {
        if (bundle.meta().topic().global()) {
            SingularityConfig.getGlobalStore(bundle.meta().topic())
                    .getOrCreateDefaultContainer(JavaTypes.STRING, "id", bundle.meta().id())
                    .put(DATA_BUNDLE, "data", bundle);
            return;
        }

        SingularityConfig.getSyncGroup(server)
                .ifPresent(store -> store.getTopicStore(bundle.meta().topic())
                        .getOrCreateDefaultContainer(JavaTypes.STRING, "id", bundle.meta().id())
                        .put(DATA_BUNDLE, "data", bundle));
    }

    @Override
    public void onReady(ProtoConnection connection) {
        ProtoProxy.getConnectedServer(connection)
                .flatMap(SingularityConfig::getSyncGroup)
                .ifPresent(group -> connection.send(group.getSettings()));
    }

    @Override
    public void handlePacket(ProtoConnection connection, Object packet) {
        ProtoProxy.getConnectedServer(connection).ifPresent(server -> {
            switch (packet) {
                // Handle a topic subscription
                case Topic sub -> subscriptions.computeIfAbsent(sub, k -> Sets.newConcurrentHashSet()).add(server);
                // Process data bundle actions such as requesting and removing data
                case DataBundle.Meta meta -> {
                    switch (meta.action()) {
                        case GET -> SingularityConfig.getSyncGroup(server)
                                .flatMap(store -> store.getTopicStore(meta.topic()).getContainer("id", meta.id()))
                                .flatMap(c -> c.get(DATA_BUNDLE, "data"))
                                .ifPresentOrElse(data -> connection.send(data.meta(meta)), () -> connection.send(meta));

                        case REMOVE -> SingularityConfig.getSyncGroup(server)
                                .flatMap(store -> store.getTopicStore(meta.topic()).getContainer("id", meta.id()))
                                .ifPresent(DataContainer::delete);
                    }
                }
                // Process data bundle put action and data propagation
                case DataBundle bundle -> {
                    if (!DataBundle.Action.PUT.equals(bundle.meta().action())) return;

                    // Run expensive lookups outside the filters
                    Set<ProtoServer> group = SingularityConfig.getSyncGroup(server).map(SingularityConfig.SyncGroup::getServers).orElse(new HashSet<>());
                    Optional<ProtoServer> location = Velocity.getPlayerLocation(bundle);

                    subscriptions.getOrDefault(bundle.meta().topic(), new HashSet<>()).stream()     // Grab all servers subbed to this topic
                            .filter(s -> !Objects.equals(s, server))                                // Filter out the server the packet was received from
                            .filter(s -> bundle.meta().topic().global() || group.contains(s))       // Filter out any servers that aren't part of the current sync group unless the topic is global
                            .filter(s -> {                                                          // Filter out servers based on propagation rules
                                if (bundle.meta().propagation().equals(DataBundle.Propagation.ALL)) return true;
                                if (bundle.meta().propagation().equals(DataBundle.Propagation.NONE)) return false;
                                // Propagation type must be PLAYER
                                return location.isPresent() && Objects.equals(location.get(), s);
                            })
                            .forEach(s -> s.getConnection(PROTOCOL).ifPresent(con -> con.send(bundle)));

                    if (bundle.meta().persist()) storeBundle(server, bundle);
                }
                default -> PROTOCOL.logWarn("Ignoring unknown packet: " + packet);
            }
        });
    }
}