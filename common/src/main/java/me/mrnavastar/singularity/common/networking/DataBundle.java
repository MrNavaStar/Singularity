package me.mrnavastar.singularity.common.networking;

import lombok.EqualsAndHashCode;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import me.mrnavastar.protoweaver.core.util.ObjectSerializer;

import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

@Accessors(fluent = true)
@Getter
@EqualsAndHashCode
public class DataBundle {

    public enum Action {
        PUT,
        GET,
        REMOVE
    }

    @Setter
    @Getter
    @EqualsAndHashCode
    public static class Meta {
        private UUID id;
        private String topic;
        private Action action;

        @Override
        public String toString() {
            return topic + ":" + id;
        }
    }

    private static final ObjectSerializer serializer = new ObjectSerializer();

    private final HashMap<String, byte[]> data = new HashMap<>();
    private final Meta meta = new Meta();
    private boolean shouldPropagate = false;

    public static void register(Class<?> type) {
        serializer.register(type);
    }

    public DataBundle put(String key, Object object) {
        data.put(key, serializer.serialize(object));
        return this;
    }

    public <T> Optional<T> get(String key, Class<T> type) {
        return Optional.ofNullable(this.data.get(key))
                .flatMap(data -> Optional.ofNullable(serializer.deserialize(data))
                        .flatMap(o -> Optional.of(type.cast(o))));
    }

    public boolean remove(String key) {
        return data.remove(key) != null;
    }

    public DataBundle propagate() {
        shouldPropagate = true;
        return this;
    }
}