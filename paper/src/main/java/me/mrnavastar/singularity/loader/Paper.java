package me.mrnavastar.singularity.loader;

import me.mrnavastar.singularity.common.Constants;
import me.mrnavastar.singularity.common.networking.DataBundle;
import me.mrnavastar.singularity.common.networking.Settings;
import me.mrnavastar.singularity.loader.api.Singularity;
import me.mrnavastar.singularity.loader.impl.sync.SynchronizedPlayerData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.spigotmc.SpigotConfig;

import java.util.logging.Logger;

public class Paper extends JavaPlugin {

    private final Logger logger = getLogger();

    public static class EventListener extends SynchronizedPlayerData implements Listener {

        public EventListener() {
            init(MinecraftServer.getServer());
        }

        @EventHandler
        public void onSave(WorldSaveEvent event) {
            syncPlayerData();
        }

        /*@EventHandler
        public void onToggle(WhitelistToggleEvent event) {
            syncServerData();
        }*/

        @EventHandler
        public void onPlayerJoin(PlayerJoinEvent event) {
            onJoin(((CraftPlayer) event.getPlayer()).getHandle());
        }

        @EventHandler
        public void onPlayerQuit(PlayerQuitEvent event) {
            onLeave(((CraftPlayer) event.getPlayer()).getHandle());
        }

        protected static void processData(ServerPlayer player, DataBundle data) {
            player.valid = false;
            Singularity.RECEIVE_DATA.getInvoker().trigger(player, data);
            player.valid = true;
        }

        @Override
        protected void processSettings(Settings settings) {
            SpigotConfig.disablePlayerDataSaving = settings.syncPlayerData;
            SpigotConfig.disableStatSaving = settings.syncPlayerStats;
            SpigotConfig.disableAdvancementSaving = settings.syncPlayerAdvancements;
        }
    }

    @Override
    public void onEnable() {
        logger.info(Constants.SINGULARITY_BOOT_MESSAGE);
        Constants.WORMHOLE.setServerHandler(EventListener.class).load();
        getServer().getPluginManager().registerEvents(new EventListener(), this);
    }
}