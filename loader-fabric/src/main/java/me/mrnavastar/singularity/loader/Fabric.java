package me.mrnavastar.singularity.loader;

import me.mrnavastar.protoweaver.api.ProtoWeaver;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;

public class Fabric extends Singularity implements DedicatedServerModInitializer {

    public static MinecraftServer server;

    @Override
    public void onInitializeServer() {
        ProtoWeaver.load(protocol);

        ServerLifecycleEvents.SERVER_STARTED.register(s -> server = s);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> onPlayerLeave(handler.getPlayer()));
    }

    @Override
    protected MinecraftServer getServer() {
        return server;
    }
}