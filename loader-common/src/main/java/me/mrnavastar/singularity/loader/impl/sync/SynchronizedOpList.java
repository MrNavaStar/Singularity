package me.mrnavastar.singularity.loader.impl.sync;

import com.mojang.authlib.GameProfile;
import lombok.SneakyThrows;
import me.mrnavastar.r.R;
import me.mrnavastar.singularity.common.Constants;
import me.mrnavastar.singularity.common.networking.DataBundle;
import me.mrnavastar.singularity.loader.impl.Broker;
import me.mrnavastar.singularity.loader.util.Mappings;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.*;
import org.jetbrains.annotations.Nullable;

public class SynchronizedOpList extends ServerOpList {

    public SynchronizedOpList() {
        super(PlayerList.OPLIST_FILE);
        PlayerList.OPLIST_FILE.delete();
    }

    @Override
    public void add(ServerOpListEntry entry) {
        GameProfile profile = R.of(entry).get("user", GameProfile.class);
        Broker.putPlayerTopic(profile.getId(), Constants.OPERATOR, new DataBundle()
                .put("level", entry.getLevel())
                .put("bypass", entry.getBypassesPlayerLimit()));
    }

    @Override
    public void remove(GameProfile profile) {
        Broker.removePlayerTopic(profile.getId(), Constants.OPERATOR);
    }

    @Override
    @SneakyThrows
    public @Nullable ServerOpListEntry get(GameProfile profile) {
        return Broker.getPlayerTopic(profile.getId(), Constants.OPERATOR).get()
                .flatMap(data -> data.get("level", int.class)
                .flatMap(level -> data.get("bypass", boolean.class)
                        .map(bypass -> new ServerOpListEntry(profile, level, bypass))))
                .orElse(null);
    }

    @Override
    @SneakyThrows
    protected boolean contains(GameProfile profile) {
        return Broker.getPlayerTopic(profile.getId(), Constants.OPERATOR).get().isPresent();
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