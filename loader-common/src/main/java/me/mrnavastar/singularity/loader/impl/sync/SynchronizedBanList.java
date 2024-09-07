package me.mrnavastar.singularity.loader.impl.sync;

import com.mojang.authlib.GameProfile;
import lombok.SneakyThrows;
import me.mrnavastar.r.R;
import me.mrnavastar.singularity.common.Constants;
import me.mrnavastar.singularity.common.networking.DataBundle;
import me.mrnavastar.singularity.loader.Dead;
import me.mrnavastar.singularity.loader.util.Mappings;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.*;
import org.jetbrains.annotations.Nullable;

import java.util.Date;

public class SynchronizedBanList extends UserBanList {

    public SynchronizedBanList() {
        super(PlayerList.USERBANLIST_FILE);
        PlayerList.USERBANLIST_FILE.delete();
    }

    @Override
    public void add(UserBanListEntry entry) {
        GameProfile profile = R.of(entry).get("user", GameProfile.class);
        Dead.putPlayerTopic(profile.getId(), Constants.BANNED_PLAYERS, new DataBundle()
                .put("created", entry.getCreated())
                .put("source", entry.getSource())
                .put("expires", entry.getExpires())
                .put("reason", entry.getReason()));
    }

    @Override
    public void remove(GameProfile profile) {
        Dead.removePlayerTopic(profile.getId(), Constants.BANNED_PLAYERS);
    }

    @Override
    @SneakyThrows
    public @Nullable UserBanListEntry get(GameProfile profile) {
        return Dead.getPlayerTopic(profile.getId(), Constants.BANNED_PLAYERS).get()
                .map(data -> {
                    Date created = data.get("created", Date.class).orElse(null);
                    String source = data.get("source", String.class).orElse(null);
                    Date expires = data.get("expires", Date.class).orElse(null);
                    String reason = data.get("reason", String.class).orElse(null);
                    return new UserBanListEntry(profile, created, source, expires, reason);
                }).orElse(null);
    }

    @Override
    @SneakyThrows
    protected boolean contains(GameProfile profile) {
        return Dead.getPlayerTopic(profile.getId(), Constants.BANNED_PLAYERS).get().isPresent();
    }

    // Bye bye
    @Override
    public void save() {}

    // Bye bye
    @Override
    public void load() {}

    public static void install(MinecraftServer server) {
        R.of(server.getPlayerList()).set(Mappings.of("bans", "field_14344"), new SynchronizedBanList());
    }
}