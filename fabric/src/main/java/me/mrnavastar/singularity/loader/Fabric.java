package me.mrnavastar.singularity.loader;

import me.mrnavastar.singularity.common.Constants;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.Level;

public class Fabric extends Singularity implements DedicatedServerModInitializer {

    public static MinecraftServer server;

    @Override
    public void onInitializeServer() {
        log(Level.INFO, Constants.BOOT_MESSAGE);

        ServerLifecycleEvents.SERVER_STARTED.register(s -> server = s);
        ServerLifecycleEvents.BEFORE_SAVE.register((server, flush, force) -> server.getPlayerManager().getPlayerList().forEach(this::syncData));
        //ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> syncData(handler.getPlayer()));

        //CommandRegistrationCallback.EVENT.register(Singularity::registerCommands);
    }

    @Override
    protected MinecraftServer getServer() {
        return server;
    }
}