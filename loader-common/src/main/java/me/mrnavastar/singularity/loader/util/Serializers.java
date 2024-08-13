package me.mrnavastar.singularity.loader.util;

import com.esotericsoftware.kryo.kryo5.Kryo;
import com.esotericsoftware.kryo.kryo5.Serializer;
import com.esotericsoftware.kryo.kryo5.io.Input;
import com.esotericsoftware.kryo.kryo5.io.Output;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.SneakyThrows;
import me.mrnavastar.protoweaver.api.ProtoWeaver;
import me.mrnavastar.protoweaver.api.netty.ProtoConnection;
import me.mrnavastar.singularity.loader.impl.IpBanListHack;
import me.mrnavastar.singularity.loader.impl.ServerOpListHack;
import me.mrnavastar.singularity.loader.impl.UserBanListHack;
import me.mrnavastar.singularity.loader.impl.UserWhiteListHack;
import net.minecraft.nbt.*;
import net.minecraft.server.players.StoredUserEntry;
import net.minecraft.server.players.StoredUserList;

import java.util.Map;

public class Serializers {

    public static final Gson GSON = new Gson();

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

    public static class SUL extends Serializer<StoredUserList> {

        @Override
        public void write(Kryo kryo, Output output, StoredUserList object) {
            object.getEntries().forEach(v -> {
                JsonObject j = new JsonObject();
                ReflectionUtil.invokeMethod(v, "serialize", null, j);
                output.writeString(j.toString());
            });
        }

        @Override
        public StoredUserList read(Kryo kryo, Input input, Class<? extends StoredUserList> type) {
            if (type == UserWhiteListHack.class) return new UserWhiteListHack(input);
            else if (type == ServerOpListHack.class) return new ServerOpListHack(input);
            else if (type == UserBanListHack.class) return new UserBanListHack(input);
            else if (type == IpBanListHack.class) return new IpBanListHack(input);
            return null;
        }
    }
}
