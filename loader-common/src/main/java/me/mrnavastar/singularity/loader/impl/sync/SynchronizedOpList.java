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

public class SynchronizedOpList extends ServerOpList {

    private static final ConcurrentHashMap<UUID, CompletableFuture<Boolean>> requests = new ConcurrentHashMap<>();

    public SynchronizedOpList() {
        super(PlayerList.OPLIST_FILE);
        PlayerList.OPLIST_FILE.delete();
    }

    public static void update(Profile profile) {
        Optional.ofNullable(requests.remove(profile.getUuid())).ifPresent(future -> future.complete(profile.isValue()));
    }

    @Override
    @SneakyThrows
    protected boolean contains(GameProfile profile) {
        return Optional.ofNullable(requests.get(profile.getId())).orElseGet(() -> {
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            requests.put(profile.getId(), future);
            Singularity.send(new Profile(profile.getId(), profile.getName()).setProperty(Profile.Property.OP));
            return future;
        }).get();
    }

    @Override
    public void add(ServerOpListEntry storedUserEntry) {
        GameProfile profile = R.of(storedUserEntry).get("user", GameProfile.class);
        Singularity.syncStaticData(Constants.OPERATORS, new Profile(profile.getId(), profile.getName()));
    }

    // Bye bye
    @Override
    public void save() {}

    // Bye bye
    @Override
    public void load() {}

    public static void install(MinecraftServer server) {
        R.of(server.getPlayerList()).set(Mappings.of("ops", "field_14353"), new SynchronizedOpList());
    }
}