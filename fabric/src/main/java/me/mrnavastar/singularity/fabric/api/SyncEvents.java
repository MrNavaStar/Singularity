package me.mrnavastar.singularity.fabric.api;

import me.mrnavastar.singularity.common.networking.SyncData;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.IOException;

public class SyncEvents {

    public static final Event<PlayerData> SEND_DATA = new Event<>(callbacks -> (player, data) -> {
        callbacks.forEach(callback -> callback.trigger(player, data));
    });

    public static final Event<PlayerData> RECEIVE_DATA = new Event<>(callbacks -> (player, data) -> {
        callbacks.forEach(callback -> callback.trigger(player, data));
    });

    @FunctionalInterface
    public interface PlayerData {
        void trigger(ServerPlayerEntity player, SyncData data) throws Exception;
    }
}