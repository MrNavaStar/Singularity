package me.mrnavastar.singularity.loader.impl.serialization;

import lombok.RequiredArgsConstructor;
import me.mrnavastar.r.R;
import me.mrnavastar.singularity.common.networking.DataBundle;
import net.minecraft.server.players.StoredUserEntry;

@RequiredArgsConstructor
public class StoredUserEntrySerializer implements DataBundle.Serializer<StoredUserEntry> {


    @Override
    public byte[] serialize(StoredUserEntry object) {


        R.of(object);

        return new byte[0];
    }

    @Override
    public StoredUserEntry deserialize(byte[] bytes) {
        return null;
    }
}
