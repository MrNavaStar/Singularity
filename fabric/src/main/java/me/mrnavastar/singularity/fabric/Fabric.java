package me.mrnavastar.singularity.fabric;

import me.mrnavastar.protoweaver.api.ProtoConnectionHandler;
import me.mrnavastar.protoweaver.api.ProtoWeaver;
import me.mrnavastar.protoweaver.api.netty.ProtoConnection;
import me.mrnavastar.protoweaver.api.protocol.Protocol;
import me.mrnavastar.singularity.common.Constants;
import me.mrnavastar.singularity.common.networking.Networking;
import me.mrnavastar.singularity.common.networking.Settings;
import me.mrnavastar.singularity.common.networking.SyncData;
import me.mrnavastar.singularity.fabric.api.SyncEvents;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

public class Fabric implements DedicatedServerModInitializer, ProtoConnectionHandler {

    // This array should only ever have one connection in it, mostly just a list encase weird stuff happens
    private static final ArrayList<ProtoConnection> proxyConnections = new ArrayList<>();
    private static final Protocol fabricSyncProtocol = Networking.getSyncProtocol().modify()
            .setServerHandler(Fabric.class)
            .build();

    public static PlayerManager playerManager;
    private MinecraftServer server;

    @Override
    public void onInitializeServer() {
        ProtoWeaver.load(fabricSyncProtocol);

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            this.server = server;
            playerManager = server.getPlayerManager();
        });
    }

    public void onReady(ProtoConnection protoConnection) {
        proxyConnections.add(protoConnection);

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            try {
                ServerPlayerEntity player = handler.getPlayer();
                SyncData data = new SyncData(player.getUuid(), player.getName().getString());
                SyncEvents.SEND_DATA.getInvoker().trigger(player, data);
                proxyConnections.forEach(connection -> connection.send(data));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public void onDisconnect(ProtoConnection connection) {
        proxyConnections.remove(connection);
    }

    @Override
    public void handlePacket(ProtoConnection protoConnection, Object packet) {
        if (packet instanceof SyncData syncData) {
            try {
                ServerPlayerEntity player = Fabric.playerManager.getPlayer(syncData.getId());
                SyncEvents.RECEIVE_DATA.getInvoker().trigger(player, syncData);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        else if (packet instanceof Settings settings) {
            if (settings.syncPlayerData()) syncPlayerData();
        }
    }

    private void syncPlayerData() {
        SyncEvents.SEND_DATA.register(((player, data) -> {
            NbtCompound nbt = new NbtCompound();
            player.writeNbt(nbt);

            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                NbtIo.writeCompressed(nbt, out);
                data.put(Constants.PLAYER_DATA, out.toByteArray());
            }
        }));

        SyncEvents.RECEIVE_DATA.register(((player, data) -> {
            byte[] playerData = data.get(Constants.PLAYER_DATA);
            if (playerData == null) return;

            try (ByteArrayInputStream in = new ByteArrayInputStream(playerData)) {
                player.readNbt(NbtIo.readCompressed(in));
            }
        }));
    }

    private void syncPlayerStats() {
        SyncEvents.SEND_DATA.register((player, data) -> {

        });
    }
}