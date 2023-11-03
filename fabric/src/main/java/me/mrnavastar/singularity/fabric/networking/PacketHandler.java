package me.mrnavastar.singularity.fabric.networking;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.mrnavastar.protoweaver.api.ProtoPacket;
import me.mrnavastar.protoweaver.api.ProtoPacketHandler;
import me.mrnavastar.protoweaver.api.netty.ProtoConnection;
import me.mrnavastar.singularity.common.networking.PlayerDataPacket;
import me.mrnavastar.singularity.fabric.Fabric;
import me.mrnavastar.singularity.fabric.api.SyncEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;

public class PacketHandler implements ProtoPacketHandler {

    // This array should only every have one connection in it, mostly just a list encase weird stuff happens
    private static final ArrayList<ProtoConnection> proxyConnections = new ArrayList<>();

    @Override
    public void onReady(ProtoConnection protoConnection) {
        proxyConnections.add(protoConnection);
    }

    @Override
    public void onDisconnect(ProtoConnection connection) {
        proxyConnections.remove(connection);
    }

    @Override
    public void handlePacket(ProtoConnection protoConnection, ProtoPacket protoPacket) {
        // Handle player data
        if (protoPacket instanceof PlayerDataPacket playerDataPacket) {
            ServerPlayerEntity player = Fabric.playerManager.getPlayer(playerDataPacket.getId());

            try {
                NbtCompound nbt = StringNbtReader.parse(playerDataPacket.getData());
                SyncEvents.RECEIVE_PLAYER_DATA.getInvoker().trigger(player, nbt);
            } catch (CommandSyntaxException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void registerEvents() {
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayerEntity player = handler.getPlayer();

            NbtCompound nbt = new NbtCompound();
            SyncEvents.SEND_PLAYER_DATA.getInvoker().trigger(player, nbt);

            proxyConnections.forEach(connection -> {
                connection.send(new PlayerDataPacket(player.getUuid(), player.getName().getString(), nbt.toString()));
            });
        });
    }
}
