package me.mrnavastar.singularity.loader;

import me.mrnavastar.singularity.common.Constants;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.Level;

public class Fabric extends Singularity implements DedicatedServerModInitializer {

    @Override
    public void onInitializeServer() {
        log(Level.INFO, Constants.SINGULARITY_BOOT_MESSAGE);
        Constants.PROTOCOL.setServerHandler(Fabric.class).load();

        ServerLifecycleEvents.SERVER_STARTING.register(s -> server = s);
        //ServerLifecycleEvents.BEFORE_SAVE.register((server, flush, force) -> server.getPlayerList().getPlayers().forEach(this::syncData));
        //CommandRegistrationCallback.EVENT.register(Singularity::registerCommands);

        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof ServerPlayer player) proxy.send(player.getUUID());
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            syncData(handler.getPlayer());
        });
    }
}