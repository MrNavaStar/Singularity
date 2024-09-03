package me.mrnavastar.singularity.loader;

import me.mrnavastar.singularity.common.Constants;
import me.mrnavastar.singularity.loader.util.Mappings;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.Level;

public class Fabric extends Singularity implements DedicatedServerModInitializer {

    static {
        Mappings.setDev(FabricLoader.getInstance().isDevelopmentEnvironment());
        Constants.WORMHOLE.setServerHandler(Fabric.class).load();
    }

    @Override
    public void onInitializeServer() {
        log(Level.INFO, Constants.SINGULARITY_BOOT_MESSAGE);

        ServerLifecycleEvents.SERVER_STARTING.register(s -> server = s);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server1) -> onJoin(handler.getPlayer()));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server1) -> onLeave(handler.getPlayer()));
    }
}