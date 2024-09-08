package me.mrnavastar.singularity.common;

import me.mrnavastar.protoweaver.api.protocol.CompressionType;
import me.mrnavastar.protoweaver.api.protocol.Protocol;
import me.mrnavastar.protoweaver.api.protocol.velocity.VelocityAuth;
import me.mrnavastar.singularity.common.networking.DataBundle;
import me.mrnavastar.singularity.common.networking.Settings;
import me.mrnavastar.singularity.common.networking.Profile;
import me.mrnavastar.singularity.common.networking.Subscription;

import java.util.UUID;

public class Constants {

    public static final String SINGULARITY_NAME = "Singularity";
    public static final String SINGULARITY_ID = "singularity";
    public static final String SINGULARITY_VERSION = "debug-build";
    public static final String SINGULARITY_AUTHOR = "MrNavaStar";
    public static final String SINGULARITY_BOOT_MESSAGE = "Warping space and time ...";

    public static final Protocol.Builder WORMHOLE = Protocol.create(SINGULARITY_ID, "wormhole")
            .setServerAuthHandler(VelocityAuth.class)
            .setClientAuthHandler(VelocityAuth.class)
            .setCompression(CompressionType.GZIP)
            .setMaxPacketSize(67108864) // 64mb
            .setMaxConnections(1)
            .addPacket(Settings.class)
            .addPacket(DataBundle.class)
            .addPacket(Subscription.class)
            .addPacket(Profile.class);

    public static final UUID STATIC_DATA = new UUID(0, 0);
    public static final UUID GLOBAL_DATA = new UUID(0, 1);
    public static final String PLAYER_TOPIC = SINGULARITY_ID + ":player";

    public static final String PLAYER_ADVANCEMENTS = SINGULARITY_ID + ":adv";
    public static final String PLAYER_STATS = SINGULARITY_ID + ":stats";
    public static final String WHITELIST = SINGULARITY_ID + ":white";
    public static final String OPERATOR = SINGULARITY_ID + ":op";
    public static final String BANNED_PLAYERS = SINGULARITY_ID + ":bans";
    public static final String BANNED_IPS = SINGULARITY_ID + ":ibans";
}