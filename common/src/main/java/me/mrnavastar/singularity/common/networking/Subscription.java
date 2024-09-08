package me.mrnavastar.singularity.common.networking;

public record Subscription(TopicType type, String topic) {

    public enum TopicType {
        PLAYER,
        STATIC
    }

    @Override
    public String toString() {
        return type + ":" + topic;
    }
}