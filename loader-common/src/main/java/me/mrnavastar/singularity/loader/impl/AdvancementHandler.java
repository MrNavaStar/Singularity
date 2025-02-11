package me.mrnavastar.singularity.loader.impl;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import me.mrnavastar.r.R;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.Set;

public class AdvancementHandler {

    public static JsonElement save(ServerPlayer player) {
        R advancements = R.of(player.getAdvancements());
        Codec codec = advancements.call("codec", Codec.class);
        Object data = advancements.call("asData", Object.class);
        return (JsonElement) codec.encodeStart(JsonOps.INSTANCE, data).getOrThrow();
    }

    public static void load(ServerPlayer player, JsonElement json) {
        R advancements = R.of(player.getAdvancements());
        ServerAdvancementManager manager = player.getServer().getAdvancements();
        Codec codec = advancements.get("codec", Codec.class);

        // Mimic the official reload function
        // We don't need to re update the advancement tree as there won't be any new advancements added
        player.getAdvancements().stopListening();
        advancements.get("progress", Map.class).clear();
        advancements.get("visible", Set.class).clear();
        advancements.get("rootsToUpdate", Set.class).clear();
        advancements.get("progressChanged", Set.class).clear();
        advancements.set("isFirstPacket", true);
        advancements.set("lastSelectedTab", null);
        advancements.set("tree", manager.tree());

        System.out.println(json);

        // Mimic the official load function
        Object data = codec.parse(JsonOps.INSTANCE, json).getOrThrow();
        advancements.call("applyFrom", void.class, manager, data);
        advancements.call("checkForAutomaticTriggers", void.class, manager);
        advancements.call("registerListeners", void.class, manager);
    }
}