package me.mrnavastar.singularity.loader;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import me.mrnavastar.protoweaver.api.ProtoConnectionHandler;
import me.mrnavastar.protoweaver.api.netty.ProtoConnection;
import me.mrnavastar.singularity.common.Constants;
import me.mrnavastar.singularity.common.networking.Settings;
import me.mrnavastar.singularity.common.networking.SyncData;
import me.mrnavastar.singularity.loader.api.SyncEvents;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;

import java.util.Date;

public abstract class Singularity implements ProtoConnectionHandler {

    private static ProtoConnection proxy;
    private static Settings settings = new Settings();

    static {
        Constants.PROTOCOL.modify().setServerHandler(Singularity.class).load();
    }

    protected abstract MinecraftServer getServer();

    public static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal(Constants.MOD_ID).requires(source -> source.hasPermissionLevel(4))
                .then(CommandManager.literal("config")
                        .then(CommandManager.literal("syncPlayerData")
                                .then(CommandManager.argument("enabled", BoolArgumentType.bool())
                                        .executes(ctx -> {
                                            settings.syncPlayerData = BoolArgumentType.getBool(ctx, "enabled");
                                            proxy.send(settings);
                                            ctx.getSource().sendMessage(Text.of("SyncPlayerData is now set to: " + settings.syncPlayerData));
                                            return 0;
                                        })
                                )
                        )

                        .then(CommandManager.literal("syncPlayerStatistics")
                                .then(CommandManager.argument("enabled", BoolArgumentType.bool())
                                        .executes(ctx -> {
                                            settings.syncPlayerStats = BoolArgumentType.getBool(ctx, "enabled");
                                            proxy.send(settings);
                                            ctx.getSource().sendMessage(Text.of("SyncPlayerStatistics is now set to: " + settings.syncPlayerStats));
                                            return 0;
                                        })
                                )
                        )
                )
        );
    }

    public void onReady(ProtoConnection protoConnection) {
        proxy = protoConnection;

        // Player Data
        SyncEvents.SEND_DATA.register(((player, data) -> {
            if (!settings.syncPlayerData) return;
            NbtCompound nbt = new NbtCompound();
            player.writeNbt(nbt);
            data.put(Constants.PLAYER_DATA, nbt);
        }));
        SyncEvents.RECEIVE_DATA.register(((player, data) -> {
            if (!settings.syncPlayerData) return;
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
            if (!settings.syncPlayerStats) return;
            data.put(Constants.PLAYER_STATS, player.getStatHandler().asString());
        });
        SyncEvents.RECEIVE_DATA.register((player, data) -> {
            if (!settings.syncPlayerStats) return;
            player.getStatHandler().parse(getServer().getDataFixer(), data.get(Constants.PLAYER_STATS, String.class));
        });
    }

    @Override
    public void handlePacket(ProtoConnection protoConnection, Object packet) {
        switch (packet) {
            case Settings s -> settings = s;
            case SyncData syncData -> {
                ServerPlayerEntity player = getServer().getPlayerManager().getPlayer(syncData.getId());
                SyncEvents.RECEIVE_DATA.getInvoker().trigger(player, syncData);
            }
            default -> log(Level.WARN, "Ignoring unknown packet: " + packet);
        };
    }

    protected void syncData(ServerPlayerEntity player) {
        SyncData data = new SyncData(player.getUuid(), player.getName().getString(), new Date());
        SyncEvents.SEND_DATA.getInvoker().trigger(player, data);
        proxy.send(data);
    }

    protected static void log(Level level, String message) {
        LogManager.getLogger().log(level, "[" + Constants.MOD_NAME + "] " + message);
    }
}