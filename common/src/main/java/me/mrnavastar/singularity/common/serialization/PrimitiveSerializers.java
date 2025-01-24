package me.mrnavastar.singularity.common.serialization;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

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

    public static class ByteSerializer implements SingularitySerializer.Serializer<Byte> {

        @Override
        public void serialize(Byte object, ByteArrayOutputStream out) {
            out.write(object);
        }

        @SneakyThrows
        @Override
        public Byte deserialize(ByteArrayInputStream in) {
            return in.readNBytes(1)[0];
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

    // Serialize enum by name so data is preserved if enum is reordered
    @RequiredArgsConstructor
    public static class EnumSerializer implements SingularitySerializer.Serializer<Enum<?>> {

        private final SingularitySerializer.Serializer<Class<?>> classSerializer;
        private final SingularitySerializer.Serializer<String> stringSerializer;

        @Override
        public void serialize(Enum<?> object, ByteArrayOutputStream out) {
            classSerializer.serialize(object.getClass(), out);
            stringSerializer.serialize(object.name(), out);
        }

        @SneakyThrows
        @Override
        public Enum<?> deserialize(ByteArrayInputStream in) {
            Class<Enum<?>> enumClass = (Class<Enum<?>>) classSerializer.deserialize(in);
            String enumName = stringSerializer.deserialize(in);
            return Arrays.stream(enumClass.getEnumConstants()).filter(en -> en.name().equals(enumName)).findFirst().orElse(null);
        }
    }

    public static void registerAll(SingularitySerializer serializer) {
        serializer.register(StringSerializer.class);
        serializer.register(BooleanSerializer.class);
        serializer.register(ByteSerializer.class);
        serializer.register(ClassSerializer.class);
        serializer.register(EnumSerializer.class);
    }
}
