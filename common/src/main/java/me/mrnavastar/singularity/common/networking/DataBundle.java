package me.mrnavastar.singularity.common.networking;

import lombok.*;

import lombok.experimental.Accessors;
import me.mrnavastar.singularity.common.serialization.SingularitySerializer;

import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Accessors(fluent = true)
@Setter
@Getter
@ToString
@EqualsAndHashCode
public class DataBundle {

    public enum Action {
        PUT,
        GET,
        GET_ALL,
        REMOVE,
        NONE
    }

    public enum Propagation {
        ALL,
        PLAYER,
        NONE
    }

    @Setter
    @Getter
    @ToString
    @EqualsAndHashCode
    public static class Meta {
        private String id;
        private Action action = Action.NONE;
        private Topic topic;
        private Propagation propagation = Propagation.ALL;
        private boolean persist = true;
    }

    private static final SingularitySerializer dataBundleSerializer = new SingularitySerializer();

    public static void register(Class<? extends SingularitySerializer.Serializer<?>> serializer) {
        dataBundleSerializer.register(serializer);
    }

    private Meta meta = new Meta();
    private transient final ConcurrentHashMap<String, Object> objects = new ConcurrentHashMap<>();
    private HashMap<String, byte[]> data = new HashMap<>();

    public DataBundle put(@NonNull String key, @NonNull Object object) {
        objects.put(key, object);
        data.put(key, dataBundleSerializer.serialize(object));
        return this;
    }

    public <T> Optional<T> get(@NonNull String key, @NonNull Class<T> type) {
        return Optional.ofNullable(type.cast(objects.computeIfAbsent(key, k ->
                Optional.ofNullable(data.get(k)).map(buf -> dataBundleSerializer.deserialize(buf, type))
                        .orElse(null))));
    }
}