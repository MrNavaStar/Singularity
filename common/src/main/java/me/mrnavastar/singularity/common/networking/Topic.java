package me.mrnavastar.singularity.common.networking;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;

@Getter()
@Accessors(fluent = true)
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class Topic {

    private final String topic;
    @EqualsAndHashCode.Exclude
    private final Behaviour behaviour;
    private final boolean global;

    public enum Behaviour {
        PLAYER,
        NONE,
    }

    public String databaseKey() {
        return topic.replace(":", "_");
    }

    public void validate() {
        String[] names = topic.split(":");
        if (names.length < 2) throw new IllegalArgumentException("Invalid Topic! Topic names should be namespaced: 'namespace:field`");
    }
}