package me.mrnavastar.singularity.common.networking;

import lombok.Getter;
import me.mrnavastar.protoweaver.api.ProtoBuilder;
import me.mrnavastar.protoweaver.api.protocol.CompressionType;
import me.mrnavastar.protoweaver.api.protocol.Protocol;
import me.mrnavastar.protoweaver.api.protocol.velocity.VelocityAuth;
import me.mrnavastar.singularity.common.Constants;

public class Networking {

    @Getter
    private static final Protocol syncProtocol = ProtoBuilder.protocol(Constants.MOD_ID, "sync")
            .setServerAuthHandler(VelocityAuth.class)
            .setClientAuthHandler(VelocityAuth.class)
            .enableCompression(CompressionType.GZIP)
            .addPacket(PlayerDataPacket.class)
            .build();
}