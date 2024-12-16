package me.mrnavastar.singularity.loader;

import com.destroystokyo.paper.event.server.WhitelistToggleEvent;
import me.mrnavastar.protoweaver.api.ProtoWeaver;
import me.mrnavastar.singularity.common.Constants;
import me.mrnavastar.singularity.loader.impl.Broker;
import me.mrnavastar.singularity.loader.impl.sync.SynchronizedMinecraft;
import me.mrnavastar.singularity.loader.impl.sync.SynchronizedWhiteList;
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

import java.nio.file.Path;
import java.util.logging.Logger;

public class Paper extends JavaPlugin {

    private final Logger logger = getLogger();

    public static class EventListener extends SynchronizedMinecraft implements Listener {

        public EventListener(Path importPath) {
            init(MinecraftServer.getServer(), importPath);
        }

        @EventHandler
        public void onSave(WorldSaveEvent event) {
            syncPlayerData();
        }

        @EventHandler
        public void onToggle(WhitelistToggleEvent event) {
            SynchronizedWhiteList.setEnabled(event.isEnabled());
        }

        @EventHandler
        public void onPlayerJoin(PlayerJoinEvent event) {
            ServerPlayer player = ((CraftPlayer) event.getPlayer()).getHandle();
            onJoin(player);
            //player.valid = !player.valid;
        }

        @EventHandler
        public void onPlayerQuit(PlayerQuitEvent event) {
            onLeave(((CraftPlayer) event.getPlayer()).getHandle());
        }
    }

    @Override
    public void onEnable() {
        logger.info(Constants.SINGULARITY_BOOT_MESSAGE);
        ProtoWeaver.load(Broker.PROTOCOL);

        Broker.setSettingsCallback(settings -> {
            SpigotConfig.disablePlayerDataSaving = settings.syncPlayerData;
            SpigotConfig.disableStatSaving = settings.syncPlayerStats;
            SpigotConfig.disableAdvancementSaving = settings.syncPlayerAdvancements;
        });
        //SynchronizedMinecraft.setPlayerCallback(player -> player.valid = !player.valid);
        getServer().getPluginManager().registerEvents(new EventListener(getDataPath()), this);
    }
}