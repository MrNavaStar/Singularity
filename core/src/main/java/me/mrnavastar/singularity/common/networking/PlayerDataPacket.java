package me.mrnavastar.singularity.common.networking;

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import me.mrnavastar.protoweaver.api.ProtoPacket;
import me.mrnavastar.protoweaver.api.util.BufUtils;

import java.util.UUID;

@Getter
@NoArgsConstructor
@RequiredArgsConstructor
public class PlayerDataPacket implements ProtoPacket {

    private UUID id;
    private String name;
    private String data;

    @Override
    public void encode(ByteBuf byteBuf) {
        BufUtils.writeString(byteBuf, id.toString());
        BufUtils.writeString(byteBuf, name);
        BufUtils.writeString(byteBuf, data);
    }

    @Override
    public void decode(ByteBuf byteBuf) throws IndexOutOfBoundsException {
        id = UUID.fromString(BufUtils.readString(byteBuf));
        name = BufUtils.readString(byteBuf);
        data = BufUtils.readString(byteBuf);
    }
}