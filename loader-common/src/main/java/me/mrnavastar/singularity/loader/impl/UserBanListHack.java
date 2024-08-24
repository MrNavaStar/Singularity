package me.mrnavastar.singularity.loader.impl;

import me.mrnavastar.r.R;
import me.mrnavastar.singularity.loader.Singularity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.*;

public class UserBanListHack extends UserBanList {

    public UserBanListHack() {
        super(PlayerList.USERBANLIST_FILE);
        PlayerList.USERBANLIST_FILE.delete();
    }

    @Override
    public void save() {
        Singularity.syncServerData();
    }

    // Bye bye
    @Override
    public void load() {}

    public static void install(MinecraftServer server, UserBanListHack hack) {
        R.of(server.getPlayerList()).set("bans", hack);
    }

    public static void install(MinecraftServer server) {
        install(server, new UserBanListHack());
    }
}
