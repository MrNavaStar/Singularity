package me.mrnavastar.singularity.loader.impl;

import com.esotericsoftware.kryo.kryo5.io.Input;
import com.google.gson.JsonObject;
import me.mrnavastar.singularity.loader.Singularity;
import me.mrnavastar.singularity.loader.util.ReflectionUtil;
import me.mrnavastar.singularity.loader.util.Serializers;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.*;

public class UserWhiteListHack extends UserWhiteList {

    private boolean ready = false;

    public UserWhiteListHack() {
        super(PlayerList.WHITELIST_FILE);
        PlayerList.WHITELIST_FILE.delete();
        ready = true;
    }

    public UserWhiteListHack(Input input) {
        super(PlayerList.WHITELIST_FILE);
        while (!input.end()) {
            add(new UserWhiteListEntry(Serializers.GSON.fromJson(input.readString(), JsonObject.class)));
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

    public static void install(MinecraftServer server) {
        ReflectionUtil.setParentFieldValue(server.getPlayerList(), "whitelist", UserWhiteList.class, new UserWhiteListHack());
    }
}
