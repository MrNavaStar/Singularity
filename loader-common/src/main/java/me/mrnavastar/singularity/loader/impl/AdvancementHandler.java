package me.mrnavastar.singularity.loader.impl;

import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import me.mrnavastar.singularity.loader.util.R;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.Set;

public class AdvancementHandler {

    /*public static String save(ServerPlayer player) {
        PlayerAdvancements pa = player.getAdvancements();
        Codec codec = R.getFieldValue(pa, "codec", Codec.class);
        Object data = R.invokeMethod(pa, "asData", Object.class);
        return codec.encodeStart(JsonOps.INSTANCE, data).getOrThrow().toString();
    }

    public static void load(ServerPlayer player, String json) {
        PlayerAdvancements pa = player.getAdvancements();
        ServerAdvancementManager am = player.getServer().getAdvancements();
        Codec codec = R.getFieldValue(pa, "codec", Codec.class);

        // Mimic the official reload function
        // We don't need to re update the advancement tree as there won't be any new advancements added
        pa.stopListening();
        R.getFieldValue(pa, "progress", Map.class).clear();
        R.getFieldValue(pa, "visible", Set.class).clear();
        R.getFieldValue(pa, "rootsToUpdate", Set.class).clear();
        R.getFieldValue(pa, "progressChanged", Set.class).clear();
        *//*ReflectionUtil.setFieldValue(pa, "isFirstPacket", true);
        ReflectionUtil.setFieldValue(pa, "lastSelectedTab", null);
        ReflectionUtil.setFieldValue(pa, "tree", am.tree());*//*

        System.out.println(json);

        // Mimic the official load function
        Object data = codec.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
        R.invokeMethod(pa, "applyFrom", void.class, am, data);
        R.invokeMethod(pa, "checkForAutomaticTriggers", void.class, am);
        R.invokeMethod(pa, "registerListeners", void.class, am);
    }*/
}