package me.mrnavastar.singularity.loader.impl;

import me.mrnavastar.r.R;
import me.mrnavastar.singularity.loader.Singularity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.*;

public class IpBanListHack extends IpBanList {

    public IpBanListHack() {
        super(PlayerList.IPBANLIST_FILE);
        PlayerList.IPBANLIST_FILE.delete();
    }

    @Override
    public void save() {
        //Singularity.syncServerData();
    }

    // Bye bye
    @Override
    public void load() {}

    public static void install(MinecraftServer server, IpBanListHack hack) {
        R.of(server.getPlayerList()).set("ipBans", hack);
    }

    public static void install(MinecraftServer server) {
        install(server, new IpBanListHack());
    }
}
