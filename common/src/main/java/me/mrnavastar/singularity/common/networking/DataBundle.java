package me.mrnavastar.singularity.common.networking;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.mrnavastar.protoweaver.core.util.ObjectSerializer;

import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

@Getter
@EqualsAndHashCode
@AllArgsConstructor
public class DataBundle {
    private UUID id;

    private static final ObjectSerializer serializer = new ObjectSerializer();

    private final HashMap<String, byte[]> data = new HashMap<>();

    public static void register(Class<?> type) {
        serializer.register(type);
    }

    public DataBundle put(String key, Object object) {
        data.put(key, serializer.serialize(object));
        return this;
    }

    public <T> Optional<T> get(String key, Class<T> type) {
        byte [] data = this.data.get(key);
        if (data == null) return Optional.empty();
        return Optional.of(type.cast(serializer.deserialize(data)));
    }

    public boolean remove(String key) {
        return data.remove(key) != null;
    }
}