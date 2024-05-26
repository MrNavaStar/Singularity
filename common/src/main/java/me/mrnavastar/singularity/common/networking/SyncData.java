package me.mrnavastar.singularity.common.networking;

import com.esotericsoftware.kryo.kryo5.Kryo;
import com.esotericsoftware.kryo.kryo5.io.Input;
import com.esotericsoftware.kryo.kryo5.io.Output;
import lombok.*;

import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

@AllArgsConstructor
@EqualsAndHashCode
public class SyncData {

    private static final Kryo kryo = new Kryo();

    static {
        kryo.setRegistrationRequired(false);
    }

    @Getter private final UUID id;
    @Getter private final String name;
    @Getter private final Date date;
    private final HashMap<String, byte[]> map = new HashMap<>();

    @SneakyThrows
    public void put(String key, Object object) {
        try (Output out = new Output()) {
            kryo.writeObject(out, object);
            map.put(key, out.toBytes());
        }
    }

    @SneakyThrows
    public <T> T get(String key, Class<T> type) {
        byte [] data = map.get(key);
        if (data == null) return null;

        try (Input in = new Input()) {
            return kryo.readObject(in, type);
        }
    }

    public boolean remove(String key) {
        return map.remove(key) != null;
    }
}