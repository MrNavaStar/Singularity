package me.mrnavastar.singularity.loader.impl.serialization;

import me.mrnavastar.singularity.common.networking.DataBundle;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class NbtSerializer implements DataBundle.Serializer<CompoundTag> {

    @Override
    public byte[] serialize(CompoundTag object) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            NbtIo.writeCompressed(object, out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public CompoundTag deserialize(byte[] bytes) {
        try (ByteArrayInputStream in = new ByteArrayInputStream(bytes)) {
            return NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void register() {
        DataBundle.register(CompoundTag.class, new NbtSerializer());
    }
}