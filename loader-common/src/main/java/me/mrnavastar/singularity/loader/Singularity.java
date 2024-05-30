package me.mrnavastar.singularity.loader;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import lombok.Cleanup;
import lombok.SneakyThrows;
import me.mrnavastar.protoweaver.api.ProtoConnectionHandler;
import me.mrnavastar.protoweaver.api.netty.ProtoConnection;
import me.mrnavastar.singularity.common.Constants;
import me.mrnavastar.singularity.common.networking.Settings;
import me.mrnavastar.singularity.common.networking.SyncData;
import me.mrnavastar.singularity.loader.api.SyncEvents;
import me.mrnavastar.singularity.loader.util.Serializers;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.HashSet;

public class Singularity implements ProtoConnectionHandler {

    protected static final HashSet<String> nbtBlacklist = new HashSet<>();
    protected static Settings settings = new Settings();
    protected static ProtoConnection proxy;
    protected static MinecraftServer server;

    @SneakyThrows
    public Singularity() {
        @Cleanup InputStream stream = Singularity.class.getClassLoader().getResourceAsStream("singularity.blacklist.default");
        assert stream != null;
        @Cleanup BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        nbtBlacklist.addAll(reader.lines().filter(l -> !l.isBlank()).toList());

        SyncData.registerSerializer(CompoundTag.class, new Serializers.Nbt());
    }

    protected void setServer(MinecraftServer server) {
        Singularity.server = server;
    }

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {
        dispatcher.register(Commands.literal(Constants.SINGULARITY_ID).requires(source -> source.hasPermission(4))
                .then(Commands.literal("config")
                        .then(Commands.literal("syncPlayerData")
                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                        .executes(ctx -> {
                                            settings.syncPlayerData = BoolArgumentType.getBool(ctx, "enabled");
                                            proxy.send(settings);
                                            ctx.getSource().sendSystemMessage(Component.literal("SyncPlayerData is now set to: " + settings.syncPlayerData));
                                            return 0;
                                        })
                                )
                        )

                        .then(Commands.literal("syncPlayerStatistics")
                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                        .executes(ctx -> {
                                            settings.syncPlayerStats = BoolArgumentType.getBool(ctx, "enabled");
                                            proxy.send(settings);
                                            ctx.getSource().sendSystemMessage(Component.literal("SyncPlayerStatistics is now set to: " + settings.syncPlayerStats));
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
            CompoundTag nbt = new CompoundTag();
            player.saveWithoutId(nbt);
            data.put(Constants.PLAYER_DATA, nbt);
        }));

        SyncEvents.RECEIVE_DATA.register(((player, data) -> {
            if (!settings.syncPlayerData) return;
            CompoundTag playerData = data.get(Constants.PLAYER_DATA, CompoundTag.class);
            if (playerData == null) return;

            CompoundTag current = new CompoundTag();
            player.saveWithoutId(current);
            nbtBlacklist.forEach(key -> {
                if (!current.contains(key)) return;
                playerData.put(key, current.get(key));
            });
            player.load(playerData);
        }));

        // Player Advancements
        /*SyncEvents.SEND_DATA.register((player, data) -> {

        });
        SyncEvents.RECEIVE_DATA.register(((player, data) -> {

        }));*/

        // Player Stats
        /*SyncEvents.SEND_DATA.register((player, data) -> {
            if (!settings.syncPlayerStats) return;
            data.put(Constants.PLAYER_STATS, ReflectionUtil.invokePrivateMethod(player.getStats(), "toJson", String.class));
        });
        SyncEvents.RECEIVE_DATA.register((player, data) -> {
            if (!settings.syncPlayerStats) return;
            player.getStats().parseLocal(getServer().getFixerUpper(), data.get(Constants.PLAYER_STATS, String.class));
        });*/
    }

    // Used by paper
    protected void preProcessData(ServerPlayer player, SyncData data) {}
    protected void postProcessData(ServerPlayer player, SyncData data) {}

    @Override
    public void handlePacket(ProtoConnection protoConnection, Object packet) {
        switch (packet) {
            case Settings s -> settings = s;
            case SyncData data -> {
                ServerPlayer player = server.getPlayerList().getPlayer(data.getId());
                preProcessData(player, data);
                SyncEvents.RECEIVE_DATA.getInvoker().trigger(player, data);
                postProcessData(player, data);
            }
            default -> log(Level.WARN, "Ignoring unknown packet: " + packet);
        };
    }

    protected void syncData(ServerPlayer player) {
        SyncData data = new SyncData(player.getUUID(), player.getName().getString(), new Date());
        SyncEvents.SEND_DATA.getInvoker().trigger(player, data);
        proxy.send(data);
    }

    protected static void log(Level level, String message) {
        LogManager.getLogger().log(level, "[" + Constants.SINGULARITY_NAME + "] " + message);
    }
}