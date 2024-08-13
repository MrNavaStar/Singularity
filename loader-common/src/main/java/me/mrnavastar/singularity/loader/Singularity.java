package me.mrnavastar.singularity.loader;

import lombok.Getter;
import lombok.SneakyThrows;
import me.mrnavastar.protoweaver.api.ProtoConnectionHandler;
import me.mrnavastar.protoweaver.api.netty.ProtoConnection;
import me.mrnavastar.singularity.common.Constants;
import me.mrnavastar.singularity.common.networking.PlayerData;
import me.mrnavastar.singularity.common.networking.Settings;
import me.mrnavastar.singularity.common.networking.ServerData;
import me.mrnavastar.singularity.loader.api.SyncEvents;
import me.mrnavastar.singularity.loader.impl.IpBanListHack;
import me.mrnavastar.singularity.loader.impl.ServerOpListHack;
import me.mrnavastar.singularity.loader.impl.UserBanListHack;
import me.mrnavastar.singularity.loader.impl.UserWhiteListHack;
import me.mrnavastar.singularity.loader.util.R;
import me.mrnavastar.singularity.loader.util.Serializers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.*;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;

import java.io.*;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Singularity implements ProtoConnectionHandler {

    protected static final HashSet<String> nbtBlacklist = new HashSet<>();
    @Getter
    protected static Settings settings = new Settings();
    protected static ProtoConnection proxy;
    protected static MinecraftServer server;

    protected static final ConcurrentHashMap<UUID, PlayerData> incoming = new ConcurrentHashMap<>();

    @SneakyThrows
    public Singularity() {
        reloadBlacklists();
        ServerData.registerSerializer(CompoundTag.class, new Serializers.Nbt());
        ServerData.registerSerializer(StoredUserList.class, new Serializers.SUL());
    }

    protected void onJoin(ServerPlayer player) {
        PlayerData data = incoming.remove(player.getUUID());
        if (data != null) processData(player, data);
    }

    protected void onLeave(ServerPlayer player) {
        proxy.send(createPlayerDataPacket(player));
    }

    // Used by paper
    protected void processData(ServerPlayer player, ServerData data) {
        SyncEvents.RECEIVE_DATA.getInvoker().trigger(player, data);
    }

    // Used by paper
    protected void processSettings(Settings settings) {}

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

    protected static PlayerData createPlayerDataPacket(ServerPlayer player) {
        PlayerData data = new PlayerData(player.getUUID());
        SyncEvents.SEND_DATA.getInvoker().trigger(player, data);
        return data;
    }

    public static void syncServerData() {
        if (proxy != null) proxy.send(new ServerData()
            .put(Constants.OPERATORS, server.getPlayerList().getOps())
            .put(Constants.WHITELIST_ENABLED, server.getPlayerList().isUsingWhitelist())
            .put(Constants.WHITELIST, server.getPlayerList().getWhiteList())
            .put(Constants.BANNED_PLAYERS, server.getPlayerList().getBans())
            .put(Constants.BANNED_IPS, server.getPlayerList().getIpBans()));
    }

    public static void syncPlayerData() {
        if (proxy != null) server.getPlayerList().getPlayers().forEach(player -> proxy.send(createPlayerDataPacket(player)));
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
        SyncEvents.SEND_DATA.register((player, data) -> {
            if (!settings.syncPlayerStats) return;
            data.put(Constants.PLAYER_STATS, R.of(player.getStats()).call("toJson", String.class));
        });
        SyncEvents.RECEIVE_DATA.register((player, data) -> {
            if (!settings.syncPlayerStats) return;
            player.getStats().parseLocal(server.getFixerUpper(), data.get(Constants.PLAYER_STATS, String.class));
        });
    }

    @Override
    public void handlePacket(ProtoConnection protoConnection, Object packet) {
        switch (packet) {
            case Settings s -> {
                processSettings(s);
                settings = s;
                reloadBlacklists();
            }
            case PlayerData data -> {
                ServerPlayer player = server.getPlayerList().getPlayer(data.getPlayer());
                if (player != null) processData(player, data);
                else incoming.put(data.getPlayer(), data);
            }
            case ServerData data -> {
                server.getPlayerList().setUsingWhiteList(data.get(Constants.WHITELIST_ENABLED, boolean.class));
                UserWhiteListHack.install(server, data.get(Constants.WHITELIST, UserWhiteListHack.class));

                ServerOpListHack.install(server, data.get(Constants.OPERATORS, ServerOpListHack.class));
                server.getPlayerList().getPlayers().forEach(p -> server.getPlayerList().sendPlayerPermissionLevel(p));

                UserBanListHack.install(server, data.get(Constants.BANNED_PLAYERS, UserBanListHack.class));
                IpBanListHack.install(server, data.get(Constants.BANNED_IPS, IpBanListHack.class));
            }
            default -> log(Level.WARN, "Ignoring unknown packet: " + packet);
        };
    }

    protected static void log(Level level, String message) {
        LogManager.getLogger().log(level, "[" + Constants.SINGULARITY_NAME + "] " + message);
    }
}