package me.mrnavastar.singularity.loader.impl.serialization;

import lombok.SneakyThrows;
import me.mrnavastar.singularity.common.serialization.SingularitySerializer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class NbtSerializer implements SingularitySerializer.Serializer<CompoundTag> {

    @SneakyThrows
    @Override
    public void serialize(CompoundTag object, ByteArrayOutputStream out) {
        NbtIo.writeCompressed(object, out);
    }

    @SneakyThrows
    @Override
    public CompoundTag deserialize(ByteArrayInputStream in) {
        return NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap());
    }
}