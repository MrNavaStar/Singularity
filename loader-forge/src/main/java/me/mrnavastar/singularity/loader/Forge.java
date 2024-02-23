package me.mrnavastar.singularity.loader;

import me.mrnavastar.singularity.common.Constants;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.Level;

import java.util.function.Consumer;

@Mod(Constants.MOD_ID)
public class Forge {

    private MinecraftServer server;

    public Forge() {
        //log(Level.INFO, Constants.BOOT_MESSAGE);
        //ProtoWeaver.load(protocol);

        //MinecraftForge.EVENT_BUS.addListener((Consumer<ServerStartedEvent>) event -> server = event.getServer());
        //MinecraftForge.EVENT_BUS.addListener((Consumer<PlayerEvent.PlayerLoggedOutEvent>) event -> onPlayerLeave((ServerPlayerEntity) event.getEntity()));
    }

    //@Override
    protected MinecraftServer getServer() {
        return server;
    }
}
