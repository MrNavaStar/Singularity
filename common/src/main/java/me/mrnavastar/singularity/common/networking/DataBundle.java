package me.mrnavastar.singularity.common.networking;

import lombok.EqualsAndHashCode;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import me.mrnavastar.protoweaver.core.util.ObjectSerializer;

import java.util.HashMap;
import java.util.Optional;

@Accessors(fluent = true)
@Setter
@Getter
@ToString
@EqualsAndHashCode
public class DataBundle {

    public enum Action {
        PUT,
        GET,
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