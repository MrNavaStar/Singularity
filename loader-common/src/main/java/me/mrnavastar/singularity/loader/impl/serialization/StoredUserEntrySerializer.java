package me.mrnavastar.singularity.loader.impl.serialization;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import me.mrnavastar.r.R;
import me.mrnavastar.singularity.common.serialization.SingularitySerializer;
import me.mrnavastar.singularity.loader.util.Mappings;
import net.minecraft.server.players.StoredUserEntry;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

@RequiredArgsConstructor
public class StoredUserEntrySerializer implements SingularitySerializer.Serializer<StoredUserEntry<?>> {

    private final SingularitySerializer.Serializer<JsonElement> jsonSerializer;
    private final SingularitySerializer.Serializer<Class<?>> classSerializer;

    @SneakyThrows
    @Override
    public void serialize(StoredUserEntry<?> object, ByteArrayOutputStream out) {
        JsonObject json = new JsonObject();
        R.of(object).call(Mappings.of("serialize", "method_24896"), json);

        classSerializer.serialize(object.getClass(), out);
        jsonSerializer.serialize(json, out);
    }

    @SneakyThrows
    public static <T> StoredUserEntry<T> deserialize(JsonObject json, Class<? extends StoredUserEntry<T>> clazz) {
        return clazz.getDeclaredConstructor(JsonObject.class).newInstance(json);
    }

    @SneakyThrows
    @Override
    public StoredUserEntry<?> deserialize(ByteArrayInputStream in) {
        Class<?> clazz = classSerializer.deserialize(in);
        JsonObject json = (JsonObject) jsonSerializer.deserialize(in);
        return (StoredUserEntry<?>) clazz.getDeclaredConstructor(JsonObject.class).newInstance(json);
    }
}