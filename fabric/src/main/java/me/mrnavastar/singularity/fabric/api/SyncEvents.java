package me.mrnavastar.singularity.fabric.api;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;

public class SyncEvents {

    public static final Event<PlayerData> SEND_PLAYER_DATA = new Event<>(callbacks -> (player, nbt) -> {
        callbacks.forEach(callback -> callback.trigger(player, nbt));
    });

    public static final Event<PlayerData> RECEIVE_PLAYER_DATA = new Event<>(callbacks -> (player, nbt) -> {
        callbacks.forEach(callback -> callback.trigger(player, nbt));
    });

    @FunctionalInterface
    public interface PlayerData {
        void trigger(ServerPlayerEntity player, NbtCompound nbt);
    }
}