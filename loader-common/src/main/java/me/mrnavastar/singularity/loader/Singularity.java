package me.mrnavastar.singularity.loader;

import com.mojang.authlib.GameProfile;
import lombok.Getter;
import lombok.SneakyThrows;
import me.mrnavastar.protoweaver.api.ProtoConnectionHandler;
import me.mrnavastar.protoweaver.api.netty.ProtoConnection;
import me.mrnavastar.r.R;
import me.mrnavastar.singularity.common.Constants;
import me.mrnavastar.singularity.common.networking.DataBundle;
import me.mrnavastar.singularity.common.networking.Settings;
import me.mrnavastar.singularity.common.networking.Profile;
import me.mrnavastar.singularity.loader.api.SyncEvents;
import me.mrnavastar.singularity.loader.impl.sync.SynchronizedBanList;
import me.mrnavastar.singularity.loader.impl.sync.SynchronizedOpList;
import me.mrnavastar.singularity.loader.impl.sync.SynchronizedUserCache;
import me.mrnavastar.singularity.loader.impl.sync.SynchronizedWhiteList;
import me.mrnavastar.singularity.loader.util.Mappings;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;

import java.io.*;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Singularity implements ProtoConnectionHandler {

    protected static final HashSet<String> nbtBlacklist = new HashSet<>();
    @Getter
    protected static Settings settings = new Settings();
    protected static ProtoConnection proxy;
    protected static MinecraftServer server;

    protected static final ConcurrentHashMap<UUID, DataBundle> incoming = new ConcurrentHashMap<>();

    @SneakyThrows
    public Singularity() {
        reloadBlacklists();
        // Register NBT Types
        DataBundle.register(ByteArrayTag.class);
        DataBundle.register(ByteTag.class);
        DataBundle.register(CollectionTag.class);
        DataBundle.register(CompoundTag.class);
        DataBundle.register(DoubleTag.class);
        DataBundle.register(EndTag.class);
        DataBundle.register(FloatTag.class);
        DataBundle.register(IntArrayTag.class);
        DataBundle.register(IntTag.class);
        DataBundle.register(ListTag.class);
        DataBundle.register(LongArrayTag.class);
        DataBundle.register(LongTag.class);
        DataBundle.register(NumericTag.class);
        DataBundle.register(ShortTag.class);
        DataBundle.register(StringTag.class);
    }

    protected void onJoin(ServerPlayer player) {
        DataBundle data = incoming.remove(player.getUUID());
        if (data != null) processData(player, data);
    }

    protected void onLeave(ServerPlayer player) {
        proxy.send(createPlayerDataPacket(player));
    }

    // Used by paper
    protected void processData(ServerPlayer player, DataBundle data) {
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

    protected static DataBundle createPlayerDataPacket(ServerPlayer player) {
        DataBundle data = new DataBundle(player.getUUID());
        SyncEvents.SEND_DATA.getInvoker().trigger(player, data);
        return data;
    }

    public static void send(Object packet) {
        if (proxy != null && proxy.isOpen()) proxy.send(packet);
    }

    public static void syncPlayerData() {
        if (proxy != null && proxy.isOpen()) server.getPlayerList().getPlayers().forEach(player -> proxy.send(createPlayerDataPacket(player)));
    }

    public static void syncStaticData(String key, Object value) {
        if (proxy != null && proxy.isOpen()) proxy.send(new DataBundle(Constants.STATIC_DATA).put(key, value));
    }

    public void onReady(ProtoConnection protoConnection) {
        proxy = protoConnection;
        SynchronizedUserCache.install(server);
        SynchronizedOpList.install(server);
        SynchronizedWhiteList.install(server);
        SynchronizedBanList.install(server);

        // Player Data
        SyncEvents.SEND_DATA.register(((player, data) -> {
            if (!settings.syncPlayerData) return;
            CompoundTag nbt = new CompoundTag();
            player.saveWithoutId(nbt);
            data.put(Constants.PLAYER_DATA, nbt);
        }));

        SyncEvents.RECEIVE_DATA.register(((player, data) -> {
            if (!settings.syncPlayerData) return;
            data.get(Constants.PLAYER_DATA, CompoundTag.class).ifPresent(nbt -> {
                CompoundTag current = new CompoundTag();
                player.saveWithoutId(current);
                nbtBlacklist.forEach(key -> {
                    if (!current.contains(key)) return;
                    Optional.ofNullable(current.get(key)).ifPresent(tag -> nbt.put(key, tag));
                });
                player.load(nbt);
            });
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
            data.put(Constants.PLAYER_STATS, R.of(player.getStats()).call(Mappings.of("toJson", "method_14911"), String.class));
        });
        SyncEvents.RECEIVE_DATA.register((player, data) -> {
            if (!settings.syncPlayerStats) return;
            data.get(Constants.PLAYER_STATS, String.class).ifPresent(stats -> player.getStats().parseLocal(server.getFixerUpper(), stats));
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

            case DataBundle data -> {
                ServerPlayer player = server.getPlayerList().getPlayer(data.getId());
                if (player != null) processData(player, data);
                else incoming.put(data.getId(), data);
            }

            case Profile profile -> {
                switch (profile.getProperty()) {
                    case NAME_LOOKUP, UUID_LOOKUP -> SynchronizedUserCache.update(profile, new GameProfile(profile.getUuid(), profile.getName()));
                    case BAD_LOOKUP -> SynchronizedUserCache.update(profile, null);

                    case OP -> SynchronizedOpList.update(profile);
                    case WHITELISTED -> SynchronizedWhiteList.update(profile);
                    case BANNED -> SynchronizedBanList.update(profile);
                }
            }

            default -> log(Level.WARN, "Ignoring unknown packet: " + packet);
        };
    }

    protected static void log(Level level, String message) {
        LogManager.getLogger().log(level, "[" + Constants.SINGULARITY_NAME + "]: " + message);
    }
}