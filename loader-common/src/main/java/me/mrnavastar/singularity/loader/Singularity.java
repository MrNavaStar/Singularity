package me.mrnavastar.singularity.loader;

import me.mrnavastar.protoweaver.api.ProtoConnectionHandler;
import me.mrnavastar.protoweaver.api.netty.ProtoConnection;
import me.mrnavastar.protoweaver.api.protocol.Protocol;
import me.mrnavastar.singularity.common.Constants;
import me.mrnavastar.singularity.common.networking.Settings;
import me.mrnavastar.singularity.common.networking.SyncData;
import me.mrnavastar.singularity.loader.api.SyncEvents;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;

import java.util.Date;

public abstract class Singularity implements ProtoConnectionHandler {

    private ProtoConnection proxyConnection;
    protected static final Protocol protocol = Constants.PROTOCOL.modify()
            .setServerHandler(Singularity.class)
            .build();

    private Settings settings;

    protected abstract MinecraftServer getServer();

    public void onReady(ProtoConnection protoConnection) {
        this.proxyConnection = protoConnection;

        // Player Data
        SyncEvents.SEND_DATA.register(((player, data) -> {
            if (!settings.syncPlayerData()) return;
            NbtCompound nbt = new NbtCompound();
            player.writeNbt(nbt);
            data.put(Constants.PLAYER_DATA, nbt);
        }));
        SyncEvents.RECEIVE_DATA.register(((player, data) -> {
            if (!settings.syncPlayerData()) return;
            NbtCompound playerData = data.get(Constants.PLAYER_DATA, NbtCompound.class);
            if (playerData == null) return;
            player.readNbt(playerData);
        }));

        // Player Advancements
        /*SyncEvents.SEND_DATA.register((player, data) -> {

        });
        SyncEvents.RECEIVE_DATA.register(((player, data) -> {

        }));*/

        // Player Stats
        SyncEvents.SEND_DATA.register((player, data) -> {
            if (!settings.syncPlayerStats()) return;
            data.put(Constants.PLAYER_STATS, player.getStatHandler().asString());
        });
        SyncEvents.RECEIVE_DATA.register((player, data) -> {
            if (!settings.syncPlayerStats()) return;
            player.getStatHandler().parse(getServer().getDataFixer(), data.get(Constants.PLAYER_STATS, String.class));
        });
    }

    @Override
    public void handlePacket(ProtoConnection protoConnection, Object packet) {
        if (packet instanceof SyncData syncData) {
            ServerPlayerEntity player = getServer().getPlayerManager().getPlayer(syncData.getId());
            SyncEvents.RECEIVE_DATA.getInvoker().trigger(player, syncData);
            return;
        }
        if (packet instanceof Settings settings) this.settings = settings;
    }

    protected void onPlayerLeave(ServerPlayerEntity player) {
        SyncData data = new SyncData(player.getUuid(), player.getName().getString(), new Date());
        SyncEvents.SEND_DATA.getInvoker().trigger(player, data);
        proxyConnection.send(data);
    }

    protected static void log(Level level, String message) {
        LogManager.getLogger().log(level, "[" + Constants.MOD_NAME + "] " + message);
    }
}