package me.mrnavastar.singularity.common.networking;

import lombok.*;
import me.mrnavastar.protoweaver.core.util.ObjectSerializer;

import java.util.HashMap;
import java.util.Optional;

@Getter
@EqualsAndHashCode
public class ServerData {

    private static final ObjectSerializer serializer = new ObjectSerializer();

    private final HashMap<String, byte[]> data = new HashMap<>();

    public static void register(Class<?> type) {
        serializer.register(type);
    }

    public ServerData put(String key, Object object) {
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