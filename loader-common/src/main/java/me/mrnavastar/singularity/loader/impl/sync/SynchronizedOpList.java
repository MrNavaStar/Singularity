package me.mrnavastar.singularity.loader.impl.sync;

import com.mojang.authlib.GameProfile;
import lombok.SneakyThrows;
import me.mrnavastar.r.R;
import me.mrnavastar.singularity.common.Constants;
import me.mrnavastar.singularity.common.networking.DataBundle;
import me.mrnavastar.singularity.loader.impl.Broker;
import me.mrnavastar.singularity.loader.util.Mappings;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.*;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public class SynchronizedOpList extends ServerOpList {

    public SynchronizedOpList() {
        super(PlayerList.OPLIST_FILE);
    }

    @Override
    public void add(ServerOpListEntry entry) {
        if (!Broker.getSettings().syncOps) {
            super.add(entry);
            return;
        }
        GameProfile profile = R.of(entry).get("user", GameProfile.class);
        Broker.putTopic(Constants.OPERATOR, profile.getId().toString(), new DataBundle()
                .meta(new DataBundle.Meta().propagation(DataBundle.Propagation.ALL))
                .put("entry", entry));
    }

    @Override
    public void remove(GameProfile profile) {
        if (!Broker.getSettings().syncOps) {
            super.remove(profile);
            return;
        }
        Broker.removeTopic(Constants.OPERATOR, profile.getId().toString());
    }

    @Override
    @SneakyThrows
    public @Nullable ServerOpListEntry get(GameProfile profile) {
        if (!Broker.getSettings().syncOps) return super.get(profile);
        return Broker.getTopic(Constants.OPERATOR, profile.getId().toString()).get()
                .flatMap(bundle -> bundle.get("entry", ServerOpListEntry.class))
                .orElse(null);
    }

    @Override
    protected boolean contains(GameProfile profile) {
        if (!Broker.getSettings().syncOps) return super.contains(profile);
        return get(profile) != null;
    }

    @Override
    public void save() throws IOException {
        if (!Broker.getSettings().syncOps) super.save();
    }

    @Override
    public void load() throws IOException {
        if (!Broker.getSettings().syncOps) super.load();
    }

    public static void install(MinecraftServer server) {
        DataBundle.register(ServerOpListEntry.class);
        R.of(server.getPlayerList()).set(Mappings.of("ops", "field_14353"), new SynchronizedOpList());
        Broker.subTopic(Constants.OPERATOR, bundle -> {

            /*bundle.get("entry", ServerOpListEntry.class).ifPresent(entry -> {
                entry.
            });

            server.getPlayerList().getOps().getUserList()

            commandSourceStack.sendSuccess(() -> Component.translatable("commands.op.success", new Object[]{((GameProfile)collection.iterator().next()).getName()}), true);*/
        });
    }
}