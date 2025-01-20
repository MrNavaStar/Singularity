package me.mrnavastar.singularity.loader.impl.sync;

import com.mojang.authlib.GameProfile;
import lombok.SneakyThrows;
import me.mrnavastar.r.R;
import me.mrnavastar.singularity.common.Constants;
import me.mrnavastar.singularity.common.networking.DataBundle;
import me.mrnavastar.singularity.loader.impl.Broker;
import me.mrnavastar.singularity.loader.impl.serialization.StoredUserEntrySerializer;
import me.mrnavastar.singularity.loader.util.Mappings;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.*;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

public class SynchronizedBanList extends UserBanList {

    public SynchronizedBanList() {
        super(PlayerList.USERBANLIST_FILE);
    }

    @Override
    public void add(UserBanListEntry entry) {
        if (!Broker.getSettings().syncBans) {
            super.add(entry);
            return;
        }
        GameProfile profile = R.of(entry).get("user", GameProfile.class);
        Broker.putTopic(Constants.BANNED_PLAYERS, profile.getId().toString(), new DataBundle()
                .meta(new DataBundle.Meta().propagation(DataBundle.Propagation.ALL))
                .put("entry", entry));
    }

    @Override
    public void remove(GameProfile profile) {
        if (!Broker.getSettings().syncBans) {
            super.remove(profile);
            return;
        }
        Broker.removeTopic(Constants.BANNED_PLAYERS, profile.getId().toString());
    }

    @Override
    @SneakyThrows
    public @Nullable UserBanListEntry get(GameProfile profile) {
        if (!Broker.getSettings().syncBans) return super.get(profile);
        return Broker.getTopic(Constants.BANNED_PLAYERS, profile.getId().toString()).get()
                .flatMap(bundle -> bundle.get("entry", UserBanListEntry.class))
                .orElse(null);
    }

    @Override
    protected boolean contains(GameProfile profile) {
        if (!Broker.getSettings().syncBans) super.contains(profile);
        return get(profile) != null;
    }

    @Override
    public void save() throws IOException {
        if (!Broker.getSettings().syncBans) super.save();
    }

    @Override
    public void load() throws IOException {
        if (!Broker.getSettings().syncBans) super.load();
    }

    public static void install(MinecraftServer server) {
        DataBundle.register(StoredUserEntrySerializer.class);
        R.of(server.getPlayerList()).set(Mappings.of("bans", "field_14344"), new SynchronizedBanList());
        // Disconnect player if banned from another server
        Broker.subTopic(Constants.BANNED_PLAYERS, bundle -> {
            Optional.ofNullable(server.getPlayerList().getPlayer(UUID.fromString(bundle.meta().id())))
                    .ifPresent(player -> bundle.get("entry", UserBanListEntry.class)
                            .ifPresent(entry -> player.connection.disconnect(Component.literal(entry.getReason()))));
        });
    }
}