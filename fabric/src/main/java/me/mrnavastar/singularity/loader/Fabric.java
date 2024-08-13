package me.mrnavastar.singularity.loader;

import me.mrnavastar.singularity.common.Constants;
import me.mrnavastar.singularity.loader.impl.IpBanListHack;
import me.mrnavastar.singularity.loader.impl.ServerOpListHack;
import me.mrnavastar.singularity.loader.impl.UserBanListHack;
import me.mrnavastar.singularity.loader.impl.UserWhiteListHack;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.apache.logging.log4j.Level;

public class Fabric extends Singularity implements DedicatedServerModInitializer {

    @Override
    public void onInitializeServer() {
        log(Level.INFO, Constants.SINGULARITY_BOOT_MESSAGE);
        Constants.PROTOCOL.setServerHandler(Fabric.class).load();

        ServerLifecycleEvents.SERVER_STARTING.register(s -> server = s);
        ServerLifecycleEvents.SERVER_STARTED.register(UserWhiteListHack::install);
        ServerLifecycleEvents.SERVER_STARTED.register(ServerOpListHack::install);
        ServerLifecycleEvents.SERVER_STARTED.register(UserBanListHack::install);
        ServerLifecycleEvents.SERVER_STARTED.register(IpBanListHack::install);
        //ServerLifecycleEvents.BEFORE_SAVE.register((server, flush, force) -> server.getPlayerList().getPlayers().forEach(this::syncData));
        //CommandRegistrationCallback.EVENT.register(Singularity::registerCommands);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server1) -> onJoin(handler.getPlayer()));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server1) -> onLeave(handler.getPlayer()));


    }
}