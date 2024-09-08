package me.mrnavastar.singularity.loader.impl.sync;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.GameProfileRepository;
import lombok.SneakyThrows;
import me.mrnavastar.r.R;
import me.mrnavastar.singularity.common.networking.Profile;
import me.mrnavastar.singularity.loader.impl.Broker;
import me.mrnavastar.singularity.loader.util.Mappings;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.Services;
import net.minecraft.server.players.GameProfileCache;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class SynchronizedUserCache extends GameProfileCache {

    private static final ConcurrentHashMap<String, CompletableFuture<Optional<GameProfile>>> requests = new ConcurrentHashMap<>();

    public SynchronizedUserCache(GameProfileRepository gameProfileRepository, File file) {
        super(gameProfileRepository, file);
        file.delete();
    }

    private static String getKey(Profile profile) {
        if (profile.getUuid() != null && profile.getProperty().equals(Profile.Property.UUID_LOOKUP)) return profile.getUuid().toString();
        if (profile.getName() != null && profile.getProperty().equals(Profile.Property.NAME_LOOKUP)) return profile.getName().toLowerCase(Locale.ROOT);
        return null;
    }

    private static CompletableFuture<Optional<GameProfile>> getCacheEntry(Profile profile) {
        String key = getKey(profile);
        if (key != null) return Optional.ofNullable(requests.get(key)).orElseGet(() -> {
            CompletableFuture<Optional<GameProfile>> future = new CompletableFuture<>();
            requests.put(key, future);
            Broker.getProxy().send(profile);
            return future;
        });
        return null;
    }

    public static void update(Profile profile, GameProfile gameProfile) {
        Optional.ofNullable(getKey(profile))
                .flatMap(key -> Optional.ofNullable(requests.remove(key)))
                .ifPresent(future -> future.complete(Optional.ofNullable(gameProfile)));
    }

    // Bye bye
    @Override
    public void add(GameProfile profile) {}

    @Override
    @SneakyThrows
    public Optional<GameProfile> get(String player) {
        return getCacheEntry(new Profile(null, player).setProperty(Profile.Property.NAME_LOOKUP)).get();
    }

    @Override
    public CompletableFuture<Optional<GameProfile>> getAsync(String player) {
        return getCacheEntry(new Profile(null, player).setProperty(Profile.Property.NAME_LOOKUP));
    }

    @Override
    @SneakyThrows
    public Optional<GameProfile> get(UUID uuid) {
        return getCacheEntry(new Profile(uuid, null).setProperty(Profile.Property.UUID_LOOKUP)).get();
    }

    // Bye bye
    @Override
    public void save() {}

    // Bye bye
    @Override
    public List load() {
        return new ArrayList();
    }

    public static void install(MinecraftServer server) {
        String mapping = Mappings.of("services", "field_39440");
        Services services = R.of(server).get(mapping, Services.class);
        R.of(server).set(mapping, new Services(services.sessionService(), services.servicesKeySet(), services.profileRepository(),
                new SynchronizedUserCache(services.profileRepository(), R.of(services.profileCache()).get("file", File.class))));
    }
}