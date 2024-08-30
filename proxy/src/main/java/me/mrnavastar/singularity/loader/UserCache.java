package me.mrnavastar.singularity.loader;

import com.velocitypowered.api.proxy.Player;
import me.mrnavastar.singularity.common.Constants;
import me.mrnavastar.singularity.common.networking.Profile;
import me.mrnavastar.sqlib.SQLib;
import me.mrnavastar.sqlib.api.DataStore;
import me.mrnavastar.sqlib.api.types.JavaTypes;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public class UserCache {

    private static final DataStore cache = SQLib.getDatabase().dataStore(Constants.SINGULARITY_ID, "user_cache");

    public static void addUser(Player player) {
        cache.getOrCreateContainer("uuid", player.getUniqueId(), container -> container.put(JavaTypes.UUID, "uuid", player.getUniqueId()))
                .transaction()
                .put(JavaTypes.STRING, "name", player.getUsername().toLowerCase(Locale.ROOT))
                .put(JavaTypes.STRING, "brand", player.getClientBrand())
                .commit();
    }

    private static Optional<Profile> getUser(String field, Object value) {
        return cache.getContainer(field, value)
                .flatMap(container -> container.get(JavaTypes.UUID, "uuid")
                    .flatMap(uuid -> container.get(JavaTypes.STRING, "name")
                        .map(name -> new Profile(uuid, name))));
    }

    public static Optional<Profile> getUser(String name) {
        return getUser("name", name);
    }

    public static Optional<Profile> getUser(UUID uuid) {
        return getUser("uuid", uuid);
    }
}
