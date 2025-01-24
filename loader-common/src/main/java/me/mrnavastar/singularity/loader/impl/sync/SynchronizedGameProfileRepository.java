package me.mrnavastar.singularity.loader.impl.sync;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.ProfileLookupCallback;
import lombok.RequiredArgsConstructor;
import me.mrnavastar.r.R;
import me.mrnavastar.singularity.common.Constants;
import me.mrnavastar.singularity.common.networking.DataBundle;
import me.mrnavastar.singularity.loader.impl.Broker;
import me.mrnavastar.singularity.loader.impl.serialization.GameProfileSerializer;
import me.mrnavastar.singularity.loader.util.Mappings;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.Services;

import java.util.ArrayList;
import java.util.Arrays;

@RequiredArgsConstructor
public class SynchronizedGameProfileRepository implements GameProfileRepository {

    private final GameProfileRepository parent;

    @Override
    public void findProfilesByNames(String[] names, ProfileLookupCallback callback) {
        ArrayList<String> notFound = new ArrayList<>();

        Arrays.stream(names).forEach(name ->
                Broker.getGlobalTopic(Constants.USER_CACHE, name).whenComplete((bundle, t) ->
                        bundle.flatMap(data -> data.get("profile", GameProfile.class))
                .ifPresentOrElse(callback::onProfileLookupSucceeded, () -> notFound.add(name))));

        parent.findProfilesByNames(notFound.toArray(new String[0]), new ProfileLookupCallback() {
            @Override
            public void onProfileLookupSucceeded(GameProfile profile) {
                callback.onProfileLookupSucceeded(profile);
                Broker.putGlobalTopic(Constants.USER_CACHE, profile.getName(), new DataBundle().put("profile", profile));
            }

            @Override
            public void onProfileLookupFailed(String profileName, Exception exception) {
                callback.onProfileLookupFailed(profileName, exception);
            }
        });
    }

    public static void install(MinecraftServer server) {
        String mapping = Mappings.of("services", "field_39440");
        Services services = R.of(server).get(mapping, Services.class);
        R.of(server).set(mapping, new Services(services.sessionService(), services.servicesKeySet(),
                new SynchronizedGameProfileRepository(services.profileRepository()), services.profileCache()));
    }
}