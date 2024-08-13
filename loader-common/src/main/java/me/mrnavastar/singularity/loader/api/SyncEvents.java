package me.mrnavastar.singularity.loader.api;

import me.mrnavastar.protoweaver.api.util.Event;
import me.mrnavastar.singularity.common.networking.ServerData;
import net.minecraft.server.level.ServerPlayer;

public class SyncEvents {

    public static final Event<PlayerData> SEND_DATA = new Event<>(callbacks -> (player, data) -> {
        callbacks.forEach(callback -> callback.trigger(player, data));
    });

    public static final Event<PlayerData> RECEIVE_DATA = new Event<>(callbacks -> (player, data) -> {
        callbacks.forEach(callback -> callback.trigger(player, data));
    });

    @FunctionalInterface
    public interface PlayerData {
        void trigger(ServerPlayer player, ServerData data);
    }
}