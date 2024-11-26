package me.mrnavastar.singularity.loader;

import me.mrnavastar.protoweaver.api.ProtoWeaver;
import me.mrnavastar.singularity.common.Constants;
import me.mrnavastar.singularity.loader.impl.Broker;
import me.mrnavastar.singularity.loader.impl.sync.SynchronizedMinecraft;
import me.mrnavastar.singularity.loader.util.Mappings;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.Level;

import java.nio.file.Path;

public class Fabric extends SynchronizedMinecraft implements DedicatedServerModInitializer {

    static {
        Mappings.setDev(FabricLoader.getInstance().isDevelopmentEnvironment());
        ProtoWeaver.load(Broker.PROTOCOL);
    }

    @Override
    public void onInitializeServer() {
        log(Level.INFO, Constants.SINGULARITY_BOOT_MESSAGE);

        ServerLifecycleEvents.SERVER_STARTED.register(SynchronizedMinecraft::init);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server1) -> onJoin(handler.getPlayer()));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server1) -> onLeave(handler.getPlayer()));

        SynchronizedMinecraft.ImportPlayerData(Path.of(FabricLoader.getInstance().getGameDir() + "/import_playerdata"));
    }
}