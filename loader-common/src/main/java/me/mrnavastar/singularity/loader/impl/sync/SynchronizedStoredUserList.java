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
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BooleanSupplier;

// TODO: improve performance by keeping an in memory copy of the last value for any players currently on the server
public class SynchronizedStoredUserList<T> extends StoredUserList<T, StoredUserEntry<T>> {

    private final String topic;
    private final Class<? extends StoredUserEntry<T>> entryType;
    private final BooleanSupplier enabled;

    public SynchronizedStoredUserList(String topic, Class<? extends StoredUserEntry<T>> entryType, BooleanSupplier enabled, File file) {
        super(file);
        this.topic = topic;
        this.entryType = entryType;
        this.enabled = enabled;
    }

    @Override
    protected @NotNull StoredUserEntry<T> createEntry(JsonObject json) {
        return StoredUserEntrySerializer.deserialize(json, entryType);
    }

    private Optional<String> getKey(Object object) {
        if (object instanceof String key) return Optional.of(key);
        if (object instanceof GameProfile key) return Optional.of(key.getId().toString());
        return Optional.empty();
    }

    public Optional<String> getKey(StoredUserEntry<T> entry) {
       return getKey(R.of(entry).get(Mappings.of("user", "field_14368"), Object.class));
    }

    @Override
    public void add(StoredUserEntry<T> entry) {
        if (!enabled.getAsBoolean()) {
            super.add(entry);
            return;
        }
        getKey(entry).ifPresent(key ->
                Broker.putTopic(topic, key, new DataBundle()
                .meta(new DataBundle.Meta().propagation(DataBundle.Propagation.ALL))
                .put("entry", entry)));
    }

    @Override
    public void remove(T object) {
        if (!enabled.getAsBoolean()) {
            super.remove(object);
            return;
        }
        getKey(object).ifPresent(key ->  Broker.removeTopic(topic, key));
    }

    @SneakyThrows
    @Override
    public @Nullable StoredUserEntry<T> get(T object) {
        if (!enabled.getAsBoolean()) return super.get(object);
        return getKey(object).flatMap(key -> {
                    try {
                        return Broker.getTopic(topic, key).get(5, TimeUnit.SECONDS);
                    } catch (InterruptedException | ExecutionException | TimeoutException e) {
                        return Optional.empty();
                    }
                })
                .flatMap(bundle -> bundle.get("entry", entryType)).orElse(null);
    }

    @Override
    protected boolean contains(T object) {
        if (!enabled.getAsBoolean()) super.contains(object);
        return get(object) != null;
    }

    @Override
    public boolean isEmpty() {
        return super.isEmpty();
    }

    @Override
    public void save() throws IOException {
        if (!enabled.getAsBoolean()) super.save();
    }

    @Override
    public void load() throws IOException {
        if (!enabled.getAsBoolean()) super.load();
    }

    public static void install() {
        DataBundle.register(StoredUserEntrySerializer.class);
    }
}
