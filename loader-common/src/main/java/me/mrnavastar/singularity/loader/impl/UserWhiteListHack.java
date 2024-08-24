package me.mrnavastar.singularity.loader.impl;

import me.mrnavastar.r.R;
import me.mrnavastar.singularity.loader.Singularity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.*;

public class UserWhiteListHack extends UserWhiteList {

    public UserWhiteListHack() {
        super(PlayerList.WHITELIST_FILE);
        PlayerList.WHITELIST_FILE.delete();
    }

    @Override
    public void save() {
        Singularity.syncServerData();
    }

    // Bye bye
    @Override
    public void load() {}

    public static void install(MinecraftServer server, UserWhiteListHack hack) {
        R.of(server.getPlayerList()).set("whitelist", hack);
    }

    public static void install(MinecraftServer server) {
        install(server, new UserWhiteListHack());
    }
}
