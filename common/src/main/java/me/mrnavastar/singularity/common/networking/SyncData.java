package me.mrnavastar.singularity.common.networking;

import com.esotericsoftware.kryo.kryo5.Kryo;
import com.esotericsoftware.kryo.kryo5.Serializer;
import com.esotericsoftware.kryo.kryo5.io.Input;
import com.esotericsoftware.kryo.kryo5.io.Output;
import com.esotericsoftware.kryo.kryo5.objenesis.strategy.StdInstantiatorStrategy;
import com.esotericsoftware.kryo.kryo5.util.DefaultInstantiatorStrategy;
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

    @Getter private UUID id;
    @Getter private String name;
    @Getter private Date date;
    private final HashMap<String, byte[]> map = new HashMap<>();

    static {
        kryo.setRegistrationRequired(false);
        kryo.setInstantiatorStrategy(new DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));
    }

    public static <T> void registerSerializer(Class<T> type, Serializer<T> serializer) {
        kryo.register(type, serializer);
    }

    @SneakyThrows
    public void put(String key, Object object) {
        try (Output out = new Output(new ByteArrayOutputStream())) {
            kryo.writeObject(out, object);
            map.put(key, out.toBytes());
        }
    }

    @SneakyThrows
    public <T> T get(String key, Class<T> type) {
        byte [] data = map.get(key);
        if (data == null) return null;

        try (Input in = new Input(new ByteArrayInputStream(data))) {
            return kryo.readObject(in, type);
        }
    }

    public boolean remove(String key) {
        return map.remove(key) != null;
    }
}