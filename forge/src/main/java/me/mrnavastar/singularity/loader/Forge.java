package me.mrnavastar.singularity.loader;

import me.mrnavastar.singularity.common.Constants;
import me.mrnavastar.singularity.loader.impl.IpBanListHack;
import me.mrnavastar.singularity.loader.impl.ServerOpListHack;
import me.mrnavastar.singularity.loader.impl.UserBanListHack;
import me.mrnavastar.singularity.loader.impl.UserWhiteListHack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.Level;

import java.util.function.Consumer;

@Mod(Constants.SINGULARITY_ID)
public class Forge extends Singularity{

    public Forge() {
        log(Level.INFO, Constants.SINGULARITY_BOOT_MESSAGE);

        MinecraftForge.EVENT_BUS.addListener((Consumer<ServerStartingEvent>) event -> server = event.getServer());
        MinecraftForge.EVENT_BUS.addListener((Consumer<ServerStartedEvent>) event -> UserWhiteListHack.install(server));
        MinecraftForge.EVENT_BUS.addListener((Consumer<ServerStartedEvent>) event -> ServerOpListHack.install(server));
        MinecraftForge.EVENT_BUS.addListener((Consumer<ServerStartedEvent>) event -> UserBanListHack.install(server));
        MinecraftForge.EVENT_BUS.addListener((Consumer<ServerStartedEvent>) event -> IpBanListHack.install(server));

        MinecraftForge.EVENT_BUS.addListener((Consumer<PlayerEvent.PlayerLoggedInEvent>) event -> onJoin((ServerPlayer) event.getEntity()));
        MinecraftForge.EVENT_BUS.addListener((Consumer<PlayerEvent.PlayerLoggedOutEvent>) event -> onLeave((ServerPlayer) event.getEntity()));
    }

}