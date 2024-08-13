package me.mrnavastar.singularity.loader.impl;

import com.esotericsoftware.kryo.kryo5.io.Input;
import com.google.gson.JsonObject;
import me.mrnavastar.singularity.loader.Singularity;
import me.mrnavastar.singularity.loader.util.ReflectionUtil;
import me.mrnavastar.singularity.loader.util.Serializers;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.*;

public class ServerOpListHack extends ServerOpList {

    private boolean ready = false;

    public ServerOpListHack() {
        super(PlayerList.OPLIST_FILE);
        PlayerList.OPLIST_FILE.delete();
        ready = true;
    }

    public ServerOpListHack(Input input) {
        super(PlayerList.OPLIST_FILE);
        while (!input.end()) {
            add(new ServerOpListEntry(Serializers.GSON.fromJson(input.readString(), JsonObject.class)));
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

    public static void install(MinecraftServer server, ServerOpListHack hack) {
        ReflectionUtil.setParentFieldValue(server.getPlayerList(), "ops", ServerOpList.class, hack);
    }

    public static void install(MinecraftServer server) {
        install(server, new ServerOpListHack());
    }
}
