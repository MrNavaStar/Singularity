package me.mrnavastar.singularity.common.networking;

import lombok.Getter;
import me.mrnavastar.protoweaver.api.protocol.CompressionType;
import me.mrnavastar.protoweaver.api.protocol.Protocol;
import me.mrnavastar.protoweaver.api.protocol.velocity.VelocityAuth;
import me.mrnavastar.singularity.common.Constants;

public class Networking {

    @Getter
    private static final Protocol syncProtocol = Protocol.create(Constants.MOD_ID, "sync")
            .setServerAuthHandler(VelocityAuth.class)
            .setClientAuthHandler(VelocityAuth.class)
            .enableCompression(CompressionType.GZIP)
            .addPacket(Settings.class)
            .addPacket(SyncData.class)
            .build();
}