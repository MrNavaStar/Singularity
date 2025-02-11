package me.mrnavastar.singularity.loader.impl.sync;

import com.mojang.authlib.GameProfile;
import lombok.SneakyThrows;
import me.mrnavastar.r.R;
import me.mrnavastar.singularity.common.Constants;
import me.mrnavastar.singularity.common.networking.DataBundle;
import me.mrnavastar.singularity.loader.impl.Broker;
import me.mrnavastar.singularity.loader.util.Mappings;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.*;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

public class SynchronizedLists {

    private static final SynchronizedStoredUserList<GameProfile> ops = new SynchronizedStoredUserList<>(Constants.OPERATOR, ServerOpListEntry.class, () -> Broker.getSettings().syncOps, true, PlayerList.OPLIST_FILE);
    private static final SynchronizedStoredUserList<GameProfile> bans = new SynchronizedStoredUserList<>(Constants.BANNED_PLAYERS, UserBanListEntry.class, () -> Broker.getSettings().syncBans, true, PlayerList.USERBANLIST_FILE);
    private static final SynchronizedStoredUserList<String> ipbans = new SynchronizedStoredUserList<>(Constants.BANNED_IPS, IpBanListEntry.class, () -> Broker.getSettings().syncBans, false, PlayerList.IPBANLIST_FILE);
    private static final SynchronizedStoredUserList<GameProfile> whitelist = new SynchronizedStoredUserList<>(Constants.WHITELIST, UserWhiteListEntry.class, () -> Broker.getSettings().syncWhitelist, true, PlayerList.WHITELIST_FILE);

    public static class SynchronizedOpList extends ServerOpList {

        public SynchronizedOpList() {
            super(ops.getFile());
        }

        @Override
        public void add(ServerOpListEntry entry) {
            ops.add(entry);
        }

        @Override
        public void remove(GameProfile profile) {
            ops.remove(profile);
        }

        @Override
        @SneakyThrows
        public @Nullable ServerOpListEntry get(GameProfile profile) {
            return (ServerOpListEntry) ops.get(profile);
        }

        @Override
        public void save() throws IOException {
            ops.save();
        }

        @Override
        public void load() throws IOException {
            ops.load();
        }
    }

    public static class SynchronizedBanList extends UserBanList {

        public SynchronizedBanList() {
            super(bans.getFile());
        }

        @Override
        public void add(UserBanListEntry entry) {
            bans.add(entry);
        }

        @Override
        public void remove(GameProfile profile) {
            bans.remove(profile);
        }

        @Override
        @SneakyThrows
        public @Nullable UserBanListEntry get(GameProfile profile) {
            return (UserBanListEntry) bans.get(profile);
        }

        @Override
        public void save() throws IOException {
            bans.save();
        }

        @Override
        public void load() throws IOException {
            bans.load();
        }
    }

    public static class SynchronizedIpBanList extends IpBanList {

        public SynchronizedIpBanList() {
            super(ipbans.getFile());
        }

        @Override
        public void add(IpBanListEntry entry) {
            ipbans.add(entry);
        }

        @Override
        public void remove(String profile) {
            ipbans.remove(profile);
        }

        @Override
        @SneakyThrows
        public @Nullable IpBanListEntry get(String profile) {
            return (IpBanListEntry) ipbans.get(profile);
        }

        @Override
        public void save() throws IOException {
            ipbans.save();
        }

        @Override
        public void load() throws IOException {
            ipbans.load();
        }
    }

    public static class SynchronizedWhiteList extends UserWhiteList {

        public SynchronizedWhiteList() {
            super(whitelist.getFile());
        }

        @Override
        public void add(UserWhiteListEntry entry) {
            whitelist.add(entry);
        }

        @Override
        public void remove(GameProfile profile) {
            whitelist.remove(profile);
        }

        @Override
        @SneakyThrows
        public @Nullable UserWhiteListEntry get(GameProfile profile) {
            return (UserWhiteListEntry) whitelist.get(profile);
        }

        @Override
        public void save() throws IOException {
            whitelist.save();
        }

        @Override
        public void load() throws IOException {
            whitelist.load();
        }
    }

    public static void setWhitelist(boolean enabled) {
        if (!Broker.getSettings().syncWhitelist) return;
        Broker.putTopic(Constants.WHITELIST, "enabled", new DataBundle()
                .meta(new DataBundle.Meta().propagation(DataBundle.Propagation.ALL))
                .put("enabled", enabled)
        );
    }

    public static void install(MinecraftServer server) {
        SynchronizedStoredUserList.install();
        // Overwrite vanilla list handlers
        R.of(server.getPlayerList()).set(Mappings.of("ops", "field_14353"), new SynchronizedOpList());
        R.of(server.getPlayerList()).set(Mappings.of("bans", "field_14344"), new SynchronizedBanList());
        R.of(server.getPlayerList()).set(Mappings.of("ipBans", "field_14345"), new SynchronizedIpBanList());
        R.of(server.getPlayerList()).set(Mappings.of("whitelist", "field_14361"), new SynchronizedWhiteList());

        // Op/DeOp player if their status is changed on another server
        ops.setOnAdded((profile, entry) -> Optional.ofNullable(server.getPlayerList().getPlayer(profile.getId()))
                .ifPresent(player -> server.getPlayerList().sendPlayerPermissionLevel(player)));
        ops.setOnRemoved(profile -> Optional.ofNullable(server.getPlayerList().getPlayer(profile.getId()))
                .ifPresent(player -> server.getPlayerList().sendPlayerPermissionLevel(player)));

        // Disconnect player if banned from another server
        bans.setOnAdded((profile, entry) -> Optional.ofNullable(server.getPlayerList().getPlayer(profile.getId()))
                .ifPresent(player -> player.connection.disconnect(Component.literal(((UserBanListEntry) entry).getReason()))));

        // Disconnect player if their ip is banned from another server
        ipbans.setOnAdded((profile, entry) -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                String playerIp = R.of(ipbans).call(Mappings.of("getIpFromAddress", "method_14526"), String.class, player.connection.getRemoteAddress());
                if (ipbans.getKey(entry).equals(Optional.of(playerIp))) {
                    player.connection.disconnect(Component.literal(((IpBanListEntry) entry).getReason()));
                    return;
                }
            }
        });

        // Disconnect player if they are removed from the whitelist
        whitelist.setOnRemoved(profile -> {
            if (!server.isEnforceWhitelist()) return;
            Optional.ofNullable(server.getPlayerList().getPlayer(profile.getId()))
                    .ifPresent(player -> player.connection.disconnect(Component.literal("You are no longer whitelisted on this server!")));
        });

        // Disconnect player if un-whitelisted from another server
        Broker.subTopic(Constants.WHITELIST, bundle -> {
            if (bundle.meta().id().equals("enabled")) server.setEnforceWhitelist(bundle.get("enabled", boolean.class).orElse(false));
        });
    }
}