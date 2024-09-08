package me.mrnavastar.singularity.loader.impl.sync;

import lombok.Setter;
import me.mrnavastar.r.R;
import me.mrnavastar.singularity.common.Constants;
import me.mrnavastar.singularity.common.networking.DataBundle;
import me.mrnavastar.singularity.loader.api.Singularity;
import me.mrnavastar.singularity.loader.impl.Broker;
import me.mrnavastar.singularity.loader.util.Mappings;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;

import java.io.*;
import java.util.Date;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class SynchronizedMinecraft {

    private static final ConcurrentHashMap<UUID, DataBundle> incoming = new ConcurrentHashMap<>();
    private static final HashSet<String> nbtBlacklist = new HashSet<>();
    @Setter private static Consumer<ServerPlayer> playerCallback;
    private static MinecraftServer server;

    public static void init(MinecraftServer s) {
        server = s;
        SynchronizedUserCache.install(server);
        SynchronizedOpList.install(server);
        SynchronizedWhiteList.install(server);
        SynchronizedBanList.install(server);

        DataBundle.register(Date.class);
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

        reloadBlacklists();

        // Listen for player data from velocity
        Broker.subPlayerTopic(Constants.PLAYER_TOPIC, (uuid, data) -> Optional.ofNullable(server.getPlayerList().getPlayer(data.meta().id()))
                .ifPresentOrElse(player -> processData(player, data), () -> incoming.put(data.meta().id(), data)));

        // Get current whitelist state and listen for future changes
        Broker.getStaticTopic(Constants.WHITELIST).whenComplete((bundle, throwable) -> bundle.ifPresent(data -> SynchronizedWhiteList.ENABLED.accept(server, data)));
        Broker.subStaticTopic(Constants.WHITELIST, data -> SynchronizedWhiteList.ENABLED.accept(server, data));

        // Player Data
        Singularity.SEND_DATA.register(((player, data) -> {
            if (!Broker.getSettings().syncPlayerData) return;
            CompoundTag nbt = new CompoundTag();
            player.saveWithoutId(nbt);
            data.put(Constants.PLAYER_TOPIC + ":nbt", nbt);
        }));

        Singularity.RECEIVE_DATA.register(((player, data) -> {
            System.out.println("here??");

            if (!Broker.getSettings().syncPlayerData) return;
            data.get(Constants.PLAYER_TOPIC + ":nbt", CompoundTag.class).ifPresent(nbt -> {
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
        Singularity.SEND_DATA.register((player, data) -> {
            if (!Broker.getSettings().syncPlayerStats) return;
            data.put(Constants.PLAYER_STATS, R.of(player.getStats()).call(Mappings.of("toJson", "method_14911"), String.class));
        });
        Singularity.RECEIVE_DATA.register((player, data) -> {
            if (!Broker.getSettings().syncPlayerStats) return;
            data.get(Constants.PLAYER_STATS, String.class).ifPresent(stats -> player.getStats().parseLocal(server.getFixerUpper(), stats));
        });
    }

    public static void reloadBlacklists() {
        nbtBlacklist.clear();
        Broker.getSettings().nbtBlacklists.add("singularity.never");
        Broker.getSettings().nbtBlacklists.forEach(name -> {
            try (InputStream stream = SynchronizedMinecraft.class.getClassLoader().getResourceAsStream(name)) {
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

    protected static DataBundle createPlayerDataBundle(ServerPlayer player) {
        DataBundle data = new DataBundle();
        Singularity.SEND_DATA.getInvoker().trigger(player, data);
        return data;
    }

    public static void syncPlayerData() {
        server.getPlayerList().getPlayers().forEach(player -> Broker.putPlayerTopic(player.getUUID(), Constants.PLAYER_TOPIC, createPlayerDataBundle(player)));
    }

    protected static void onJoin(ServerPlayer player) {
        DataBundle data = incoming.remove(player.getUUID());
        if (data != null) processData(player, data);
    }

    protected static void onLeave(ServerPlayer player) {
        Broker.putPlayerTopic(player.getUUID(), Constants.PLAYER_TOPIC, createPlayerDataBundle(player));
    }

    protected static void processData(ServerPlayer player, DataBundle data) {
        if (playerCallback != null) playerCallback.accept(player);
        Singularity.RECEIVE_DATA.getInvoker().trigger(player, data);
        if (playerCallback != null) playerCallback.accept(player);
    }

    protected static void log(Level level, String message) {
        LogManager.getLogger().log(level, "[" + Constants.SINGULARITY_NAME + "]: " + message);
    }
}
