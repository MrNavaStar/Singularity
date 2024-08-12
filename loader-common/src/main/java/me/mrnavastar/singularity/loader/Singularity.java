package me.mrnavastar.singularity.loader;

import com.google.gson.JsonElement;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import lombok.Getter;
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

import java.io.*;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class Singularity implements ProtoConnectionHandler {

    protected static final HashSet<String> nbtBlacklist = new HashSet<>();
    @Getter
    protected static Settings settings = new Settings();
    @Getter
    protected static ProtoConnection proxy;
    protected static MinecraftServer server;

    protected static final ConcurrentHashMap<UUID, SyncData> incoming = new ConcurrentHashMap<>();
    protected static final ConcurrentHashMap<UUID, CompletableFuture<SyncData>> outgoing = new ConcurrentHashMap<>();

    @SneakyThrows
    public Singularity() {
        reloadBlacklists();
        SyncData.registerSerializer(CompoundTag.class, new Serializers.Nbt());
        SyncData.registerSerializer(JsonElement.class, new Serializers.Json());
    }

    private void reloadBlacklists() {
        nbtBlacklist.clear();
        settings.nbtBlacklists.add("singularity.never");
        settings.nbtBlacklists.forEach(name -> {
            try (InputStream stream = Singularity.class.getClassLoader().getResourceAsStream(name)) {
                if (stream == null) {
                    log(Level.WARN, "Failed to load blacklist: " + name);
                    return;
                }

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
                    nbtBlacklist.addAll(reader.lines().filter(l -> !l.isBlank() && !l.startsWith("#")).map(String::strip).toList());
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    public void onReady(ProtoConnection protoConnection) {
        proxy = protoConnection;
        log(Level.INFO, Constants.SINGULARITY_CONNECT_MESSAGE.formatted(protoConnection.getRemoteAddress()));

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
            if (!settings.syncPlayerAdvancements) return;
            data.put(Constants.PLAYER_ADVANCEMENTS, AdvancementHandler.save(player));
        });
        SyncEvents.RECEIVE_DATA.register((player, data) -> {
            if (!settings.syncPlayerAdvancements) return;

            System.out.println(data.get(Constants.PLAYER_ADVANCEMENTS, String.class));

            AdvancementHandler.load(player, data.get(Constants.PLAYER_ADVANCEMENTS, String.class));
        });*/

        // Player Stats
        /*SyncEvents.SEND_DATA.register((player, data) -> {
            if (!settings.syncPlayerStats) return;
            data.put(Constants.PLAYER_STATS, ReflectionUtil.invokeMethod(player.getStats(), "toJson", String.class));
        });
        SyncEvents.RECEIVE_DATA.register((player, data) -> {
            if (!settings.syncPlayerStats) return;
            player.getStats().parseLocal(server.getFixerUpper(), data.get(Constants.PLAYER_STATS, String.class));
        });*/
    }

    // Used by paper
    protected void processData(ServerPlayer player, SyncData data) {
        SyncEvents.RECEIVE_DATA.getInvoker().trigger(player, data);
    }

    // Used by paper
    protected void processSettings(Settings settings) {}

    protected SyncData createSyncPacket(ServerPlayer player) {
        SyncData data = new SyncData(player.getUUID());
        SyncEvents.SEND_DATA.getInvoker().trigger(player, data);
        return data;
    }

    @Override
    public void handlePacket(ProtoConnection protoConnection, Object packet) {
        switch (packet) {
            case Settings s -> {
                processSettings(s);
                settings = s;
                reloadBlacklists();
            }
            case SyncData data -> {
                ServerPlayer player = server.getPlayerList().getPlayer(data.getPlayer());
                if (player != null) processData(player, data);
                else incoming.put(data.getPlayer(), data);
            }
            default -> log(Level.WARN, "Ignoring unknown packet: " + packet);
        };
    }

    protected void onJoin(ServerPlayer player) {
        SyncData data = incoming.remove(player.getUUID());
        if (data != null) processData(player, data);
    }

    protected void onLeave(ServerPlayer player) {
        proxy.send(createSyncPacket(player));
    }

    protected static void log(Level level, String message) {
        LogManager.getLogger().log(level, "[" + Constants.SINGULARITY_NAME + "] " + message);
    }
}