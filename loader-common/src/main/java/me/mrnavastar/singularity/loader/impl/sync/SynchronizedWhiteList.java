package me.mrnavastar.singularity.loader.impl.sync;

import com.mojang.authlib.GameProfile;
import lombok.SneakyThrows;
import me.mrnavastar.r.R;
import me.mrnavastar.singularity.common.Constants;
import me.mrnavastar.singularity.common.networking.Profile;
import me.mrnavastar.singularity.loader.Singularity;
import me.mrnavastar.singularity.loader.util.Mappings;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.*;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class SynchronizedWhiteList extends UserWhiteList {

    private static final ConcurrentHashMap<UUID, CompletableFuture<Boolean>> requests = new ConcurrentHashMap<>();

    public SynchronizedWhiteList() {
        super(PlayerList.WHITELIST_FILE);
        PlayerList.WHITELIST_FILE.delete();
    }

    public static void update(Profile profile) {
        Optional.ofNullable(requests.remove(profile.getUuid())).ifPresent(future -> future.complete(profile.isValue()));
    }

    @Override
    @SneakyThrows
    public boolean isWhiteListed(GameProfile profile) {
        return Optional.ofNullable(requests.get(profile.getId())).orElseGet(() -> {
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            requests.put(profile.getId(), future);
            Singularity.send(new Profile(profile.getId(), profile.getName()).setProperty(Profile.Property.WHITELISTED));
            return future;
        }).get();
    }

    @Override
    public void add(UserWhiteListEntry storedUserEntry) {
        GameProfile profile = R.of(storedUserEntry).get("user", GameProfile.class);
        Singularity.syncStaticData(Constants.WHITELIST, new Profile(profile.getId(), profile.getName()));
    }

    // Bye bye
    @Override
    public void save() {}

    // Bye bye
    @Override
    public void load() {}

    public static void install(MinecraftServer server) {
        R.of(server.getPlayerList()).set(Mappings.of("whitelist", "field_14361"), new SynchronizedWhiteList());
    }
}