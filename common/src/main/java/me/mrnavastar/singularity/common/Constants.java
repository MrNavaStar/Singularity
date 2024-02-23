package me.mrnavastar.singularity.common;

import me.mrnavastar.protoweaver.api.protocol.CompressionType;
import me.mrnavastar.protoweaver.api.protocol.Protocol;
import me.mrnavastar.protoweaver.api.protocol.velocity.VelocityAuth;
import me.mrnavastar.singularity.common.networking.Settings;
import me.mrnavastar.singularity.common.networking.SyncData;

public class Constants {

    public static final String MOD_NAME = "Singularity";
    public static final String MOD_ID = "singularity";
    public static final String VERSION = "debug-build";
    public static final String BOOT_MESSAGE = "Warping space and time ...";

    public static final Protocol PROTOCOL = Protocol.create(MOD_ID, "sync")
            .setServerAuthHandler(VelocityAuth.class)
            .setClientAuthHandler(VelocityAuth.class)
            .setCompression(CompressionType.GZIP)
            .setMaxConnections(1)
            .addPacket(Settings.class)
            .addPacket(SyncData.class)
            .build();

    public static final String PLAYER_DATA = MOD_ID + ":data";
    public static final String PLAYER_ADVANCEMENTS = MOD_ID + ":adv";
    public static final String PLAYER_STATS = MOD_ID + ":stats";
}
