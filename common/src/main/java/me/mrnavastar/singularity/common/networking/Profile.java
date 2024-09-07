package me.mrnavastar.singularity.common.networking;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@Getter
@RequiredArgsConstructor
public class Profile {

    static {
        DataBundle.register(Profile.class);
    }

    private final UUID uuid;
    private final String name;
    private Property property;

    public enum Property {
        NAME_LOOKUP,
        UUID_LOOKUP,
        BAD_LOOKUP,
    }

    public Profile setProperty(Property property) {
        this.property = property;
        return this;
    }

    @Override
    public String toString() {
        return "[" + name + ":" + uuid + "]: " + property;
    }
}