package me.mrnavastar.singularity.loader;

import me.mrnavastar.singularity.common.Constants;
import me.mrnavastar.singularity.common.networking.SyncData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public class Paper extends JavaPlugin {

    private final Logger logger = getLogger();

    public static class EventListener extends Singularity implements Listener {

        public EventListener() {
            setServer(MinecraftServer.getServer());
        }

        @EventHandler
        public void onPlayerQuit(PlayerQuitEvent event) {
            syncData(((CraftPlayer) event.getPlayer()).getHandle());
        }

        @Override
        protected void preProcessData(ServerPlayer player, SyncData data) {
            player.valid = false;
        }

        @Override
        protected void postProcessData(ServerPlayer player, SyncData data) {
            player.valid = true;
        }
    }

    @Override
    public void onEnable() {
        logger.info(Constants.SINGULARITY_BOOT_MESSAGE);
        Constants.PROTOCOL.setServerHandler(EventListener.class).load();
        getServer().getPluginManager().registerEvents(new EventListener(), this);
    }
}