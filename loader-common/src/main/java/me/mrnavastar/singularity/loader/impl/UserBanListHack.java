package me.mrnavastar.singularity.loader.impl;

import com.esotericsoftware.kryo.kryo5.io.Input;
import com.google.gson.JsonObject;
import me.mrnavastar.singularity.loader.Singularity;
import me.mrnavastar.singularity.loader.util.ReflectionUtil;
import me.mrnavastar.singularity.loader.util.Serializers;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.*;

public class UserBanListHack extends UserBanList {

    private boolean ready = false;

    public UserBanListHack() {
        super(PlayerList.USERBANLIST_FILE);
        PlayerList.USERBANLIST_FILE.delete();
        ready = true;
    }

    public UserBanListHack(Input input) {
        super(PlayerList.USERBANLIST_FILE);
        while (!input.end()) {
            add(new UserBanListEntry(Serializers.GSON.fromJson(input.readString(), JsonObject.class)));
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

    public static void install(MinecraftServer server, UserBanListHack hack) {
        ReflectionUtil.setParentFieldValue(server.getPlayerList(), "bans", UserBanList.class, hack);
    }

    public static void install(MinecraftServer server) {
        install(server, new UserBanListHack());
    }
}
