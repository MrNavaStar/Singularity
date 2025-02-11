package me.mrnavastar.singularity.common;

import me.mrnavastar.protoweaver.api.protocol.CompressionType;
import me.mrnavastar.protoweaver.api.protocol.Protocol;
import me.mrnavastar.protoweaver.api.protocol.velocity.VelocityAuth;
import me.mrnavastar.singularity.common.networking.DataBundle;
import me.mrnavastar.singularity.common.networking.Settings;
import me.mrnavastar.singularity.common.networking.Topic;

public class Constants {

    public static final String SINGULARITY_NAME = "Singularity";
    public static final String SINGULARITY_ID = "singularity";
    public static final String SINGULARITY_VERSION = "debug-build";
    public static final String SINGULARITY_AUTHOR = "MrNavaStar";
    public static final String SINGULARITY_BOOT_MESSAGE = "Warping space and time ...";

    public static final Protocol.Builder PROTOCOL = Protocol.create(SINGULARITY_ID, "wormhole")
            .setServerAuthHandler(VelocityAuth.class)
            .setClientAuthHandler(VelocityAuth.class)
            .setCompression(CompressionType.GZIP)
            .setMaxPacketSize(67108864) // 64mb
            .setMaxConnections(1)
            .addPacket(Settings.class)
            .addPacket(DataBundle.class)
            .addPacket(Topic.class);

    public static final String PLAYER_TOPIC = SINGULARITY_ID + ":player";

    public static final String USER_CACHE = SINGULARITY_ID + ":users";
    public static final String WHITELIST = SINGULARITY_ID + ":white";
    public static final String OPERATOR = SINGULARITY_ID + ":op";
    public static final String BANNED_PLAYERS = SINGULARITY_ID + ":bans";
    public static final String BANNED_IPS = SINGULARITY_ID + ":ibans";
}