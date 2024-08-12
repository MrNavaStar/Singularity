package me.mrnavastar.singularity.common.networking;

import com.esotericsoftware.kryo.kryo5.Kryo;
import com.esotericsoftware.kryo.kryo5.Serializer;
import com.esotericsoftware.kryo.kryo5.io.Input;
import com.esotericsoftware.kryo.kryo5.io.Output;
import com.esotericsoftware.kryo.kryo5.objenesis.strategy.StdInstantiatorStrategy;
import com.esotericsoftware.kryo.kryo5.serializers.DefaultSerializers;
import com.esotericsoftware.kryo.kryo5.util.DefaultInstantiatorStrategy;
import lombok.*;
import me.mrnavastar.singularity.common.Constants;

import java.util.HashMap;
import java.util.UUID;

@Getter
@EqualsAndHashCode
public class SyncData {

    private static final Kryo kryo = new Kryo();

    private UUID player;
    private HashMap<String, byte[]> data = new HashMap<>();

    public SyncData(UUID player) {
        this.player = player;
    }

    static {
        kryo.setRegistrationRequired(false);
        kryo.setInstantiatorStrategy(new DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));
        kryo.addDefaultSerializer(UUID.class, new DefaultSerializers.UUIDSerializer());
    }

    public static <T> void registerSerializer(Class<T> type, Serializer<T> serializer) {
        kryo.addDefaultSerializer(type, serializer);
    }

    public void put(String key, Object object) {
        try (Output out = new Output(Constants.MAX_DATA_SIZE)) {
            kryo.writeObject(out, object);
            data.put(key, out.toBytes());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public <T> T get(String key, Class<T> type) {
        byte [] data = this.data.get(key);
        if (data == null) return null;

        try (Input in = new Input(data)) {
            return kryo.readObject(in, type);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean remove(String key) {
        return data.remove(key) != null;
    }
}