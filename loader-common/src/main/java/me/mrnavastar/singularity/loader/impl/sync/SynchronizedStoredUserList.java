package me.mrnavastar.singularity.loader.impl.sync;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import lombok.SneakyThrows;
import me.mrnavastar.r.R;
import me.mrnavastar.singularity.common.networking.DataBundle;
import me.mrnavastar.singularity.loader.impl.Broker;
import me.mrnavastar.singularity.loader.impl.serialization.StoredUserEntrySerializer;
import me.mrnavastar.singularity.loader.util.Mappings;
import net.minecraft.server.players.StoredUserEntry;
import net.minecraft.server.players.StoredUserList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;

public class SynchronizedStoredUserList<T> extends StoredUserList<T, StoredUserEntry<T>> {

    private final String topic;
    private final Class<? extends StoredUserEntry<T>> entryType;
    private final boolean enabled;

    public SynchronizedStoredUserList(String topic, Class<? extends StoredUserEntry<T>> entryType, boolean enabled, File file) {
        super(file);
        this.topic = topic;
        this.entryType = entryType;
        this.enabled = enabled;
    }

    @Override
    protected @NotNull StoredUserEntry<T> createEntry(JsonObject json) {
        return StoredUserEntrySerializer.deserialize(json, entryType);
    }

    @Override
    public void add(StoredUserEntry<T> entry) {
        if (!enabled) {
            super.add(entry);
            return;
        }
        GameProfile profile = R.of(entry).call(Mappings.of("getUser", "method_14626"), GameProfile.class);
        Broker.putTopic(topic, profile.getId().toString(), new DataBundle()
                .meta(new DataBundle.Meta().propagation(DataBundle.Propagation.ALL))
                .put("entry", entry));
    }

    @Override
    public void remove(T object) {
        if (!enabled) {
            super.remove(object);
            return;
        }
        if (object instanceof GameProfile profile) Broker.removeTopic(topic, profile.getId().toString());
    }

    @SneakyThrows
    @Override
    public @Nullable StoredUserEntry<T> get(T object) {
        if (!enabled) return super.get(object);
        if (object instanceof GameProfile profile) return Broker.getTopic(topic, profile.getId().toString()).get()
                .flatMap(bundle -> bundle.get("entry", entryType))
                .orElse(null);
        return super.get(object);
    }

    @Override
    protected boolean contains(T object) {
        if (!enabled) super.contains(object);
        return get(object) != null;
    }

    @Override
    public boolean isEmpty() {
        return super.isEmpty();
    }

    @Override
    public void save() throws IOException {
        if (!enabled) super.save();
    }

    @Override
    public void load() throws IOException {
        if (!enabled) super.load();
    }

    public static void install() {
        DataBundle.register(StoredUserEntrySerializer.class);
    }
}
