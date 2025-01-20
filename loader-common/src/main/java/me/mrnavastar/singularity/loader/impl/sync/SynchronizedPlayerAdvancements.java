package me.mrnavastar.singularity.loader.impl.sync;

import com.mojang.datafixers.DataFixer;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;

import java.nio.file.Path;

public class SynchronizedPlayerAdvancements extends PlayerAdvancements {

    public SynchronizedPlayerAdvancements(DataFixer dataFixer, PlayerList playerList, ServerAdvancementManager serverAdvancementManager, Path path, ServerPlayer serverPlayer) {
        super(dataFixer, playerList, serverAdvancementManager, path, serverPlayer);
    }

    public void save() {

    }

    private void load(ServerAdvancementManager advancementLoader) {

    }
}
