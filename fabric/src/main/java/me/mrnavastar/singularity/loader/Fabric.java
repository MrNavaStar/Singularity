package me.mrnavastar.singularity.loader;

import me.mrnavastar.singularity.common.Constants;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.apache.logging.log4j.Level;

public class Fabric extends Singularity implements DedicatedServerModInitializer {

    @Override
    public void onInitializeServer() {
        log(Level.INFO, Constants.SINGULARITY_BOOT_MESSAGE);
        Constants.PROTOCOL.setServerHandler(Fabric.class).load();

        ServerLifecycleEvents.SERVER_STARTED.register(this::setServer);
        ServerLifecycleEvents.BEFORE_SAVE.register((server, flush, force) -> server.getPlayerList().getPlayers().forEach(this::syncData));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> syncData(handler.getPlayer()));

        CommandRegistrationCallback.EVENT.register(Singularity::registerCommands);
    }
}