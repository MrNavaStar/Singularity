package me.mrnavastar.singularity.loader.impl.serialization;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import me.mrnavastar.singularity.common.SingularitySerializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

public class PrimitiveSerializers {

    public static class StringSerializer implements SingularitySerializer.Serializer<String> {

        @SneakyThrows
        @Override
        public void serialize(String object, ByteArrayOutputStream out) {
            out.write(object.length());
            out.write(object.getBytes(StandardCharsets.UTF_8));
        }

        @SneakyThrows
        @Override
        public String deserialize(ByteArrayInputStream in) {
            return new String(in.readNBytes(in.read()), StandardCharsets.UTF_8);
        }
    }

    public static class BooleanSerializer implements SingularitySerializer.Serializer<Boolean> {

        @SneakyThrows
        @Override
        public void serialize(Boolean object, ByteArrayOutputStream out) {
            out.write(object ? 1 : 0);
        }

        @Override
        public Boolean deserialize(ByteArrayInputStream in) {
            return in.read() == 1;
        }
    }

    @RequiredArgsConstructor
    public static class ClassSerializer implements SingularitySerializer.Serializer<Class<?>> {

        private final SingularitySerializer.Serializer<String> stringSerializer;

        @Override
        public void serialize(Class<?> object, ByteArrayOutputStream out) {
            stringSerializer.serialize(object.getName(), out);
        }

        @SneakyThrows
        @Override
        public Class<?> deserialize(ByteArrayInputStream in) {
            return Class.forName(stringSerializer.deserialize(in));
        }
    }

    public static void registerAll(SingularitySerializer serializer) {
        serializer.register(StringSerializer.class);
        serializer.register(BooleanSerializer.class);
        serializer.register(ClassSerializer.class);
    }
}
