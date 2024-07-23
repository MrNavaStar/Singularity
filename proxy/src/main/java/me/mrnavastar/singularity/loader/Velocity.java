package me.mrnavastar.singularity.loader;

import com.google.gson.Gson;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import me.mrnavastar.protoweaver.api.ProtoConnectionHandler;
import me.mrnavastar.protoweaver.api.netty.ProtoConnection;
import me.mrnavastar.singularity.common.Constants;
import me.mrnavastar.singularity.common.networking.Settings;
import me.mrnavastar.singularity.common.networking.SyncData;
import me.mrnavastar.sqlib.SQLib;
import me.mrnavastar.sqlib.api.DataStore;
import me.mrnavastar.sqlib.api.types.JavaTypes;
import me.mrnavastar.sqlib.api.types.SQLibType;
import me.mrnavastar.sqlib.impl.SQLPrimitive;

import java.util.UUID;

@Plugin(
        id = Constants.SINGULARITY_ID,
        name = Constants.SINGULARITY_NAME,
        version = Constants.SINGULARITY_VERSION,
        authors = Constants.SINGULARITY_AUTHOR,
        dependencies = {
            @Dependency(id = "protoweaver"),
            @Dependency(id = "sqlib")
        }
)
public class Velocity implements ProtoConnectionHandler {

    private static final Gson GSON = new Gson();
    private static final DataStore dataStore = SQLib.getDatabase().dataStore(Constants.SINGULARITY_ID, "data");;
    private static final SQLibType<SyncData> SYNC_DATA = new SQLibType<>(SQLPrimitive.STRING, v -> GSON.toJsonTree(v).toString(), v -> GSON.fromJson(v, SyncData.class));

    private Settings settings = new Settings();

    static {
        Constants.PROTOCOL.setClientHandler(Velocity.class).load();
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        //logger.info(Constants.SINGULARITY_BOOT_MESSAGE);
    }

    @Override
    public void onReady(ProtoConnection connection) {
        connection.send(settings);
    }

    @Override
    public void handlePacket(ProtoConnection connection, Object packet) {
        switch (packet) {
            case Settings s -> settings = s;
            case SyncData data -> dataStore.getOrCreateContainer("player", data.getPlayer(), container -> container.put(JavaTypes.UUID, "player", data.getPlayer())).put(SYNC_DATA, "data", data);
            case UUID player -> dataStore.getContainer("player", player).map(c -> c.get(SYNC_DATA, "data")).ifPresent(connection::send);
            default -> {
                //logger.warn("Ignoring unknown packet: {}", packet)
            }
        }
    }
}