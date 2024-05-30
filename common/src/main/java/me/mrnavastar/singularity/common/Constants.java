package me.mrnavastar.singularity.common;

import me.mrnavastar.protoweaver.api.protocol.CompressionType;
import me.mrnavastar.protoweaver.api.protocol.Protocol;
import me.mrnavastar.protoweaver.api.protocol.velocity.VelocityAuth;
import me.mrnavastar.singularity.common.networking.Settings;
import me.mrnavastar.singularity.common.networking.SyncData;

import java.util.UUID;

public class Constants {

    public static final String SINGULARITY_NAME = "Singularity";
    public static final String SINGULARITY_ID = "singularity";
    public static final String SINGULARITY_VERSION = "debug-build";
    public static final String SINGULARITY_AUTHOR = "MrNavaStar";
    public static final String SINGULARITY_BOOT_MESSAGE = "Warping space and time ...";

    public static final Protocol.Builder PROTOCOL = Protocol.create(SINGULARITY_ID, "sync")
            .setServerAuthHandler(VelocityAuth.class)
            .setClientAuthHandler(VelocityAuth.class)
            .setCompression(CompressionType.GZIP)
            .setMaxPacketSize(16777216) // 16mb
            .setMaxConnections(1)
            .addPacket(Settings.class)
            .addPacket(SyncData.class);

    public static final String PLAYER_DATA = SINGULARITY_ID + ":data";
    public static final String PLAYER_ADVANCEMENTS = SINGULARITY_ID + ":adv";
    public static final String PLAYER_STATS = SINGULARITY_ID + ":stats";
}
