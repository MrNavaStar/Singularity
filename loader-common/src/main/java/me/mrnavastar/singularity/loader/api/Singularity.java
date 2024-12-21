package me.mrnavastar.singularity.loader.api;

import me.mrnavastar.protoweaver.api.util.Event;
import me.mrnavastar.singularity.common.Constants;
import me.mrnavastar.singularity.common.networking.DataBundle;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.BiConsumer;

public class Singularity {

    public static ResourceLocation PLAYER_SYNC = ResourceLocation.tryParse(Constants.PLAYER_TOPIC);

    protected static MinecraftServer server;

    public static final Event<PlayerData> PRE_PUSH_PLAYER_DATA = new Event<>(callbacks -> (player, data) -> {
        callbacks.forEach(callback -> callback.trigger(player, data));
    });

    public static final Event<PlayerData> POST_RECEIVE_PLAYER_DATA = new Event<>(callbacks -> (player, data) -> {
        callbacks.forEach(callback -> callback.trigger(player, data));
    });

    @FunctionalInterface
    public interface PlayerData {
        void trigger(ServerPlayer player, DataBundle data);
    }

    public static void subPlayerTopic(ResourceLocation topic, BiConsumer<ServerPlayer, DataBundle> consumer) {

    }
}