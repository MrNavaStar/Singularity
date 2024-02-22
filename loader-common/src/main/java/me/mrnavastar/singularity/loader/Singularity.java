package me.mrnavastar.singularity.loader;

import me.mrnavastar.protoweaver.api.ProtoConnectionHandler;
import me.mrnavastar.protoweaver.api.netty.ProtoConnection;
import me.mrnavastar.protoweaver.api.protocol.Protocol;
import me.mrnavastar.singularity.common.Constants;
import me.mrnavastar.singularity.common.networking.Networking;
import me.mrnavastar.singularity.common.networking.Settings;
import me.mrnavastar.singularity.common.networking.SyncData;
import me.mrnavastar.singularity.loader.api.SyncEvents;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public abstract class Singularity implements ProtoConnectionHandler {

    // This array should only ever have one connection in it, mostly just a list encase weird stuff happens
    private static final ArrayList<ProtoConnection> proxyConnections = new ArrayList<>();
    protected static final Protocol protocol = Networking.getSyncProtocol().modify()
            .setServerHandler(Singularity.class)
            .build();

    protected abstract MinecraftServer getServer();

    public void onReady(ProtoConnection protoConnection) {
        proxyConnections.add(protoConnection);
    }

    @Override
    public void onDisconnect(ProtoConnection connection) {
        proxyConnections.remove(connection);
    }

    @Override
    public void handlePacket(ProtoConnection protoConnection, Object packet) {
        if (packet instanceof SyncData syncData) {
            try {
                ServerPlayerEntity player = getServer().getPlayerManager().getPlayer(syncData.getId());
                SyncEvents.RECEIVE_DATA.getInvoker().trigger(player, syncData);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        else if (packet instanceof Settings settings) {
            if (settings.syncPlayerData()) syncPlayerData();
        }
    }

    protected void onPlayerLeave(ServerPlayerEntity player) {
        try {
            SyncData data = new SyncData(player.getUuid(), player.getName().getString());
            SyncEvents.SEND_DATA.getInvoker().trigger(player, data);
            proxyConnections.forEach(connection -> connection.send(data));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void syncPlayerData() {
        SyncEvents.SEND_DATA.register(((player, data) -> {
            NbtCompound nbt = new NbtCompound();
            player.writeNbt(nbt);

            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                NbtIo.writeCompressed(nbt, out);
                data.put(Constants.PLAYER_DATA, out.toByteArray());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }));

        SyncEvents.RECEIVE_DATA.register(((player, data) -> {
            byte[] playerData = data.get(Constants.PLAYER_DATA);
            if (playerData == null) return;

            try (ByteArrayInputStream in = new ByteArrayInputStream(playerData)) {

                player.readNbt(NbtIo.readCompressed(in, new NbtSizeTracker(in.available(), 100)));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }));
    }

    private void syncPlayerStats() {
        SyncEvents.SEND_DATA.register((player, data) -> {

        });
    }
}
