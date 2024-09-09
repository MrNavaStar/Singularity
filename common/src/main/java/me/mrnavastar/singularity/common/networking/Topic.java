package me.mrnavastar.singularity.common.networking;

public record Topic(TopicType type, String topic) {

    public enum TopicType {
        PLAYER,
        STATIC
    }

    public String databaseKey() {
        return type + "_" + topic.replace(":", "_");
    }
}