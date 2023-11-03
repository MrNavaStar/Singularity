package me.mrnavastar.singularity.fabric;

import me.mrnavastar.protoweaver.api.ProtoBuilder;
import me.mrnavastar.protoweaver.api.ProtoWeaver;
import me.mrnavastar.protoweaver.api.protocol.Protocol;
import me.mrnavastar.singularity.common.networking.Networking;
import me.mrnavastar.singularity.fabric.api.SyncEvents;
import me.mrnavastar.singularity.fabric.networking.PacketHandler;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.entity.Entity;
import net.minecraft.server.PlayerManager;

public class Fabric implements DedicatedServerModInitializer {

    private static final Protocol fabricSyncProtocol = ProtoBuilder.protocol(Networking.getSyncProtocol())
            .setServerHandler(PacketHandler.class)
            .build();

    public static PlayerManager playerManager;

    @Override
    public void onInitializeServer() {
        ProtoWeaver.load(fabricSyncProtocol);
        PacketHandler.registerEvents();

        ServerLifecycleEvents.SERVER_STARTED.register(server -> playerManager = server.getPlayerManager());
        registerEvents();
    }

    private static void registerEvents() {
        // Read & write player data
        SyncEvents.RECEIVE_PLAYER_DATA.register(Entity::readNbt);   // Set data
        SyncEvents.SEND_PLAYER_DATA.register(Entity::writeNbt);     // Get data
    }
}