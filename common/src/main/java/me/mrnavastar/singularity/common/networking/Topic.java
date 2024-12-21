package me.mrnavastar.singularity.common.networking;

public record Topic(String topic, Behaviour behaviour, boolean global) {

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