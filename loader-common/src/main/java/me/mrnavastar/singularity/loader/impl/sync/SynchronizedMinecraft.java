package me.mrnavastar.singularity.loader.impl.sync;

import com.google.gson.JsonElement;
import lombok.Setter;
import lombok.SneakyThrows;
import me.mrnavastar.r.R;
import me.mrnavastar.singularity.common.Constants;
import me.mrnavastar.singularity.common.networking.DataBundle;
import me.mrnavastar.singularity.common.networking.Topic;
import me.mrnavastar.singularity.loader.api.Singularity;
import me.mrnavastar.singularity.loader.impl.AdvancementHandler;
import me.mrnavastar.singularity.loader.impl.Broker;
import me.mrnavastar.singularity.loader.impl.serialization.GsonSerializer;
import me.mrnavastar.singularity.loader.impl.serialization.NbtSerializer;
import me.mrnavastar.singularity.loader.util.Mappings;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.zip.ZipException;

public class SynchronizedMinecraft {

    private static final ConcurrentHashMap<UUID, DataBundle> incoming = new ConcurrentHashMap<>();
    private static final HashSet<String> nbtBlacklist = new HashSet<>();
    @Setter private static Consumer<ServerPlayer> playerCallback;
    private static MinecraftServer server;

    public static void init(MinecraftServer s, Path importPath) {
        server = s;
        //SynchronizedGameProfileRepository.install(server);
        //SynchronizedOpList.install(server);
        //SynchronizedWhiteList.install(server);
        //SynchronizedBanList.install(server);

        DataBundle.register(NbtSerializer.class);
        DataBundle.register(GsonSerializer.class);

        importPlayerData(Path.of(importPath + "/import_playerdata"));
        reloadBlacklists();

        // Listen for player data from velocity
        Broker.subTopic(Constants.PLAYER_TOPIC, Topic.Behaviour.PLAYER, data -> Optional.ofNullable(server.getPlayerList().getPlayer(UUID.fromString(data.meta().id())))
                .ifPresentOrElse(player -> processData(player, data), () -> incoming.put(UUID.fromString(data.meta().id()), data)));

        // Player Data
        Singularity.PRE_PUSH_PLAYER_DATA.register(((player, data) -> {
            if (!Broker.getSettings().syncPlayerData) return;
            CompoundTag nbt = new CompoundTag();
            player.saveWithoutId(nbt);
            data.put(Constants.PLAYER_TOPIC + ":nbt", nbt);
        }));

        Singularity.POST_RECEIVE_PLAYER_DATA.register(((player, data) -> {
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
        /*Singularity.PRE_PUSH_PLAYER_DATA.register((player, data) -> {
            if (!Broker.getSettings().syncPlayerAdvancements) return;
            data.put(Constants.PLAYER_ADVANCEMENTS, AdvancementHandler.save(player));
        });
        Singularity.POST_RECEIVE_PLAYER_DATA.register((player, data) -> {
            if (!Broker.getSettings().syncPlayerAdvancements) return;
            data.get(Constants.PLAYER_ADVANCEMENTS, JsonElement.class).ifPresent(advancements -> AdvancementHandler.load(player, advancements));
        });*/

        // Player Stats
        Singularity.PRE_PUSH_PLAYER_DATA.register((player, data) -> {
            if (!Broker.getSettings().syncPlayerStats) return;
            data.put(Constants.PLAYER_STATS, R.of(player.getStats()).call(Mappings.of("toJson", "method_14911"), String.class));
        });
        Singularity.POST_RECEIVE_PLAYER_DATA.register((player, data) -> {
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
        data.meta().propagation(DataBundle.Propagation.PLAYER);
        Singularity.PRE_PUSH_PLAYER_DATA.getInvoker().trigger(player, data);
        return data;
    }

    public static void syncPlayerData() {
        server.getPlayerList().getPlayers().forEach(player -> Broker.putTopic(Constants.PLAYER_TOPIC, player.getUUID().toString(), createPlayerDataBundle(player)));
    }

    @SneakyThrows
    public static void importPlayerData(Path path) {
        if (!new File(String.valueOf(path)).exists()) return;

        try (Stream<Path> s = Files.walk(path)) {
            s.filter(Files::isRegularFile)
            .filter(f -> f.getFileName().toString().endsWith(".dat"))
            .forEach(file -> {
                try {
                    Optional.of(NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap())).ifPresent(playerData -> {
                        UUID uuid = new UUID(playerData.getLong("UUIDMost"), playerData.getLong("UUIDLeast"));
                        if (uuid.equals(new UUID(0, 0))) return;
                        Broker.putTopic(Constants.PLAYER_TOPIC, uuid.toString(), new DataBundle().put(Constants.PLAYER_TOPIC + ":nbt", playerData));
                    });
                } catch (ZipException ignore) {
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    protected static void onJoin(ServerPlayer player) {
        //SynchronizedGameProfileRepository.saveProfile(player.getGameProfile());
        DataBundle data = incoming.remove(player.getUUID());
        if (data != null) processData(player, data);
    }

    protected static void onLeave(ServerPlayer player) {
        Broker.putTopic(Constants.PLAYER_TOPIC, player.getUUID().toString(), createPlayerDataBundle(player));
    }

    protected static void processData(ServerPlayer player, DataBundle data) {
        if (playerCallback != null) playerCallback.accept(player);
        Singularity.POST_RECEIVE_PLAYER_DATA.getInvoker().trigger(player, data);
        if (playerCallback != null) playerCallback.accept(player);
    }

    protected static void log(Level level, String message) {
        LogManager.getLogger().log(level, "[" + Constants.SINGULARITY_NAME + "]: " + message);
    }
}