package me.mrnavastar.singularity.loader.impl.serialization;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import lombok.SneakyThrows;
import me.mrnavastar.singularity.common.serialization.SingularitySerializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

public class GsonSerializer implements SingularitySerializer.Serializer<JsonElement> {

    private static final Gson GSON = new Gson();

    @SneakyThrows
    @Override
    public void serialize(JsonElement object, ByteArrayOutputStream out) {
        out.write(GSON.toJson(object).getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public JsonElement deserialize(ByteArrayInputStream in) {
        return GSON.fromJson(new String(in.readAllBytes(), StandardCharsets.UTF_8), JsonElement.class);
    }

    /*public static void register() {
        DataBundle.register(JsonElement.class, new GsonSerializer());
    }*/
}