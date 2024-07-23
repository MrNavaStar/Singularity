package me.mrnavastar.singularity.loader.util;

import com.esotericsoftware.kryo.kryo5.Kryo;
import com.esotericsoftware.kryo.kryo5.Serializer;
import com.esotericsoftware.kryo.kryo5.io.Input;
import com.esotericsoftware.kryo.kryo5.io.Output;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.SneakyThrows;
import net.minecraft.nbt.*;

import java.io.*;

public class Serializers {

    public static class Nbt extends Serializer<CompoundTag> {

        @Override
        @SneakyThrows
        public void write(Kryo kryo, Output output, CompoundTag tag) {
            output.writeString(tag.toString());
        }

        @Override
        @SneakyThrows
        public CompoundTag read(Kryo kryo, Input input, Class<? extends CompoundTag> type) {
            return TagParser.parseTag(input.readString());
        }
    }

    public static class Json extends Serializer<JsonElement> {

        private static final Gson GSON = new Gson();

        @Override
        @SneakyThrows
        public void write(Kryo kryo, Output output, JsonElement element) {
            System.out.println(element);
            output.writeBytes(GSON.toJson(element).getBytes());

        }

        @Override
        @SneakyThrows
        public JsonElement read(Kryo kryo, Input input, Class<? extends JsonElement> type) {
            System.out.println(new String(input.readAllBytes()));
            return new JsonObject();
        }
    }
}
