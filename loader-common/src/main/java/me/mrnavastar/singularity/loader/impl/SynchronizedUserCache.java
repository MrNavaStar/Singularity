package me.mrnavastar.singularity.loader.impl;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.GameProfileRepository;
import lombok.SneakyThrows;
import me.mrnavastar.r.R;
import me.mrnavastar.singularity.common.networking.Profile;
import me.mrnavastar.singularity.loader.Singularity;
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

    public static void update(Profile user) {
        Optional.ofNullable(requests.remove(user.uuid().toString())).ifPresent(future -> future.complete(Optional.of(new GameProfile(user.uuid(), user.name()))));
        Optional.ofNullable(requests.remove(user.name())).ifPresent(future -> future.complete(Optional.of(new GameProfile(user.uuid(), user.name()))));
    }

    public static void reject(Object key) {
        Optional.ofNullable(requests.remove(key.toString())).ifPresent(future -> future.complete(Optional.empty()));
    }

    private static CompletableFuture<Optional<GameProfile>> getCacheEntry(Object key) {
        return Optional.ofNullable(requests.get(key.toString())).orElseGet(() -> {
            Singularity.send(key);
            CompletableFuture<Optional<GameProfile>> future = new CompletableFuture<>();
            requests.put(key.toString(), future);
            return future;
        });
    }

    // Ignore
    @Override
    public void add(GameProfile profile) {}

    @Override
    @SneakyThrows
    public Optional<GameProfile> get(String player) {
        return getCacheEntry(player.toLowerCase(Locale.ROOT)).get();
    }

    @Override
    public CompletableFuture<Optional<GameProfile>> getAsync(String player) {
        return getCacheEntry(player.toLowerCase(Locale.ROOT));
    }

    @Override
    @SneakyThrows
    public Optional<GameProfile> get(UUID uuid) {
        return getCacheEntry(uuid).get();
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
        Services services = R.of(server).get("services", Services.class);
        R.of(server).set("services", new Services(services.sessionService(), services.servicesKeySet(), services.profileRepository(),
                new SynchronizedUserCache(services.profileRepository(), R.of(services.profileCache()).get("file", File.class))));
    }
}
