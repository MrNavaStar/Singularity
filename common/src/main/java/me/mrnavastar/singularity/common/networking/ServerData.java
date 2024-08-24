package me.mrnavastar.singularity.common.networking;

import lombok.*;
import me.mrnavastar.protoweaver.core.util.Furious;
import org.apache.fury.Fury;
import org.apache.fury.ThreadSafeFury;

import java.util.HashMap;
import java.util.Optional;

@Getter
@EqualsAndHashCode
public class ServerData {

    private static final ThreadSafeFury FURY = Fury.builder().withJdkClassSerializableCheck(false).buildThreadSafeFury();

    private final HashMap<String, byte[]> data = new HashMap<>();

    public static void register(Class<?> type) {
        Furious.register(FURY, type);
    }

    public ServerData put(String key, Object object) {
        data.put(key, Furious.serialize(FURY, object));
        return this;
    }

    public <T> Optional<T> get(String key, Class<T> type) {
        byte [] data = this.data.get(key);
        if (data == null) return Optional.empty();
        return Optional.of(type.cast(Furious.deserialize(FURY, data)));
    }

    public boolean remove(String key) {
        return data.remove(key) != null;
    }
}