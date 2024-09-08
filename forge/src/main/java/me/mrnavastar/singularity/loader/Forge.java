package me.mrnavastar.singularity.loader;

import me.mrnavastar.protoweaver.api.ProtoWeaver;
import me.mrnavastar.singularity.common.Constants;
import me.mrnavastar.singularity.loader.impl.Broker;
import me.mrnavastar.singularity.loader.impl.sync.SynchronizedMinecraft;
import me.mrnavastar.singularity.loader.util.Mappings;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.apache.logging.log4j.Level;

import java.util.function.Consumer;

@Mod(Constants.SINGULARITY_ID)
public class Forge extends SynchronizedMinecraft {

    static {
        Mappings.setDev(!FMLEnvironment.production);
        ProtoWeaver.load(Broker.PROTOCOL);
    }

    public Forge() {
        log(Level.INFO, Constants.SINGULARITY_BOOT_MESSAGE);

        MinecraftForge.EVENT_BUS.addListener((Consumer<ServerStartingEvent>) event -> init(event.getServer()));
        MinecraftForge.EVENT_BUS.addListener((Consumer<PlayerEvent.PlayerLoggedInEvent>) event -> onJoin((ServerPlayer) event.getEntity()));
        MinecraftForge.EVENT_BUS.addListener((Consumer<PlayerEvent.PlayerLoggedOutEvent>) event -> onLeave((ServerPlayer) event.getEntity()));
    }
}