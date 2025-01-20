package me.mrnavastar.singularity.loader.impl.serialization;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.authlib.GameProfile;
import lombok.RequiredArgsConstructor;
import me.mrnavastar.singularity.common.serialization.SingularitySerializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

@RequiredArgsConstructor
public class GameProfileSerializer implements SingularitySerializer.Serializer<GameProfile> {

    private static final Gson GSON = new GsonBuilder().registerTypeAdapter(GameProfile.class, new GameProfile.Serializer()).create();
    private final SingularitySerializer.Serializer<JsonElement> jsonSerializer;

    @Override
    public void serialize(GameProfile object, ByteArrayOutputStream out) {
        jsonSerializer.serialize(GSON.toJsonTree(object, GameProfile.class), out);
    }

    @Override
    public GameProfile deserialize(ByteArrayInputStream in) {
        return GSON.fromJson(jsonSerializer.deserialize(in), GameProfile.class);
    }
}
