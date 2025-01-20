package me.mrnavastar.singularity.common;

import lombok.SneakyThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class SingularitySerializer {

    public interface Serializer<T> {
        void serialize(T object, ByteArrayOutputStream out);
        T deserialize(ByteArrayInputStream in);
    }

    private final ConcurrentHashMap<Class<?>, Serializer<?>> serializers = new ConcurrentHashMap<>();

    @SneakyThrows
    public void register(Class<? extends Serializer<?>> serializer) {
        ArrayList<Serializer<?>> dependencies = new ArrayList<>();
        boolean valid = true;

        for (Constructor<?> constructor : serializer.getDeclaredConstructors()) {
            for (Parameter p : constructor.getParameters()) {
                if (!p.getType().equals(Serializer.class) || !(p.getParameterizedType() instanceof ParameterizedType type)) {
                    dependencies.clear();
                    valid = false;
                    break;
                }

                Class<?> serType = Class.forName(type.getActualTypeArguments()[0].getTypeName());
                Serializer<?> ser = serializers.get(serType);
                if (ser == null) throw new RuntimeException(serializer.getName() + " depends on a serializer that is not yet registered: " + serType.getCanonicalName());
                dependencies.add(ser);
            }

            if (!valid) {
                valid = true;
                continue;
            }

            Serializer<?> instance = (Serializer<?>) constructor.newInstance(dependencies.toArray());
            Class<?> type = serializer.getMethod("deserialize", ByteArrayInputStream.class).getReturnType();
            serializers.put(type, instance);
            break;
        }
    }

    private Optional<Serializer<?>> getSerializer(Class<?> type) {
        return Optional.ofNullable(serializers.computeIfAbsent(type, k -> getSerializer(type.getSuperclass()).orElse(null)));
    }

    public <T> byte[] serialize(T object) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        getSerializer(object.getClass()).ifPresent(serializer -> ((Serializer<Object>) serializer).serialize(object, out));
        return out.toByteArray();
    }

    public <T> T deserialize(byte[] bytes, Class<? extends T> type) {
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        return type.cast(getSerializer(type).map(serializer -> serializer.deserialize(in))
                .orElseThrow(() -> new RuntimeException("no serializer registered for type: " + type.getCanonicalName())));
    }
}
