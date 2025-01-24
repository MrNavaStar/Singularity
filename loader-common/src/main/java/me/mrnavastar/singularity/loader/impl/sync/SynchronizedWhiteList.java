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

public class SynchronizedWhiteList extends UserWhiteList {

    public SynchronizedWhiteList() {
        super(PlayerList.WHITELIST_FILE);
    }

    public static void setEnabled(boolean enabled) {
        if (!Broker.getSettings().syncWhitelist) return;
        Broker.putTopic(Constants.WHITELIST, "enabled", new DataBundle()
                .meta(new DataBundle.Meta().propagation(DataBundle.Propagation.ALL))
                .put("enabled", enabled)
        );
    }

    @Override
    public void add(UserWhiteListEntry entry) {
        if (!Broker.getSettings().syncWhitelist) {
            super.add(entry);
            return;
        }
        GameProfile profile = R.of(entry).call(Mappings.of("getUser", "method_14626"), GameProfile.class);
        Broker.putTopic(Constants.WHITELIST, profile.getId().toString(), new DataBundle());
    }

    @Override
    public void remove(GameProfile profile) {
        if (!Broker.getSettings().syncWhitelist) {
            super.remove(profile);
            return;
        }
        Broker.removeTopic(Constants.WHITELIST, profile.getId().toString());
    }

    @Override
    @SneakyThrows
    public @Nullable UserWhiteListEntry get(GameProfile profile) {
        if (!Broker.getSettings().syncWhitelist) return super.get(profile);
        return Broker.getTopic(Constants.WHITELIST, profile.getId().toString()).get()
                .map(bundle -> new UserWhiteListEntry(profile))
                .orElse(null);
    }

    @Override
    protected boolean contains(GameProfile profile) {
        if (!Broker.getSettings().syncWhitelist) return super.contains(profile);
        return get(profile) != null;
    }

    @Override
    public void save() throws IOException {
        if (!Broker.getSettings().syncWhitelist) super.save();
    }

    @Override
    public void load() throws IOException {
        if (!Broker.getSettings().syncWhitelist) super.load();
    }

    public static void install(MinecraftServer server) {
        DataBundle.register(StoredUserEntrySerializer.class);
        R.of(server.getPlayerList()).set(Mappings.of("whitelist", "field_14361"), new SynchronizedWhiteList());
        Broker.subTopic(Constants.WHITELIST, bundle -> {
            if (bundle.meta().id().equals("enabled")) {
                server.setEnforceWhitelist(bundle.get("enabled", boolean.class).orElse(false));
                return;
            }

            if (bundle.meta().action().equals(DataBundle.Action.REMOVE) && server.isEnforceWhitelist()) {
                Optional.ofNullable(server.getPlayerList().getPlayer(UUID.fromString(bundle.meta().id())))
                        .ifPresent(player -> player.connection.disconnect(Component.literal("You are no longer whitelisted on this server!")));
            }
        });
    }
}