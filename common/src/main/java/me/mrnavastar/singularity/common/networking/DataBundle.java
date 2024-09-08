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
@Setter
@Getter
@EqualsAndHashCode
public class DataBundle {

    public enum Action {
        PUT,
        GET,
        REMOVE,
        NONE
    }

    @Setter
    @Getter
    @EqualsAndHashCode
    public static class Meta {
        private UUID id;
        private String topic;
        private Action action = Action.NONE;
        private Subscription.TopicType topicType;
        private boolean propagate = false;

        @Override
        public String toString() {
            return topic + ":" + id;
        }
    }

    private static final ObjectSerializer serializer = new ObjectSerializer();

    private HashMap<String, byte[]> data = new HashMap<>();
    private Meta meta = new Meta();

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
}