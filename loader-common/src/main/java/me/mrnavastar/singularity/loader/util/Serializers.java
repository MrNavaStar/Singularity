package me.mrnavastar.singularity.loader.util;

import com.esotericsoftware.kryo.kryo5.Kryo;
import com.esotericsoftware.kryo.kryo5.io.Input;
import com.esotericsoftware.kryo.kryo5.io.Output;
import com.esotericsoftware.kryo.kryo5.serializers.ImmutableSerializer;
import lombok.SneakyThrows;
import net.minecraft.nbt.*;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;

public class Serializers {

    public static class Nbt extends ImmutableSerializer<CompoundTag> {

        @Override
        @SneakyThrows
        public void write(Kryo kryo, Output output, CompoundTag object) {
            DataOutput dataOutput = new DataOutputStream(output);
            NbtIo.write(object, dataOutput);
        }

        @Override
        @SneakyThrows
        public CompoundTag read(Kryo kryo, Input input, Class<? extends CompoundTag> type) {
            DataInput dataInput = new DataInputStream(input);
            return NbtIo.read(dataInput, NbtAccounter.unlimitedHeap());
        }
    }
}
