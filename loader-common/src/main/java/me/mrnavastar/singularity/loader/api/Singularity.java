package me.mrnavastar.singularity.loader.api;

import me.mrnavastar.protoweaver.api.util.Event;
import me.mrnavastar.singularity.common.networking.DataBundle;
import net.minecraft.server.level.ServerPlayer;

public class Singularity {

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
}