package me.mrnavastar.singularity.loader.impl;

import com.esotericsoftware.kryo.kryo5.io.Input;
import com.google.gson.JsonObject;
import me.mrnavastar.singularity.loader.Singularity;
import me.mrnavastar.singularity.loader.util.ReflectionUtil;
import me.mrnavastar.singularity.loader.util.Serializers;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.*;

public class IpBanListHack extends IpBanList {

    private boolean ready = false;

    public IpBanListHack() {
        super(PlayerList.IPBANLIST_FILE);
        PlayerList.IPBANLIST_FILE.delete();
        ready = true;
    }

    public IpBanListHack(Input input) {
        super(PlayerList.IPBANLIST_FILE);
        while (!input.end()) {
            add(new IpBanListEntry(Serializers.GSON.fromJson(input.readString(), JsonObject.class)));
        }
        ready = true;
    }

    @Override
    public void save() {
        if (ready) Singularity.syncServerData();
    }

    // Bye bye
    @Override
    public void load() {}

    public static void install(MinecraftServer server, IpBanListHack hack) {
        ReflectionUtil.setParentFieldValue(server.getPlayerList(), "ipBans", IpBanList.class, hack);
    }

    public static void install(MinecraftServer server) {
        install(server, new IpBanListHack());
    }
}
