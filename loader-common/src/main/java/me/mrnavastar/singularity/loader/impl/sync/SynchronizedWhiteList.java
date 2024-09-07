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

public class SynchronizedWhiteList extends UserWhiteList {

    public SynchronizedWhiteList() {
        super(PlayerList.WHITELIST_FILE);
        PlayerList.WHITELIST_FILE.delete();
    }

    @Override
    public void add(UserWhiteListEntry entry) {
        GameProfile profile = R.of(entry).get("user", GameProfile.class);
        Dead.putPlayerTopic(profile.getId(), Constants.WHITELIST, new DataBundle());
    }

    @Override
    public void remove(GameProfile profile) {
        Dead.removePlayerTopic(profile.getId(), Constants.WHITELIST);
    }

    @Override
    @SneakyThrows
    public @Nullable UserWhiteListEntry get(GameProfile profile) {
        return contains(profile) ? new UserWhiteListEntry(profile) : null;
    }

    @Override
    @SneakyThrows
    protected boolean contains(GameProfile profile) {
         return Dead.getPlayerTopic(profile.getId(), Constants.WHITELIST).get().isPresent();
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