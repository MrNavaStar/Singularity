package me.mrnavastar.singularity.common;

import me.mrnavastar.protoweaver.api.protocol.CompressionType;
import me.mrnavastar.protoweaver.api.protocol.Protocol;
import me.mrnavastar.protoweaver.api.protocol.velocity.VelocityAuth;
import me.mrnavastar.singularity.common.networking.PlayerData;
import me.mrnavastar.singularity.common.networking.Settings;
import me.mrnavastar.singularity.common.networking.ServerData;
import me.mrnavastar.singularity.common.networking.UserCache;

import java.util.UUID;

public class Constants {

    public static final String SINGULARITY_NAME = "Singularity";
    public static final String SINGULARITY_ID = "singularity";
    public static final String SINGULARITY_VERSION = "debug-build";
    public static final String SINGULARITY_AUTHOR = "MrNavaStar";
    public static final String SINGULARITY_BOOT_MESSAGE = "Warping space and time ...";
    public static final String SINGULARITY_CONNECT_MESSAGE = "Wormhole opened by: %s";

    public static final Protocol.Builder PROTOCOL = Protocol.create(SINGULARITY_ID, "wormhole")
            .setServerAuthHandler(VelocityAuth.class)
            .setClientAuthHandler(VelocityAuth.class)
            .setCompression(CompressionType.GZIP)
            .setMaxPacketSize(67108864) // 64mb
            .setMaxConnections(1)
            .addPacket(Settings.class)
            .addPacket(PlayerData.class)
            .addPacket(ServerData.class)
            .addPacket(UserCache.class);

    public static final int MAX_DATA_SIZE = 16777216; // 16mb
    public static final UUID SERVER_DATA = new UUID(0, 0);
    public static final String PLAYER_DATA = SINGULARITY_ID + ":player";
    public static final String PLAYER_ADVANCEMENTS = SINGULARITY_ID + ":adv";
    public static final String PLAYER_STATS = SINGULARITY_ID + ":stats";
    public static final String WHITELIST_ENABLED = SINGULARITY_ID + ":elist";
    public static final String WHITELIST = SINGULARITY_ID + ":list";
    public static final String OPERATORS = SINGULARITY_ID + ":ops";
    public static final String BANNED_PLAYERS = SINGULARITY_ID + ":bans";
    public static final String BANNED_IPS = SINGULARITY_ID + ":ibans";
}
