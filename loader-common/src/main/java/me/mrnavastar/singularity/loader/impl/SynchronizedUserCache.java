package me.mrnavastar.singularity.loader.impl;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.GameProfileRepository;
import lombok.SneakyThrows;
import me.mrnavastar.r.R;
import me.mrnavastar.singularity.common.networking.UserCache;
import me.mrnavastar.singularity.loader.Singularity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.Services;
import net.minecraft.server.players.GameProfileCache;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class SynchronizedUserCache extends GameProfileCache {

    private static final ConcurrentHashMap<UUID, CompletableFuture<Optional<GameProfile>>> uuidRequests = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, CompletableFuture<Optional<GameProfile>>> nameRequests = new ConcurrentHashMap<>();

    public SynchronizedUserCache(GameProfileRepository gameProfileRepository, File file) {
        super(gameProfileRepository, file);
        file.delete();
    }

    public static void update(UserCache user) {
        Optional.ofNullable(uuidRequests.remove(user.uuid())).ifPresent(future -> future.complete(Optional.of(new GameProfile(user.uuid(), user.name()))));
        Optional.ofNullable(nameRequests.remove(user.name())).ifPresent(future -> future.complete(Optional.of(new GameProfile(user.uuid(), user.name()))));
    }

    public static void reject(String name) {
        Optional.ofNullable(nameRequests.remove(name)).ifPresent(future -> future.complete(Optional.empty()));
    }

    public static void reject(UUID uuid) {
        Optional.ofNullable(uuidRequests.remove(uuid)).ifPresent(future -> future.complete(Optional.empty()));
    }

    private static CompletableFuture<Optional<GameProfile>> getCacheEntry(String player) {
        Singularity.send(player);
        CompletableFuture<Optional<GameProfile>> future = new CompletableFuture<>();
        nameRequests.put(player, future);
        return future;
    }

    @Override
    public void add(GameProfile profile) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.MONTH, 1);
        Singularity.send(new UserCache(profile.getId(), profile.getName(), calendar.getTime()));
    }

    @Override
    @SneakyThrows
    public Optional<GameProfile> get(String player) {
        return getCacheEntry(player).get();
    }

    @Override
    public CompletableFuture<Optional<GameProfile>> getAsync(String player) {
        return getCacheEntry(player);
    }

    @Override
    @SneakyThrows
    public Optional<GameProfile> get(UUID uuid) {
        Singularity.send(uuid);
        CompletableFuture<Optional<GameProfile>> future = new CompletableFuture<>();
        uuidRequests.put(uuid, future);
        return future.get();
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
