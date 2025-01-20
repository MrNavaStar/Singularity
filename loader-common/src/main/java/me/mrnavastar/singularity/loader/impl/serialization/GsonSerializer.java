package me.mrnavastar.singularity.loader.impl.serialization;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import me.mrnavastar.singularity.common.networking.DataBundle;

import java.nio.charset.StandardCharsets;

public class GsonSerializer implements DataBundle.Serializer<JsonElement> {

    private final Gson GSON = new Gson();

    @Override
    public byte[] serialize(JsonElement object) {
        return GSON.toJson(object, JsonElement.class).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public JsonElement deserialize(byte[] bytes) {
        return GSON.fromJson(new String(bytes, StandardCharsets.UTF_8), JsonElement.class);
    }

    public static void register() {
        DataBundle.register(JsonElement.class, new GsonSerializer());
    }
}