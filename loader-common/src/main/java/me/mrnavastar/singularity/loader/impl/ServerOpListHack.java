package me.mrnavastar.singularity.loader.impl;

import me.mrnavastar.r.R;
import me.mrnavastar.singularity.loader.Singularity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.*;

public class ServerOpListHack extends ServerOpList {

    public ServerOpListHack() {
        super(PlayerList.OPLIST_FILE);
        PlayerList.OPLIST_FILE.delete();
    }

    @Override
    public void save() {
        Singularity.syncServerData();
    }

    // Bye bye
    @Override
    public void load() {}

    public static void install(MinecraftServer server, ServerOpListHack hack) {
        R.of(server.getPlayerList()).set("ops", hack);
    }

    public static void install(MinecraftServer server) {
        install(server, new ServerOpListHack());
    }
}
