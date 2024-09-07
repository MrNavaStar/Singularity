package me.mrnavastar.singularity.loader;

import com.mojang.authlib.GameProfile;
import me.mrnavastar.protoweaver.api.ProtoConnectionHandler;
import me.mrnavastar.protoweaver.api.netty.ProtoConnection;
import me.mrnavastar.singularity.common.Constants;
import me.mrnavastar.singularity.common.networking.DataBundle;
import me.mrnavastar.singularity.common.networking.Settings;
import me.mrnavastar.singularity.common.networking.Profile;
import me.mrnavastar.singularity.loader.impl.sync.SynchronizedUserCache;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;

import java.util.*;

public class Dead implements ProtoConnectionHandler {






    public static void syncPlayerData() {
        if (proxy != null && proxy.isOpen())
            server.getPlayerList().getPlayers()
                    .forEach(player -> putPlayerTopic(player.getUUID(), Constants.PLAYER_TOPIC, createPlayerDataBundle(player)));
    }

    public void onReady(ProtoConnection protoConnection) {
        proxy = protoConnection;



    }

    @Override
    public void handlePacket(ProtoConnection protoConnection, Object packet) {
        switch (packet) {
            case Settings s -> {
                processSettings(s);
                settings = s;
                reloadBlacklists();
            }

            case DataBundle data -> callbacks.getOrDefault(data.meta().toString(), new ArrayList<>()).forEach(consumer -> consumer.accept(data));

            case DataBundle data -> Optional.ofNullable(server.getPlayerList().getPlayer(data.meta().id()))
                    .ifPresentOrElse(player -> processData(player, data), () -> incoming.put(data.meta().id(), data));

            case Profile profile -> {
                switch (profile.getProperty()) {
                    case NAME_LOOKUP, UUID_LOOKUP -> SynchronizedUserCache.update(profile, new GameProfile(profile.getUuid(), profile.getName()));
                    case BAD_LOOKUP -> SynchronizedUserCache.update(profile, null);
                }
            }

            default -> log(Level.WARN, "Ignoring unknown packet: " + packet);
        };
    }

    protected static void log(Level level, String message) {
        LogManager.getLogger().log(level, "[" + Constants.SINGULARITY_NAME + "]: " + message);
    }
}