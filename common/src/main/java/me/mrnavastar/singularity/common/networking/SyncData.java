package me.mrnavastar.singularity.common.networking;

import com.esotericsoftware.kryo.kryo5.Kryo;
import com.esotericsoftware.kryo.kryo5.io.Input;
import com.esotericsoftware.kryo.kryo5.io.Output;
import lombok.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            kryo.writeObject(new Output(out), object);
            map.put(key, out.toByteArray());
        }
    }

    @SneakyThrows
    public <T> T get(String key, Class<T> type) {
        byte [] data = map.get(key);
        if (data == null) return null;

        try (ByteArrayInputStream in = new ByteArrayInputStream(data)) {
            return kryo.readObject(new Input(in), type);
        }
    }

    public boolean remove(String key) {
        return map.remove(key) != null;
    }
}