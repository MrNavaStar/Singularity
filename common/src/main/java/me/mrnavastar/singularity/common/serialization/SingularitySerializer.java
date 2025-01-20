package me.mrnavastar.singularity.common.serialization;

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

        @SneakyThrows
        default Class<T> getType() {
            return (Class<T>) getClass().getMethod("deserialize", ByteArrayInputStream.class).getReturnType();
        }
    }

    private final ConcurrentHashMap<Class<?>, Serializer<?>> serializers = new ConcurrentHashMap<>();

    public SingularitySerializer() {
        PrimitiveSerializers.registerAll(this);
        register(ExternalizableSerializer.class);
    }

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

                Class<?> serType = Class.forName(type.getActualTypeArguments()[0].getTypeName().split("<")[0]);
                Serializer<?> ser = serializers.get(serType);
                if (ser == null) throw new RuntimeException(serializer.getName() + " depends on a serializer that is not yet registered: " + serType.getCanonicalName());
                dependencies.add(ser);
            }

            if (!valid) {
                valid = true;
                continue;
            }

            Serializer<?> instance = (Serializer<?>) constructor.newInstance(dependencies.toArray());
            serializers.put(instance.getType(), instance);
            break;
        }
    }

    private Optional<Serializer<?>> getSerializer(Class<?> type) {
        Optional<Serializer<?>> maybeSerializer = Optional.ofNullable(serializers.computeIfAbsent(type, k -> getSerializer(type.getSuperclass()).orElse(null)));
        if (maybeSerializer.isPresent()) return maybeSerializer;

        for (Class<?> anInterface : type.getInterfaces()) {
            Optional<Serializer<?>> serializer = getSerializer(anInterface);
            if (serializer.isPresent()) return serializer;
        }
        throw new RuntimeException("no serializer registered for type: " + type.getCanonicalName());
    }

    public <T> byte[] serialize(T object) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ((Serializer<Object>) getSerializer(object.getClass()).get()).serialize(object, out);
        return out.toByteArray();
    }

    public <T> T deserialize(byte[] bytes, Class<? extends T> type) {
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        return type.cast(getSerializer(type).get().deserialize(in));
    }
}
