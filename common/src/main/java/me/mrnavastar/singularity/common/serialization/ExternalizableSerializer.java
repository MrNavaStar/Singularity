package me.mrnavastar.singularity.loader.impl.serialization;

import lombok.RequiredArgsConstructor;
import me.mrnavastar.singularity.common.SingularitySerializer;

import java.io.*;
import java.lang.reflect.InvocationTargetException;

@RequiredArgsConstructor
public class ExternalizableSerializer implements SingularitySerializer.Serializer<Externalizable> {

    private final SingularitySerializer.Serializer<Class<?>> classSerializer;

    @Override
    public void serialize(Externalizable object, ByteArrayOutputStream out) {
        try (ObjectOutput buf = new ObjectOutputStream(out)) {
            classSerializer.serialize(object.getClass(), out);
            object.writeExternal(buf);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Externalizable deserialize(ByteArrayInputStream in) {
        try (ObjectInput buf = new ObjectInputStream(in)) {
            Externalizable instance = (Externalizable) classSerializer.deserialize(in).getDeclaredConstructor().newInstance();
            instance.readExternal(buf);
            return instance;
        } catch (IOException | ClassNotFoundException | InvocationTargetException | InstantiationException |
                 IllegalAccessException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    /*public static void register() {
        DataBundle.register(Externalizable.class, new ExternalizableSerializer());
    }*/
}
