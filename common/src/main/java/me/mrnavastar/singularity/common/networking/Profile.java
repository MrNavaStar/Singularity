package me.mrnavastar.singularity.common.networking;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

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
    @Setter
    private boolean value;

    public enum Property {
        NAME_LOOKUP,
        UUID_LOOKUP,
        BAD_LOOKUP,

        WHITELISTED,
        BANNED,
        OP,
    }

    public Profile setProperty(Property property) {
        this.property = property;
        return this;
    }

    public Profile setPropertyValue(Boolean value) {
        this.value = value;
        return this;
    }

    @Override
    public String toString() {
        return name + ":" + uuid;
    }
}